#!/bin/bash

set -e
set -x

find . -name '*.java' --exec javac {} \;
