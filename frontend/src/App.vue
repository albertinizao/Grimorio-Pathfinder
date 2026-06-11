<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue";
import {
  fetchSpellDetail,
  fetchSpellLists,
  formatDescriptors,
  formatTranslationStatus,
  fromDescriptorsInput,
  searchSpells,
  toDescriptorsInput,
  updatePersonalNotes,
  updateSpellFields,
  updateTranslationStatus,
} from "./lib/spellApi";
import type {
  SpellDetail,
  SpellListSummary,
  SpellSearchResult,
  TranslationStatus,
} from "./types/spell";

const listType = "CLASS";

const spellLists = ref<SpellListSummary[]>([]);
const searchResults = ref<SpellSearchResult[]>([]);
const selectedSpell = ref<SpellDetail | null>(null);
const errorMessage = ref<string | null>(null);
const infoMessage = ref<string | null>(null);
const searchBusy = ref(false);
const detailBusy = ref(false);
let infoMessageTimeoutId: ReturnType<typeof window.setTimeout> | null = null;

const searchForm = reactive({
  listType,
  listName: "Clérigo",
  maxLevel: 3,
  q: "",
});

const editableFieldNames = [
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
] as const;

type EditableFieldName = (typeof editableFieldNames)[number];
const translationStatuses: TranslationStatus[] = [
  "NOT_TRANSLATED",
  "AI_TRANSLATED",
  "REVIEW_REQUIRED",
  "REVIEWED",
  "MANUALLY_EDITED",
  "LOCKED",
];

const fieldDrafts = reactive<Record<EditableFieldName, string>>({
  nameEs: "",
  school: "",
  subschool: "",
  descriptors: "",
  castingTime: "",
  components: "",
  range: "",
  target: "",
  effect: "",
  area: "",
  duration: "",
  savingThrow: "",
  spellResistance: "",
  descriptionEs: "",
});

const personalNotesDraft = ref("");
const translationStatusDraft = ref<TranslationStatus>("AI_TRANSLATED");
const detailReasonDraft = ref("");
const hasLoadedSpell = ref(false);

const availableLevels = computed<number[]>(() => {
  const current = spellLists.value.find((item) => item.listType === searchForm.listType && item.listName === searchForm.listName);
  return current?.levels ?? [];
});

type SearchResultField = {
  label: string;
  value: string;
  wide?: boolean;
};

function formatSearchValue(value: string | null | undefined): string {
  return value && value.trim().length > 0 ? value : "—";
}

function getSearchResultFields(result: SpellSearchResult): SearchResultField[] {
  return [
    { label: "Escuela", value: formatSearchValue(result.school) },
    { label: "Subescuela", value: formatSearchValue(result.subschool) },
    { label: "Descriptores", value: formatSearchValue(formatDescriptors(result.descriptors)) },
    { label: "Tiempo de lanzamiento", value: formatSearchValue(result.castingTime) },
    { label: "Componentes", value: formatSearchValue(result.components) },
    { label: "Alcance", value: formatSearchValue(result.range) },
    { label: "Objetivo", value: formatSearchValue(result.target) },
    { label: "Efecto", value: formatSearchValue(result.effect) },
    { label: "Área", value: formatSearchValue(result.area) },
    { label: "Duración", value: formatSearchValue(result.duration) },
    { label: "TS", value: formatSearchValue(result.savingThrow) },
    { label: "RC", value: formatSearchValue(result.spellResistance) },
    { label: "Descripción española", value: formatSearchValue(result.descriptionEs), wide: true },
  ];
}

function setMessage(kind: "error" | "info", message: string | null): void {
  if (kind === "error") {
    errorMessage.value = message;
    if (message) {
      if (infoMessageTimeoutId !== null) {
        window.clearTimeout(infoMessageTimeoutId);
        infoMessageTimeoutId = null;
      }
      infoMessage.value = null;
    }
    return;
  }
  if (infoMessageTimeoutId !== null) {
    window.clearTimeout(infoMessageTimeoutId);
    infoMessageTimeoutId = null;
  }
  infoMessage.value = message;
  if (message) {
    errorMessage.value = null;
    infoMessageTimeoutId = window.setTimeout(() => {
      infoMessage.value = null;
      infoMessageTimeoutId = null;
    }, 10_000);
  }
}

function syncDraftsFromDetail(detail: SpellDetail | null): void {
  fieldDrafts.nameEs = detail?.nameEs ?? "";
  fieldDrafts.school = detail?.school ?? "";
  fieldDrafts.subschool = detail?.subschool ?? "";
  fieldDrafts.descriptors = toDescriptorsInput(detail?.descriptors ?? []);
  fieldDrafts.castingTime = detail?.castingTime ?? "";
  fieldDrafts.components = detail?.components ?? "";
  fieldDrafts.range = detail?.range ?? "";
  fieldDrafts.target = detail?.target ?? "";
  fieldDrafts.effect = detail?.effect ?? "";
  fieldDrafts.area = detail?.area ?? "";
  fieldDrafts.duration = detail?.duration ?? "";
  fieldDrafts.savingThrow = detail?.savingThrow ?? "";
  fieldDrafts.spellResistance = detail?.spellResistance ?? "";
  fieldDrafts.descriptionEs = detail?.descriptionEs ?? "";
  personalNotesDraft.value = detail?.personalNotes ?? "";
  translationStatusDraft.value = detail?.translationStatus ?? "AI_TRANSLATED";
  detailReasonDraft.value = "";
}

function buildFieldPatch(): Record<string, string | string[] | null> {
  return {
    nameEs: fieldDrafts.nameEs,
    school: fieldDrafts.school,
    subschool: fieldDrafts.subschool,
    descriptors: fromDescriptorsInput(fieldDrafts.descriptors),
    castingTime: fieldDrafts.castingTime,
    components: fieldDrafts.components,
    range: fieldDrafts.range,
    target: fieldDrafts.target,
    effect: fieldDrafts.effect,
    area: fieldDrafts.area,
    duration: fieldDrafts.duration,
    savingThrow: fieldDrafts.savingThrow,
    spellResistance: fieldDrafts.spellResistance,
    descriptionEs: fieldDrafts.descriptionEs,
  };
}

function mergeUpdatedSpell(updated: SpellDetail): void {
  selectedSpell.value = updated;
  syncDraftsFromDetail(updated);
  searchResults.value = searchResults.value.map((result) =>
    result.spellId === updated.spellId
          ? {
          ...result,
          nameEs: updated.nameEs ?? "",
          school: updated.school,
          subschool: updated.subschool,
          descriptors: updated.descriptors,
          castingTime: updated.castingTime,
          components: updated.components,
          range: updated.range,
          target: updated.target,
          effect: updated.effect,
          area: updated.area,
          duration: updated.duration,
          savingThrow: updated.savingThrow,
          spellResistance: updated.spellResistance,
          descriptionEs: updated.descriptionEs,
          translationStatus: updated.translationStatus,
          snippet: updated.descriptionEs ? updated.descriptionEs.slice(0, 140) : result.snippet,
          hasPersonalNotes: Boolean(updated.personalNotes && updated.personalNotes.trim().length > 0),
        }
      : result,
  );
}

async function loadListsAndSearch(): Promise<void> {
  try {
    setMessage("error", null);
    searchBusy.value = true;
    const lists = await fetchSpellLists(listType);
    spellLists.value = lists;
    if (lists.length > 0 && !lists.some((item) => item.listName === searchForm.listName)) {
      searchForm.listName = lists[0].listName;
      searchForm.maxLevel = lists[0].levels[lists[0].levels.length - 1] ?? 0;
    }
    if (availableLevels.value.length > 0 && !availableLevels.value.includes(searchForm.maxLevel)) {
      searchForm.maxLevel = availableLevels.value[availableLevels.value.length - 1] ?? 0;
    }
    await runSearch();
  } catch (error) {
    setMessage("error", toReadableError(error));
  } finally {
    searchBusy.value = false;
  }
}

async function runSearch(): Promise<void> {
  try {
    setMessage("error", null);
    searchBusy.value = true;
    const response = await searchSpells(searchForm);
    searchResults.value = response.results;
    setMessage(
      "info",
      response.results.length === 1
        ? "1 conjuro encontrado."
        : response.results.length > 1
          ? `${response.results.length} conjuros encontrados.`
          : "No hay conjuros para este filtro.",
    );
    if (selectedSpell.value) {
      const stillVisible = response.results.some((result) => result.spellId === selectedSpell.value?.spellId);
      if (!stillVisible) {
        selectedSpell.value = null;
        syncDraftsFromDetail(null);
      }
    }
  } catch (error) {
    setMessage("error", toReadableError(error));
  } finally {
    searchBusy.value = false;
  }
}

async function openSpell(spellId: string): Promise<void> {
  try {
    setMessage("error", null);
    detailBusy.value = true;
    const detail = await fetchSpellDetail(spellId);
    selectedSpell.value = detail;
    syncDraftsFromDetail(detail);
    hasLoadedSpell.value = true;
  } catch (error) {
    setMessage("error", toReadableError(error));
  } finally {
    detailBusy.value = false;
  }
}

async function saveFields(): Promise<void> {
  if (!selectedSpell.value) {
    return;
  }
  try {
    setMessage("error", null);
    detailBusy.value = true;
    const updated = await updateSpellFields(selectedSpell.value.spellId, {
      fields: buildFieldPatch(),
      expectedUpdatedAt: selectedSpell.value.updatedAt,
      reason: detailReasonDraft.value || null,
    });
    mergeUpdatedSpell(updated);
    await runSearch();
    setMessage("info", "Campos españoles guardados.");
  } catch (error) {
    setMessage("error", toReadableError(error));
  } finally {
    detailBusy.value = false;
  }
}

async function saveNotes(): Promise<void> {
  if (!selectedSpell.value) {
    return;
  }
  try {
    setMessage("error", null);
    detailBusy.value = true;
    const updated = await updatePersonalNotes(selectedSpell.value.spellId, {
      personalNotes: personalNotesDraft.value,
      expectedUpdatedAt: selectedSpell.value.updatedAt,
    });
    mergeUpdatedSpell(updated);
    await runSearch();
    setMessage("info", "Notas personales guardadas.");
  } catch (error) {
    setMessage("error", toReadableError(error));
  } finally {
    detailBusy.value = false;
  }
}

async function saveTranslationStatus(): Promise<void> {
  if (!selectedSpell.value) {
    return;
  }
  try {
    setMessage("error", null);
    detailBusy.value = true;
    const updated = await updateTranslationStatus(selectedSpell.value.spellId, {
      translationStatus: translationStatusDraft.value,
      expectedUpdatedAt: selectedSpell.value.updatedAt,
      reason: detailReasonDraft.value || null,
    });
    mergeUpdatedSpell(updated);
    await runSearch();
    setMessage("info", "Estado actualizado.");
  } catch (error) {
    setMessage("error", toReadableError(error));
  } finally {
    detailBusy.value = false;
  }
}

function onListNameChanged(): void {
  const current = spellLists.value.find((item) => item.listType === searchForm.listType && item.listName === searchForm.listName);
  if (current) {
    searchForm.maxLevel = current.levels[current.levels.length - 1] ?? 0;
  }
}

function goBackToResults(): void {
  selectedSpell.value = null;
  syncDraftsFromDetail(null);
}

function toReadableError(error: unknown): string {
  if (error instanceof Error) {
    const status = (error as Error & { status?: number }).status;
    if (status) {
      return `Error ${status}: ${error.message}`;
    }
    return error.message;
  }
  return "Se produjo un error inesperado.";
}

watch(
  () => searchForm.listName,
  () => onListNameChanged(),
);

onMounted(() => {
  void loadListsAndSearch();
});

onBeforeUnmount(() => {
  if (infoMessageTimeoutId !== null) {
    window.clearTimeout(infoMessageTimeoutId);
    infoMessageTimeoutId = null;
  }
});
</script>

<template>
  <main class="app-shell">
    <header class="hero">
      <div>
        <p class="eyebrow">Grimorio Pathfinder</p>
        <h1>Edición local de conjuros</h1>
      </div>
    </header>

    <section v-if="errorMessage" class="banner banner-error" role="alert">
      {{ errorMessage }}
    </section>
    <section v-if="infoMessage" class="toast toast-info" role="status" aria-live="polite">
      {{ infoMessage }}
    </section>

    <section class="layout">
      <article class="panel search-panel">
        <div class="panel-header">
          <div>
            <p class="eyebrow">Búsqueda</p>
            <h2>Lista de clase lanzadora</h2>
          </div>
          <button class="ghost-button" type="button" @click="goBackToResults" :disabled="!selectedSpell">
            Volver a resultados
          </button>
        </div>

        <form class="search-form" @submit.prevent="runSearch">
          <label>
            <span>Lista de clase lanzadora</span>
            <select v-model="searchForm.listName" :disabled="searchBusy">
              <option v-for="list in spellLists" :key="list.listName" :value="list.listName">
                {{ list.listName }} ({{ list.minLevel }}-{{ list.maxLevel }})
              </option>
            </select>
          </label>

          <label>
            <span>Nivel máximo</span>
            <select v-model.number="searchForm.maxLevel" :disabled="searchBusy">
              <option v-for="level in availableLevels" :key="level" :value="level">
                {{ level }}
              </option>
            </select>
          </label>

          <label>
            <span>Término o frase</span>
            <input
              v-model="searchForm.q"
              type="text"
              placeholder="veneno, drow, &quot;daño de fuego&quot;"
              :disabled="searchBusy"
            />
          </label>

          <div class="search-actions">
            <button class="primary-button" type="submit" :disabled="searchBusy">
              {{ searchBusy ? "Buscando..." : "Buscar" }}
            </button>
            <button
              class="secondary-button"
              type="button"
              :disabled="searchBusy"
              @click="
                searchForm.q = '';
                void runSearch();
              "
            >
              Limpiar texto
            </button>
          </div>
        </form>
        <div class="results-list">
          <article
            v-for="result in searchResults"
            :key="result.spellId"
            class="result-card"
            :class="{ active: selectedSpell?.spellId === result.spellId }"
          >
            <div class="result-topline">
              <div class="result-title">
                <strong>{{ result.nameEs }}</strong>
                <span class="result-level">Lv {{ result.selectedList.level }}</span>
              </div>
              <button class="edit-button" type="button" @click="openSpell(result.spellId)">
                Editar
              </button>
            </div>
            <dl class="result-fields">
              <div
                v-for="field in getSearchResultFields(result)"
                :key="field.label"
                class="result-field"
                :class="{ 'result-field--wide': field.wide }"
              >
                <dt>{{ field.label }}</dt>
                <dd>{{ field.value }}</dd>
              </div>
            </dl>
          </article>
        </div>
      </article>

      <article class="panel detail-panel">
        <div class="panel-header">
          <div>
            <p class="eyebrow">Edición</p>
            <h2 v-if="selectedSpell">{{ selectedSpell.nameEs || "Conjuro" }}</h2>
            <h2 v-else>Haz clic en un conjuro</h2>
          </div>
          <span class="muted" v-if="selectedSpell">
            {{ formatTranslationStatus(selectedSpell.translationStatus) }}
          </span>
        </div>

        <div v-if="!selectedSpell" class="empty-state">
          <p>Haz clic en un resultado para editar sus campos españoles, notas o estado.</p>
        </div>

        <div v-else class="detail-grid">
          <section class="detail-section">
            <div class="section-header">
              <h3>Campos españoles editables</h3>
              <button class="primary-button" type="button" @click="saveFields" :disabled="detailBusy">
                Guardar campos
              </button>
            </div>

            <div class="fields-grid">
              <label>
                <span>Nombre español</span>
                <input v-model="fieldDrafts.nameEs" type="text" />
              </label>
              <label>
                <span>Escuela</span>
                <input v-model="fieldDrafts.school" type="text" />
              </label>
              <label>
                <span>Subescuela</span>
                <input v-model="fieldDrafts.subschool" type="text" />
              </label>
              <label>
                <span>Descriptores</span>
                <input v-model="fieldDrafts.descriptors" type="text" placeholder="fuego, luz, miedo" />
              </label>
              <label>
                <span>Tiempo de lanzamiento</span>
                <input v-model="fieldDrafts.castingTime" type="text" />
              </label>
              <label>
                <span>Componentes</span>
                <input v-model="fieldDrafts.components" type="text" />
              </label>
              <label>
                <span>Alcance</span>
                <input v-model="fieldDrafts.range" type="text" />
              </label>
              <label>
                <span>Objetivo</span>
                <input v-model="fieldDrafts.target" type="text" />
              </label>
              <label>
                <span>Efecto</span>
                <input v-model="fieldDrafts.effect" type="text" />
              </label>
              <label>
                <span>Área</span>
                <input v-model="fieldDrafts.area" type="text" />
              </label>
              <label>
                <span>Duración</span>
                <input v-model="fieldDrafts.duration" type="text" />
              </label>
              <label>
                <span>Tirada de salvación</span>
                <input v-model="fieldDrafts.savingThrow" type="text" />
              </label>
              <label>
                <span>Resistencia a conjuros</span>
                <input v-model="fieldDrafts.spellResistance" type="text" />
              </label>
              <label class="textarea-field">
                <span>Descripción española</span>
                <textarea v-model="fieldDrafts.descriptionEs" rows="7"></textarea>
              </label>
            </div>
          </section>

          <section class="detail-section">
            <div class="section-header">
              <h3>Notas personales</h3>
              <button class="secondary-button" type="button" @click="saveNotes" :disabled="detailBusy">
                Guardar notas
              </button>
            </div>
            <label class="textarea-field">
              <span>personalNotes</span>
              <textarea v-model="personalNotesDraft" rows="6" placeholder="Muy útil contra drow..."></textarea>
            </label>
          </section>

          <section class="detail-section">
            <div class="section-header">
              <h3>Estado de traducción</h3>
              <button class="secondary-button" type="button" @click="saveTranslationStatus" :disabled="detailBusy">
                Guardar estado
              </button>
            </div>
            <label>
              <span>translationStatus</span>
              <select v-model="translationStatusDraft">
                <option v-for="status in translationStatuses" :key="status" :value="status">
                  {{ formatTranslationStatus(status) }}
                </option>
              </select>
            </label>
            <label class="textarea-field">
              <span>Motivo / razón</span>
              <textarea v-model="detailReasonDraft" rows="3" placeholder="Corrección de mesa, bloqueo, etc."></textarea>
            </label>
          </section>

        </div>

        <section v-if="hasLoadedSpell && selectedSpell" class="detail-footer">
          <small>El estado y los resultados se refrescan tras guardar, sin perder el contexto de búsqueda.</small>
        </section>
      </article>
    </section>
  </main>
</template>
