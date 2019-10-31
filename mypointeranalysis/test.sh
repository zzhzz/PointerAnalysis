#!/bin/bash

set -e
set -x

mvn compile
mvn dependency:copy-dependencies
mvn -Dclassifier=sources dependency:copy-dependencies

CLASSPATH=$(JARS=(target/dependency/*.jar); IFS=:; echo "${JARS[*]}"):target/classes
echo 'xxx'
java -cp $CLASSPATH com.mypointeranalysis.MyPointerAnalysis ../code test.Hello
