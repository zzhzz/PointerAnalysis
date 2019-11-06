#!/bin/bash

set -e

CLASSPATH=$(JARS=(target/dependency/*.jar); IFS=:; echo "${JARS[*]}"):target/classes/
mvn compile

run_program () {
    echo TEST ${1}
    java -cp $CLASSPATH com.mypointeranalysis.MyPointerAnalysis ../code test.${1}
    cp result.txt result.${1}.txt
}

run_program ArrayTest
run_program CastTest
run_program FieldSensitivity
run_program Hello
run_program SubCallTest
run_program UnknownTest
