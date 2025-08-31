package org.lucentrix.metaframe.search;


import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.lucentrix.metaframe.LxDocument;

import java.util.List;

@ToString
@EqualsAndHashCode
@Getter
@Builder
public class SearchResult {
    long totalCount;

    int numberOfPages;

    List<LxDocument> documents;
}
