package org.lucentrix.metaframe.plugin.solr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@EqualsAndHashCode
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SolrCommitOptions {
    boolean softCommit;
    boolean waitForSearcher;
    boolean waitForFlush;
    @Builder.Default
    int commitWithin = -1;
}
