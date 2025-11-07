#!/bin/bash

# Load environment variables
if [ -f .env.local ]; then
    export $(cat .env.local | grep -v '^#' | xargs)
fi

# Set Java home
export JAVA_HOME=/Users/caseymanos/Library/Java/JavaVirtualMachines/openjdk-21.0.1/Contents/Home

# Run the application
mvn spring-boot:run
