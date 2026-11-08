#!/bin/sh
java -Xms16m -Xmx48m -XX:MaxMetaspaceSize=32m \
    -XX:ReservedCodeCacheSize=16m -XX:ActiveProcessorCount=1 \
    -Djava.io.tmpdir=/tmp -cp /tmp/classes "$1"
result=$?
if [ "$result" -ne 0 ]; then
    exit 70
fi
exit 0
