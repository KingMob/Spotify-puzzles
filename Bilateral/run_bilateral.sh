#!/bin/bash
F=$1
cat $F | java $2
cat $F | java -ea -agentlib:hprof=cpu=times,interval=5,file=java.hprof.$F $2 >/dev/null
