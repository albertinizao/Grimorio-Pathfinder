package com.grimoriopathfinder.web.dto;

import java.time.Instant;
import java.util.List;

public final class SpellApiDtos {

    private SpellApiDtos() {
    }

    public record SpellListsResponseDto(List<SpellListSummaryDto> items) {
    }

    public record SpellListSummaryDto(
            String listType,
            String listName,
            int minLevel,
            int maxLevel,
            List<Integer> levels,
            int spellCount
    ) {
    }

    public record SpellListLevelsResponseDto(
            String listType,
            String listName,
            int minLevel,
            int maxLevel,
            List<Integer> levels
    ) {
    }

    public record SpellSearchFiltersDto(
            String listType,
            String listName,
            int maxLevel,
            String q
    ) {
    }

    public record SpellSearchPageDto(
            int page,
            int size,
            int totalItems,
            int totalPages,
            boolean hasNext
    ) {
    }

    public record SpellSelectedListDto(
            String listType,
            String listName,
            int level
    ) {
    }

    public record SpellSearchResultDto(
            String spellId,
            String slug,
            String nameEs,
            SpellSelectedListDto selectedList,
            String school,
            List<String> descriptors,
            String castingTime,
            String range,
            String savingThrow,
            String spellResistance,
            String translationStatus,
            String snippet,
            String matchSource,
            boolean hasPersonalNotes
    ) {
    }

    public record SpellSearchResponseDto(
            SpellSearchFiltersDto filters,
            SpellSearchPageDto page,
            String sort,
            List<SpellSearchResultDto> results
    ) {
    }

    public record SpellListEntryDto(
            String listType,
            String listName,
            int level
    ) {
    }

    public record SpellSourceDto(
            String sourceBook,
            Integer sourcePage,
            String sourceName
    ) {
    }

    public record SpellDetailResponseDto(
            String spellId,
            String slug,
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
            String personalNotes,
            String translationStatus,
            List<SpellListEntryDto> lists,
            SpellSourceDto source,
            List<String> editableFields,
            Instant updatedAt,
            Instant reviewedAt
    ) {
    }
}
