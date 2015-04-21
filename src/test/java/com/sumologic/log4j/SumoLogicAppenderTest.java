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
import com.sumologic.log4j.server.MockHttpServer;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
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


    private void setUpLogger() {
        SumoLogicAppender sla = new SumoLogicAppender();
        sla.setUrl(ENDPOINT_URL);
        // TODO: Shouldn't there be a default layout?
        sla.setLayout(new PatternLayout("-- %m%n"));
        sla.activateOptions();

        loggerInTest = Logger.getLogger("TestSingleMessage");
        loggerInTest.addAppender(sla);
        loggerInTest.setAdditivity(false);

    }


    @Before
    public void setUp() throws Exception {
        handler = new AggregatingHttpHandler();
        server = new MockHttpServer(PORT, handler);

        server.start();

        setUpLogger();
    }

    @After
    public void tearDown() throws Exception {
        server.stop();
        loggerInTest.removeAllAppenders();
    }

    @Test
    public void testSingleMessage() throws Exception {

        loggerInTest.info("This is a message");

        assertEquals(handler.getExchanges().size(), 1);
        assertEquals(handler.getExchanges().get(0).getBody(), "-- This is a message\n");
    }

    @Test
    public void testMultipleMessages() throws Exception {

        int numMessages = 20;
        for (int i = 0; i < numMessages / 2; i ++) {
            loggerInTest.info("info " + i);
            loggerInTest.error("error " + i);
        }

        assertEquals(handler.getExchanges().size(), numMessages);
    }
}
