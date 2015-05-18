package com.sumologic.log4j.aggregation;

import com.sumologic.log4j.http.SumoBufferFlushingTask;
import com.sumologic.log4j.http.SumoHttpSender;
import com.sumologic.log4j.queue.BufferWithEviction;

import java.util.concurrent.*;

/**
 * @author: Jose Muniz (jose@sumologic.com)
 */
public class SumoBufferFlusher {
    private SumoBufferFlushingTask flushingTask;
    private ScheduledFuture future;
    private ScheduledExecutorService executor;
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

        executor =
            Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setName("SumoBufferFlusherThread");
                    thread.setDaemon(true);
                    return thread;
                }
            });


        future =
            executor.
                scheduleAtFixedRate(flushingTask, 0, flushingAccuracy, TimeUnit.MILLISECONDS);

    }


    public void stop() {
        // Keep the current task running until it's done sending
        if (future != null) {
            future.cancel(false);
            future = null;
        }

        if (executor != null) {
            executor.shutdownNow();
        }
    }


}
