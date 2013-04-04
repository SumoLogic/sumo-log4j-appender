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
package com.sumologic.log4j;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
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
  private int connectionTimeout = 1000;
  private int socketTimeout = 60000;

  private HttpClient httpClient = null;

  public void setUrl(String url) {
    this.url = url;
  }

  public void setConnectionTimeout(int connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

  public void setSocketTimeout(int socketTimeout) {
    this.socketTimeout = socketTimeout;
  }

  @Override
  public void activateOptions() {
    HttpParams params = new BasicHttpParams();
    HttpConnectionParams.setConnectionTimeout(params, connectionTimeout);
    HttpConnectionParams.setSoTimeout(params, socketTimeout);
    httpClient = new DefaultHttpClient(new ThreadSafeClientConnManager(), params);
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
    HttpPost post = null;
    try {
      post = new HttpPost(url);
      post.setEntity(new StringEntity(log, HTTP.PLAIN_TEXT_TYPE, HTTP.UTF_8));
      HttpResponse response = httpClient.execute(post);
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != 200) {
        LogLog.warn(String.format("Received HTTP error from Sumo Service: %d", statusCode));
      }
      //need to consume the body if you want to re-use the connection.
      EntityUtils.consume(response.getEntity());
    } catch (IOException e) {
      LogLog.warn("Could not send log to Sumo Logic", e);
      try { post.abort(); } catch (Exception ignore) {}
    }
  }
}
