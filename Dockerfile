# Step 1: Use Maven to build and run the tests
FROM maven:3.8.4-openjdk-17 AS builder

WORKDIR /app

# Copy the pom.xml and install dependencies (to leverage Docker layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the source code into the container
COPY src /app/src

# Run tests during the build process
RUN mvn clean test

# Step 2: Package the application into a WAR file (skip tests)
RUN mvn clean package -DskipTests

RUN WAR_FILE_NAME=$(mvn help:evaluate -Dexpression=project.artifactId -q -DforceStdout)-$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout).war

# Step 3: Use a Tomcat base image to deploy the WAR file
FROM tomcat:9.0-jdk17-openjdk-slim

WORKDIR /usr/local/tomcat/webapps

# Copy the WAR file from the builder stage into Tomcat's webapps directory
COPY --from=builder /app/target/${WAR_FILE_NAME} /usr/local/tomcat/webapps/ROOT.war

# Expose the default Tomcat port (8080)
EXPOSE 8080

# Start Tomcat
CMD ["catalina.sh", "run"]