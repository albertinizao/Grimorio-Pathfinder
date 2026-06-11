package com.grimoriopathfinder.dataset;

import com.grimoriopathfinder.spells.Spell;

import java.util.List;

public record SpellImportResult(List<Spell> effectiveSpells, List<String> warnings) {
}
