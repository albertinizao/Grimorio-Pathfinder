export type TranslationStatus =
  | "NOT_TRANSLATED"
  | "AI_TRANSLATED"
  | "REVIEW_REQUIRED"
  | "REVIEWED"
  | "MANUALLY_EDITED"
  | "LOCKED";

export interface SpellListSummary {
  listType: string;
  listName: string;
  minLevel: number;
  maxLevel: number;
  levels: number[];
  spellCount: number;
}

export interface SpellListLevels {
  listType: string;
  listName: string;
  minLevel: number;
  maxLevel: number;
  levels: number[];
}

export interface SearchFilters {
  listType: string;
  listName: string;
  maxLevel: number;
  q: string;
}

export interface SearchPage {
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
  hasNext: boolean;
}

export interface SpellSelectedList {
  listType: string;
  listName: string;
  level: number;
}

export interface SpellSearchResult {
  spellId: string;
  slug: string;
  nameEs: string;
  selectedList: SpellSelectedList;
  school: string | null;
  subschool: string | null;
  descriptors: string[];
  castingTime: string | null;
  components: string | null;
  range: string | null;
  target: string | null;
  effect: string | null;
  area: string | null;
  duration: string | null;
  savingThrow: string | null;
  spellResistance: string | null;
  descriptionEs: string | null;
  translationStatus: TranslationStatus;
  snippet: string | null;
  matchSource: string | null;
  hasPersonalNotes: boolean;
}

export interface SpellSearchResponse {
  filters: SearchFilters;
  page: SearchPage;
  sort: string;
  results: SpellSearchResult[];
}

export interface SpellListEntry {
  listType: string;
  listName: string;
  level: number;
}

export interface SpellSource {
  sourceBook: string | null;
  sourcePage: number | null;
  sourceName: string | null;
}

export interface SpellDetail {
  spellId: string;
  slug: string;
  nameEs: string | null;
  nameEn: string | null;
  school: string | null;
  subschool: string | null;
  descriptors: string[];
  castingTime: string | null;
  components: string | null;
  range: string | null;
  target: string | null;
  effect: string | null;
  area: string | null;
  duration: string | null;
  savingThrow: string | null;
  spellResistance: string | null;
  descriptionEs: string | null;
  descriptionEn: string | null;
  personalNotes: string | null;
  translationStatus: TranslationStatus;
  lists: SpellListEntry[];
  source: SpellSource;
  editableFields: string[];
  updatedAt: string | null;
  reviewedAt: string | null;
}

export interface UpdateSpellFieldsRequest {
  fields: Record<string, string | string[] | null>;
  expectedUpdatedAt?: string | null;
  reason?: string | null;
}

export interface UpdatePersonalNotesRequest {
  personalNotes: string;
  expectedUpdatedAt?: string | null;
}

export interface UpdateTranslationStatusRequest {
  translationStatus: TranslationStatus;
  expectedUpdatedAt?: string | null;
  reason?: string | null;
}

export const TRANSLATION_STATUSES: TranslationStatus[] = [
  "NOT_TRANSLATED",
  "AI_TRANSLATED",
  "REVIEW_REQUIRED",
  "REVIEWED",
  "MANUALLY_EDITED",
  "LOCKED",
];
