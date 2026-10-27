#!/bin/sh
# Starts the Spring Boot API on port 8080.
cd "$(dirname "$0")"

if [ -z "$JAVA_HOME" ] && [ -d "/opt/homebrew/opt/openjdk@17" ]; then
  export JAVA_HOME="/opt/homebrew/opt/openjdk@17"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

mvn spring-boot:run
