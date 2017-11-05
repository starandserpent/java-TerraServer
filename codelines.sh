#!/bin/sh
find */src/*/java/ -name '*.java' | xargs wc -l
