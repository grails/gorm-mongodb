#!/bin/bash
EXIT_STATUS=0


if [[ $TRAVIS_TAG =~ ^v[[:digit:]] ]]; then
	echo "Skipping tests to Publish release"
	./travis-publish.sh || EXIT_STATUS=$?
else
	./gradlew --no-daemon compileGroovy
	./gradlew --no-daemon compileTestGroovy

	./gradlew -Dgeb.env=chromeHeadless check -no-daemon -x gorm-mongodb-spring-boot:test  || EXIT_STATUS=$?
	if [[ $EXIT_STATUS -eq 0 ]]; then 
	    ./gradlew gorm-mongodb-spring-boot:test -no-daemon || EXIT_STATUS=$?
	fi

	if [[ $EXIT_STATUS -eq 0 ]]; then
	    ./travis-publish.sh || EXIT_STATUS=$?
	fi

fi

exit $EXIT_STATUS



