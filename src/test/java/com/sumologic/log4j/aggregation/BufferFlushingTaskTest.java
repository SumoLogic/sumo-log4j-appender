/**
 *    _____ _____ _____ _____    __    _____ _____ _____ _____
 *   |   __|  |  |     |     |  |  |  |     |   __|     |     |
 *   |__   |  |  | | | |  |  |  |  |__|  |  |  |  |-   -|   --|
 *   |_____|_____|_|_|_|_____|  |_____|_____|_____|_____|_____|
 *
 *                UNICORNS AT WARP SPEED SINCE 2010
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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