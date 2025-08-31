package org.lucentrix.metaframe.plugin.dummy;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.lucentrix.metaframe.DocumentPage;
import org.lucentrix.metaframe.LxEvent;
import org.lucentrix.metaframe.metadata.Cursor;
import org.lucentrix.metaframe.metadata.field.Field;
import org.lucentrix.metaframe.metadata.field.FieldType;
import org.lucentrix.metaframe.plugin.dummy.model.insurance.*;
import org.lucentrix.metaframe.runtime.DocumentRetriever;
import org.lucentrix.metaframe.runtime.plugin.AbstractPlugin;
import org.lucentrix.metaframe.runtime.plugin.RetrieverPluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

@ToString
@EqualsAndHashCode(callSuper = true)
public class DummySourcePlugin extends AbstractPlugin<DummySourceConfig, RetrieverPluginContext> implements DocumentRetriever {
    private final Logger logger = LoggerFactory.getLogger(DummySourcePlugin.class);

    public static final Field<Integer> clientCountField = Field.of("client_count", FieldType.INT);
    public static final Field<Integer> claimCountField = Field.of("claim_count", FieldType.INT);
    public static final Field<Integer> securityCountField = Field.of("security_count", FieldType.INT);
    public static final Field<Integer> policyCountField = Field.of("policy_count", FieldType.INT);
    public static final Field<Long> seedField = Field.of("seed", FieldType.LONG);

    private TrackingRandom random;
    private InsuranceGenerator generator;

    private Cursor cursor;

    public DummySourcePlugin(DummySourceConfig config, RetrieverPluginContext context) {
        super(config, context);

        if (getPersistence() == null) {
            throw new IllegalArgumentException("Cursor persistence is null!");
        }
    }

    @Override
    public void start() {
        super.start();

        String name = config.getName();

        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("DummySourceConfig name field is blank or null!");
        }

        this.cursor = loadCursor(name);

        long seed = this.cursor.getFields().get(seedField, 0L);
        this.random = new TrackingRandom(seed);

        int securityCount = this.cursor.getFields().get(securityCountField, 0);
        int claimCount = this.cursor.getFields().get(claimCountField, 0);
        int clientCount = this.cursor.getFields().get(clientCountField, 0);
        int policyCount = this.cursor.getFields().get(policyCountField, 0);

        DummySourceConfig.Settings cfg = config.getSettings();

        InsuranceModel model = new InsuranceModel(
                cfg.getClaimMaxCount(),
                cfg.getClientMaxCount(),
                cfg.getPolicyMaxCount(),
                cfg.getSecurityMaxCount(),
                cfg.getUserCount(),
                cfg.getGroupCount());

        this.generator = InsuranceGenerator.builder()
                .securityGenerator(new SecurityGenerator(random, securityCount, model))
                .clientGenerator(new ClientGenerator(random, clientCount, model))
                .policyGenerator(new PolicyGenerator(random, policyCount, model))
                .claimGenerator(new ClaimGenerator(random, claimCount, model))
                .build();

        logger.info("Dummy document source started: {}", this);
    }

    private Cursor loadCursor(String name) {
        Cursor.SuffixCursor suffixCursor = getPersistence().load(name, Cursor.SuffixCursor.class);
        if(suffixCursor != null) {
            return Cursor.SuffixCursor.toCursor(suffixCursor);
        }

        Cursor cursor = new Cursor(name);
        suffixCursor = Cursor.SuffixCursor.toSuffixCursor(cursor);
        getPersistence().save(suffixCursor);

        return cursor;
    }

    @Override
    public boolean hasNext() {
        return this.generator.hasNext();
    }

    @Override
    public DocumentPage next() {
        if (!hasNext()) {
            return DocumentPage.builder().cursor(cursor).items(Collections.emptyList()).build();
        }

        List<LxEvent> events = generator.next(context.getPageSize());

        if (!events.isEmpty()) {
            this.cursor = this.cursor.toBuilder()
                    .fields(this.cursor.getFields().toBuilder()
                            .field(seedField, random.getNextCallCount())
                            .field(securityCountField, generator.getSecurityCount())
                            .field(clientCountField, generator.getClientCount())
                            .field(policyCountField, generator.getPolicyCount())
                            .field(claimCountField, generator.getClaimCount())
                            .build())
                    .build();
        }

        return DocumentPage.builder().items(events).cursor(cursor).hasNext(hasNext()).build();
    }

    @Override
    public void save() {
        Cursor.SuffixCursor suffixCursor = Cursor.SuffixCursor.toSuffixCursor(cursor);
        getPersistence().save(suffixCursor);
    }
}
