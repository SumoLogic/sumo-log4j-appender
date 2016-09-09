sumo-log4j-appender
===================

A Log4J appender that sends straight to Sumo Logic.

Note: We are actively developing a Sumo Logic Appender that is compatible with Log4J2. If that is of interest to you, please add your vote here:  https://sumologic.aha.io/ideas/ideas/SL-I-2219 

Usage
-----

Here is a sample log4.properties file. Make sure to replace [collector-url] with the URL from the Sumo Logic UI.

    # Root logger option
    log4j.rootLogger=INFO, sumo

    # Direct log messages to sumo
    log4j.appender.sumo=com.sumologic.log4j.BufferedSumoLogicAppender
    log4j.appender.sumo.layout=org.apache.log4j.PatternLayout
    log4j.appender.sumo.layout.ConversionPattern=%d{DATE} %5p %c{1}:%L - %m%n
    log4j.appender.sumo.url=<YOUR_URL_HERE>
    log4j.appender.sumo.proxyAuth=<YOUR AUTHTYPE: basic or ntlm>
    log4j.appender.sumo.proxyHost=<YOUR HOSTNAME>
    log4j.appender.sumo.proxyPort=<YOUR PORT>
    log4j.appender.sumo.proxyUser=<YOUR_USERNAME>
    log4j.appender.sumo.proxyPassword=<YOUR_PASSWORD>
    log4j.appender.sumo.proxyDomain=<YOUR_NTLM_DOMAIN>


To build:
- Run "mvn clean package" on the pom.xml in the main level of this project.
- The pom is packaging all of the dependent JAR files into one massive jar file called "uber-sumo-log4j-appender-1.0-SNAPSHOT.jar". If you do not want all of this, remove the following XML from the pom.xml file:

	<build>
		<plugins>
			<plugin>
			    <groupId>org.apache.maven.plugins</groupId>
			    <artifactId>maven-shade-plugin</artifactId>
			    <executions>
			        <execution>
			            <phase>package</phase>
			            <goals>
			                <goal>shade</goal>
			            </goals>
			        </execution>
			    </executions>
			    <configuration>
			        <finalName>uber-${artifactId}-${version}</finalName>
			    </configuration>
			</plugin>
		</plugins>
	</build>

To run this as a stand alone Java application:
- create a Java main, follow "com.sumologic.log4j.SumoLogicAppenderExample".
- place the log4j.properties file under "/src/main/resources/"
- if you created a main called "com.sumologic.log4j.SumoLogicAppenderExample", 
then run: "java -cp target/uber-sumo-log4j-appender-1.0-SNAPSHOT.jar com.sumologic.log4j.SumoLogicAppenderExample" to see it in action. 

To run this as web application make sure the log4j.properties file is in the classpath. In many cases you will want it in your "WEB-INF/lib" folder.
 
