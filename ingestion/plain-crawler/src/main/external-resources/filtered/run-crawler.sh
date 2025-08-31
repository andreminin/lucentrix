#!/bin/sh

printHelp() {
echo
echo "Metaframe crawler v ${project.version}"
echo
echo "Usage: ./run-crawler.sh"
echo
echo
}

if [ "$1" = '--help' ]; then
        printHelp
        exit 1
fi

if [ -f /.dockerenv ]; then
    echo "Running within a container. Using container memory limitations."
    JAVA_MEM_OPTS="-XX:MaxRAMPercentage=75.0";
else
    JAVA_MEM_OPTS="-Xmx3072m -XX:MaxGCPauseMillis=200 -XX:+UseG1GC"
fi

JAVA_DEBUG_OPTS=""
#JAVA_DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
CLASSNAME=org.lucentrix.metaframe.crawler.Crawler
LOGGER_CONFIG=config/logback.xml
JAVA_OPTS="${JAVA_MEM_OPTS} ${JAVA_DEBUG_OPTS} -Dlogback.configurationFile=${LOG_FILE}"

echo Starting ${JAVA_HOME}/bin/java ${JAVA_OPTS} -cp "lib/*:config/*:." ${CLASSNAME}

${JAVA_HOME}/bin/java ${JAVA_OPTS} -cp "lib/*:config/*:." -Dlogback.configurationFile=${LOGGER_CONFIG} ${CLASSNAME}

exit 0