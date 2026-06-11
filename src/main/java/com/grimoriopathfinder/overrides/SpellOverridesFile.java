package com.grimoriopathfinder.overrides;

import java.util.Map;

public record SpellOverridesFile(int version, String updatedAt, Map<String, SpellOverrideEntry> spells) {
}
