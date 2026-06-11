package com.grimoriopathfinder.sqlite;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grimoriopathfinder.spells.Spell;
import com.grimoriopathfinder.spells.SpellListEntry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class SpellCatalogSqliteRepository {

    private static final Pattern NON_WORDS = Pattern.compile("[^\\p{L}\\p{Nd}]+");
    private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

    private final Path databasePath;

    @Autowired
    public SpellCatalogSqliteRepository(@Value("${grimorio.sqlite.path}") String databasePath) {
        this(Path.of(databasePath));
    }

    SpellCatalogSqliteRepository(Path databasePath) {
        this.databasePath = databasePath;
        ensureSchema();
    }

    public void rebuild(List<Spell> spells) {
        try {
            Files.createDirectories(parentDirectory());
            Files.deleteIfExists(databasePath);
            try (var connection = openConnection()) {
                ensureSchema(connection);
                connection.setAutoCommit(false);
                try {
                    clearDatabase(connection);
                    insertSpells(connection, spells);
                    connection.commit();
                } catch (SQLException | IOException ex) {
                    connection.rollback();
                    throw ex;
                } finally {
                    connection.setAutoCommit(true);
                }
            }
        } catch (SQLException | IOException ex) {
            throw new IllegalStateException("Unable to rebuild SQLite projection", ex);
        }
    }

    public List<ListSummary> listSpellLists(String listType) {
        var sql = """
                SELECT list_type, list_name, COUNT(DISTINCT spell_id) AS spell_count,
                       MIN(level) AS min_level, MAX(level) AS max_level
                FROM spell_list_entries
                WHERE (? IS NULL OR ? = '' OR list_type = ?)
                GROUP BY list_type, list_name
                ORDER BY list_type ASC, list_name COLLATE NOCASE ASC
                """;
        try (var connection = openConnection();
             var statement = connection.prepareStatement(sql)) {
            bindOptionalText(statement, 1, listType);
            bindOptionalText(statement, 2, listType);
            bindOptionalText(statement, 3, listType);
            var results = new ArrayList<ListSummary>();
            try (var rs = statement.executeQuery()) {
                while (rs.next()) {
                    var levels = readLevels(connection, rs.getString("list_type"), rs.getString("list_name"));
                    results.add(new ListSummary(
                            rs.getString("list_type"),
                            rs.getString("list_name"),
                            rs.getInt("min_level"),
                            rs.getInt("max_level"),
                            levels,
                            rs.getInt("spell_count")
                    ));
                }
            }
            return List.copyOf(results);
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to list spell lists", ex);
        }
    }

    public Optional<ListLevels> getSpellListLevels(String listType, String listName) {
        var sql = """
                SELECT level
                FROM spell_list_entries
                WHERE list_type = ? AND list_name = ?
                ORDER BY level ASC
                """;
        try (var connection = openConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, listType);
            statement.setString(2, listName);
            var levels = new ArrayList<Integer>();
            try (var rs = statement.executeQuery()) {
                while (rs.next()) {
                    levels.add(rs.getInt("level"));
                }
            }
            if (levels.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new ListLevels(listType, listName, levels.getFirst(), levels.getLast(), List.copyOf(levels)));
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to read spell list levels", ex);
        }
    }

    public List<SearchCandidate> findCandidates(String listType, String listName, int maxLevel) {
        var sql = """
                SELECT
                    s.id, s.slug, s.source_id, s.source_hash, s.name_es, s.name_en, s.school, s.subschool,
                    s.descriptors_json, s.casting_time, s.components, s.range, s.target, s.effect, s.area,
                    s.duration, s.saving_throw, s.spell_resistance, s.description_es, s.description_en,
                    s.source_book, s.source_page, s.source_name, s.translation_status, s.personal_notes,
                    s.created_at, s.updated_at,
                    sle.spell_id, sle.list_type, sle.list_name, sle.level
                FROM spells s
                INNER JOIN spell_list_entries sle ON sle.spell_id = s.id
                WHERE sle.list_type = ? AND sle.list_name = ? AND sle.level <= ?
                ORDER BY sle.level ASC, s.name_es COLLATE NOCASE ASC
                """;
        try (var connection = openConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, listType);
            statement.setString(2, listName);
            statement.setInt(3, maxLevel);
            var results = new ArrayList<SearchCandidate>();
            try (var rs = statement.executeQuery()) {
                while (rs.next()) {
                    var selectedList = readListEntryFromCurrentRow(rs);
                    var spell = readSpellFromCurrentRow(rs, List.of(selectedList));
                    results.add(new SearchCandidate(spell, selectedList));
                }
            }
            return List.copyOf(results);
        } catch (SQLException | IOException ex) {
            throw new IllegalStateException("Unable to query spell candidates", ex);
        }
    }

    public Optional<Spell> findSpellById(String spellId) {
        var sql = """
                SELECT
                    id, slug, source_id, source_hash, name_es, name_en, school, subschool,
                    descriptors_json, casting_time, components, range, target, effect, area,
                    duration, saving_throw, spell_resistance, description_es, description_en,
                    source_book, source_page, source_name, translation_status, personal_notes,
                    created_at, updated_at
                FROM spells
                WHERE id = ?
                """;
        try (var connection = openConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, spellId);
            try (var rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                var lists = readListEntries(connection, spellId);
                return Optional.of(readSpellFromCurrentRow(rs, lists));
            }
        } catch (SQLException | IOException ex) {
            throw new IllegalStateException("Unable to read spell detail", ex);
        }
    }

    private void ensureSchema() {
        try (var connection = openConnection()) {
            ensureSchema(connection);
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to initialize SQLite projection", ex);
        }
    }

    private void ensureSchema(Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS spells (
                        id TEXT PRIMARY KEY NOT NULL,
                        slug TEXT NOT NULL,
                        source_id TEXT NOT NULL,
                        source_hash TEXT NOT NULL,
                        name_es TEXT,
                        name_en TEXT,
                        school TEXT,
                        subschool TEXT,
                        descriptors_json TEXT NOT NULL DEFAULT '[]',
                        casting_time TEXT,
                        components TEXT,
                        range TEXT,
                        target TEXT,
                        effect TEXT,
                        area TEXT,
                        duration TEXT,
                        saving_throw TEXT,
                        spell_resistance TEXT,
                        description_es TEXT,
                        description_en TEXT,
                        source_book TEXT,
                        source_page INTEGER,
                        source_name TEXT,
                        translation_status TEXT NOT NULL DEFAULT 'NOT_TRANSLATED',
                        personal_notes TEXT NOT NULL DEFAULT '',
                        created_at TEXT,
                        updated_at TEXT,
                        search_text TEXT NOT NULL DEFAULT ''
                    ) STRICT;
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS spell_list_entries (
                        spell_id TEXT NOT NULL,
                        list_type TEXT NOT NULL,
                        list_name TEXT NOT NULL,
                        level INTEGER NOT NULL,
                        PRIMARY KEY (spell_id, list_type, list_name),
                        FOREIGN KEY (spell_id) REFERENCES spells(id) ON DELETE CASCADE
                    ) STRICT;
                    """);
            statement.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_spell_list_entries_lookup
                    ON spell_list_entries(list_type, list_name, level, spell_id);
                    """);
            statement.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_spell_list_entries_spell_id
                    ON spell_list_entries(spell_id);
                    """);
            statement.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_spells_translation_status
                    ON spells(translation_status);
                    """);
        }
    }

    private void clearDatabase(Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM spell_list_entries");
            statement.executeUpdate("DELETE FROM spells");
        }
    }

    private void insertSpells(Connection connection, List<Spell> spells) throws SQLException, IOException {
        try (var spellStatement = connection.prepareStatement("""
                INSERT INTO spells (
                    id, slug, source_id, source_hash, name_es, name_en, school, subschool,
                    descriptors_json, casting_time, components, range, target, effect, area,
                    duration, saving_throw, spell_resistance, description_es, description_en,
                    source_book, source_page, source_name, translation_status, personal_notes,
                    created_at, updated_at, search_text
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """);
             var listStatement = connection.prepareStatement("""
                INSERT INTO spell_list_entries (spell_id, list_type, list_name, level)
                VALUES (?, ?, ?, ?)
                """)) {
            for (var spell : spells) {
                spellStatement.setString(1, spell.id());
                spellStatement.setString(2, spell.slug());
                spellStatement.setString(3, spell.sourceId());
                spellStatement.setString(4, spell.sourceHash());
                spellStatement.setString(5, spell.nameEs());
                spellStatement.setString(6, spell.nameEn());
                spellStatement.setString(7, spell.school());
                spellStatement.setString(8, spell.subschool());
                spellStatement.setString(9, MAPPER.writeValueAsString(spell.descriptors() == null ? List.of() : spell.descriptors()));
                spellStatement.setString(10, spell.castingTime());
                spellStatement.setString(11, spell.components());
                spellStatement.setString(12, spell.range());
                spellStatement.setString(13, spell.target());
                spellStatement.setString(14, spell.effect());
                spellStatement.setString(15, spell.area());
                spellStatement.setString(16, spell.duration());
                spellStatement.setString(17, spell.savingThrow());
                spellStatement.setString(18, spell.spellResistance());
                spellStatement.setString(19, spell.descriptionEs());
                spellStatement.setString(20, spell.descriptionEn());
                spellStatement.setString(21, spell.sourceBook());
                if (spell.sourcePage() == null) {
                    spellStatement.setNull(22, java.sql.Types.INTEGER);
                } else {
                    spellStatement.setInt(22, spell.sourcePage());
                }
                spellStatement.setString(23, spell.sourceName());
                spellStatement.setString(24, spell.translationStatus());
                spellStatement.setString(25, spell.personalNotes() == null ? "" : spell.personalNotes());
                spellStatement.setString(26, toIsoString(spell.createdAt()));
                spellStatement.setString(27, toIsoString(spell.updatedAt()));
                spellStatement.setString(28, buildSearchText(spell));
                spellStatement.addBatch();

                for (var listEntry : spell.lists()) {
                    listStatement.setString(1, spell.id());
                    listStatement.setString(2, listEntry.listType());
                    listStatement.setString(3, listEntry.listName());
                    listStatement.setInt(4, listEntry.level());
                    listStatement.addBatch();
                }
            }
            spellStatement.executeBatch();
            listStatement.executeBatch();
        }
    }

    private Spell readSpellFromCurrentRow(ResultSet rs, List<SpellListEntry> lists) throws SQLException, IOException {
        return new Spell(
                rs.getString("id"),
                rs.getString("slug"),
                rs.getString("source_id"),
                rs.getString("source_hash"),
                rs.getString("name_es"),
                rs.getString("name_en"),
                rs.getString("school"),
                rs.getString("subschool"),
                readDescriptors(rs.getString("descriptors_json")),
                rs.getString("casting_time"),
                rs.getString("components"),
                rs.getString("range"),
                rs.getString("target"),
                rs.getString("effect"),
                rs.getString("area"),
                rs.getString("duration"),
                rs.getString("saving_throw"),
                rs.getString("spell_resistance"),
                rs.getString("description_es"),
                rs.getString("description_en"),
                rs.getString("source_book"),
                getNullableInteger(rs, "source_page"),
                rs.getString("source_name"),
                rs.getString("translation_status"),
                List.copyOf(lists),
                rs.getString("personal_notes"),
                readInstant(rs, "created_at"),
                readInstant(rs, "updated_at")
        );
    }

    private List<SpellListEntry> readListEntries(Connection connection, String spellId) throws SQLException {
        var sql = """
                SELECT spell_id, list_type, list_name, level
                FROM spell_list_entries
                WHERE spell_id = ?
                ORDER BY level ASC, list_type ASC, list_name COLLATE NOCASE ASC
                """;
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, spellId);
            var listEntries = new ArrayList<SpellListEntry>();
            try (var rs = statement.executeQuery()) {
                while (rs.next()) {
                    listEntries.add(readListEntryFromCurrentRow(rs));
                }
            }
            return List.copyOf(listEntries);
        }
    }

    private List<Integer> readLevels(Connection connection, String listType, String listName) throws SQLException {
        var sql = """
                SELECT level
                FROM spell_list_entries
                WHERE list_type = ? AND list_name = ?
                ORDER BY level ASC
                """;
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, listType);
            statement.setString(2, listName);
            var levels = new ArrayList<Integer>();
            try (var rs = statement.executeQuery()) {
                while (rs.next()) {
                    levels.add(rs.getInt("level"));
                }
            }
            return List.copyOf(levels);
        }
    }

    private SpellListEntry readListEntryFromCurrentRow(ResultSet rs) throws SQLException {
        return new SpellListEntry(
                rs.getString("spell_id"),
                rs.getString("list_type"),
                rs.getString("list_name"),
                rs.getInt("level")
        );
    }

    private List<String> readDescriptors(String json) throws IOException {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        return MAPPER.readValue(json, new TypeReference<List<String>>() {});
    }

    private Instant readInstant(ResultSet rs, String column) throws SQLException {
        var value = rs.getString(column);
        return value == null || value.isBlank() ? null : Instant.parse(value);
    }

    private Integer getNullableInteger(ResultSet rs, String column) throws SQLException {
        var value = rs.getObject(column);
        return value == null ? null : rs.getInt(column);
    }

    private String toIsoString(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    private String buildSearchText(Spell spell) {
        var parts = new ArrayList<String>();
        addPart(parts, spell.nameEs());
        addPart(parts, spell.descriptionEs());
        addPart(parts, spell.school());
        addPart(parts, spell.subschool());
        if (spell.descriptors() != null) {
            parts.addAll(spell.descriptors());
        }
        addPart(parts, spell.castingTime());
        addPart(parts, spell.components());
        addPart(parts, spell.range());
        addPart(parts, spell.target());
        addPart(parts, spell.effect());
        addPart(parts, spell.area());
        addPart(parts, spell.duration());
        addPart(parts, spell.savingThrow());
        addPart(parts, spell.spellResistance());
        addPart(parts, spell.personalNotes());
        return normalize(String.join(" ", parts));
    }

    private void addPart(List<String> parts, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(value);
        }
    }

    private String normalize(String input) {
        if (input == null) {
            return "";
        }
        var normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}+", "");
        normalized = normalized.toLowerCase(Locale.ROOT);
        normalized = NON_WORDS.matcher(normalized).replaceAll(" ");
        return normalized.trim().replaceAll("\\s+", " ");
    }

    private Path parentDirectory() {
        return databasePath.toAbsolutePath().getParent();
    }

    private Connection openConnection() throws SQLException {
        try {
            Files.createDirectories(parentDirectory());
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to create SQLite directory", ex);
        }
        var connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath.toAbsolutePath());
        try (var statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys=ON");
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA synchronous=NORMAL");
            statement.execute("PRAGMA busy_timeout=5000");
        }
        return connection;
    }

    private void bindOptionalText(PreparedStatement statement, int index, String value) throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, java.sql.Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    public record ListSummary(String listType, String listName, int minLevel, int maxLevel, List<Integer> levels, int spellCount) {
    }

    public record ListLevels(String listType, String listName, int minLevel, int maxLevel, List<Integer> levels) {
    }

    public record SearchCandidate(Spell spell, SpellListEntry selectedList) {
    }
}
