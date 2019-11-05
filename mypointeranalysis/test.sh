#!/bin/bash

set -e
# set -x

TEST_CASE=$1
if [ "$TEST_CASE"x == x ]; then
    TEST_CASE=test.Hello
fi

mvn compile
mvn dependency:copy-dependencies
mvn -Dclassifier=sources dependency:copy-dependencies

CLASSPATH=$(JARS=(target/dependency/*.jar); IFS=:; echo "${JARS[*]}"):target/classes/

echo 'Start Test'
echo '---------------------------------'
java -cp $CLASSPATH com.mypointeranalysis.MyPointerAnalysis ../code $TEST_CASE
# java -cp $CLASSPATH soot.Main -soot-classpath ../code:../code/jce.jar:../code/rt.jar -f jimple $TEST_CASE
