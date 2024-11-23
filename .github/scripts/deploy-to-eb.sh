#!/bin/bash

set -e

# Variables for Elastic Beanstalk
EB_APP_NAME="turtrack-server"
EB_ENV_NAME="Turtrack-server-env"
VERSION_LABEL=$GITHUB_SHA
JAR_FILE="target/turtrack-server-0.0.1-SNAPSHOT.jar"

# Enhanced logging
echo "Starting deployment process..."
echo "Application: $EB_APP_NAME"
echo "Environment: $EB_ENV_NAME"
echo "Version: $VERSION_LABEL"

# Ensure JAR file exists
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file $JAR_FILE not found!"
    exit 1
fi

# Create deployment package
echo "Creating deployment package..."
mkdir -p deploy
cp $JAR_FILE deploy/application.jar
cd deploy
zip -r app.zip ./ && echo "ZIP created successfully"
cd ..

# Just initialize and use the existing environment
eb init $EB_APP_NAME --region $AWS_REGION --platform "64bit Amazon Linux 2023 v4.4.1 running Corretto 17"
eb use $EB_ENV_NAME

# Deploy the damn thing
echo "Deploying to existing environment..."
eb deploy --label $VERSION_LABEL --timeout 20 --verbose

# Check if it worked
if [ $? -ne 0 ]; then
    echo "Deployment failed. Checking logs..."
    eb logs --all
    exit 1
fi

echo "Deployment complete!"