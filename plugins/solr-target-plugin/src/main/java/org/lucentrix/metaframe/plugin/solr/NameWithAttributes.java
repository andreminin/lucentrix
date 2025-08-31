package org.lucentrix.metaframe.plugin.solr;

import lombok.*;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class NameWithAttributes {
    String name;
    Map<String, Object> attributes;
}
