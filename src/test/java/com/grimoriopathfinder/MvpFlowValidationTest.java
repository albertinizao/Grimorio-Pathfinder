package com.grimoriopathfinder;

import static org.assertj.core.api.Assertions.assertThat;

import com.grimoriopathfinder.catalog.SpellCatalogService;
import com.grimoriopathfinder.dataset.SpellDatasetImportService;
import com.grimoriopathfinder.overrides.SpellOverridesJsonRepository;
import com.grimoriopathfinder.sqlite.SpellCatalogSqliteRepository;
import com.grimoriopathfinder.web.dto.SpellApiDtos.UpdatePersonalNotesRequestDto;
import com.grimoriopathfinder.web.dto.SpellApiDtos.UpdateSpellFieldsRequestDto;
import com.grimoriopathfinder.web.dto.SpellApiDtos.UpdateTranslationStatusRequestDto;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MvpFlowValidationTest {

    @TempDir
    Path tempDir;

    @Test
    void validatesTheOfflineMvpFlowFromBuildToReimport() throws Exception {
        var generatedPath = tempDir.resolve("data/generated/spells-es.generated.json");
        var overridesPath = tempDir.resolve("data/overrides/spells-es.overrides.json");
        var databasePath = tempDir.resolve("data/local/grimorio.sqlite");
        Files.createDirectories(generatedPath.getParent());
        Files.createDirectories(overridesPath.getParent());
        Files.createDirectories(databasePath.getParent());

        copyFixture("src/test/resources/data/generated/spells-es.generated.json", generatedPath);
        copyFixture("src/test/resources/data/overrides/spells-es.overrides.json", overridesPath);

        var importService = new SpellDatasetImportService();
        var overridesRepository = new SpellOverridesJsonRepository();
        var repository = new SpellCatalogSqliteRepository(databasePath.toString());
        var service = new SpellCatalogService(
                repository,
                importService,
                overridesRepository,
                generatedPath.toString(),
                overridesPath.toString()
        );

        var initialImport = importService.importDataset(generatedPath, overridesPath);
        repository.rebuild(initialImport.effectiveSpells());
        assertThat(Files.exists(databasePath)).isTrue();

        var searchByTerm = service.searchSpells("CLASS", "Clérigo", 3, "UP_TO", "veneno", 0, 50);
        assertThat(searchByTerm.results()).hasSize(1);

        var searchResult = searchByTerm.results().getFirst();
        assertThat(searchResult.spellId()).isEqualTo("delay-poison");
        assertThat(searchResult.selectedList().listType()).isEqualTo("CLASS");
        assertThat(searchResult.selectedList().listName()).isEqualTo("Clérigo");
        assertThat(searchResult.selectedList().level()).isEqualTo(3);
        assertThat(searchResult.matchSource()).isEqualTo("nameEs");
        assertThat(searchResult.hasPersonalNotes()).isTrue();

        var detailFromResults = service.getSpellDetail(searchResult.spellId());
        assertThat(detailFromResults.spellId()).isEqualTo(searchResult.spellId());
        assertThat(detailFromResults.nameEs()).isEqualTo(searchResult.nameEs());
        assertThat(detailFromResults.school()).isEqualTo(searchResult.school());
        assertThat(detailFromResults.range()).isEqualTo(searchResult.range());
        assertThat(detailFromResults.savingThrow()).isEqualTo(searchResult.savingThrow());
        assertThat(detailFromResults.spellResistance()).isEqualTo(searchResult.spellResistance());
        assertThat(detailFromResults.translationStatus()).isEqualTo("AI_TRANSLATED");
        assertThat(detailFromResults.personalNotes()).isEqualTo("Preparar si esperamos drow o criaturas venenosas.");
        assertThat(detailFromResults.lists())
                .extracting("listType", "listName", "level")
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("CLASS", "Clérigo", 3),
                        org.assertj.core.groups.Tuple.tuple("CLASS", "Druida", 2)
                );

        var browsedWithoutTerm = service.searchSpells("CLASS", "Clérigo", 3, "UP_TO", "", 0, 50);
        assertThat(browsedWithoutTerm.results()).hasSize(1);
        assertThat(browsedWithoutTerm.results().getFirst().spellId()).isEqualTo("delay-poison");

        var updatedFields = service.updateSpellFields(
                "delay-poison",
                new UpdateSpellFieldsRequestDto(
                        Map.of("descriptionEs", "Protege temporalmente contra el veneno."),
                        detailFromResults.updatedAt(),
                        "Validación del flujo MVP"
                )
        );
        assertThat(updatedFields.descriptionEs()).isEqualTo("Protege temporalmente contra el veneno.");
        assertThat(updatedFields.translationStatus()).isEqualTo("MANUALLY_EDITED");

        var updatedNotes = service.updatePersonalNotes(
                "delay-poison",
                new UpdatePersonalNotesRequestDto(
                        "Preparar si esperamos drow.",
                        updatedFields.updatedAt()
                )
        );
        assertThat(updatedNotes.personalNotes()).isEqualTo("Preparar si esperamos drow.");

        var locked = service.updateTranslationStatus(
                "neutralize-poison",
                new UpdateTranslationStatusRequestDto(
                        "LOCKED",
                        service.getSpellDetail("neutralize-poison").updatedAt(),
                        "Cierre de mesa"
                )
        );
        assertThat(locked.translationStatus()).isEqualTo("LOCKED");

        var overridesAfterEdits = overridesRepository.read(overridesPath);
        assertThat(overridesAfterEdits.spells()).containsKeys("delay-poison", "neutralize-poison");
        assertThat(overridesAfterEdits.spells().get("delay-poison").fields())
                .containsEntry("descriptionEs", "Protege temporalmente contra el veneno.")
                .containsEntry("translationStatus", "MANUALLY_EDITED");
        assertThat(overridesAfterEdits.spells().get("delay-poison").personalNotes())
                .isEqualTo("Preparar si esperamos drow.");
        assertThat(overridesAfterEdits.spells().get("neutralize-poison").fields())
                .containsEntry("translationStatus", "LOCKED");

        var regeneratedDataset = Files.readString(generatedPath)
                .replace(
                        "El objetivo queda temporalmente protegido contra el veneno.",
                        "El objetivo queda temporalmente protegido contra los venenos."
                )
                .replace(
                        "Neutralizas cualquier tipo de veneno presente en la criatura u objeto tocado.",
                        "Texto regenerado que no debe sustituir el override."
                );
        Files.writeString(generatedPath, regeneratedDataset);

        var reimport = importService.importDataset(generatedPath, overridesPath);
        repository.rebuild(reimport.effectiveSpells());

        var detailAfterReimport = service.getSpellDetail("delay-poison");
        assertThat(detailAfterReimport.descriptionEs()).isEqualTo("Protege temporalmente contra el veneno.");
        assertThat(detailAfterReimport.personalNotes()).isEqualTo("Preparar si esperamos drow.");
        assertThat(detailAfterReimport.translationStatus()).isEqualTo("MANUALLY_EDITED");

        var lockedAfterReimport = service.getSpellDetail("neutralize-poison");
        assertThat(lockedAfterReimport.translationStatus()).isEqualTo("LOCKED");
        assertThat(lockedAfterReimport.nameEs()).isEqualTo("Neutralizar veneno");
        assertThat(lockedAfterReimport.descriptionEs())
                .isEqualTo("Neutralizas cualquier tipo de veneno presente en la criatura u objeto tocado.");
        assertThat(lockedAfterReimport.nameEn()).isEqualTo("Neutralize Poison");

        var finalBrowse = service.searchSpells("CLASS", "Clérigo", 3, "UP_TO", "veneno", 0, 50);
        assertThat(finalBrowse.results()).hasSize(1);
        assertThat(finalBrowse.results().getFirst().spellId()).isEqualTo("delay-poison");
    }

    private void copyFixture(String source, Path target) throws Exception {
        Files.copy(Path.of(source), target, StandardCopyOption.REPLACE_EXISTING);
    }
}
