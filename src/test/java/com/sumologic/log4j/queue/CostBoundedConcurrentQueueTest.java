package com.sumologic.log4j.queue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static com.sumologic.log4j.queue.CostBoundedConcurrentQueue.CostAssigner;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author: Jose Muniz (jose@sumologic.com)
 */
public class CostBoundedConcurrentQueueTest {

    public CostAssigner<String> sizeElements = new CostAssigner<String>() {
        @Override
        public long cost(String e) {
            return e.length();
        }
    };

    public String stringOfSize(int n) {
        StringBuffer buf = new StringBuffer(n);
        for (int i = 0; i < n; i++) {
            buf.append('*');
        }
        return buf.toString();
    }

    @Test
    public void testInsertWithinCapacity() {
        CostBoundedConcurrentQueue<String> queue =
                new CostBoundedConcurrentQueue<String>(100, sizeElements);
        String theString = stringOfSize(100);
        queue.offer(theString);
        String res = queue.poll();
        assertEquals(theString, res);

        queue.offer(theString);
        res = queue.poll();
        assertEquals(theString, res);
    }

    @Test
    public void testInsertBeyondCapacity() {
        CostBoundedConcurrentQueue<String> queue =
                new CostBoundedConcurrentQueue<String>(20, sizeElements);
        String theString = stringOfSize(10);
        assertTrue(queue.offer(theString));
        assertTrue(queue.offer(theString));
        assertFalse(queue.offer(theString));
        assertFalse(queue.offer(theString));

        // Free up, try again
        assertNotNull(queue.poll());
        assertFalse(queue.offer(stringOfSize(11)));
        assertTrue(queue.offer(stringOfSize(10)));
    }

    @Test
    public void testSizeAndCapacity() {
        CostBoundedConcurrentQueue<String> queue =
                new CostBoundedConcurrentQueue<String>(1000, sizeElements);

        queue.offer(stringOfSize(10));
        assertEquals(1, queue.size());
        assertEquals(10, queue.cost());

        queue.offer(stringOfSize(127));
        assertEquals(2, queue.size());
        assertEquals(10 + 127, queue.cost());

        queue.poll();
        assertEquals(1, queue.size());
        assertEquals(127, queue.cost());
    }

    @Test
    public void testDrainTo() {
        CostBoundedConcurrentQueue<String> queue =
                new CostBoundedConcurrentQueue<String>(1000, sizeElements);

        queue.offer(stringOfSize(600));
        queue.offer(stringOfSize(200));

        assertEquals(800, queue.cost());

        List<String> list = new ArrayList<String>(2);
        queue.drainTo(list);

        assertEquals(0, queue.cost());

    }
}
