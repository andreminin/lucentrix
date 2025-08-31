package org.lucentrix.metaframe.plugin.solr;

import lombok.ToString;
import org.lucentrix.metaframe.metadata.field.Field;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ToString
public class SolrFieldMapper {

    private final static Set<SolrFieldMapping> DEFAULT_MAPPING = Set.of(
            SolrFieldMapping.builder().docField(Field.ID).solrField("id").build(),
            SolrFieldMapping.builder().docField(Field.CONTENT).solrField("content_tt").build()
    );


    private final ConcurrentHashMap<String, Set<Field<?>>> solrToDocMapping;
    private final ConcurrentHashMap<Field<?>, Set<String>> docToSolrMapping;

    public SolrFieldMapper(Set<SolrFieldMapping> mappings) {
        if (mappings == null || mappings.isEmpty()) {
            mappings = DEFAULT_MAPPING;
        }

        solrToDocMapping = new ConcurrentHashMap<>();
        docToSolrMapping = new ConcurrentHashMap<>();

        mappings.forEach(mapping -> {
            Field<?> docField = Field.of(mapping.getDocField());
            String solrField = mapping.getSolrField();
            this.solrToDocMapping.computeIfAbsent(solrField, k -> new HashSet<>()).add(docField);
            this.docToSolrMapping.computeIfAbsent(docField, k -> new HashSet<>()).add(solrField);
        });
    }

    public Set<Field<?>> toDocumentFields(String solrField) {
        return this.solrToDocMapping.computeIfAbsent(solrField, field -> Set.of(SolrFieldSerde.INSTANCE.fromString(field)));
    }

    public Set<String> toSolrFields(Field<?> docField) {
        return this.docToSolrMapping.computeIfAbsent(docField, field -> Set.of(SolrFieldSerde.INSTANCE.toString(field)));
    }
}
