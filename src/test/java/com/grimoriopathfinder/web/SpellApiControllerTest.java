package com.grimoriopathfinder.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "grimorio.dataset.generated-path=src/test/resources/data/generated/spells-es.generated.json",
        "grimorio.dataset.overrides-path=src/test/resources/data/overrides/spells-es.overrides.json"
})
@AutoConfigureMockMvc
class SpellApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsSpellListsAndOrdersThemByTypeThenNormalizedName() throws Exception {
        mockMvc.perform(get("/api/spell-lists"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].listType").value("CLASS"))
                .andExpect(jsonPath("$.items[0].listName").value("Clérigo"))
                .andExpect(jsonPath("$.items[0].spellCount").value(2))
                .andExpect(jsonPath("$.items[1].listName").value("Druida"));
    }

    @Test
    void exposesLevelsForAConcreteSpellList() throws Exception {
        mockMvc.perform(get("/api/spell-lists/levels")
                        .param("listType", "CLASS")
                        .param("listName", "Clérigo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.levels").value(org.hamcrest.Matchers.contains(3, 4)))
                .andExpect(jsonPath("$.minLevel").value(3))
                .andExpect(jsonPath("$.maxLevel").value(4));
    }

    @Test
    void searchesOnlyInSpanishEffectiveFieldsAndPersonalNotes() throws Exception {
        mockMvc.perform(get("/api/spells/search")
                        .param("listType", "CLASS")
                        .param("listName", "Clérigo")
                        .param("maxLevel", "3")
                        .param("q", "drow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.length()").value(1))
                .andExpect(jsonPath("$.results[0].spellId").value("delay-poison"))
                .andExpect(jsonPath("$.results[0].matchSource").value("personalNotes"))
                .andExpect(jsonPath("$.results[0].hasPersonalNotes").value(true))
                .andExpect(jsonPath("$.results[0].translationStatus").value("AI_TRANSLATED"));
    }

    @Test
    void returnsEffectiveDetailWithReferenceTextAndNoLockedFlag() throws Exception {
        mockMvc.perform(get("/api/spells/neutralize-poison"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.spellId").value("neutralize-poison"))
                .andExpect(jsonPath("$.nameEs").value("Neutralizar veneno"))
                .andExpect(jsonPath("$.nameEn").value("Neutralize Poison"))
                .andExpect(jsonPath("$.personalNotes").value("Traducción revisada y cerrada."))
                .andExpect(jsonPath("$.translationStatus").value("LOCKED"))
                .andExpect(jsonPath("$.locked").doesNotExist())
                .andExpect(jsonPath("$.source.sourceBook").value("Core Rulebook"))
                .andExpect(jsonPath("$.editableFields[0]").value("nameEs"));
    }

    @Test
    void returnsUnprocessableEntityWhenTheListDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/spell-lists/levels")
                        .param("listType", "CLASS")
                        .param("listName", "Inexistente"))
                .andExpect(status().isUnprocessableEntity());
    }
}
