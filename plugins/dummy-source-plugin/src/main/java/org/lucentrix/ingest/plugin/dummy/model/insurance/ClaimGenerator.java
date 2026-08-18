package org.lucentrix.ingest.plugin.dummy.model.insurance;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.lucentrix.ingest.ChangeOp;
import org.lucentrix.ingest.SourceDocument;
import org.lucentrix.ingest.ContentChange;
import org.lucentrix.ingest.metadata.field.Field;
import org.lucentrix.ingest.plugin.dummy.DocumentGenerator;

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
    protected ContentChange generate() {
        SourceDocument.SourceDocumentBuilder<?, ?> builder = SourceDocument.builder();

        builder.field(Field.ID, new UUID(random.nextLong(), random.nextLong()).toString());

        Instant now = Instant.now();

        builder.field(Field.CLASS_NAME, "Claim");
        builder.field(Field.MODIFY_DATETIME, now);
        builder.field(Field.CREATE_DATETIME, now);

        builder.field(Field.of("claim_id"), count.get());
        builder.field(Field.TITLE, "Claim " + count.get());

        return ContentChange.builder().action(ChangeOp.REPLACE).document(builder.build()).build();
    }
}
