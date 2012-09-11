sumo-log4j-appender
===================

A Log4J appender that sends straight to Sumo Logic.

Usage
-----

Here is a sample log4.properties file. Make sure to replave [collector-url] with the URL from the Sumo Logic UI.

    # Root logger option
    log4j.rootLogger=INFO, sumo

    # Direct log messages to sumo
    log4j.appender.sumo=com.sumologic.log4j.SumoLogicAppender
    log4j.appender.sumo.url=[collector-url]
    log4j.appender.sumo.layout=org.apache.log4j.PatternLayout
    log4j.appender.sumo.layout.ConversionPattern=%d{DATE} %5p %c{1}:%L - %m%n
