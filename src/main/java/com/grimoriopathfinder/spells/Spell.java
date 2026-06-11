package com.grimoriopathfinder.spells;

import java.time.Instant;
import java.util.List;

public record Spell(
        String id,
        String slug,
        String sourceId,
        String sourceHash,
        String nameEs,
        String nameEn,
        String school,
        String subschool,
        List<String> descriptors,
        String castingTime,
        String components,
        String range,
        String target,
        String effect,
        String area,
        String duration,
        String savingThrow,
        String spellResistance,
        String descriptionEs,
        String descriptionEn,
        String sourceBook,
        Integer sourcePage,
        String sourceName,
        String translationStatus,
        List<SpellListEntry> lists,
        String personalNotes,
        Instant createdAt,
        Instant updatedAt) {
}
