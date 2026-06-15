package com.grimoriopathfinder.sqlite;

import static org.assertj.core.api.Assertions.assertThat;

import com.grimoriopathfinder.catalog.SpellCatalogService;
import com.grimoriopathfinder.dataset.SpellDatasetImportService;
import com.grimoriopathfinder.spells.Spell;
import com.grimoriopathfinder.spells.SpellListEntry;
import java.sql.DriverManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.assertj.core.groups.Tuple;

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

        var search = service.searchSpells("CLASS", "Clérigo", 3, "UP_TO", "drow", 0, 50);
        assertThat(search.results()).hasSize(1);
        assertThat(search.results().getFirst().spellId()).isEqualTo("delay-poison");
        assertThat(search.results().getFirst().matchSource()).isEqualTo("personalNotes");

        var detail = service.getSpellDetail("neutralize-poison");
        assertThat(detail.translationStatus()).isEqualTo("LOCKED");
        assertThat(detail.personalNotes()).isEqualTo("Traducción revisada y cerrada.");
    }

    @Test
    void deduplicatesLevelsWhenListingSpellLevels() {
        var databasePath = tempDir.resolve("grimorio.sqlite");
        var repository = new SpellCatalogSqliteRepository(databasePath);
        repository.rebuild(List.of(
                spell("spell-a", "Conjuro A", 2),
                spell("spell-b", "Conjuro B", 2),
                spell("spell-c", "Conjuro C", 5)
        ));

        var service = new SpellCatalogService(repository);

        var levels = service.getSpellListLevels("CLASS", "Clérigo");
        assertThat(levels.levels()).containsExactly(2, 5);

        var lists = service.listSpellLists("CLASS");
        assertThat(lists.items()).hasSize(1);
        assertThat(lists.items().getFirst().levels()).containsExactly(2, 5);
        assertThat(lists.items().getFirst().spellCount()).isEqualTo(3);
    }

    @Test
    void preservesMultipleLevelsForTheSameClassEntry() {
        var databasePath = tempDir.resolve("grimorio-levels.sqlite");
        var repository = new SpellCatalogSqliteRepository(databasePath);
        repository.rebuild(List.of(spellWithTwoLevels("wall-of-sound")));

        var service = new SpellCatalogService(repository);

        var detail = service.getSpellDetail("wall-of-sound");
        assertThat(detail.lists()).extracting("listName", "level").containsExactlyInAnyOrder(
                Tuple.tuple("Bardo", 4),
                Tuple.tuple("Mago", 4),
                Tuple.tuple("Hechicero", 5),
                Tuple.tuple("Mago", 5),
                Tuple.tuple("Bruto de sangre", 4),
                Tuple.tuple("Psíquico", 5),
                Tuple.tuple("Espiritualista", 5)
        );

        assertThat(service.listSpellLists("CLASS").items())
                .extracting(item -> item.listName())
                .contains("Mago");
        assertThat(service.getSpellListLevels("CLASS", "Mago").levels())
                .containsExactly(4, 5);
    }

    @Test
    void returnsAllSearchMatchesWhenPaginationIsOmitted() {
        var databasePath = tempDir.resolve("grimorio-all.sqlite");
        var repository = new SpellCatalogSqliteRepository(databasePath);
        var spells = new ArrayList<Spell>();
        for (int i = 0; i < 55; i++) {
            spells.add(spell("spell-" + i, "Conjuro " + i, 0));
        }
        repository.rebuild(spells);

        var service = new SpellCatalogService(repository);
        var search = service.searchSpells("CLASS", "Clérigo", 0, "UP_TO", "", null, null);

        assertThat(search.results()).hasSize(55);
        assertThat(search.page().totalItems()).isEqualTo(55);
        assertThat(search.page().size()).isEqualTo(55);
        assertThat(search.page().totalPages()).isEqualTo(1);
        assertThat(search.page().hasNext()).isFalse();
    }

    @Test
    void ordersSearchResultsByNameMatchesThenOtherFieldsThenDescriptionAndThenLevel() {
        var databasePath = tempDir.resolve("grimorio-order.sqlite");
        var repository = new SpellCatalogSqliteRepository(databasePath);
        repository.rebuild(List.of(
                spellWithSearchData("name-high", "Veneno superior", 4, "Texto neutro", List.of()),
                spellWithSearchData("name-low", "Veneno básico", 1, "Texto neutro", List.of()),
                spellWithSearchData("other-mid", "Conjuro neutro", 2, "Texto neutro", List.of("veneno")),
                spellWithSearchData("description-low", "Conjuro neutro 2", 0, "Contiene veneno en la descripción.", List.of())
        ));

        var service = new SpellCatalogService(repository);
        var search = service.searchSpells("CLASS", "Clérigo", 4, "UP_TO", "veneno", null, null);

        assertThat(search.results()).extracting("spellId").containsExactly(
                "name-low",
                "name-high",
                "other-mid",
                "description-low"
        );
        assertThat(search.results()).extracting("matchSource").containsExactly(
                "nameEs",
                "nameEs",
                "descriptors",
                "descriptionEs"
        );
    }

    private Spell spell(String id, String nameEs, int level) {
        return new Spell(
                id,
                id,
                id,
                "sha256:" + id,
                nameEs,
                nameEs + " EN",
                "abjuración",
                null,
                List.of(),
                "1 acción estándar",
                "V, S",
                "toque",
                null,
                null,
                null,
                "instantáneo",
                "ninguno",
                "no",
                nameEs + " descripción",
                nameEs + " description",
                "Core Rulebook",
                1,
                "spells.csv",
                "AI_TRANSLATED",
                List.of(new SpellListEntry(id, "CLASS", "Clérigo", level)),
                "",
                Instant.parse("2026-06-11T00:00:00Z"),
                Instant.parse("2026-06-11T00:00:00Z")
        );
    }

    private Spell spellWithSearchData(String id, String nameEs, int level, String descriptionEs, List<String> descriptors) {
        return new Spell(
                id,
                id,
                id,
                "sha256:" + id,
                nameEs,
                nameEs + " EN",
                "abjuración",
                null,
                descriptors,
                "1 acción estándar",
                "V, S",
                "toque",
                null,
                null,
                null,
                "instantáneo",
                "ninguno",
                "no",
                descriptionEs,
                descriptionEs + " description",
                "Core Rulebook",
                1,
                "spells.csv",
                "AI_TRANSLATED",
                List.of(new SpellListEntry(id, "CLASS", "Clérigo", level)),
                "",
                Instant.parse("2026-06-11T00:00:00Z"),
                Instant.parse("2026-06-11T00:00:00Z")
        );
    }

    private Spell spellWithTwoLevels(String id) {
        return new Spell(
                id,
                id,
                id,
                "sha256:" + id,
                "Muro de Sonido",
                "Wall of Sound",
                "ilusión",
                null,
                List.of(),
                "1 acción estándar",
                "V, S",
                "largo",
                null,
                null,
                null,
                "instantáneo",
                "sí",
                "no",
                "Descripción",
                "Description",
                "Core Rulebook",
                1,
                "spells.csv",
                "AI_TRANSLATED",
                List.of(
                        new SpellListEntry(id, "CLASS", "Bardo", 4),
                        new SpellListEntry(id, "CLASS", "Mago", 4),
                        new SpellListEntry(id, "CLASS", "Hechicero", 5),
                        new SpellListEntry(id, "CLASS", "Mago", 5),
                        new SpellListEntry(id, "CLASS", "Bruto de sangre", 4),
                        new SpellListEntry(id, "CLASS", "Psíquico", 5),
                        new SpellListEntry(id, "CLASS", "Espiritualista", 5)
                ),
                "",
                Instant.parse("2026-06-11T00:00:00Z"),
                Instant.parse("2026-06-11T00:00:00Z")
        );
    }
}
