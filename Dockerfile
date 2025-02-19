FROM maven:3.8.4-openjdk-17 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src /app/src

RUN mvn clean test
RUN mvn clean package -DskipTests

# Use Rocky Linux 8 as the base image
FROM rockylinux:8

# Install required dependencies and Java 17 JDK
RUN yum update -y && \
    yum install -y java-17-openjdk-devel wget && \
    yum clean all

# Set environment variables correctly
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk
ENV CATALINA_HOME=/opt/tomcat
ENV PATH="${JAVA_HOME}/bin:${CATALINA_HOME}/bin:${PATH}"

# Download and install Tomcat 10
WORKDIR /opt
RUN wget https://archive.apache.org/dist/tomcat/tomcat-10/v10.1.36/bin/apache-tomcat-10.1.36.tar.gz && \
    tar -xvzf apache-tomcat-10.1.36.tar.gz && \
    mv apache-tomcat-10.1.36 tomcat && \
    rm -f apache-tomcat-10.1.36.tar.gz

# Set permissions for Tomcat scripts
RUN chmod +x $CATALINA_HOME/bin/*.sh

WORKDIR /opt/tomcat/webapps

RUN mkdir -p /var/log/audit

COPY --from=builder /app/target/*.war /opt/tomcat/webapps/audit.war
COPY --from=builder /app/src/main/resources/logback.xml /opt/tomcat/webapps/ROOT/WEB-INF/classes/logback.xml

EXPOSE 8080

# Start Tomcat server
CMD ["catalina.sh", "run"]