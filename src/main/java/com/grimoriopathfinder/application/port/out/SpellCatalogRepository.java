package com.grimoriopathfinder.application.port.out;

import com.grimoriopathfinder.spells.Spell;
import com.grimoriopathfinder.spells.SpellListEntry;
import java.util.List;
import java.util.Optional;

public interface SpellCatalogRepository {
    void rebuild(List<Spell> spells);
    List<ListSummary> listSpellLists(String listType);
    Optional<ListLevels> getSpellListLevels(String listType, String listName);
    List<SearchCandidate> findCandidates(String listType, String listName, int level, boolean exactLevel);
    Optional<Spell> findSpellById(String spellId);

    record ListSummary(String listType, String listName, int minLevel, int maxLevel, List<Integer> levels, int spellCount) {}
    record ListLevels(String listType, String listName, int minLevel, int maxLevel, List<Integer> levels) {}
    record SearchCandidate(Spell spell, SpellListEntry selectedList) {}
}
