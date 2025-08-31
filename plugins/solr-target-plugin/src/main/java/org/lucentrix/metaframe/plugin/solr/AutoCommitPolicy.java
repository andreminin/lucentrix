package org.lucentrix.metaframe.plugin.solr;

import lombok.*;

@Data
@Builder
@Getter
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class AutoCommitPolicy {
    public static AutoCommitPolicy DEFAULT = AutoCommitPolicy.builder().enabled(true).commitDocCount(1000).commitIntervalMs(20000L).build();

    @Builder.Default
    private final boolean enabled = true;
    private int commitDocCount;
    private long commitIntervalMs;
}
