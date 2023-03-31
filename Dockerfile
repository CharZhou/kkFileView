FROM maven:3.9 as builder
MAINTAINER charzhou "17255546+charzhou@users.noreply.github.com"
WORKDIR /app
ADD . /app
RUN mvn clean package -T 1C -Dmaven.test.skip=true

FROM keking/kkfileview-jdk:4.1.1
MAINTAINER chenjh "842761733@qq.com"
COPY --from=builder /app/server/target/kkFileView-*.tar.gz /opt/
RUN tar -zxvf /opt/kkFileView-4.2.0-SNAPSHOT.tar.gz -C /opt/
ENV KKFILEVIEW_BIN_FOLDER /opt/kkFileView-4.2.0-SNAPSHOT/bin
ENTRYPOINT ["java","-Dfile.encoding=UTF-8","-Dspring.config.location=/opt/kkFileView-4.2.0-SNAPSHOT/config/application.properties","-jar","/opt/kkFileView-4.2.0-SNAPSHOT/bin/kkFileView-4.2.0-SNAPSHOT.jar"]
