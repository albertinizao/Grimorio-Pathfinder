package com.grimoriopathfinder.spells;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class SpellListNameNormalizer {

    private static final Pattern NON_WORDS = Pattern.compile("[^\\p{L}\\p{Nd}]+");
    private static final Pattern DESENCADENADO = Pattern.compile("\\bdesencadenad[oa]\\b");
    private static final Map<String, String> CLASS_ALIASES = Map.ofEntries(
            Map.entry("alchemista", "Alquimista"),
            Map.entry("antipaladin", "Antipaladín"),
            Map.entry("antipladain", "Antipaladín"),
            Map.entry("antipladin", "Antipaladín"),
            Map.entry("bloodrager", "Bruto de sangre"),
            Map.entry("cleriga", "Clérigo"),
            Map.entry("clerigo", "Clérigo"),
            Map.entry("espiritista", "Espiritualista"),
            Map.entry("escalda", "Escaldo"),
            Map.entry("furioso de sangre", "Bruto de sangre"),
            Map.entry("guardabosques", "Explorador"),
            Map.entry("hechicero", "Hechicero"),
            Map.entry("inquisidora", "Inquisidor"),
            Map.entry("inquisidor", "Inquisidor"),
            Map.entry("invocador", "Invocador"),
            Map.entry("magus um", "Magus"),
            Map.entry("magusum", "Magus"),
            Map.entry("medium", "Medium"),
            Map.entry("medio", "Medium"),
            Map.entry("mesmerist", "Mesmerista"),
            Map.entry("mago", "Mago"),
            Map.entry("oraculo", "Oráculo"),
            Map.entry("paladin", "Paladín"),
            Map.entry("psiquica", "Psíquico"),
            Map.entry("psiquico", "Psíquico"),
            Map.entry("ranger", "Explorador"),
            Map.entry("sangrienta", "Bruto de sangre"),
            Map.entry("sangriento", "Bruto de sangre"),
            Map.entry("skald", "Escaldo"),
            Map.entry("espiritista", "Espiritualista")
    );

    private SpellListNameNormalizer() {
    }

    public static List<SpellListEntry> normalizeClassEntries(List<SpellListEntry> entries) {
        if (entries == null) {
            return null;
        }

        var normalizedEntries = new LinkedHashMap<String, SpellListEntry>();
        for (var entry : entries) {
            if (entry == null) {
                continue;
            }
            if (!"CLASS".equals(entry.listType())) {
                normalizedEntries.putIfAbsent(key(entry), entry);
                continue;
            }

            for (var normalizedName : expandClassListNames(entry.listName())) {
                var normalizedEntry = new SpellListEntry(
                        entry.spellId(),
                        entry.listType(),
                        normalizedName,
                        entry.level()
                );
                normalizedEntries.putIfAbsent(key(normalizedEntry), normalizedEntry);
            }
        }
        return List.copyOf(normalizedEntries.values());
    }

    public static String canonicalClassListName(String listName) {
        if (listName == null) {
            return null;
        }

        var expanded = expandClassListNames(listName);
        if (expanded.size() == 1) {
            return expanded.getFirst();
        }
        return listName.trim().replaceAll("\\s+", " ");
    }

    public static List<String> expandClassListNames(String listName) {
        if (listName == null || listName.isBlank()) {
            return List.of();
        }

        var expanded = new LinkedHashSet<String>();
        for (var part : listName.split("\\s*/\\s*")) {
            var canonical = canonicalizeSingleName(part);
            if (!canonical.isBlank()) {
                expanded.add(canonical);
            }
        }
        return List.copyOf(expanded);
    }

    private static String canonicalizeSingleName(String rawName) {
        if (rawName == null) {
            return "";
        }

        var trimmed = rawName.trim().replaceAll("\\s+", " ");
        if (trimmed.isBlank()) {
            return "";
        }

        var normalizedKey = normalizeKey(trimmed);
        normalizedKey = DESENCADENADO.matcher(normalizedKey).replaceAll(" ");
        normalizedKey = normalizedKey.trim().replaceAll("\\s+", " ");

        var alias = CLASS_ALIASES.get(normalizedKey);
        if (alias != null) {
            return alias;
        }

        return trimmed;
    }

    private static String normalizeKey(String input) {
        var normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}+", "");
        normalized = normalized.toLowerCase(Locale.ROOT);
        normalized = NON_WORDS.matcher(normalized).replaceAll(" ");
        return normalized.trim().replaceAll("\\s+", " ");
    }

    private static String key(SpellListEntry entry) {
        return entry.spellId() + "|" + entry.listType() + "|" + entry.listName() + "|" + entry.level();
    }
}
