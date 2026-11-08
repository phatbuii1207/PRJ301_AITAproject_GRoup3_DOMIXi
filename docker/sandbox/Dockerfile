FROM eclipse-temurin:8-jdk-alpine

RUN apk add --no-cache coreutils

RUN addgroup -S -g 10001 sandboxgroup \
    && adduser -S -D -H -u 10001 -G sandboxgroup sandboxuser
COPY run.sh run-java.sh /opt/aita/
RUN chmod 0555 /opt/aita/run.sh /opt/aita/run-java.sh
USER sandboxuser
WORKDIR /workspace
ENTRYPOINT ["/bin/sh", "/opt/aita/run.sh"]
CMD ["Main"]
