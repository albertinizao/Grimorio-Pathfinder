package com.grimoriopathfinder.overrides;

import com.grimoriopathfinder.spells.Spell;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SpellOverridesComposer {

    private static final List<String> TRANSLATED_FIELDS = List.of(
            "nameEs",
            "school",
            "subschool",
            "descriptors",
            "castingTime",
            "components",
            "range",
            "target",
            "effect",
            "area",
            "duration",
            "savingThrow",
            "spellResistance",
            "descriptionEs"
    );

    private static final List<String> ALLOWED_FIELDS = List.of(
            "nameEs",
            "school",
            "subschool",
            "descriptors",
            "castingTime",
            "components",
            "range",
            "target",
            "effect",
            "area",
            "duration",
            "savingThrow",
            "spellResistance",
            "descriptionEs",
            "translationStatus"
    );

    public SpellOverridesResult apply(List<Spell> generatedSpells, SpellOverridesFile overrides) {
        var spellsById = new LinkedHashMap<String, Spell>();
        for (var generatedSpell : generatedSpells) {
            spellsById.put(generatedSpell.id(), generatedSpell);
        }

        var warnings = new ArrayList<String>();
        var effectiveSpells = new ArrayList<Spell>();

        for (var generatedSpell : generatedSpells) {
            var override = overrides.spells() == null ? null : overrides.spells().get(generatedSpell.id());
            if (override == null) {
                effectiveSpells.add(generatedSpell);
                continue;
            }

            effectiveSpells.add(applyOverride(generatedSpell, override, warnings));
        }

        if (overrides.spells() != null) {
            for (var entry : overrides.spells().entrySet()) {
                if (!spellsById.containsKey(entry.getKey())) {
                    warnings.add("Orphan override: " + entry.getKey());
                }
            }
        }

        return new SpellOverridesResult(List.copyOf(effectiveSpells), List.copyOf(warnings));
    }

    private Spell applyOverride(Spell generatedSpell, SpellOverrideEntry override, List<String> warnings) {
        var fields = override.fields() == null ? Map.<String, Object>of() : override.fields();
        for (var fieldName : fields.keySet()) {
            if (!ALLOWED_FIELDS.contains(fieldName)) {
                warnings.add("Unsupported override field: " + generatedSpell.id() + "." + fieldName);
            }
        }

        var translationStatus = generatedSpell.translationStatus();
        if (fields.containsKey("translationStatus")) {
            translationStatus = stringValue(fields.get("translationStatus"), generatedSpell.translationStatus());
        } else if (hasTranslatedFieldOverride(fields)) {
            translationStatus = "MANUALLY_EDITED";
        }

        return new Spell(
                generatedSpell.id(),
                generatedSpell.slug(),
                generatedSpell.sourceId(),
                generatedSpell.sourceHash(),
                valueOr(generatedSpell.nameEs(), fields, "nameEs"),
                generatedSpell.nameEn(),
                valueOr(generatedSpell.school(), fields, "school"),
                valueOr(generatedSpell.subschool(), fields, "subschool"),
                listValueOr(generatedSpell.descriptors(), fields, "descriptors"),
                valueOr(generatedSpell.castingTime(), fields, "castingTime"),
                valueOr(generatedSpell.components(), fields, "components"),
                valueOr(generatedSpell.range(), fields, "range"),
                valueOr(generatedSpell.target(), fields, "target"),
                valueOr(generatedSpell.effect(), fields, "effect"),
                valueOr(generatedSpell.area(), fields, "area"),
                valueOr(generatedSpell.duration(), fields, "duration"),
                valueOr(generatedSpell.savingThrow(), fields, "savingThrow"),
                valueOr(generatedSpell.spellResistance(), fields, "spellResistance"),
                valueOr(generatedSpell.descriptionEs(), fields, "descriptionEs"),
                generatedSpell.descriptionEn(),
                generatedSpell.sourceBook(),
                generatedSpell.sourcePage(),
                generatedSpell.sourceName(),
                translationStatus,
                generatedSpell.lists(),
                override.personalNotes() != null ? override.personalNotes() : generatedSpell.personalNotes(),
                generatedSpell.createdAt(),
                generatedSpell.updatedAt()
        );
    }

    private boolean hasTranslatedFieldOverride(Map<String, Object> fields) {
        for (var field : TRANSLATED_FIELDS) {
            if (fields.containsKey(field)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private List<String> listValueOr(List<String> fallback, Map<String, Object> fields, String key) {
        if (!fields.containsKey(key)) {
            return fallback;
        }
        var value = fields.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return fallback;
    }

    private String valueOr(String fallback, Map<String, Object> fields, String key) {
        if (!fields.containsKey(key)) {
            return fallback;
        }
        return stringValue(fields.get(key), null);
    }

    private String stringValue(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }
}
