cd ../rest
JAVA_OPTS="-Xmx14G  -XX:+UseConcMarkSweepGC -XX:MaxPermSize=7G"
MAVEN_OPTS="-Xmx14G -XX:+UseConcMarkSweepGC -XX:MaxPermSize=7G"
SCALA_OPTS="-Xmx14G -XX:+UseConcMarkSweepGC -XX:MaxPermSize=7G"
mvn scala:run -Dlog4j.configuration=file:../core/src/main/resources/log4j.properties -DmainClass=org.dbpedia.spotlight.web.rest.Server -DaddArgs=../conf/server.properties
