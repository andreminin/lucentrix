package org.lucentrix.ingest;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.lucentrix.ingest.metadata.CursorPage;

@ToString
@EqualsAndHashCode(callSuper = true)
@Getter
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class ChangePage extends CursorPage<ContentChange> {
}
