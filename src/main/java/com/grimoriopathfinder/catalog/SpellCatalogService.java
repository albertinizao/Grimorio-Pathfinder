package com.grimoriopathfinder.catalog;

import com.grimoriopathfinder.dataset.SpellDatasetImportService;
import com.grimoriopathfinder.overrides.SpellOverrideEntry;
import com.grimoriopathfinder.overrides.SpellOverridesFile;
import com.grimoriopathfinder.overrides.SpellOverridesJsonRepository;
import com.grimoriopathfinder.sqlite.SpellCatalogSqliteRepository;
import com.grimoriopathfinder.spells.Spell;
import com.grimoriopathfinder.spells.SpellListEntry;
import com.grimoriopathfinder.web.dto.SpellApiDtos;
import com.grimoriopathfinder.web.error.SpellConflictException;
import com.grimoriopathfinder.web.error.SpellEditValidationException;
import com.grimoriopathfinder.web.error.SpellListNotFoundException;
import com.grimoriopathfinder.web.error.SpellNotFoundException;
import com.grimoriopathfinder.web.error.SpellQueryValidationException;
import com.grimoriopathfinder.web.error.SpellRequestValidationException;
import java.io.IOException;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SpellCatalogService {

    private static final Logger log = LoggerFactory.getLogger(SpellCatalogService.class);
    private static final Pattern NON_WORDS = Pattern.compile("[^\\p{L}\\p{Nd}]+");
    private static final Set<String> ALLOWED_OVERRIDE_FIELDS = Set.of(
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
    private static final Set<String> TRANSLATION_STATUSES = Set.of(
            "NOT_TRANSLATED",
            "AI_TRANSLATED",
            "REVIEW_REQUIRED",
            "REVIEWED",
            "MANUALLY_EDITED",
            "LOCKED"
    );
    private static final List<String> DETAIL_EDITABLE_FIELDS = List.of(
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
            "personalNotes",
            "translationStatus"
    );
    private static final String SORT_NAME = "LEVEL_ASC_NAME_ES_ASC";

    private final SpellCatalogSqliteRepository repository;
    private final SpellDatasetImportService importService;
    private final SpellOverridesJsonRepository overridesRepository;
    private final Path generatedPath;
    private final Path overridesPath;

    public SpellCatalogService(SpellCatalogSqliteRepository repository) {
        this.repository = repository;
        this.importService = null;
        this.overridesRepository = null;
        this.generatedPath = null;
        this.overridesPath = null;
    }

    @Autowired
    public SpellCatalogService(
            SpellCatalogSqliteRepository repository,
            SpellDatasetImportService importService,
            SpellOverridesJsonRepository overridesRepository,
            @Value("${grimorio.dataset.generated-path:data/generated/spells-es.generated.json}") String generatedPath,
            @Value("${grimorio.dataset.overrides-path:data/overrides/spells-es.overrides.json}") String overridesPath
    ) {
        this.repository = repository;
        this.importService = importService;
        this.overridesRepository = overridesRepository;
        this.generatedPath = generatedPath == null || generatedPath.isBlank() ? null : Path.of(generatedPath);
        this.overridesPath = overridesPath == null || overridesPath.isBlank() ? null : Path.of(overridesPath);
    }

    public SpellApiDtos.SpellListsResponseDto listSpellLists(String listType) {
        var filtered = repository.listSpellLists(listType).stream()
                .map(summary -> new SpellApiDtos.SpellListSummaryDto(
                        summary.listType(),
                        summary.listName(),
                        summary.minLevel(),
                        summary.maxLevel(),
                        summary.levels(),
                        summary.spellCount()
                ))
                .sorted(Comparator.comparing(SpellApiDtos.SpellListSummaryDto::listType)
                        .thenComparing(summary -> normalizeForOrdering(summary.listName())))
                .toList();
        return new SpellApiDtos.SpellListsResponseDto(filtered);
    }

    public SpellApiDtos.SpellListLevelsResponseDto getSpellListLevels(String listType, String listName) {
        validateRequiredText("listType", listType);
        validateRequiredText("listName", listName);

        var levels = repository.getSpellListLevels(listType, listName)
                .orElseThrow(() -> new SpellListNotFoundException("Spell list not found: " + listType + " / " + listName));

        return new SpellApiDtos.SpellListLevelsResponseDto(
                levels.listType(),
                levels.listName(),
                levels.minLevel(),
                levels.maxLevel(),
                levels.levels()
        );
    }

    public SpellApiDtos.SpellSearchResponseDto searchSpells(
            String listType,
            String listName,
            int maxLevel,
            String q,
            int page,
            int size
    ) {
        validateRequiredText("listType", listType);
        validateRequiredText("listName", listName);
        if (maxLevel < 0) {
            throw new SpellRequestValidationException("maxLevel must be greater than or equal to 0");
        }
        if (page < 0) {
            throw new SpellRequestValidationException("page must be greater than or equal to 0");
        }
        if (size < 1 || size > 200) {
            throw new SpellRequestValidationException("size must be between 1 and 200");
        }

        var query = q == null ? "" : q.trim();
        if (query.length() > 200) {
            throw new SpellQueryValidationException("q must not exceed 200 characters");
        }

        var levels = repository.getSpellListLevels(listType, listName)
                .orElseThrow(() -> new SpellListNotFoundException("Spell list not found: " + listType + " / " + listName));
        if (!levels.levels().contains(maxLevel)) {
            throw new SpellQueryValidationException("maxLevel is not compatible with the selected list");
        }

        var candidates = repository.findCandidates(listType, listName, maxLevel).stream()
                .filter(candidate -> matchesQuery(candidate.spell(), query))
                .sorted(SEARCH_COMPARATOR)
                .toList();

        var totalItems = candidates.size();
        var fromIndex = Math.min(page * size, totalItems);
        var toIndex = Math.min(fromIndex + size, totalItems);
        var pageItems = candidates.subList(fromIndex, toIndex);

        var results = pageItems.stream()
                .map(candidate -> toSearchResult(candidate, query))
                .toList();

        var totalPages = totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / size);
        var pageDto = new SpellApiDtos.SpellSearchPageDto(page, size, totalItems, totalPages, page + 1 < totalPages);
        return new SpellApiDtos.SpellSearchResponseDto(
                new SpellApiDtos.SpellSearchFiltersDto(listType, listName, maxLevel, query),
                pageDto,
                SORT_NAME,
                results
        );
    }

    public SpellApiDtos.SpellDetailResponseDto getSpellDetail(String spellId) {
        validateRequiredText("spellId", spellId);
        var spell = repository.findSpellById(spellId)
                .orElseThrow(() -> new SpellNotFoundException("Spell not found: " + spellId));

        return toDetailResponse(spell);
    }

    public SpellApiDtos.SpellDetailResponseDto updateSpellFields(
            String spellId,
            SpellApiDtos.UpdateSpellFieldsRequestDto request
    ) {
        ensureEditingEnabled();
        validateRequiredText("spellId", spellId);
        if (request == null) {
            throw new SpellRequestValidationException("request body is required");
        }
        var fields = normalizeFieldsRequest(request.fields());
        var currentSpell = loadCurrentEffectiveSpell(spellId);
        checkConflict(request.expectedUpdatedAt(), currentSpell.updatedAt());
        if (request.reason() != null && request.reason().length() > 500) {
            throw new SpellEditValidationException("reason must not exceed 500 characters");
        }

        var overridesFile = readOverridesFile();
        var currentEntry = entryFor(overridesFile, spellId);
        var mergedFields = mergedFields(currentEntry);
        mergedFields.putAll(fields);
        if (isLocked(currentSpell) || isLocked(currentEntry)) {
            mergedFields.put("translationStatus", "LOCKED");
        } else {
            mergedFields.put("translationStatus", "MANUALLY_EDITED");
        }

        var updatedEntry = new SpellOverrideEntry(
                mergedFields,
                notesFor(currentEntry, currentSpell),
                Instant.now().toString(),
                reasonFor(request.reason(), currentEntry)
        );
        writeOverrideEntry(overridesFile, spellId, updatedEntry);
        refreshProjectionFromFiles();
        return getSpellDetail(spellId);
    }

    public SpellApiDtos.SpellDetailResponseDto updatePersonalNotes(
            String spellId,
            SpellApiDtos.UpdatePersonalNotesRequestDto request
    ) {
        ensureEditingEnabled();
        validateRequiredText("spellId", spellId);
        if (request == null) {
            throw new SpellRequestValidationException("request body is required");
        }
        if (request.personalNotes() == null) {
            throw new SpellRequestValidationException("personalNotes is required");
        }
        if (request.personalNotes().length() > 20_000) {
            throw new SpellEditValidationException("personalNotes must not exceed 20000 characters");
        }

        var currentSpell = loadCurrentEffectiveSpell(spellId);
        checkConflict(request.expectedUpdatedAt(), currentSpell.updatedAt());

        var overridesFile = readOverridesFile();
        var currentEntry = entryFor(overridesFile, spellId);
        var mergedFields = mergedFields(currentEntry);
        var updatedEntry = new SpellOverrideEntry(
                mergedFields.isEmpty() ? null : mergedFields,
                request.personalNotes(),
                Instant.now().toString(),
                currentEntry == null ? null : currentEntry.reason()
        );
        writeOverrideEntry(overridesFile, spellId, updatedEntry);
        refreshProjectionFromFiles();
        return getSpellDetail(spellId);
    }

    public SpellApiDtos.SpellDetailResponseDto updateTranslationStatus(
            String spellId,
            SpellApiDtos.UpdateTranslationStatusRequestDto request
    ) {
        ensureEditingEnabled();
        validateRequiredText("spellId", spellId);
        if (request == null) {
            throw new SpellRequestValidationException("request body is required");
        }
        validateTranslationStatus(request.translationStatus());
        if (request.reason() != null && request.reason().length() > 500) {
            throw new SpellEditValidationException("reason must not exceed 500 characters");
        }

        var currentSpell = loadCurrentEffectiveSpell(spellId);
        checkConflict(request.expectedUpdatedAt(), currentSpell.updatedAt());

        var overridesFile = readOverridesFile();
        var currentEntry = entryFor(overridesFile, spellId);
        LinkedHashMap<String, Object> mergedFields;
        if (isLocked(currentSpell) || "LOCKED".equals(request.translationStatus())) {
            mergedFields = materializeEditableFields(currentSpell);
        } else {
            mergedFields = mergedFields(currentEntry);
        }
        mergedFields.put("translationStatus", request.translationStatus());

        var updatedEntry = new SpellOverrideEntry(
                mergedFields,
                notesFor(currentEntry, currentSpell),
                Instant.now().toString(),
                reasonFor(request.reason(), currentEntry)
        );
        writeOverrideEntry(overridesFile, spellId, updatedEntry);
        refreshProjectionFromFiles();
        return getSpellDetail(spellId);
    }

    private SpellApiDtos.SpellDetailResponseDto toDetailResponse(Spell spell) {
        return new SpellApiDtos.SpellDetailResponseDto(
                spell.id(),
                spell.slug(),
                spell.nameEs(),
                spell.nameEn(),
                spell.school(),
                spell.subschool(),
                safeStrings(spell.descriptors()),
                spell.castingTime(),
                spell.components(),
                spell.range(),
                spell.target(),
                spell.effect(),
                spell.area(),
                spell.duration(),
                spell.savingThrow(),
                spell.spellResistance(),
                spell.descriptionEs(),
                spell.descriptionEn(),
                spell.personalNotes(),
                spell.translationStatus(),
                spell.lists().stream().map(this::toListEntryDto).toList(),
                new SpellApiDtos.SpellSourceDto(spell.sourceBook(), spell.sourcePage(), spell.sourceName()),
                DETAIL_EDITABLE_FIELDS,
                spell.updatedAt(),
                null
        );
    }

    private SpellApiDtos.SpellSearchResultDto toSearchResult(SpellCatalogSqliteRepository.SearchCandidate candidate, String query) {
        var spell = candidate.spell();
        var searchHit = locateMatch(spell, query);
        var selectedList = new SpellApiDtos.SpellSelectedListDto(
                candidate.selectedList().listType(),
                candidate.selectedList().listName(),
                candidate.selectedList().level()
        );
        return new SpellApiDtos.SpellSearchResultDto(
                spell.id(),
                spell.slug(),
                spell.nameEs(),
                selectedList,
                spell.school(),
                spell.subschool(),
                safeStrings(spell.descriptors()),
                spell.castingTime(),
                spell.components(),
                spell.range(),
                spell.target(),
                spell.effect(),
                spell.area(),
                spell.duration(),
                spell.savingThrow(),
                spell.spellResistance(),
                spell.descriptionEs(),
                spell.translationStatus(),
                searchHit == null ? null : searchHit.snippet(),
                searchHit == null ? null : searchHit.matchSource(),
                !isBlank(spell.personalNotes())
        );
    }

    private SpellApiDtos.SpellListEntryDto toListEntryDto(SpellListEntry entry) {
        return new SpellApiDtos.SpellListEntryDto(entry.listType(), entry.listName(), entry.level());
    }

    private void validateRequiredText(String field, String value) {
        if (isBlank(value)) {
            throw new SpellRequestValidationException(field + " is required");
        }
    }

    private void ensureEditingEnabled() {
        if (importService == null || overridesRepository == null || generatedPath == null || overridesPath == null) {
            throw new IllegalStateException("Editing is not configured");
        }
    }

    private Spell loadCurrentEffectiveSpell(String spellId) {
        var currentImport = importCurrentDataset();
        return currentImport.effectiveSpells().stream()
                .filter(spell -> spell.id().equals(spellId))
                .findFirst()
                .orElseThrow(() -> new SpellNotFoundException("Spell not found: " + spellId));
    }

    private SpellOverridesFile readOverridesFile() {
        try {
            return overridesRepository.read(overridesPath);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read overrides file", ex);
        }
    }

    private void writeOverrideEntry(SpellOverridesFile overridesFile, String spellId, SpellOverrideEntry entry) {
        try {
            var spells = new LinkedHashMap<String, SpellOverrideEntry>();
            if (overridesFile.spells() != null) {
                spells.putAll(overridesFile.spells());
            }
            spells.put(spellId, entry);
            overridesRepository.write(overridesPath, new SpellOverridesFile(1, Instant.now().toString(), spells));
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to persist overrides", ex);
        }
    }

    private void refreshProjectionFromFiles() {
        var importResult = importCurrentDataset();
        importResult.warnings().forEach(warning -> log.warn("{}", warning));
        repository.rebuild(importResult.effectiveSpells());
    }

    private com.grimoriopathfinder.dataset.SpellImportResult importCurrentDataset() {
        try {
            var result = importService.importDataset(generatedPath, overridesPath);
            result.warnings().forEach(warning -> log.warn("{}", warning));
            return result;
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to import dataset", ex);
        }
    }

    private void checkConflict(Instant expectedUpdatedAt, Instant actualUpdatedAt) {
        if (expectedUpdatedAt != null && !Objects.equals(expectedUpdatedAt, actualUpdatedAt)) {
            throw new SpellConflictException("The spell was updated by another operation");
        }
    }

    private void validateTranslationStatus(String translationStatus) {
        if (isBlank(translationStatus)) {
            throw new SpellRequestValidationException("translationStatus is required");
        }
        if (!TRANSLATION_STATUSES.contains(translationStatus)) {
            throw new SpellEditValidationException("translationStatus is not allowed in the MVP");
        }
    }

    private LinkedHashMap<String, Object> normalizeFieldsRequest(Map<String, Object> requestFields) {
        if (requestFields == null) {
            throw new SpellRequestValidationException("fields is required");
        }
        if (requestFields.isEmpty()) {
            throw new SpellRequestValidationException("fields must not be empty");
        }

        var normalized = new LinkedHashMap<String, Object>();
        for (var entry : requestFields.entrySet()) {
            var fieldName = entry.getKey();
            if (isBlank(fieldName)) {
                throw new SpellEditValidationException("fields contains an empty field name");
            }
            if (!ALLOWED_OVERRIDE_FIELDS.contains(fieldName)) {
                throw new SpellEditValidationException(fieldName + " is not editable in the MVP");
            }
            normalized.put(fieldName, normalizeEditableFieldValue(fieldName, entry.getValue()));
        }
        return normalized;
    }

    private Object normalizeEditableFieldValue(String fieldName, Object value) {
        if (value == null) {
            return null;
        }
        if ("descriptors".equals(fieldName)) {
            if (!(value instanceof List<?> descriptors)) {
                throw new SpellEditValidationException("descriptors must be an array of strings");
            }
            if (descriptors.size() > 50) {
                throw new SpellEditValidationException("descriptors must not exceed 50 items");
            }
            return descriptors.stream()
                    .map(descriptor -> {
                        if (descriptor == null) {
                            throw new SpellEditValidationException("descriptors must not contain null values");
                        }
                        var text = String.valueOf(descriptor);
                        if (text.length() > 100) {
                            throw new SpellEditValidationException("descriptors items must not exceed 100 characters");
                        }
                        return text;
                    })
                    .toList();
        }

        var text = String.valueOf(value);
        var maxLength = maxLengthForField(fieldName);
        if (text.length() > maxLength) {
            throw new SpellEditValidationException(fieldName + " must not exceed " + maxLength + " characters");
        }
        return text;
    }

    private int maxLengthForField(String fieldName) {
        if ("nameEs".equals(fieldName)) {
            return 300;
        }
        if ("descriptionEs".equals(fieldName)) {
            return 100_000;
        }
        return 1_000;
    }

    private String notesFor(SpellOverrideEntry currentEntry, Spell currentSpell) {
        if (currentEntry != null && currentEntry.personalNotes() != null) {
            return currentEntry.personalNotes();
        }
        return currentSpell.personalNotes();
    }

    private String reasonFor(String requestReason, SpellOverrideEntry currentEntry) {
        if (!isBlank(requestReason)) {
            return requestReason;
        }
        return currentEntry == null ? null : currentEntry.reason();
    }

    private SpellOverrideEntry entryFor(SpellOverridesFile overridesFile, String spellId) {
        if (overridesFile.spells() == null) {
            return null;
        }
        return overridesFile.spells().get(spellId);
    }

    private LinkedHashMap<String, Object> mergedFields(SpellOverrideEntry currentEntry) {
        var fields = new LinkedHashMap<String, Object>();
        if (currentEntry != null && currentEntry.fields() != null) {
            fields.putAll(currentEntry.fields());
        }
        return fields;
    }

    private boolean isLocked(Spell spell) {
        return spell != null && "LOCKED".equals(spell.translationStatus());
    }

    private boolean isLocked(SpellOverrideEntry entry) {
        if (entry == null || entry.fields() == null) {
            return false;
        }
        return "LOCKED".equals(String.valueOf(entry.fields().get("translationStatus")));
    }

    private LinkedHashMap<String, Object> materializeEditableFields(Spell spell) {
        var fields = new LinkedHashMap<String, Object>();
        fields.put("nameEs", spell.nameEs());
        fields.put("school", spell.school());
        fields.put("subschool", spell.subschool());
        fields.put("descriptors", safeStrings(spell.descriptors()));
        fields.put("castingTime", spell.castingTime());
        fields.put("components", spell.components());
        fields.put("range", spell.range());
        fields.put("target", spell.target());
        fields.put("effect", spell.effect());
        fields.put("area", spell.area());
        fields.put("duration", spell.duration());
        fields.put("savingThrow", spell.savingThrow());
        fields.put("spellResistance", spell.spellResistance());
        fields.put("descriptionEs", spell.descriptionEs());
        return fields;
    }

    private boolean matchesQuery(Spell spell, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }

        var queryNormalized = normalize(query);
        if (query.contains("\"")) {
            var phrase = normalize(query.replace("\"", ""));
            return !phrase.isBlank() && searchableFields(spell).values().stream()
                    .map(this::normalize)
                    .anyMatch(candidate -> candidate.contains(phrase));
        }

        var terms = tokenize(queryNormalized);
        return terms.stream().allMatch(term -> searchableFields(spell).values().stream()
                .filter(Objects::nonNull)
                .anyMatch(field -> tokenMatches(field, term)));
    }

    private SearchHit locateMatch(Spell spell, String query) {
        if (query == null || query.isBlank()) {
            return null;
        }

        var fields = searchableFields(spell);
        if (query.contains("\"")) {
            var phrase = normalize(query.replace("\"", ""));
            if (phrase.isBlank()) {
                return null;
            }
            for (var entry : fields.entrySet()) {
                var normalizedField = normalize(entry.getValue());
                if (!normalizedField.isBlank() && normalizedField.contains(phrase)) {
                    return new SearchHit(entry.getKey(), snippet(entry.getValue()));
                }
            }
            return null;
        }

        var terms = tokenize(normalize(query));
        for (var entry : fields.entrySet()) {
            var normalizedField = normalize(entry.getValue());
            if (normalizedField.isBlank()) {
                continue;
            }
            for (var term : terms) {
                if (tokenMatches(normalizedField, term)) {
                    return new SearchHit(entry.getKey(), snippet(entry.getValue()));
                }
            }
        }
        return null;
    }

    private boolean tokenMatches(String fieldValue, String term) {
        var normalizedField = normalize(fieldValue);
        if (normalizedField.isBlank()) {
            return false;
        }
        for (var token : tokenize(normalizedField)) {
            if (term.length() >= 3) {
                if (token.startsWith(term)) {
                    return true;
                }
            } else if (token.equals(term)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, String> searchableFields(Spell spell) {
        var fields = new LinkedHashMap<String, String>();
        fields.put("nameEs", spell.nameEs());
        fields.put("descriptionEs", spell.descriptionEs());
        fields.put("school", spell.school());
        fields.put("subschool", spell.subschool());
        fields.put("descriptors", spell.descriptors() == null ? null : String.join(" ", spell.descriptors()));
        fields.put("castingTime", spell.castingTime());
        fields.put("components", spell.components());
        fields.put("range", spell.range());
        fields.put("target", spell.target());
        fields.put("effect", spell.effect());
        fields.put("area", spell.area());
        fields.put("duration", spell.duration());
        fields.put("savingThrow", spell.savingThrow());
        fields.put("spellResistance", spell.spellResistance());
        fields.put("personalNotes", spell.personalNotes());
        return fields;
    }

    private List<String> tokenize(String normalizedText) {
        if (normalizedText == null || normalizedText.isBlank()) {
            return List.of();
        }
        return List.of(normalizedText.split(" ")).stream()
                .filter(token -> !token.isBlank())
                .toList();
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

    private String snippet(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        var trimmed = value.trim().replaceAll("\\s+", " ");
        return trimmed.length() <= 140 ? trimmed : trimmed.substring(0, 137).trim() + "...";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private List<String> safeStrings(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static final Comparator<SpellCatalogSqliteRepository.SearchCandidate> SEARCH_COMPARATOR =
            Comparator.comparingInt((SpellCatalogSqliteRepository.SearchCandidate candidate) -> candidate.selectedList().level())
                    .thenComparing(candidate -> normalizeForOrdering(candidate.spell().nameEs()))
                    .thenComparing(candidate -> candidate.spell().id());

    private static String normalizeForOrdering(String input) {
        if (input == null) {
            return "";
        }
        var normalized = Normalizer.normalize(input, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        normalized = normalized.toLowerCase(Locale.ROOT);
        normalized = NON_WORDS.matcher(normalized).replaceAll(" ");
        return normalized.trim().replaceAll("\\s+", " ");
    }

    private record SearchHit(String matchSource, String snippet) {
    }
}
