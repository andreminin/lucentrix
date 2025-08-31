package org.lucentrix.metaframe.plugin.dummy.model.insurance;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.lucentrix.metaframe.LxAction;
import org.lucentrix.metaframe.LxDocument;
import org.lucentrix.metaframe.LxEvent;
import org.lucentrix.metaframe.metadata.field.Field;
import org.lucentrix.metaframe.metadata.field.FieldType;
import org.lucentrix.metaframe.plugin.dummy.DocumentGenerator;

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
    protected LxEvent generate() {
        LxDocument.LxDocumentBuilder<?, ?> builder = LxDocument.builder();

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

        return LxEvent.builder().action(LxAction.REPLACE).document(builder.build()).build();
    }
}
