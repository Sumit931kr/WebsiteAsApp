# Use Ubuntu as base image
FROM ubuntu:22.04

# Set environment variables
ENV DEBIAN_FRONTEND=noninteractive
ENV ANDROID_HOME=/opt/android-sdk
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${ANDROID_HOME}/build-tools

# Install system dependencies
RUN apt-get update && apt-get install -y \
    wget \
    unzip \
    git \
    curl \
    openjdk-17-jdk \
    build-essential \
    file \
    && rm -rf /var/lib/apt/lists/*

# Set Java environment
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH=${PATH}:${JAVA_HOME}/bin

# Create Android SDK directory
RUN mkdir -p ${ANDROID_HOME}

# Download and install Android SDK Command Line Tools
RUN wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/cmdtools.zip && \
    unzip -q /tmp/cmdtools.zip -d /tmp && \
    mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    mv /tmp/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
    rm /tmp/cmdtools.zip

# Accept licenses and install required SDK components
RUN yes | sdkmanager --licenses
RUN sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# Set working directory
WORKDIR /app

# Copy Gradle wrapper and build files first (for better caching)
COPY gradlew gradlew.bat gradle.properties settings.gradle.kts build.gradle.kts ./
COPY gradle/ ./gradle/

# Make Gradle wrapper executable
RUN chmod +x gradlew

# Copy app build configuration
COPY app/build.gradle.kts ./app/
COPY app/proguard-rules.pro ./app/

# Download dependencies (this layer will be cached if dependencies don't change)
RUN ./gradlew dependencies

# Copy source code and resources
COPY app/src/ ./app/src/
COPY app/.gitignore ./app/

# Build the project
RUN ./gradlew assembleDebug

# Create output directory
RUN mkdir -p /output

# Copy built APK to output directory
RUN cp app/build/outputs/apk/debug/app-debug.apk /output/

# Set default command to build the app
CMD ["./gradlew", "assembleDebug"]