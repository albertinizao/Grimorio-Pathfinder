package com.grimoriopathfinder.dataset;

import com.grimoriopathfinder.overrides.SpellOverridesComposer;
import com.grimoriopathfinder.overrides.SpellOverridesFile;
import com.grimoriopathfinder.overrides.SpellOverridesJsonRepository;
import com.grimoriopathfinder.overrides.SpellOverridesResult;
import com.grimoriopathfinder.spells.Spell;
import com.grimoriopathfinder.spells.SpellListEntry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class SpellDatasetImportService {

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "NOT_TRANSLATED",
            "AI_TRANSLATED",
            "REVIEW_REQUIRED",
            "REVIEWED",
            "MANUALLY_EDITED",
            "LOCKED"
    );

    private final SpellDatasetJsonRepository generatedRepository;
    private final SpellOverridesJsonRepository overridesRepository;
    private final SpellOverridesComposer overridesComposer;

    public SpellDatasetImportService() {
        this(new SpellDatasetJsonRepository(), new SpellOverridesJsonRepository(), new SpellOverridesComposer());
    }

    public SpellDatasetImportService(
            SpellDatasetJsonRepository generatedRepository,
            SpellOverridesJsonRepository overridesRepository,
            SpellOverridesComposer overridesComposer
    ) {
        this.generatedRepository = generatedRepository;
        this.overridesRepository = overridesRepository;
        this.overridesComposer = overridesComposer;
    }

    public SpellImportResult importDataset(Path generatedPath, Path overridesPath) throws IOException {
        var generatedFile = generatedRepository.read(generatedPath);
        validateGeneratedFile(generatedFile);

        SpellOverridesFile overridesFile = Files.exists(overridesPath)
                ? overridesRepository.read(overridesPath)
                : new SpellOverridesFile(1, null, java.util.Map.of());
        validateOverridesFile(overridesFile);

        SpellOverridesResult result = overridesComposer.apply(generatedFile.spells(), overridesFile);
        return new SpellImportResult(result.effectiveSpells(), result.warnings());
    }

    private void validateGeneratedFile(SpellDatasetFile generatedFile) {
        if (generatedFile.version() != 1) {
            throw new IllegalStateException("Unsupported generated dataset version: " + generatedFile.version());
        }
        if (generatedFile.spells() == null) {
            throw new IllegalStateException("Generated dataset must include spells");
        }

        for (var spell : generatedFile.spells()) {
            validateSpell(spell);
        }
    }

    private void validateOverridesFile(SpellOverridesFile overridesFile) {
        if (overridesFile.version() != 1) {
            throw new IllegalStateException("Unsupported overrides version: " + overridesFile.version());
        }
    }

    private void validateSpell(Spell spell) {
        if (spell == null) {
            throw new IllegalStateException("Generated dataset contains a null spell");
        }
        if (isBlank(spell.id())) {
            throw new IllegalStateException("Generated spell id is required");
        }
        if (isBlank(spell.sourceHash())) {
            throw new IllegalStateException("Generated spell sourceHash is required");
        }
        if (!ALLOWED_STATUSES.contains(spell.translationStatus())) {
            throw new IllegalStateException("Unsupported translationStatus: " + spell.translationStatus());
        }
        if (spell.lists() == null) {
            throw new IllegalStateException("Generated spell lists must not be null");
        }
        for (var listEntry : spell.lists()) {
            validateListEntry(spell.id(), listEntry);
        }
    }

    private void validateListEntry(String spellId, SpellListEntry listEntry) {
        if (listEntry == null) {
            throw new IllegalStateException("Generated spell " + spellId + " contains a null list entry");
        }
        if (listEntry.level() < 0) {
            throw new IllegalStateException("Generated spell " + spellId + " contains a negative list level");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
