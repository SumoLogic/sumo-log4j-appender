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

import com.sumologic.log4j.http.SumoHttpSender;
import com.sumologic.log4j.http.SumoQueueFlushingTask;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Appender that sends log messages to Sumo Logic.
 *
 * @author Stefan Zier (stefan@sumologic.com)
 */
public class BufferedSumoLogicAppender extends AppenderSkeleton {

    //private static long ms(long seconds) {
    //    return seconds * 1000;
    //}

    private String url = null;
    private int connectionTimeout = 1000;
    private int socketTimeout = 60000;
    private int retryInterval = 10000;

    private long messagesPerRequest = 100;
    private long requestRate = 10000;
    private String name = "Log4J-SumoObject";
    private long precisionRate = 5000;        // How often we'll look into the Sumo queue

    private SumoHttpSender sender;
    private SumoQueueFlushingTask flushingTask;
    private LinkedBlockingQueue<String> queue;

    /* All the parameters */

    public void setUrl(String url) {
        this.url = url;
    }

    public void setMessagesPerRequest(long messagesPerRequest) {
        this.messagesPerRequest = messagesPerRequest;
    }

    public void setRequestRate(long requestRate) {
        this.requestRate = requestRate;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPrecisionRate(long precisionRate) {
        this.precisionRate = precisionRate;
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

        /* Initialize queue */
        queue = new LinkedBlockingQueue<String>();

        /* Initialize sender */
        if (sender == null)
            sender = new SumoHttpSender();

        sender.setConnectionTimeout(connectionTimeout);
        sender.setRetryInterval(retryInterval);
        sender.setSocketTimeout(socketTimeout);
        sender.setUrl(url);
        sender.init();


        /* Initialize FlushingTask */
        if (flushingTask == null)
            flushingTask = new SumoQueueFlushingTask(queue);

        flushingTask.setMessagesPerRequest(messagesPerRequest);
        flushingTask.setRequestRate(requestRate);
        flushingTask.setName(name);
        flushingTask.setSender(sender);

        /* Start flushing! */
        Executors.newSingleThreadScheduledExecutor().
                scheduleAtFixedRate(
                        flushingTask,
                        0,
                        precisionRate,
                        TimeUnit.MILLISECONDS);
    }

    @Override
    protected void append(LoggingEvent event) {
        if (!checkEntryConditions()) {
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
        //sender.send(builder.toString(), name);
    }

    @Override
    public void close() {
        sender.close();
        sender = null;
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
