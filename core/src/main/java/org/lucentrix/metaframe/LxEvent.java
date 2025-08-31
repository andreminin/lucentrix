package org.lucentrix.metaframe;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.lucentrix.metaframe.metadata.Identity;

@EqualsAndHashCode
@ToString
@Getter
@Builder(toBuilder = true)
public class LxEvent implements Identity {
    LxAction action;
    LxDocument document;

    @Override
    public String getId() {
        return document == null ? null : document.getId();
    }
}
