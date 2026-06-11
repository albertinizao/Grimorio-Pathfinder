package com.grimoriopathfinder.dataset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpellDatasetImportServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void importsGeneratedDatasetAppliesOverridesAndReportsOrphans() throws Exception {
        var generatedPath = tempDir.resolve("spells-es.generated.json");
        var overridesPath = tempDir.resolve("spells-es.overrides.json");

        Files.writeString(generatedPath, """
                {
                  "version": 1,
                  "generatedAt": "2026-06-11T00:00:00Z",
                  "sourceName": "fixture",
                  "spells": [
                    {
                      "id": "neutralize-poison",
                      "slug": "neutralize-poison",
                      "sourceId": "neutralize-poison",
                      "sourceHash": "sha256:111111",
                      "nameEs": "Neutralizar veneno",
                      "nameEn": "Neutralize Poison",
                      "school": "conjuración",
                      "subschool": "curación",
                      "descriptors": [],
                      "castingTime": "1 acción estándar",
                      "components": "V, S, FD",
                      "range": "toque",
                      "target": "criatura u objeto tocado",
                      "effect": null,
                      "area": null,
                      "duration": "instantáneo",
                      "savingThrow": "Voluntad niega (inofensivo, objeto)",
                      "spellResistance": "sí (inofensivo, objeto)",
                      "descriptionEs": "Neutralizas cualquier tipo de veneno presente en la criatura u objeto tocado.",
                      "descriptionEn": "You detoxify any sort of venom in the creature or object touched.",
                      "sourceBook": "Core Rulebook",
                      "sourcePage": null,
                      "sourceName": null,
                      "translationStatus": "AI_TRANSLATED",
                      "lists": [
                        { "spellId": "neutralize-poison", "listType": "CLASS", "listName": "Clérigo", "level": 4 }
                      ]
                    }
                  ]
                }
                """);

        Files.writeString(overridesPath, """
                {
                  "version": 1,
                  "updatedAt": "2026-06-11T00:00:00Z",
                  "spells": {
                    "neutralize-poison": {
                      "fields": {
                        "descriptionEs": "Neutralizas cualquier tipo de veneno.",
                        "translationStatus": "MANUALLY_EDITED"
                      },
                      "personalNotes": "Muy útil contra venenos.",
                      "updatedAt": "2026-06-11T00:00:00Z"
                    },
                    "orphan-spell": {
                      "fields": {
                        "translationStatus": "LOCKED"
                      },
                      "personalNotes": "Huérfano",
                      "updatedAt": "2026-06-11T00:00:00Z"
                    }
                  }
                }
                """);

        var importer = new SpellDatasetImportService();
        var result = importer.importDataset(generatedPath, overridesPath);

        assertThat(result.effectiveSpells()).hasSize(1);
        var effective = result.effectiveSpells().getFirst();
        assertThat(effective.id()).isEqualTo("neutralize-poison");
        assertThat(effective.descriptionEs()).isEqualTo("Neutralizas cualquier tipo de veneno.");
        assertThat(effective.translationStatus()).isEqualTo("MANUALLY_EDITED");
        assertThat(effective.personalNotes()).isEqualTo("Muy útil contra venenos.");
        assertThat(effective.sourceHash()).isEqualTo("sha256:111111");
        assertThat(result.warnings()).contains("Orphan override: orphan-spell");
    }

    @Test
    void failsWhenGeneratedDatasetIsMissing() {
        var importer = new SpellDatasetImportService();

        assertThatThrownBy(() -> importer.importDataset(
                tempDir.resolve("missing.spells-es.generated.json"),
                tempDir.resolve("spells-es.overrides.json")))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("Missing generated dataset");
    }
}
