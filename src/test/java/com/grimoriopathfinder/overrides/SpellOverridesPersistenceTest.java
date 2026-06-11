package com.grimoriopathfinder.overrides;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.grimoriopathfinder.spells.Spell;
import com.grimoriopathfinder.spells.SpellListEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpellOverridesPersistenceTest {

    @TempDir
    Path tempDir;

    @Test
    void writesAndReadsOverridesWithoutLosingPersonalNotesOrTranslationStatus() throws Exception {
        var path = tempDir.resolve("spells-es.overrides.json");
        var repository = new SpellOverridesJsonRepository();

        var fields = new LinkedHashMap<String, Object>();
        fields.put("descriptionEs", "Neutralizas cualquier tipo de veneno.");
        fields.put("translationStatus", "LOCKED");

        var overrides = new SpellOverridesFile(
                1,
                "2026-06-11T00:00:00Z",
                Map.of(
                        "neutralize-poison",
                        new SpellOverrideEntry(
                                fields,
                                "Muy útil contra venenos.",
                                "2026-06-11T00:00:00Z",
                                "Corrección de mesa"
                        )
                )
        );

        repository.write(path, overrides);

        assertThat(Files.exists(path)).isTrue();

        var loaded = repository.read(path);

        assertThat(loaded.version()).isEqualTo(1);
        assertThat(loaded.spells()).containsKey("neutralize-poison");
        assertThat(loaded.spells().get("neutralize-poison").personalNotes()).isEqualTo("Muy útil contra venenos.");
        assertThat(loaded.spells().get("neutralize-poison").fields()).containsEntry("translationStatus", "LOCKED");
        assertThat(loaded.spells().get("neutralize-poison").fields()).containsEntry("descriptionEs", "Neutralizas cualquier tipo de veneno.");
    }

    @Test
    void readingAMissingOverrideFileReturnsAnEmptyOverrideEnvelope() throws Exception {
        var repository = new SpellOverridesJsonRepository();

        var loaded = repository.read(tempDir.resolve("missing-spells-es.overrides.json"));

        assertThat(loaded.version()).isEqualTo(1);
        assertThat(loaded.updatedAt()).isNull();
        assertThat(loaded.spells()).isEmpty();
    }

    @Test
    void composesEffectiveSpellDataWithOverridesAndReportsOrphans() {
        var generated = List.of(
                new Spell(
                        "neutralize-poison",
                        "neutralize-poison",
                        "neutralize-poison",
                        "sha256:111111",
                        "Neutralizar veneno",
                        "Neutralize Poison",
                        "conjuración",
                        "curación",
                        List.of(),
                        "1 acción estándar",
                        "V, S, FD",
                        "toque",
                        "criatura u objeto tocado",
                        null,
                        null,
                        "instantáneo",
                        "Voluntad niega (inofensivo, objeto)",
                        "sí (inofensivo, objeto)",
                        "Neutralizas cualquier tipo de veneno presente en la criatura u objeto tocado.",
                        "You detoxify any sort of venom in the creature or object touched.",
                        "Core Rulebook",
                        null,
                        null,
                        "AI_TRANSLATED",
                        List.of(new SpellListEntry("neutralize-poison", "CLASS", "Clérigo", 4)),
                        "",
                        Instant.parse("2026-06-11T00:00:00Z"),
                        Instant.parse("2026-06-11T00:00:00Z")
                )
        );

        var overrideFields = new LinkedHashMap<String, Object>();
        overrideFields.put("descriptionEs", "Neutralizas cualquier tipo de veneno.");
        overrideFields.put("translationStatus", "MANUALLY_EDITED");

        var overrides = new SpellOverridesFile(
                1,
                "2026-06-11T00:00:00Z",
                Map.of(
                        "neutralize-poison",
                        new SpellOverrideEntry(
                                overrideFields,
                                "Muy útil contra venenos.",
                                "2026-06-11T00:00:00Z",
                                null
                        ),
                        "missing-spell",
                        new SpellOverrideEntry(
                                Map.of("translationStatus", "LOCKED"),
                                "Huérfano",
                                "2026-06-11T00:00:00Z",
                                null
                        )
                )
        );

        var composer = new SpellOverridesComposer();
        var result = composer.apply(generated, overrides);

        assertThat(result.effectiveSpells()).hasSize(1);
        assertThat(result.warnings()).contains("Orphan override: missing-spell");

        var effective = result.effectiveSpells().getFirst();
        assertThat(effective.descriptionEs()).isEqualTo("Neutralizas cualquier tipo de veneno.");
        assertThat(effective.translationStatus()).isEqualTo("MANUALLY_EDITED");
        assertThat(effective.personalNotes()).isEqualTo("Muy útil contra venenos.");
        assertThat(effective.nameEn()).isEqualTo("Neutralize Poison");
    }
}
