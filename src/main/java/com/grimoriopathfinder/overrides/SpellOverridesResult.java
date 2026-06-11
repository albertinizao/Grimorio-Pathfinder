package com.grimoriopathfinder.overrides;

import java.util.List;
import com.grimoriopathfinder.spells.Spell;

public record SpellOverridesResult(List<Spell> effectiveSpells, List<String> warnings) {
}
