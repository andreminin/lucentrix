package org.lucentrix.metaframe.plugin.solr;

import lombok.*;
import org.lucentrix.metaframe.metadata.field.Field;
import org.lucentrix.metaframe.metadata.mapping.SuffixSerde;


@Builder
@Getter
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class SolrFieldMapping {
    String docField;
    String solrField;


    public static class SolrFieldMappingBuilder {
        public SolrFieldMappingBuilder docField(Field<?> field) {
            this.docField(SuffixSerde.SUFFIX_MAPPING.toString(field));
            return this;
        }

        public SolrFieldMappingBuilder docField(String field) {
            this.docField = field;
            return this;
        }
    }
}
