import type {
  SearchFilters,
  SearchPage,
  SpellDetail,
  SpellListLevels,
  SpellListSummary,
  SpellSearchResponse,
  SpellSearchResult,
  LevelFilterMode,
  TranslationStatus,
  UpdatePersonalNotesRequest,
  UpdateSpellFieldsRequest,
  UpdateTranslationStatusRequest,
} from "../types/spell";

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

async function requestJson<T>(path: string, init?: RequestInit): Promise<T> {
  const method = init?.method?.toUpperCase() ?? "GET";
  const headers = new Headers(init?.headers ?? {});
  if (method !== "GET" && method !== "HEAD") {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...init,
    headers,
  });

  if (!response.ok) {
    const text = await response.text();
    const error = new Error(text || `HTTP ${response.status}`);
    (error as Error & { status?: number }).status = response.status;
    throw error;
  }

  return (await response.json()) as T;
}

export async function fetchSpellLists(listType: string): Promise<SpellListSummary[]> {
  const response = await requestJson<{ items: SpellListSummary[] }>(`/api/spell-lists?listType=${encodeURIComponent(listType)}`);
  return response.items;
}

export async function fetchSpellListLevels(listType: string, listName: string): Promise<SpellListLevels> {
  return requestJson<SpellListLevels>(
    `/api/spell-lists/levels?listType=${encodeURIComponent(listType)}&listName=${encodeURIComponent(listName)}`,
  );
}

export async function searchSpells(params: {
  listType: string;
  listName: string;
  maxLevel: number;
  levelMode: LevelFilterMode;
  q: string;
}): Promise<SpellSearchResponse> {
  const query = new URLSearchParams({
    listType: params.listType,
    listName: params.listName,
    maxLevel: String(params.maxLevel),
    levelMode: params.levelMode,
    q: params.q,
    page: "0",
    size: "50",
  });
  return requestJson<SpellSearchResponse>(`/api/spells/search?${query.toString()}`);
}

export async function fetchSpellDetail(spellId: string): Promise<SpellDetail> {
  return requestJson<SpellDetail>(`/api/spells/${encodeURIComponent(spellId)}`);
}

export async function updateSpellFields(spellId: string, request: UpdateSpellFieldsRequest): Promise<SpellDetail> {
  return requestJson<SpellDetail>(`/api/spells/${encodeURIComponent(spellId)}/fields`, {
    method: "PATCH",
    body: JSON.stringify(request),
  });
}

export async function updatePersonalNotes(
  spellId: string,
  request: UpdatePersonalNotesRequest,
): Promise<SpellDetail> {
  return requestJson<SpellDetail>(`/api/spells/${encodeURIComponent(spellId)}/notes`, {
    method: "PATCH",
    body: JSON.stringify(request),
  });
}

export async function updateTranslationStatus(
  spellId: string,
  request: UpdateTranslationStatusRequest,
): Promise<SpellDetail> {
  return requestJson<SpellDetail>(`/api/spells/${encodeURIComponent(spellId)}/translation-status`, {
    method: "PATCH",
    body: JSON.stringify(request),
  });
}

export function formatDescriptors(descriptors: string[]): string {
  return descriptors.length ? descriptors.join(", ") : "—";
}

export function formatTranslationStatus(status: TranslationStatus): string {
  const labels: Record<TranslationStatus, string> = {
    NOT_TRANSLATED: "No traducido",
    AI_TRANSLATED: "Traducido por IA",
    REVIEW_REQUIRED: "Revisión requerida",
    REVIEWED: "Revisado",
    MANUALLY_EDITED: "Editado manualmente",
    LOCKED: "Bloqueado",
  };
  return labels[status];
}

export function formatLevelFilterMode(levelMode: LevelFilterMode): string {
  const labels: Record<LevelFilterMode, string> = {
    UP_TO: "Hasta nivel",
    EXACT: "Nivel",
  };
  return labels[levelMode];
}

export function getSearchSummary(page: SearchPage, filters: SearchFilters): string {
  return `${filters.listType} · ${filters.listName} · ${formatLevelFilterMode(filters.levelMode)} ${filters.maxLevel} · ${page.totalItems} resultados`;
}

export function toDescriptorsInput(descriptors: string[]): string {
  return descriptors.join(", ");
}

export function fromDescriptorsInput(input: string): string[] {
  return input
    .split(",")
    .map((item) => item.trim())
    .filter((item) => item.length > 0);
}
