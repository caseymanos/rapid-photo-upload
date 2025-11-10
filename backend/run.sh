#!/bin/bash

# Load environment variables
if [ -f .env.local ]; then
    set -a
    source .env.local
    set +a
fi

# Set Java home
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
export _JAVA_OPTIONS="-Djdk.lang.Process.launchMechanism=FORK"

# Run the application
mvn spring-boot:run
