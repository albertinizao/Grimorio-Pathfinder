package com.grimoriopathfinder.mariadb;

import static org.assertj.core.api.Assertions.assertThat;

import com.grimoriopathfinder.MariaDbIntegrationTest;
import com.grimoriopathfinder.dataset.SpellDatasetImportService;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "grimorio.dataset.generated-path=src/test/resources/data/generated/spells-es.generated.json",
        "grimorio.dataset.overrides-path=src/test/resources/data/overrides/spells-es.overrides.json",
        "grimorio.catalog.auto-rebuild=false"
})
class MariaDbCatalogIntegrationTest extends MariaDbIntegrationTest {
    @Autowired SpellCatalogMariaDbRepository repository;

    @Test
    void rebuildsProjectionAndPreservesSpanishAndEnglishFields() throws Exception {
        var result = new SpellDatasetImportService().importDataset(
                Path.of("src/test/resources/data/generated/spells-es.generated.json"),
                Path.of("src/test/resources/data/overrides/spells-es.overrides.json"));
        repository.rebuild(result.effectiveSpells());

        assertThat(repository.listSpellLists("CLASS")).extracting(SpellCatalogMariaDbRepository.ListSummary::listName)
                .contains("Clérigo", "Druida");
        assertThat(repository.findCandidates("CLASS", "Clérigo", 3, false)).hasSize(1);
        var detail = repository.findSpellById("neutralize-poison").orElseThrow();
        assertThat(detail.translationStatus()).isEqualTo("LOCKED");
        assertThat(detail.nameEn()).isNotBlank();
        assertThat(detail.personalNotes()).isNotBlank();
    }
}
