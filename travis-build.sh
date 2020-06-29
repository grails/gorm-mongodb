#!/bin/bash
EXIT_STATUS=0

if [[ $TRAVIS_TAG =~ ^v[[:digit:]] ]]; then
    echo "Skipping tests to Publish release"
    ./travis-publish.sh || EXIT_STATUS=$?
else
	./gradlew --no-daemon compileGroovy --console=plain || EXIT_STATUS=$?
    if [ $EXIT_STATUS -ne 0 ]; then
        exit $EXIT_STATUS
    fi
	./gradlew --no-daemon compileTestGroovy --console=plain || EXIT_STATUS=$?
    if [ $EXIT_STATUS -ne 0 ]; then
        exit $EXIT_STATUS
    fi
    ./gradlew -Dgeb.env=chromeHeadless check --no-daemon --console=plain || EXIT_STATUS=$?
    if [ $EXIT_STATUS -ne 0 ]; then
        exit $EXIT_STATUS
    fi
    ./travis-publish.sh || EXIT_STATUS=$?
fi

exit $EXIT_STATUS



