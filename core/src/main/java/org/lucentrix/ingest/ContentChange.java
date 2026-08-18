package org.lucentrix.ingest;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.lucentrix.ingest.metadata.HasId;

@EqualsAndHashCode
@ToString
@Getter
@Builder(toBuilder = true)
public class ContentChange implements HasId {
    ChangeOp action;
    SourceDocument document;

    @Override
    public String getId() {
        return document == null ? null : document.getId();
    }
}
