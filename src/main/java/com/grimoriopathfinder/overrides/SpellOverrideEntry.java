package com.grimoriopathfinder.overrides;

import java.util.Map;

public record SpellOverrideEntry(Map<String, Object> fields, String personalNotes, String updatedAt, String reason) {
}
