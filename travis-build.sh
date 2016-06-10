#!/bin/bash
EXIT_STATUS=0

./gradlew --stop


./gradlew grails-datastore-gorm-neo4j:test -no-daemon  || EXIT_STATUS=$?
if [[ $EXIT_STATUS -eq 0 ]]; then
    ./gradlew grails2-plugins/neo4j:test -no-daemon || EXIT_STATUS=$?
fi
if [[ $EXIT_STATUS -eq 0 ]]; then -no-daemon
    ./gradlew boot-plugins/gorm-neo4j-spring-boot:test || EXIT_STATUS=$?
fi

./gradlew --stop
./travis-publish.sh || EXIT_STATUS=$?

exit $EXIT_STATUS



