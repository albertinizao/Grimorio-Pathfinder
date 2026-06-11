package com.grimoriopathfinder.catalog;

import com.grimoriopathfinder.sqlite.SpellCatalogSqliteRepository;
import com.grimoriopathfinder.spells.Spell;
import com.grimoriopathfinder.spells.SpellListEntry;
import com.grimoriopathfinder.web.dto.SpellApiDtos;
import com.grimoriopathfinder.web.error.SpellListNotFoundException;
import com.grimoriopathfinder.web.error.SpellNotFoundException;
import com.grimoriopathfinder.web.error.SpellQueryValidationException;
import com.grimoriopathfinder.web.error.SpellRequestValidationException;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class SpellCatalogService {

    private static final Pattern NON_WORDS = Pattern.compile("[^\\p{L}\\p{Nd}]+");
    private static final List<String> EDITABLE_FIELDS = List.of(
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

    public SpellCatalogService(SpellCatalogSqliteRepository repository) {
        this.repository = repository;
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
                EDITABLE_FIELDS,
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
                safeStrings(spell.descriptors()),
                spell.castingTime(),
                spell.range(),
                spell.savingThrow(),
                spell.spellResistance(),
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
