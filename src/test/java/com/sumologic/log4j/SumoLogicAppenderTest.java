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
