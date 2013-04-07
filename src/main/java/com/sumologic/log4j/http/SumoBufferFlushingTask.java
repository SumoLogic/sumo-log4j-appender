package com.sumologic.log4j.http;

import com.sumologic.log4j.aggregation.BufferFlushingTask;
import com.sumologic.log4j.queue.BufferWithEviction;
import org.apache.log4j.helpers.LogLog;

import java.util.List;

/**
 * Author: Jose Muniz (jose@sumologic.com)
 * Date: 4/4/13
 * Time: 10:48 PM
 */
public class SumoBufferFlushingTask extends BufferFlushingTask<String, String> {

    private SumoHttpSender sender;
    private long maxFlushInterval;
    private long messagesPerRequest;
    private String name;

    public SumoBufferFlushingTask(BufferWithEviction<String> queue) {
        super(queue);
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSender(SumoHttpSender sender) {
        this.sender = sender;
    }

    public void setMessagesPerRequest(long messagesPerRequest) {
        this.messagesPerRequest = messagesPerRequest;
    }

    public void setMaxFlushInterval(long maxFlushInterval) {
        this.maxFlushInterval = maxFlushInterval;
    }

    @Override
    protected long getMaxFlushInterval() {
        return maxFlushInterval;
    }

    @Override
    protected long getMessagesPerRequest() {
        return messagesPerRequest;
    }

    @Override
    protected String getName() {
        return name;
    }

    @Override
    protected String aggregate(List<String> messages) {
        StringBuilder builder = new StringBuilder(messages.size() * 10);
        for (String message: messages) {
            builder.append(message);
        }
        return builder.toString();
    }

    @Override
    protected void sendOut(String body, String name) {
        if (sender.isInitialized()) {
            sender.send(body, name);
        } else {
            LogLog.error("HTTPSender is not initialized");

        }
    }
}
