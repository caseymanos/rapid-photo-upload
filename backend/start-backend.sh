#!/bin/bash

# Set Java environment
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
export _JAVA_OPTIONS="-Djdk.lang.Process.launchMechanism=FORK"

# Load environment variables from .env.local
if [ -f .env.local ]; then
    set -a
    source .env.local
    set +a
else
    echo "Warning: .env.local not found. Please create it with required environment variables."
    exit 1
fi

# Run Spring Boot
mvn spring-boot:run -DskipTests
