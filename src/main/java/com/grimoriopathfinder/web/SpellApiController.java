package com.grimoriopathfinder.web;

import com.grimoriopathfinder.catalog.SpellCatalogService;
import com.grimoriopathfinder.web.dto.SpellApiDtos;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SpellApiController {

    private final SpellCatalogService catalogService;

    public SpellApiController(SpellCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/spell-lists")
    public SpellApiDtos.SpellListsResponseDto listSpellLists(
            @RequestParam(required = false) String listType
    ) {
        return catalogService.listSpellLists(listType);
    }

    @GetMapping("/spell-lists/levels")
    public SpellApiDtos.SpellListLevelsResponseDto listSpellLevels(
            @RequestParam String listType,
            @RequestParam String listName
    ) {
        return catalogService.getSpellListLevels(listType, listName);
    }

    @GetMapping("/spells/search")
    public SpellApiDtos.SpellSearchResponseDto searchSpells(
            @RequestParam String listType,
            @RequestParam String listName,
            @RequestParam int maxLevel,
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return catalogService.searchSpells(listType, listName, maxLevel, q, page, size);
    }

    @GetMapping("/spells/{spellId}")
    public SpellApiDtos.SpellDetailResponseDto getSpellDetail(@PathVariable String spellId) {
        return catalogService.getSpellDetail(spellId);
    }
}
