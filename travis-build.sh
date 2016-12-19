#!/bin/bash
EXIT_STATUS=0

./gradlew --stop
./gradlew --no-daemon compileGroovy
./gradlew --no-daemon compileTestGroovy

if [[ $TRAVIS_TAG =~ ^v[[:digit:]] ]]; then
	echo "Skipping tests to Publish release"
	./travis-publish.sh || EXIT_STATUS=$?
else
	./gradlew check --refresh-dependencies -no-daemon -x grails2-plugin:test -x gorm-mongodb-spring-boot:test  || EXIT_STATUS=$?
	if [[ $EXIT_STATUS -eq 0 ]]; then
	    ./gradlew grails2-plugin:test --refresh-dependencies -no-daemon || EXIT_STATUS=$?
	fi
	if [[ $EXIT_STATUS -eq 0 ]]; then 
	    ./gradlew gorm-mongodb-spring-boot:test --refresh-dependencies -no-daemon || EXIT_STATUS=$?
	fi

	./gradlew --stop

	if [[ $EXIT_STATUS -eq 0 ]]; then
	    ./travis-publish.sh || EXIT_STATUS=$?
	fi

fi

exit $EXIT_STATUS



