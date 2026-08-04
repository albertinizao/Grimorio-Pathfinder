package com.grimoriopathfinder.mariadb;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Converter
class StringListJsonConverter implements AttributeConverter<List<String>, String> {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    public String convertToDatabaseColumn(List<String> value) { try { return MAPPER.writeValueAsString(value == null ? List.of() : value); } catch (IOException e) { throw new IllegalArgumentException(e); } }
    public List<String> convertToEntityAttribute(String value) { try { return value == null ? List.of() : MAPPER.readValue(value, new TypeReference<>() {}); } catch (IOException e) { throw new IllegalArgumentException(e); } }
}

@Entity
@Table(name = "spells")
class SpellEntity {
    @Id String id;
    String slug; @Column(name="source_id") String sourceId; @Column(name="source_hash") String sourceHash;
    @Column(name="name_es") String nameEs; @Column(name="name_en") String nameEn; String school; String subschool;
    @Column(name="descriptors_json", columnDefinition="json") @Convert(converter=StringListJsonConverter.class) List<String> descriptors = new ArrayList<>();
    @Column(name="casting_time") String castingTime; String components; @Column(name="spell_range") String range; String target; String effect; String area; String duration;
    @Column(name="saving_throw") String savingThrow; @Column(name="spell_resistance") String spellResistance;
    @Column(name="description_es", columnDefinition="longtext") String descriptionEs; @Column(name="description_en", columnDefinition="longtext") String descriptionEn;
    @Column(name="source_book") String sourceBook; @Column(name="source_page") Integer sourcePage; @Column(name="source_name") String sourceName;
    @Column(name="translation_status") String translationStatus; @Column(name="personal_notes", columnDefinition="longtext") String personalNotes;
    @Column(name="created_at") Instant createdAt; @Column(name="updated_at") Instant updatedAt; @Column(name="search_text", columnDefinition="longtext") String searchText;
    @OneToMany(mappedBy="spell", fetch=FetchType.LAZY, cascade=jakarta.persistence.CascadeType.ALL, orphanRemoval=true) List<SpellListEntryEntity> lists = new ArrayList<>();
}

class SpellListEntryId implements java.io.Serializable {
    String spell; String listType; String listName; int level;
    public SpellListEntryId() {}
    public boolean equals(Object o) { if (!(o instanceof SpellListEntryId x)) return false; return level==x.level && java.util.Objects.equals(spell,x.spell)&&java.util.Objects.equals(listType,x.listType)&&java.util.Objects.equals(listName,x.listName); }
    public int hashCode() { return java.util.Objects.hash(spell,listType,listName,level); }
}

@Entity @Table(name="spell_list_entries") @IdClass(SpellListEntryId.class)
class SpellListEntryEntity {
    @Id @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="spell_id") SpellEntity spell;
    @Id @Column(name="list_type") String listType; @Id @Column(name="list_name") String listName; @Id int level;
}
