package com.sumologic.log4j;

import org.apache.log4j.Logger;

/**
 * Simple example on using the Sumo Logic Log4J appender.
 *
 * @author Stefan Zier (stefan@sumologic.com)
 */
public class SumoLogicAppenderExample {
  private static Logger logger = Logger.getLogger(SumoLogicAppenderExample.class);

  public static void main(String[] args) {
    logger.info("Greetings from the SumoLogicAppender!");

    logger.error("Wow, check this out. Multiline!", new RuntimeException("I am a multiline message."));
  }
}
