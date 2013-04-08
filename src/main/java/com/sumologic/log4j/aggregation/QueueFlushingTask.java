package com.sumologic.log4j.aggregation;

import org.apache.log4j.helpers.LogLog;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Task to perform a single flushing check
 *
 * Author: Jose Muniz (jose@sumologic.com)
 */
public abstract class QueueFlushingTask<In, Out> implements Runnable {

    private Date dateOfLastFlush = new java.util.Date();
    private BlockingQueue<In> messageQueue;

    private boolean needsFlushing() {
        Date currentTime = new java.util.Date();
        Date dateOfNextFlush =
                new java.util.Date(dateOfLastFlush.getTime() + getRequestRate());

        return (messageQueue.size() >= getMessagesPerRequest()) ||
                (currentTime.after(dateOfNextFlush));
    }

    private void flushAndSend() {
        List<In> messages = new ArrayList<In>(messageQueue.size());
        messageQueue.drainTo(messages);

        if (messages.size() > 0) {
            LogLog.debug(String.format("Flushing and sending out %d messages", messages.size()));
            Out body = aggregate(messages);
            sendOut(body, getName());
        }
    }


    /* Subclasses should define from here */

    abstract protected long getRequestRate();
    abstract protected long getMessagesPerRequest();
    abstract protected String getName();

    protected QueueFlushingTask(BlockingQueue<In> messageQueue) {
        this.messageQueue = messageQueue;
    }

    // Given the list of messages, aggregate them into a single Out object
    abstract protected Out aggregate(List<In> messages);
    // Send aggregated message out. Block until we've successfully sent it.
    abstract protected void sendOut(Out body, String name);



    /* Public interface */

    @Override
    public void run() {
        if (needsFlushing()) {
            flushAndSend();
        }
    }

}
