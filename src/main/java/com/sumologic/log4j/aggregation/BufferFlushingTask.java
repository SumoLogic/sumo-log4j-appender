package com.sumologic.log4j.aggregation;

import com.sumologic.log4j.queue.BufferWithEviction;
import org.apache.log4j.helpers.LogLog;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Task to perform a single flushing check
 *
 * @author: Jose Muniz (jose@sumologic.com)
 */
public abstract class BufferFlushingTask<In, Out> implements Runnable {

    private Date dateOfLastFlush = new java.util.Date();
    private BufferWithEviction<In> messageQueue;

    private boolean needsFlushing() {
        Date currentTime = new java.util.Date();
        Date dateOfNextFlush =
                new java.util.Date(dateOfLastFlush.getTime() + getMaxFlushInterval());

        return (messageQueue.size() >= getMessagesPerRequest()) ||
                (currentTime.after(dateOfNextFlush));
    }

    private void flushAndSend() {
        List<In> messages = new ArrayList<In>(messageQueue.size());
        messageQueue.drainTo(messages);

        if (messages.size() > 0) {
            LogLog.debug(String.format("%s - Flushing and sending out %d messages (%d messages left)",
                    new java.util.Date(),
                    messages.size(),
                    messageQueue.size()));
            Out body = aggregate(messages);
            sendOut(body, getName());
        }
    }


    /* Subclasses should define from here */

    abstract protected long getMaxFlushInterval();
    abstract protected long getMessagesPerRequest();
    abstract protected String getName();

    protected BufferFlushingTask(BufferWithEviction<In> messageQueue) {
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
            try {
                flushAndSend();
            }
            catch (Exception e) {
                LogLog.warn("Exception while attempting to flush and send", e);
            }
        }
    }

}
