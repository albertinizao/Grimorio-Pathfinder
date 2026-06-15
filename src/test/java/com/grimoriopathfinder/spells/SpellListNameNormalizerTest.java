package com.grimoriopathfinder.spells;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class SpellListNameNormalizerTest {

    @Test
    void splitsCombinedClassListsIntoDistinctCanonicalEntries() {
        var entries = List.of(
                new SpellListEntry("spell-1", "CLASS", "Hechicero/Mago", 2),
                new SpellListEntry("spell-1", "CLASS", "Clérigo/Oráculo", 3),
                new SpellListEntry("spell-1", "CLASS", "Invocador/Invocador Desencadenado", 4)
        );

        var normalized = SpellListNameNormalizer.normalizeClassEntries(entries);

        assertThat(normalized).extracting(SpellListEntry::listName).containsExactly(
                "Hechicero",
                "Mago",
                "Clérigo",
                "Oráculo",
                "Invocador",
                "Invocador Desencadenado"
        );
    }

    @Test
    void canonicalizesClassAliasesUsingProjectRules() {
        var entries = List.of(
                new SpellListEntry("spell-1", "CLASS", "Alchemista", 1),
                new SpellListEntry("spell-1", "CLASS", "Antipladin", 1),
                new SpellListEntry("spell-1", "CLASS", "Bloodrager", 1),
                new SpellListEntry("spell-1", "CLASS", "Clériga", 1),
                new SpellListEntry("spell-1", "CLASS", "Escalda", 1),
                new SpellListEntry("spell-1", "CLASS", "Espiritista", 1),
                new SpellListEntry("spell-1", "CLASS", "Guardabosques", 1),
                new SpellListEntry("spell-1", "CLASS", "Hipnotizador", 1),
                new SpellListEntry("spell-1", "CLASS", "MagusUM", 1),
                new SpellListEntry("spell-1", "CLASS", "Maga", 1),
                new SpellListEntry("spell-1", "CLASS", "Mesmerist", 1),
                new SpellListEntry("spell-1", "CLASS", "Medio", 1),
                new SpellListEntry("spell-1", "CLASS", "Psíquica", 1),
                new SpellListEntry("spell-1", "CLASS", "Ranger", 1),
                new SpellListEntry("spell-1", "CLASS", "Sangrienta", 1),
                new SpellListEntry("spell-1", "CLASS", "Sangriento", 1)
        );

        var normalized = SpellListNameNormalizer.normalizeClassEntries(entries);

        assertThat(normalized).extracting(SpellListEntry::listName).containsExactly(
                "Alquimista",
                "Antipaladín",
                "Bruto de sangre",
                "Clérigo",
                "Escaldo",
                "Espiritualista",
                "Explorador",
                "Hipnotista",
                "Magus",
                "Mago",
                "Mesmerista",
                "Medium",
                "Psíquico"
        );
    }
}
