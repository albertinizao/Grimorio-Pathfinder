package com.grimoriopathfinder.sqlite;

import static org.assertj.core.api.Assertions.assertThat;

import com.grimoriopathfinder.catalog.SpellCatalogService;
import com.grimoriopathfinder.dataset.SpellDatasetImportService;
import java.sql.DriverManager;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpellCatalogSqliteRepositoryTest {

    @TempDir
    Path tempDir;

    @Test
    void rebuildsSQLiteProjectionAndServesSearchListAndDetailQueries() throws Exception {
        var generatedPath = Path.of("src/test/resources/data/generated/spells-es.generated.json");
        var overridesPath = Path.of("src/test/resources/data/overrides/spells-es.overrides.json");
        var databasePath = tempDir.resolve("grimorio.sqlite");

        var importer = new SpellDatasetImportService();
        var importResult = importer.importDataset(generatedPath, overridesPath);

        var repository = new SpellCatalogSqliteRepository(databasePath);
        repository.rebuild(importResult.effectiveSpells());

        assertThat(Files.exists(databasePath)).isTrue();
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath());
             var statement = connection.createStatement();
             var rs = statement.executeQuery("SELECT COUNT(*) FROM spells")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getInt(1)).isEqualTo(2);
        }

        var service = new SpellCatalogService(repository);

        var lists = service.listSpellLists(null);
        assertThat(lists.items()).hasSize(2);
        assertThat(lists.items().getFirst().listName()).isEqualTo("Clérigo");

        var levels = service.getSpellListLevels("CLASS", "Clérigo");
        assertThat(levels.levels()).containsExactly(3, 4);

        var search = service.searchSpells("CLASS", "Clérigo", 3, "drow", 0, 50);
        assertThat(search.results()).hasSize(1);
        assertThat(search.results().getFirst().spellId()).isEqualTo("delay-poison");
        assertThat(search.results().getFirst().matchSource()).isEqualTo("personalNotes");

        var detail = service.getSpellDetail("neutralize-poison");
        assertThat(detail.translationStatus()).isEqualTo("LOCKED");
        assertThat(detail.personalNotes()).isEqualTo("Traducción revisada y cerrada.");
    }
}
