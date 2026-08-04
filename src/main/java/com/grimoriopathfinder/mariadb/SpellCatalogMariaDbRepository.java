package com.grimoriopathfinder.mariadb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grimoriopathfinder.application.port.out.SpellCatalogRepository;
import com.grimoriopathfinder.spells.Spell;
import com.grimoriopathfinder.spells.SpellListEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Repository;

@Repository
public class SpellCatalogMariaDbRepository implements SpellCatalogRepository {
    private static final Pattern NON_WORDS = Pattern.compile("[^\\p{L}\\p{Nd}]+");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    @PersistenceContext private EntityManager em;

    @Transactional
    public void rebuild(List<Spell> spells) {
        em.createQuery("delete from SpellListEntryEntity").executeUpdate();
        em.createQuery("delete from SpellEntity").executeUpdate();
        for (Spell spell : spells) {
            var entity = toEntity(spell); em.persist(entity);
            for (var entry : spell.lists() == null ? List.<SpellListEntry>of() : spell.lists()) {
                var le = new SpellListEntryEntity(); le.spell = entity; le.listType=entry.listType(); le.listName=entry.listName(); le.level=entry.level(); em.persist(le);
            }
        }
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<SpellCatalogRepository.ListSummary> listSpellLists(String listType) {
        var rows = em.createQuery("select e.listType,e.listName,min(e.level),max(e.level),count(distinct e.spell.id) from SpellListEntryEntity e where (:t is null or :t='' or e.listType=:t) group by e.listType,e.listName order by e.listType,e.listName", Object[].class).setParameter("t", listType).getResultList();
        return rows.stream().map(r -> { var levels=getSpellListLevels((String)r[0],(String)r[1]).orElseThrow(); return new SpellCatalogRepository.ListSummary((String)r[0],(String)r[1],((Number)r[2]).intValue(),((Number)r[3]).intValue(),levels.levels(),((Number)r[4]).intValue()); }).toList();
    }
    @Transactional(Transactional.TxType.SUPPORTS)
    public Optional<SpellCatalogRepository.ListLevels> getSpellListLevels(String type,String name) { var levels=em.createQuery("select distinct e.level from SpellListEntryEntity e where e.listType=:t and e.listName=:n order by e.level",Integer.class).setParameter("t",type).setParameter("n",name).getResultList(); return levels.isEmpty()?Optional.empty():Optional.of(new SpellCatalogRepository.ListLevels(type,name,levels.getFirst(),levels.getLast(),List.copyOf(levels))); }
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<SpellCatalogRepository.SearchCandidate> findCandidates(String type,String name,int level,boolean exact) { var entities=em.createQuery("select distinct e from SpellListEntryEntity e join fetch e.spell s where e.listType=:t and e.listName=:n and e.level "+(exact?"=":"<=")+" :l order by e.level,s.nameEs",SpellListEntryEntity.class).setParameter("t",type).setParameter("n",name).setParameter("l",level).getResultList(); return entities.stream().map(e->new SpellCatalogRepository.SearchCandidate(toDomain(e.spell),new SpellListEntry(e.spell.id,e.listType,e.listName,e.level))).toList(); }
    @Transactional(Transactional.TxType.SUPPORTS)
    public Optional<Spell> findSpellById(String id) { return em.find(SpellEntity.class,id)==null?Optional.empty():Optional.of(toDomain(em.find(SpellEntity.class,id))); }

    private SpellEntity toEntity(Spell s) { var e=new SpellEntity(); e.id=s.id(); e.slug=s.slug();e.sourceId=s.sourceId();e.sourceHash=s.sourceHash();e.nameEs=s.nameEs();e.nameEn=s.nameEn();e.school=s.school();e.subschool=s.subschool();e.descriptors=s.descriptors()==null?List.of():s.descriptors();e.castingTime=s.castingTime();e.components=s.components();e.range=s.range();e.target=s.target();e.effect=s.effect();e.area=s.area();e.duration=s.duration();e.savingThrow=s.savingThrow();e.spellResistance=s.spellResistance();e.descriptionEs=s.descriptionEs();e.descriptionEn=s.descriptionEn();e.sourceBook=s.sourceBook();e.sourcePage=s.sourcePage();e.sourceName=s.sourceName();e.translationStatus=s.translationStatus();e.personalNotes=s.personalNotes()==null?"":s.personalNotes();e.createdAt=s.createdAt();e.updatedAt=s.updatedAt();e.searchText=normalize(String.join(" ",java.util.stream.Stream.of(s.nameEs(),s.descriptionEs(),s.school(),s.subschool(),s.personalNotes()).filter(java.util.Objects::nonNull).toList())); return e; }
    private Spell toDomain(SpellEntity e) { var lists=e.lists==null?List.<SpellListEntry>of():e.lists.stream().map(x->new SpellListEntry(e.id,x.listType,x.listName,x.level)).toList(); return new Spell(e.id,e.slug,e.sourceId,e.sourceHash,e.nameEs,e.nameEn,e.school,e.subschool,e.descriptors,e.castingTime,e.components,e.range,e.target,e.effect,e.area,e.duration,e.savingThrow,e.spellResistance,e.descriptionEs,e.descriptionEn,e.sourceBook,e.sourcePage,e.sourceName,e.translationStatus,lists,e.personalNotes,e.createdAt,e.updatedAt); }
    private String normalize(String x){var n=Normalizer.normalize(x==null?"":x,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(); return NON_WORDS.matcher(n).replaceAll(" ").trim();}
}


