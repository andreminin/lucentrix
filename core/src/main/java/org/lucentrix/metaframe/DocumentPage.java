package org.lucentrix.metaframe;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.lucentrix.metaframe.metadata.CursorItems;

@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class DocumentPage extends CursorItems<LxEvent> {
}
