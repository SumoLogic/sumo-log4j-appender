package com.sumologic.log4j.http;

import com.sumologic.log4j.aggregation.QueueFlushingTask;
import org.apache.log4j.helpers.LogLog;

import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Author: Jose Muniz (jose@sumologic.com)
 * Date: 4/4/13
 * Time: 10:48 PM
 */
public class SumoQueueFlushingTask extends QueueFlushingTask<String, String> {

    private final static String MESSAGE_BOUNDARY = "---";

    private SumoHttpSender sender;
    private long requestRate;
    private long messagesPerRequest;
    private String name;

    public SumoQueueFlushingTask(BlockingQueue queue) {
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

    public void setRequestRate(long requestRate) {
        this.requestRate = requestRate;
    }

    @Override
    protected long getRequestRate() {
        return requestRate;
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
            builder.append(MESSAGE_BOUNDARY);
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
