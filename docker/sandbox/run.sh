#!/bin/sh
# Only the service's generated manifest is used; no student build scripts run.
set -u
umask 077
mkdir -p /tmp/classes || exit 70
if ! javac -J-Xms16m -J-Xmx48m -proc:none -encoding UTF-8 \
    -classpath /tmp/classes -sourcepath /workspace \
    -d /tmp/classes @/workspace/.aita-sources.args; then
    exit 65
fi
java -Xms16m -Xmx48m -XX:MaxMetaspaceSize=32m \
    -XX:ReservedCodeCacheSize=16m -XX:ActiveProcessorCount=1 \
    -Djava.io.tmpdir=/tmp -cp /tmp/classes "$1"
result=$?
# Do not let System.exit(65/124/...) masquerade as a compile error or TLE.
if [ "$result" -ne 0 ]; then
    exit 70
fi
