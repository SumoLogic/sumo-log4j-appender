package com.sumologic.log4j.aggregation;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.Assert.assertEquals;

/**
 * Author: Jose Muniz (jose@sumologic.com)
 * Date: 4/4/13
 * Time: 7:40 PM
 */
public class QueueFlushingTaskTest {

    @Test
    public void testFlushBySize() throws Exception {
        final List<List<String>> tasks = new ArrayList<List<String>>();

        BlockingQueue<String> queue = new LinkedBlockingQueue<String>();
        QueueFlushingTask<String, List<String>> task =
                new QueueFlushingTask<String, List<String>>(queue) {

            @Override
            protected long getRequestRate() {
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