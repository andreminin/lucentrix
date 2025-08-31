package org.lucentrix.metaframe.plugin.solr;

import java.util.function.Supplier;
import lombok.*;

import java.util.List;

@ToString(exclude = "password")
@EqualsAndHashCode
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SolrConfig {
    private List<String> solrUrls;
    private List<String> zkHosts;
    private String zkChroot;
    private String collection;
    private String user;
    Supplier<String> passwordSupplier;
    private AutoCommitPolicy autoCommitPolicy;
    private SolrCommitOptions solrCommitOptions;

    public String getPassword() {
        return passwordSupplier == null ? null : passwordSupplier.get();
    }
}
