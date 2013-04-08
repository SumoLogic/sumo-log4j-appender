package com.sumologic.log4j.aggregation;

import com.sumologic.log4j.queue.BufferWithFifoEviction;
import com.sumologic.log4j.queue.CostBoundedConcurrentQueue;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author: Jose Muniz (jose@sumologic.com)
 */
public class BufferFlushingTaskTest {

    public CostBoundedConcurrentQueue.CostAssigner<String> sizeElements =
        new CostBoundedConcurrentQueue.CostAssigner<String>() {
            @Override
            public long cost(String e) {
                return e.length();
            }
    };

    @Test
    public void testFlushBySize() throws Exception {
        final List<List<String>> tasks = new ArrayList<List<String>>();

        BufferWithFifoEviction<String> queue =
                new BufferWithFifoEviction<String>(1000, sizeElements);
        BufferFlushingTask<String, List<String>> task =
                new BufferFlushingTask<String, List<String>>(queue) {

            @Override
            protected long getMaxFlushInterval() {
                return Integer.MAX_VALUE;
            }

            @Override
            protected long getMessagesPerRequest() {
                return 3;
            }

            @Override
            protected String getName() {
                return "No-name";
            }

            @Override
            protected List<String> aggregate(List<String> messages) {
                return messages;
            }

            @Override
            protected void sendOut(List<String> body, String name) {
                tasks.add(body);
            }
        };

        task.run();
        assertEquals(true, tasks.isEmpty());
        queue.add("msg1");
        queue.add("msg2");

        task.run();
        assertEquals(true, tasks.isEmpty());
        queue.add("msg3");

        task.run();
        assertEquals(1, tasks.size());
        List<String> aggregatedResult = tasks.get(0);
        assertEquals(aggregatedResult.size(), 3);
        assertEquals(aggregatedResult, Arrays.asList("msg1", "msg2", "msg3"));

    }


    @Test
    public void testFlushByDate() throws Exception {

    }
}