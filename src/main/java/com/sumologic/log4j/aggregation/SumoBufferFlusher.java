package com.sumologic.log4j.aggregation;

import com.sumologic.log4j.http.SumoBufferFlushingTask;
import com.sumologic.log4j.http.SumoHttpSender;
import com.sumologic.log4j.queue.BufferWithEviction;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * @author: Jose Muniz (jose@sumologic.com)
 */
public class SumoBufferFlusher {
    private SumoBufferFlushingTask flushingTask;
    private ScheduledFuture future;
    private long flushingAccuracy;


    public SumoBufferFlusher(
            long flushingAccuracy,
            long messagesPerRequest,
            long maxFlushInterval,
            String sourceName,
            SumoHttpSender sender,
            BufferWithEviction<String> buffer) {

        this.flushingAccuracy = flushingAccuracy;

        flushingTask = new SumoBufferFlushingTask(buffer);

        flushingTask.setMessagesPerRequest(messagesPerRequest);
        flushingTask.setMaxFlushInterval(maxFlushInterval);
        flushingTask.setName(sourceName);
        flushingTask.setSender(sender);
    }

    public void start() {
        /* Start flushing! */
        future =
            Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                   Thread thread = new Thread(r);
                    thread.setName("SumoBufferFlusherThread");
                    thread.setDaemon(true);
                    return thread;
                }
            }).
                scheduleAtFixedRate(flushingTask, 0, flushingAccuracy, TimeUnit.MILLISECONDS);

    }


    public void stop() {
        // Keep the current task running until it's done sending
        future.cancel(false);
        future = null;

    }


}
