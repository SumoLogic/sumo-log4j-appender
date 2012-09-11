package com.sumologic.log4j;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.protocol.HTTP;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;

import java.io.IOException;

/**
 * Appender that sends log messages to Sumo Logic.
 *
 * @author Stefan Zier (stefan@sumologic.com)
 */
public class SumoLogicAppender extends AppenderSkeleton {

  private String url = null;

  private HttpClient httpClient = null;

  public void setUrl(String url) {
    this.url = url;

    if (this.url.contains("format")) {
      LogLog.warn("URL contains the term 'format' -- this may cause issues if set to false.");
    } else {
      this.url += "&format=multiline";
    }
  }

  @Override
  public void activateOptions() {
    httpClient = new DefaultHttpClient(new ThreadSafeClientConnManager());
  }

  @Override
  protected void append(LoggingEvent event) {
    if (!checkEntryConditions()) {
      return;
    }

    StringBuilder builder = new StringBuilder();
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

    sendToSumo(builder.toString());
  }

  @Override
  public void close() {
    httpClient.getConnectionManager().shutdown();
    httpClient = null;
  }

  @Override
  public boolean requiresLayout() {
    return true;
  }

  // Private bits.

  private boolean checkEntryConditions() {
    if (httpClient == null) {
      LogLog.warn("HttpClient not initialized.");
      return false;
    }

    return true;
  }

  private void sendToSumo(String log) {
    try {
      HttpPost post = new HttpPost(url);
      post.setEntity(new StringEntity(log, HTTP.PLAIN_TEXT_TYPE, HTTP.UTF_8));
      HttpResponse response = httpClient.execute(post);
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != 200) {
        LogLog.warn(String.format("Received HTTP error from Sumo Service: %d", statusCode));
      }
    } catch (IOException e) {
      LogLog.warn("Could not send log to Sumo Logic", e);
    }
  }
}
