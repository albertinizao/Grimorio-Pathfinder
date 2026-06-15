package com.grimoriopathfinder.dataset;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class SpellDatasetMagusCorrectionRegressionTest {

    private static final Set<String> EXPECTED_MAGO_ONLY_SPELLS = Set.of(
            "Lucubración del Mago",
            "Potenciador Mnemotécnico",
            "Transcripción sanguínea",
            "La meditación del caminante de fuego",
            "Rito de Mente Centrada",
            "Ver más allá",
            "Vínculos espirituales",
            "Visualización del cuerpo",
            "Visualización de la mente"
    );

    @Test
    void onlyTheExplicitWizardExceptionsRemainAsMagoOnlySpells() throws Exception {
        var repository = new SpellDatasetJsonRepository();
        var dataset = repository.read(Path.of("data/generated/spells-es.generated.json"));

        var actual = dataset.spells().stream()
                .filter(spell -> spell.lists().stream().anyMatch(entry -> "CLASS".equals(entry.listType()) && "Mago".equals(entry.listName())))
                .filter(spell -> spell.lists().stream().noneMatch(entry -> "CLASS".equals(entry.listType()) && "Hechicero/Mago".equals(entry.listName())))
                .map(spell -> spell.nameEs())
                .collect(Collectors.toSet());

        assertThat(actual).isEqualTo(EXPECTED_MAGO_ONLY_SPELLS);
    }
}
