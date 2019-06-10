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
package com.sumologic.log4j;

import com.sumologic.log4j.server.AggregatingHttpHandler;
import com.sumologic.log4j.server.MaterializedHttpRequest;
import com.sumologic.log4j.server.MockHttpServer;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.helpers.LogLog;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class SumoLogicAppenderTest {

    private static final int PORT = 10010;
    private static final String ENDPOINT_URL = "http://localhost:" + PORT;

    private MockHttpServer server;
    private AggregatingHttpHandler handler;
    private Logger loggerInTest;
    private SumoLogicAppender appender;


    private void setUpLogger(SumoLogicAppender appender) {
        loggerInTest = Logger.getLogger("SumoLogicAppenderTest");
        loggerInTest.setAdditivity(false);
        loggerInTest.addAppender(appender);

    }

    private void setUpLogger(int batchSize, int windowSize, int precision, boolean useLegacy) {
        setUpLoggerWithOverrides(batchSize, windowSize, precision,
                null, null, null, useLegacy);
    }

    private void setUpLoggerWithOverrides(int batchSize, int windowSize, int precision,
        String sourceName, String sourceHost, String sourceCategory, boolean useLegacy) {

        LogLog.setInternalDebugging(true);

        if (useLegacy) {
            appender = new BufferedSumoLogicAppender();
        } else {
            appender = new SumoLogicAppender();
        }
        appender.setUrl(ENDPOINT_URL);
        appender.setMessagesPerRequest(batchSize);
        appender.setMaxFlushInterval(windowSize);
        appender.setFlushingAccuracy(precision);
        if (sourceName != null) {
            appender.setSourceName(sourceName);
        }
        if (sourceHost != null) {
            appender.setSourceHost(sourceHost);
        }
        if (sourceCategory != null) {
            appender.setSourceCategory(sourceCategory);
        }
        appender.setFlushAllBeforeStopping(true);
        appender.setLayout(new PatternLayout("%d{yyyy-MM-dd HH:mm:ss,SSS Z} [%t] %-5p %c - %m%n"));
        setUpLogger(appender);
        appender.activateOptions();
    }


    @Before
    public void setUp() throws Exception {
        handler = new AggregatingHttpHandler();
        server = new MockHttpServer(PORT, handler);
        server.start();
    }

    @After
    public void tearDown() throws Exception {
        if (loggerInTest != null)
            loggerInTest.removeAllAppenders();
        if (server != null)
            server.stop();
    }

    @Test
    public void testLegacy() throws Exception {
        setUpLoggerWithOverrides(1000, 100, 10,
                "mySource", "myHost", "myCategory", true);
        String message = "Test log message";
        loggerInTest.info(message);
        Thread.sleep(500);
        // Check headers
        for(MaterializedHttpRequest request: handler.getExchanges()) {
            assertEquals(true, request.getHeaders().getFirst("X-Sumo-Name").equals("mySource"));
            assertEquals(true, request.getHeaders().getFirst("X-Sumo-Category").equals("myCategory"));
            assertEquals(true, request.getHeaders().getFirst("X-Sumo-Host").equals("myHost"));
            assertEquals("log4j-appender", request.getHeaders().getFirst("X-Sumo-Client"));
        }
        // Check body
        StringBuffer actual = new StringBuffer();
        for(MaterializedHttpRequest request: handler.getExchanges()) {
            for (String line : request.getBody().split("\n")) {
                // Strip timestamp
                int mainStart = line.indexOf("[main]");
                String trimmed = line.substring(mainStart);
                actual.append(trimmed + "\n");
            }
        }
        assertEquals("[main] INFO  SumoLogicAppenderTest - Test log message\n", actual.toString());
    }

    @Test
    public void testMessagesWithMetadata() throws Exception {
        setUpLoggerWithOverrides(1000, 100, 10,
                "mySource", "myHost", "myCategory", false);
        StringBuffer expected = new StringBuffer();
        for (int i = 0; i < 100; i ++) {
            String message = "info" + i;
            loggerInTest.info(message);
            expected.append("[main] INFO  SumoLogicAppenderTest - " + message + "\n");
        }
        Thread.sleep(500);
        // Check headers
        for(MaterializedHttpRequest request: handler.getExchanges()) {
            assertEquals(true, request.getHeaders().getFirst("X-Sumo-Name").equals("mySource"));
            assertEquals(true, request.getHeaders().getFirst("X-Sumo-Category").equals("myCategory"));
            assertEquals(true, request.getHeaders().getFirst("X-Sumo-Host").equals("myHost"));
            assertEquals("log4j-appender", request.getHeaders().getFirst("X-Sumo-Client"));
        }
        // Check body
        StringBuffer actual = new StringBuffer();
        for(MaterializedHttpRequest request: handler.getExchanges()) {
            for (String line : request.getBody().split("\n")) {
                // Strip timestamp
                int mainStart = line.indexOf("[main]");
                String trimmed = line.substring(mainStart);
                actual.append(trimmed + "\n");
            }
        }
        assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void testMessagesWithoutMetadata() throws Exception {
        setUpLogger(1000, 100, 10, false);
        int numMessages = 5;
        for (int i = 0; i < numMessages; i ++) {
            loggerInTest.info("info " + i);
            Thread.sleep(500);
        }
        assertEquals(numMessages, handler.getExchanges().size());
        for(MaterializedHttpRequest request: handler.getExchanges()) {
            assertEquals(true, request.getHeaders().getFirst("X-Sumo-Name") == null);
            assertEquals(true, request.getHeaders().getFirst("X-Sumo-Category") == null);
            assertEquals(true, request.getHeaders().getFirst("X-Sumo-Host") == null);
            assertEquals("log4j-appender", request.getHeaders().getFirst("X-Sumo-Client"));
        }
    }
}
