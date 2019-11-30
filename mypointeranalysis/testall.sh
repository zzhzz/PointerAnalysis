#!/bin/bash

set -e

CLASSPATH=$(JARS=(target/dependency/*.jar); IFS=:; echo "${JARS[*]}"):target/classes/
mvn compile

run_program () {
    echo TEST ${1}
    java -cp $CLASSPATH com.mypointeranalysis.MyPointerAnalysis ../code test.${1} > run.txt
    cp result.txt result.${1}.txt
    cp run.txt run.${1}.txt
}

run_program ArrayTest
run_program CastTest
run_program ExceptionTest
run_program FieldSensitivity
run_program Hello
run_program LinkList
run_program MultiArray
run_program RecRef
run_program RecTest
run_program SubCallTest
run_program UnknownTest
