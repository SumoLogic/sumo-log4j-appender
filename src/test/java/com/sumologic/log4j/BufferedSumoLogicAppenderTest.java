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

/**
 * @author: Jose Muniz (jose@sumologic.com)
 */
public class BufferedSumoLogicAppenderTest {

    private static final int PORT = 10010;
    private static final String ENDPOINT_URL = "http://localhost:" + PORT;

    private MockHttpServer server;
    private AggregatingHttpHandler handler;
    private Logger loggerInTest;
    private BufferedSumoLogicAppender appender;


    private void setUpLogger(BufferedSumoLogicAppender appender) {
        loggerInTest = Logger.getLogger("BufferedSumoLogicAppenderTest");
        loggerInTest.addAppender(appender);
        loggerInTest.setAdditivity(false);

    }

    private void setUpLogger(int batchSize, int windowSize, int precision) {
        LogLog.setInternalDebugging(true);

        appender = new BufferedSumoLogicAppender();
        appender.setUrl(ENDPOINT_URL);
        appender.setMessagesPerRequest(batchSize);
        appender.setMaxFlushInterval(windowSize);
        appender.setFlushingAccuracy(precision);

        // TODO: Shouldn't there be a default layout?
        appender.setLayout(new PatternLayout("%m%n"));
        appender.activateOptions();

        setUpLogger(appender);
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
    public void testSingleMessage() throws Exception {
        setUpLogger(1, 10000, 10);

        loggerInTest.info("This is a message");

        Thread.sleep(100);
        assertEquals(handler.getExchanges().size(), 1);
        assertEquals(handler.getExchanges().get(0).getBody(), "This is a message\n");
    }

    @Test
    public void testMultipleMessages() throws Exception {
        setUpLogger(1, 10000, 10);

        int numMessages = 20;
        for (int i = 0; i < numMessages; i ++) {
            loggerInTest.info("info " + i);
            Thread.sleep(100);
        }

        assertEquals(numMessages, handler.getExchanges().size());
    }


    @Test
    public void testBatchingBySize() throws Exception {
        // Huge window, ensure all messages get batched into one
        setUpLogger(100, 10000, 10);

        int numMessages = 100;
        for (int i = 0; i < numMessages; i ++) {
            loggerInTest.info("info " + i);
        }


        Thread.sleep(2000);
        assertEquals(handler.getExchanges().size(), 1);
    }

    @Test
    public void testBatchingByWindow() throws Exception {
        // Small window, ensure all messages get batched by time
        setUpLogger(10000, 500, 10);

        loggerInTest.info("message1");
        loggerInTest.info("message2");
        loggerInTest.info("message3");
        loggerInTest.info("message4");
        loggerInTest.info("message5");

        Thread.sleep(520);

        loggerInTest.info("message1");
        loggerInTest.info("message2");
        loggerInTest.info("message3");
        loggerInTest.info("message4");
        loggerInTest.info("message5");

        Thread.sleep(520);


        assertEquals(2, handler.getExchanges().size());
        MaterializedHttpRequest request1 = handler.getExchanges().get(0);
        MaterializedHttpRequest request2 = handler.getExchanges().get(1);
        System.out.println(request1.getBody());

    }


    @Test
    // Start with an appender without its URL set. THEN set the property and
    // make sure everything's still there.
    public void testNoUrlSetInitially() throws Exception {
        LogLog.setInternalDebugging(true);

        appender = new BufferedSumoLogicAppender();
        appender.setMessagesPerRequest(1000);
        appender.setMaxFlushInterval(100);
        appender.setFlushingAccuracy(1);
        appender.setRetryInterval(1);

        // TODO: Shouldn't there be a default layout?
        appender.setLayout(new PatternLayout("%m%n"));
        appender.activateOptions();

        setUpLogger(appender);


        for (int i = 0; i < 100; i++) {
            loggerInTest.info("message " + i);
        }


        appender.setUrl(ENDPOINT_URL);
        appender.activateOptions();

        Thread.sleep(1000);
        assertEquals(1, handler.getExchanges().size());

    }


}
