package org.lucentrix.metaframe.plugin.dummy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class TrackingRandom extends Random {
    private final static Logger logger = LoggerFactory.getLogger(TrackingRandom.class);

    private int nextCallCount = 0;

    public TrackingRandom(long seed) {
        super(seed);
    }

    @Override
    protected int next(int bits) {
        nextCallCount++;
        int result = super.next(bits);
        logger.debug("next({}) -> {} [call {}]", bits, result, nextCallCount);
        return result;
    }

    public int getNextCallCount() {
        return nextCallCount;
    }

    public void resetCount() {
        nextCallCount = 0;
    }
}

