FROM openjdk:8u212-jre-alpine3.9
RUN echo http://mirror.yandex.ru/mirrors/alpine/v3.9/main > /etc/apk/repositories; \
    echo http://mirror.yandex.ru/mirrors/alpine/v3.9/community >> /etc/apk/repositories

RUN apk update && apk add jq && apk add bash && apk add curl

ADD target/messages-fat.jar /service.jar
#ADD cluster.xml /cluster.xml

RUN mkdir /realm
ADD realm /opt/realm
ADD docker-entrypoint.sh /docker-entrypoint.sh

WORKDIR /

EXPOSE 5701
EXPOSE 5705
EXPOSE 5709
EXPOSE 15709
EXPOSE 8089
EXPOSE 8099

HEALTHCHECK --interval=10s --timeout=3s --retries=15 CMD curl -f / http://localhost:8099/version || exit 1

ENTRYPOINT [ "/docker-entrypoint.sh" ]

