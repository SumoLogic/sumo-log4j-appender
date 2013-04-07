package com.sumologic.log4j.queue;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Author: Jose Muniz (jose@sumologic.com)
 * Date: 4/6/13
 * Time: 3:41 PM
 */
public class BufferWithFifoEvictionTest {

    private BufferWithFifoEviction<Integer> queue;

    private CostBoundedConcurrentQueue.CostAssigner<Integer> countCost;
    private CostBoundedConcurrentQueue.CostAssigner<Integer> valueCost;


    @Before
    public void setUp() {
        countCost =
            new CostBoundedConcurrentQueue.CostAssigner<Integer>() {
                @Override
                public long cost(Integer e) {
                    return 1;
                }
            };

        valueCost =
            new CostBoundedConcurrentQueue.CostAssigner<Integer>() {
                @Override
                public long cost(Integer e) {
                    return e;
                }
            };
    }

    @Test
    public void testEnforceBottomless() {
        queue = new BufferWithFifoEviction<Integer>(2, countCost);
        queue.add(1);
        queue.add(2);
        assertEquals(2, queue.size());

        for (int i = 0; i < 100; i++) {
            queue.add(3);
        }

        assertEquals(2, queue.size());
    }

    @Test
    public void testDrainTo() {
        queue = new BufferWithFifoEviction<Integer>(10, countCost);
        for (int i = 0; i < queue.getCapacity(); i++) {
            queue.add(i);
        }

        List<Integer> result = new ArrayList<Integer>(10);
        queue.drainTo(result);

        assertEquals(10, result.size());
        for (int i = 0; i < queue.size(); i++) {
            assertEquals((Object) i, result.get(i));
        }

    }

    @Test
    public void testEviction() {
        queue = new BufferWithFifoEviction<Integer>(3, countCost);
        for (int i = 1; i <= 5; i++) {
            queue.add(i);

        }

        List<Integer> result = new ArrayList<Integer>(3);
        queue.drainTo(result);

        assertEquals(3, result.size());
        assertEquals(Arrays.asList(3, 4, 5), result);

    }

    @Test
    public void testInsertLarge() {
        queue = new BufferWithFifoEviction<Integer>(1+2+3+4+5, valueCost);
        for (int i = 1; i <= 5; i++) {
            queue.add(i);
        }

        assertFalse(queue.add(1000));

        List<Integer> result = new ArrayList<Integer>(3);
        queue.drainTo(result);

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);

    }

    @Test
    public void testComplexEviction() {
        queue = new BufferWithFifoEviction<Integer>(1+2+3+4+5, valueCost);
        for (int i = 1; i <= 5; i++) {
            queue.add(i);
        }

        assertFalse(queue.add(100));
        assertTrue(queue.add(6));

        List<Integer> result = new ArrayList<Integer>(3);
        queue.drainTo(result);

        assertEquals(3, result.size());
        assertEquals(Arrays.asList(4, 5, 6), result);

    }


}
