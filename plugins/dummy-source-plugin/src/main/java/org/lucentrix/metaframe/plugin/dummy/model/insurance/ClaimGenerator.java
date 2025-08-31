package org.lucentrix.metaframe.plugin.dummy.model.insurance;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.lucentrix.metaframe.LxAction;
import org.lucentrix.metaframe.LxDocument;
import org.lucentrix.metaframe.LxEvent;
import org.lucentrix.metaframe.metadata.field.Field;
import org.lucentrix.metaframe.plugin.dummy.DocumentGenerator;

import java.time.Instant;
import java.util.Random;
import java.util.UUID;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class ClaimGenerator extends DocumentGenerator<InsuranceModel> {

    public ClaimGenerator(Random random, int count, InsuranceModel model) {
        super(random, count, model);
    }

    @Override
    public int getLimit() {
        return settings.claimMaxCount();
    }

    @Override
    protected LxEvent generate() {
        LxDocument.LxDocumentBuilder<?, ?> builder = LxDocument.builder();

        builder.field(Field.ID, new UUID(random.nextLong(), random.nextLong()).toString());

        Instant now = Instant.now();

        builder.field(Field.CLASS_NAME, "Claim");
        builder.field(Field.MODIFY_DATETIME, now);
        builder.field(Field.CREATE_DATETIME, now);

        builder.field(Field.of("claim_id"), count.get());
        builder.field(Field.TITLE, "Claim " + count.get());

        return LxEvent.builder().action(LxAction.REPLACE).document(builder.build()).build();
    }
}
