#!/bin/bash

set -e

# Variables for Elastic Beanstalk
EB_APP_NAME="turtrack-server"
EB_ENV_NAME="Turtrack-server-env"
VERSION_LABEL=$GITHUB_SHA
JAR_FILE="target/turtrack-server-0.0.1-SNAPSHOT.jar"

# Ensure JAR file exists
if [ ! -f "$JAR_FILE" ]; then
  echo "Error: JAR file $JAR_FILE not found!"
  exit 1
fi

# Create a ZIP file containing the JAR
mkdir -p deploy
cp $JAR_FILE deploy/
cd deploy
zip -r app.zip ./
cd ..

# Initialize and deploy using EB CLI
eb init -p "java" --region $AWS_REGION $EB_APP_NAME
eb use $EB_ENV_NAME
eb deploy --label $VERSION_LABEL
