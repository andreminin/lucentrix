package org.lucentrix.ingest.plugin.dummy.model.insurance;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.lucentrix.ingest.ChangeOp;
import org.lucentrix.ingest.SourceDocument;
import org.lucentrix.ingest.ContentChange;
import org.lucentrix.ingest.metadata.field.Field;
import org.lucentrix.ingest.metadata.field.FieldType;
import org.lucentrix.ingest.plugin.dummy.DocumentGenerator;

import java.time.Instant;
import java.util.Random;
import java.util.UUID;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class PolicyGenerator extends DocumentGenerator<InsuranceModel> {
    public PolicyGenerator(Random random, int count, InsuranceModel model) {
        super(random, count, model);
    }

    @Override
    public int getLimit() {
        return settings.policyMaxCount();
    }

    @Override
    protected ContentChange generate() {
        SourceDocument.SourceDocumentBuilder<?, ?> builder = SourceDocument.builder();

        builder.field(Field.ID, new UUID(random.nextLong(), random.nextLong()).toString());

        Instant now = Instant.now();

        builder.field(Field.CLASS_NAME, "Policy");
        builder.field(Field.MODIFY_DATETIME, now);
        builder.field(Field.CREATE_DATETIME, now);

        builder.field(Field.of("policy_id"), count.get());
        builder.field(Field.TITLE, "Policy " + count.get());

        //Select random client to link policy
        int clientId = (int) Math.floor(random.nextDouble() * settings.clientMaxCount());

        builder.field(Field.of("ref_client_id", FieldType.INT), clientId);
        builder.field(Field.of("ref_client_id", FieldType.STRING), clientId);

        return ContentChange.builder().action(ChangeOp.REPLACE).document(builder.build()).build();
    }
}
