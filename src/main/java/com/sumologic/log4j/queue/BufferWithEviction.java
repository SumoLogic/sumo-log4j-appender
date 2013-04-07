package com.sumologic.log4j.queue;

import java.util.Collection;

/**
 * A concurrent buffer with a maximum capacity that, upon reaching said capacity, evicts some
 * element in the queue to ensure the new element can fit.
 *
 * Author: Jose Muniz (jose@sumologic.com)
 * Date: 4/5/13
 * Time: 1:51 AM
 */
public abstract class BufferWithEviction<Q> {
    private long capacity;

    // TODO: pass in cost assigner
    public BufferWithEviction(long capacity) {
        this.capacity = capacity;
    }

    public long getCapacity() {
        return capacity;
    }


    protected abstract Q evict();
    protected abstract boolean evict(long cost);
    public abstract int size();
    public abstract int drainTo(Collection<Q> collection);
    public abstract boolean add(Q element);

}
