# Use Eclipse Temurin 17 as base image (replacement for deprecated openjdk)
FROM eclipse-temurin:17-jdk

# Install Maven
RUN apt-get update && \
    apt-get install -y maven && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy pom.xml first (for dependency caching)
COPY pom.xml .


# Install itext 2.04
COPY libs/itext-2.0.4.jar /tmp/itext-2.0.4.jar
RUN mvn install:install-file \
    -Dfile=/tmp/itext-2.0.4.jar \
    -DgroupId=com.lowagie \
    -DartifactId=itext \
    -Dversion=2.0.4 \
    -Dpackaging=jar

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Copy image files needed at runtime (sbi2.jpg is loaded from filesystem)
COPY sbi.jpg ./sbi.jpg
COPY sbi2.jpg ./sbi2.jpg
COPY image.rgb ./image.rgb
COPY smask.gray ./smask.gray
COPY kotaknew.pdf ./kotaknew.pdf
COPY sbinew.pdf ./sbinew.pdf
COPY boi.pdf ./boi.pdf
COPY hdfc.pdf ./hdfc.pdf

# Build the application
RUN mvn clean package -DskipTests -B

# Expose port (Render will set PORT env var)
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "target/pdfservice-0.0.1-SNAPSHOT.jar"]

