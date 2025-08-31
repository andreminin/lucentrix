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
import java.util.*;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class SecurityGenerator extends DocumentGenerator<InsuranceModel> {

    public SecurityGenerator(Random random, int count, InsuranceModel model) {
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

        builder.field(Field.CLASS_NAME, "Security");
        builder.field(Field.MODIFY_DATETIME, now);
        builder.field(Field.CREATE_DATETIME, now);

        builder.field(Field.of("security_id"), count.get());
        builder.field(Field.TITLE, "Security " + count.get());

        int userCount = random.nextInt(1, 6);
        Set<String> users = new HashSet<>();
        for (int i = 0; i < userCount; i++) {
            users.add("user_" + random.nextInt(settings.userCount()));
        }

        List<String> allowUsers = new ArrayList<>();
        List<String> denyUsers = new ArrayList<>();
        for (String user : users) {
            if (random.nextBoolean()) {
                allowUsers.add(user);
            } else {
                denyUsers.add(user);
            }
        }

        int groupCount = random.nextInt(1, 6);
        Set<String> groups = new HashSet<>();
        for (int i = 0; i < userCount; i++) {
            groups.add("group_" + random.nextInt(settings.userCount()));
        }

        List<String> allowGroups = new ArrayList<>();
        List<String> denyGroups = new ArrayList<>();
        for (String group : groups) {
            if (random.nextBoolean()) {
                allowGroups.add(group);
            } else {
                denyGroups.add(group);
            }
        }

        builder.field(Field.of("allow_users", FieldType.STRING_LIST), allowUsers);
        builder.field(Field.of("deny_users", FieldType.STRING_LIST), denyUsers);
        builder.field(Field.of("allow_groups", FieldType.STRING_LIST), allowGroups);
        builder.field(Field.of("deny_groups", FieldType.STRING_LIST), denyGroups);

        return LxEvent.builder().action(LxAction.REPLACE).document(builder.build()).build();
    }
}
