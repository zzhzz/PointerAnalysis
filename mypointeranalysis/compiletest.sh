#!/bin/bash

cd ../code/
find * -name '*.java' -print0 | xargs -0 javac 
