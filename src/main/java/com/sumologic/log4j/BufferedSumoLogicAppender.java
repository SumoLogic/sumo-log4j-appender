package com.sumologic.log4j;

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

import com.sumologic.log4j.aggregation.SumoBufferFlusher;
import com.sumologic.log4j.http.SumoHttpSender;
import com.sumologic.log4j.queue.BufferWithEviction;
import com.sumologic.log4j.queue.BufferWithFifoEviction;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;
import static com.sumologic.log4j.queue.CostBoundedConcurrentQueue.CostAssigner;

/**
 * Appender that sends log messages to Sumo Logic.
 *
 * @author Jose Muniz (jose@sumologic.com)
 */
public class BufferedSumoLogicAppender extends AppenderSkeleton {

    private String url = null;
    private int connectionTimeout = 1000;
    private int socketTimeout = 60000;
    private int retryInterval = 10000;        // Once a request fails, how often until we retry.

    private long messagesPerRequest = 100;    // How many messages need to be in the queue before we flush
    private long maxFlushInterval = 10000;    // Maximum interval between flushes (ms)
    private long flushingAccuracy = 250;      // How often the flushed thread looks into the message queue (ms)
    private String sourceName = "Log4J-SumoObject"; // Name to stamp for querying with _sourceName

    private long maxQueueSizeBytes = 1000000;

    volatile private SumoHttpSender sender;
    private SumoBufferFlusher flusher;
    volatile private BufferWithEviction<String> queue;

    /* All the parameters */

    public void setUrl(String url) {
        this.url = url;
    }

    public void setMaxQueueSizeBytes(long maxQueueSizeBytes) {
        this.maxQueueSizeBytes = maxQueueSizeBytes;
    }

    public void setMessagesPerRequest(long messagesPerRequest) {
        this.messagesPerRequest = messagesPerRequest;
    }

    public void setMaxFlushInterval(long maxFlushInterval) {
        this.maxFlushInterval = maxFlushInterval;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public void setFlushingAccuracy(long flushingAccuracy) {
        this.flushingAccuracy = flushingAccuracy;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public void setRetryInterval(int retryInterval) {
        this.retryInterval = retryInterval;
    }

    @Override
    public void activateOptions() {
        LogLog.debug("Activating options");

        /* Initialize queue */
        if (queue == null) {
            queue = new BufferWithFifoEviction<String>(maxQueueSizeBytes, new CostAssigner<String>() {
              @Override
              public long cost(String e) {
                 return e.length();
              }
            });
        } else {
            queue.setCapacity(maxQueueSizeBytes);
        }

        /* Initialize sender */
        if (sender == null)
            sender = new SumoHttpSender();

        sender.setRetryInterval(retryInterval);
        sender.setConnectionTimeout(connectionTimeout);
        sender.setSocketTimeout(socketTimeout);
        sender.setUrl(url);

        sender.init();

        /* Initialize flusher  */
        if (flusher != null)
            flusher.stop();

        flusher = new SumoBufferFlusher(flushingAccuracy,
                    messagesPerRequest,
                    maxFlushInterval,
                    sourceName,
                    sender,
                    queue);
        flusher.start();

    }

    @Override
    protected void append(LoggingEvent event) {
        if (!checkEntryConditions()) {
            LogLog.warn("Appender not initialized. Dropping log entry");
            return;
        }

        StringBuilder builder = new StringBuilder(1024);
        builder.append(layout.format(event));
        if (layout.ignoresThrowable()) {
            String[] throwableStrRep = event.getThrowableStrRep();
            if (throwableStrRep != null) {
                for (String line : throwableStrRep) {
                    builder.append(line);
                    builder.append(Layout.LINE_SEP);
                }
            }
        }

        queue.add(builder.toString());
    }

    @Override
    public void close() {
        sender.close();
        sender = null;

        flusher.stop();
        flusher = null;
    }

    @Override
    public boolean requiresLayout() {
        return true;
    }

    // Private bits.

    private boolean checkEntryConditions() {
        return sender != null && sender.isInitialized();
    }

}
