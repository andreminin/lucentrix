package org.lucentrix.metaframe.plugin.dummy;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import net.datafaker.Faker;
import org.lucentrix.metaframe.LxDocument;
import org.lucentrix.metaframe.LxEvent;

import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@ToString
@EqualsAndHashCode
@SuperBuilder(toBuilder = true)
public abstract class DocumentGenerator<T extends GeneratorModel> implements Iterator<LxEvent> {
    protected final Random random;
    protected final Faker faker;
    protected final AtomicInteger count;
    protected final T settings;

    public DocumentGenerator(Random random, int count, T settings) {
        this.random = random;
        this.faker = new Faker(random);
        if (count < 0) {
            throw new IllegalArgumentException("Count is less than 0: " + count);
        }
        this.count = new AtomicInteger(count);
        this.settings = settings;
    }

    public int getCount() {
        return count.get();
    }

    public abstract int getLimit();

    public boolean hasNext() {
        return count.get() < getLimit();
    }

    public LxEvent next() {
        if (hasNext()) {
            return null;
        }

        LxEvent doc = generate();
        count.incrementAndGet();

        return doc;
    }

    protected abstract LxEvent generate();

}
 
