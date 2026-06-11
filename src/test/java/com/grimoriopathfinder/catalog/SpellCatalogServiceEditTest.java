package com.grimoriopathfinder.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.grimoriopathfinder.dataset.SpellDatasetImportService;
import com.grimoriopathfinder.overrides.SpellOverridesJsonRepository;
import com.grimoriopathfinder.sqlite.SpellCatalogSqliteRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpellCatalogServiceEditTest {

    @TempDir
    Path tempDir;

    private Path generatedPath;
    private Path overridesPath;
    private Path databasePath;
    private SpellDatasetImportService importService;
    private SpellOverridesJsonRepository overridesRepository;
    private SpellCatalogSqliteRepository repository;
    private SpellCatalogService service;

    @BeforeEach
    void setUp() throws Exception {
        generatedPath = tempDir.resolve("spells-es.generated.json");
        overridesPath = tempDir.resolve("spells-es.overrides.json");
        databasePath = tempDir.resolve("grimorio.sqlite");
        importService = new SpellDatasetImportService();
        overridesRepository = new SpellOverridesJsonRepository();
        repository = new SpellCatalogSqliteRepository(databasePath.toString());

        writeGeneratedDataset("""
                {
                  "version": 1,
                  "generatedAt": "2026-06-11T00:00:00Z",
                  "sourceName": "test-fixture",
                  "spells": [
                    {
                      "id": "delay-poison",
                      "slug": "delay-poison",
                      "sourceId": "delay-poison",
                      "sourceHash": "sha256:111111",
                      "nameEs": "Retrasar veneno",
                      "nameEn": "Delay Poison",
                      "school": "abjuración",
                      "subschool": null,
                      "descriptors": [],
                      "castingTime": "1 acción estándar",
                      "components": "V, S, FD",
                      "range": "toque",
                      "target": "criatura tocada",
                      "effect": null,
                      "area": null,
                      "duration": "1 h./nivel",
                      "savingThrow": "Voluntad niega (inofensivo)",
                      "spellResistance": "sí (inofensivo)",
                      "descriptionEs": "El objetivo queda temporalmente protegido contra el veneno.",
                      "descriptionEn": "The subject becomes temporarily immune to poison.",
                      "sourceBook": "Core Rulebook",
                      "sourcePage": 256,
                      "sourceName": "spells.csv",
                      "translationStatus": "AI_TRANSLATED",
                      "lists": [
                        { "spellId": "delay-poison", "listType": "CLASS", "listName": "Clérigo", "level": 3 }
                      ],
                      "personalNotes": "",
                      "createdAt": "2026-06-11T00:00:00Z",
                      "updatedAt": "2026-06-11T00:00:00Z"
                    },
                    {
                      "id": "neutralize-poison",
                      "slug": "neutralize-poison",
                      "sourceId": "neutralize-poison",
                      "sourceHash": "sha256:222222",
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
                      "sourcePage": 264,
                      "sourceName": "spells.csv",
                      "translationStatus": "AI_TRANSLATED",
                      "lists": [
                        { "spellId": "neutralize-poison", "listType": "CLASS", "listName": "Clérigo", "level": 4 }
                      ],
                      "personalNotes": "",
                      "createdAt": "2026-06-11T00:00:00Z",
                      "updatedAt": "2026-06-11T00:00:00Z"
                    }
                  ]
                }
                """);

        Files.writeString(overridesPath, """
                {
                  "version": 1,
                  "updatedAt": "2026-06-11T00:00:00Z",
                  "spells": {
                  }
                }
                """);

        service = new SpellCatalogService(
                repository,
                importService,
                overridesRepository,
                generatedPath.toString(),
                overridesPath.toString()
        );

        var importResult = importService.importDataset(generatedPath, overridesPath);
        repository.rebuild(importResult.effectiveSpells());
    }

    @Test
    void updatesSpanishFieldsPersistsOverrideAndRefreshesSearchIndex() throws Exception {
        var updated = service.updateSpellFields(
                "neutralize-poison",
                new com.grimoriopathfinder.web.dto.SpellApiDtos.UpdateSpellFieldsRequestDto(
                        Map.of(
                                "descriptionEs", "Escudo contra venenos.",
                                "savingThrow", "Voluntad niega (inofensivo, objeto)"
                        ),
                        Instant.parse("2026-06-11T00:00:00Z"),
                        "Corrección de mesa"
                )
        );

        assertThat(updated.descriptionEs()).isEqualTo("Escudo contra venenos.");
        assertThat(updated.nameEn()).isEqualTo("Neutralize Poison");
        assertThat(updated.translationStatus()).isEqualTo("MANUALLY_EDITED");

        var overrides = overridesRepository.read(overridesPath);
        assertThat(overrides.spells()).containsKey("neutralize-poison");
        assertThat(overrides.spells().get("neutralize-poison").fields())
                .containsEntry("descriptionEs", "Escudo contra venenos.")
                .containsEntry("savingThrow", "Voluntad niega (inofensivo, objeto)")
                .containsEntry("translationStatus", "MANUALLY_EDITED");

        var search = service.searchSpells("CLASS", "Clérigo", 4, "UP_TO", "escudo", 0, 50);
        assertThat(search.results()).extracting("spellId").contains("neutralize-poison");
    }

    @Test
    void updatesPersonalNotesAndMakesThemSearchableAfterProjectionRefresh() throws Exception {
        var updated = service.updatePersonalNotes(
                "delay-poison",
                new com.grimoriopathfinder.web.dto.SpellApiDtos.UpdatePersonalNotesRequestDto(
                        "Preparar si esperamos drow.",
                        Instant.parse("2026-06-11T00:00:00Z")
                )
        );

        assertThat(updated.personalNotes()).isEqualTo("Preparar si esperamos drow.");
        assertThat(updated.translationStatus()).isEqualTo("AI_TRANSLATED");

        var overrides = overridesRepository.read(overridesPath);
        assertThat(overrides.spells().get("delay-poison").personalNotes()).isEqualTo("Preparar si esperamos drow.");
        assertThat(overrides.spells().get("delay-poison").fields()).isNull();

        var search = service.searchSpells("CLASS", "Clérigo", 3, "UP_TO", "drow", 0, 50);
        assertThat(search.results()).hasSize(1);
        assertThat(search.results().getFirst().spellId()).isEqualTo("delay-poison");
        assertThat(search.results().getFirst().matchSource()).isEqualTo("personalNotes");
    }

    @Test
    void lockingMaterializesEditableFieldsAndSurvivesReimportWithoutLoss() throws Exception {
        var locked = service.updateTranslationStatus(
                "neutralize-poison",
                new com.grimoriopathfinder.web.dto.SpellApiDtos.UpdateTranslationStatusRequestDto(
                        "LOCKED",
                        Instant.parse("2026-06-11T00:00:00Z"),
                        "Traducción revisada y cerrada."
                )
        );

        assertThat(locked.translationStatus()).isEqualTo("LOCKED");

        var overrideEntry = overridesRepository.read(overridesPath).spells().get("neutralize-poison");
        assertThat(overrideEntry.fields()).containsEntry("translationStatus", "LOCKED");
        assertThat(overrideEntry.fields()).containsEntry("nameEs", "Neutralizar veneno");
        assertThat(overrideEntry.fields()).containsEntry("descriptionEs", "Neutralizas cualquier tipo de veneno presente en la criatura u objeto tocado.");

        writeGeneratedDataset("""
                {
                  "version": 1,
                  "generatedAt": "2026-06-12T00:00:00Z",
                  "sourceName": "test-fixture",
                  "spells": [
                    {
                      "id": "delay-poison",
                      "slug": "delay-poison",
                      "sourceId": "delay-poison",
                      "sourceHash": "sha256:111111",
                      "nameEs": "Retrasar veneno",
                      "nameEn": "Delay Poison",
                      "school": "abjuración",
                      "subschool": null,
                      "descriptors": [],
                      "castingTime": "1 acción estándar",
                      "components": "V, S, FD",
                      "range": "toque",
                      "target": "criatura tocada",
                      "effect": null,
                      "area": null,
                      "duration": "1 h./nivel",
                      "savingThrow": "Voluntad niega (inofensivo)",
                      "spellResistance": "sí (inofensivo)",
                      "descriptionEs": "El objetivo queda temporalmente protegido contra el veneno.",
                      "descriptionEn": "The subject becomes temporarily immune to poison.",
                      "sourceBook": "Core Rulebook",
                      "sourcePage": 256,
                      "sourceName": "spells.csv",
                      "translationStatus": "AI_TRANSLATED",
                      "lists": [
                        { "spellId": "delay-poison", "listType": "CLASS", "listName": "Clérigo", "level": 3 }
                      ],
                      "personalNotes": "",
                      "createdAt": "2026-06-12T00:00:00Z",
                      "updatedAt": "2026-06-12T00:00:00Z"
                    },
                    {
                      "id": "neutralize-poison",
                      "slug": "neutralize-poison",
                      "sourceId": "neutralize-poison",
                      "sourceHash": "sha256:222222",
                      "nameEs": "Neutralizar veneno reformulado",
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
                      "descriptionEs": "Texto generado diferente que no debe borrar la edición local.",
                      "descriptionEn": "You detoxify any sort of venom in the creature or object touched.",
                      "sourceBook": "Core Rulebook",
                      "sourcePage": 264,
                      "sourceName": "spells.csv",
                      "translationStatus": "AI_TRANSLATED",
                      "lists": [
                        { "spellId": "neutralize-poison", "listType": "CLASS", "listName": "Clérigo", "level": 4 }
                      ],
                      "personalNotes": "",
                      "createdAt": "2026-06-12T00:00:00Z",
                      "updatedAt": "2026-06-12T00:00:00Z"
                    }
                  ]
                }
                """);

        var reimport = importService.importDataset(generatedPath, overridesPath);
        repository.rebuild(reimport.effectiveSpells());

        var detailAfterReimport = service.getSpellDetail("neutralize-poison");
        assertThat(detailAfterReimport.translationStatus()).isEqualTo("LOCKED");
        assertThat(detailAfterReimport.nameEs()).isEqualTo("Neutralizar veneno");
        assertThat(detailAfterReimport.descriptionEs()).isEqualTo("Neutralizas cualquier tipo de veneno presente en la criatura u objeto tocado.");
        assertThat(detailAfterReimport.nameEn()).isEqualTo("Neutralize Poison");
    }

    private void writeGeneratedDataset(String json) throws Exception {
        Files.writeString(generatedPath, json);
    }
}
