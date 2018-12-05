#!/bin/bash
EXIT_STATUS=0


if [[ $TRAVIS_TAG =~ ^v[[:digit:]] ]]; then
	echo "Skipping tests to Publish release"
	./travis-publish.sh || EXIT_STATUS=$?
else
	./gradlew --stop
	./gradlew --no-daemon compileGroovy
	./gradlew --no-daemon compileTestGroovy

	./gradlew check --refresh-dependencies -no-daemon -x gorm-mongodb-spring-boot:test  || EXIT_STATUS=$?
	if [[ $EXIT_STATUS -eq 0 ]]; then 
	    ./gradlew gorm-mongodb-spring-boot:test --refresh-dependencies -no-daemon || EXIT_STATUS=$?
	fi

	./gradlew --stop

	if [[ $EXIT_STATUS -eq 0 ]]; then
	    ./travis-publish.sh || EXIT_STATUS=$?
	fi

fi

exit $EXIT_STATUS



