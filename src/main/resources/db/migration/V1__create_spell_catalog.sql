CREATE TABLE spells (
    id VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL,
    source_id VARCHAR(255) NOT NULL,
    source_hash VARCHAR(255) NOT NULL,
    name_es VARCHAR(300), name_en VARCHAR(300), school VARCHAR(255), subschool VARCHAR(255),
    descriptors_json JSON NOT NULL,
    casting_time VARCHAR(1000), components VARCHAR(1000), spell_range VARCHAR(1000), target VARCHAR(1000),
    effect VARCHAR(1000), area VARCHAR(1000), duration VARCHAR(1000), saving_throw VARCHAR(1000),
    spell_resistance VARCHAR(1000), description_es LONGTEXT, description_en LONGTEXT,
    source_book VARCHAR(1000), source_page INT, source_name VARCHAR(1000),
    translation_status VARCHAR(32) NOT NULL DEFAULT 'NOT_TRANSLATED',
    personal_notes LONGTEXT NOT NULL,
    created_at TIMESTAMP(6) NULL, updated_at TIMESTAMP(6) NULL,
    search_text LONGTEXT NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_spells_translation_status (translation_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE spell_list_entries (
    spell_id VARCHAR(255) NOT NULL,
    list_type VARCHAR(64) NOT NULL,
    list_name VARCHAR(255) NOT NULL,
    level INT NOT NULL,
    PRIMARY KEY (spell_id, list_type, list_name, level),
    CONSTRAINT fk_spell_list_entries_spell FOREIGN KEY (spell_id) REFERENCES spells(id) ON DELETE CASCADE,
    INDEX idx_spell_list_entries_lookup (list_type, list_name, level, spell_id),
    INDEX idx_spell_list_entries_spell_id (spell_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
