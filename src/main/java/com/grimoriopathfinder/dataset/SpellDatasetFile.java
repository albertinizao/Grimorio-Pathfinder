package com.grimoriopathfinder.dataset;

import com.grimoriopathfinder.spells.Spell;

import java.util.List;

public record SpellDatasetFile(int version, String generatedAt, String sourceName, List<Spell> spells) {
}
