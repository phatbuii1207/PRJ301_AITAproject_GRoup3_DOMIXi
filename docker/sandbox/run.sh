#!/bin/sh
# Only the service's generated manifest is used; no student build scripts run.
set -u
umask 077
mkdir -p /tmp/classes || exit 70
/usr/bin/timeout --signal=KILL 15s javac -J-Xms16m -J-Xmx48m -proc:none -encoding UTF-8 \
    -classpath /tmp/classes -sourcepath /workspace \
    -d /tmp/classes @/workspace/.aita-sources.args
compiled=$?
if [ "$compiled" -ne 0 ]; then
    if [ "$compiled" -eq 137 ] || [ "$compiled" -eq 124 ]; then
        echo 'Compilation exceeded its separate 15 second budget.' >&2
    fi
    exit 65
fi
# The child wrapper maps every student nonzero exit to 70, including exit(124),
# so only the trusted timeout supervisor can produce the runtime timeout verdict.
/usr/bin/timeout --signal=KILL 5s /bin/sh /opt/aita/run-java.sh "$1"
result=$?
if [ "$result" -eq 137 ] || [ "$result" -eq 124 ]; then
    exit 124
fi
exit "$result"
