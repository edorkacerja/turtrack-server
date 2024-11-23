#!/bin/bash

set -e

# Variables for Elastic Beanstalk
EB_APP_NAME="turtrack-server"
EB_ENV_NAME="Turtrack-server-env"
VERSION_LABEL="${VERSION_LABEL:-v$(date +%Y%m%d-%H%M%S)}"  # Use provided version or generate timestamp
JAR_FILE="target/turtrack-server-0.0.1-SNAPSHOT.jar"

# Clean up previous deployment files
rm -rf deploy
rm -f app.zip

# Enhanced logging
echo "Starting deployment process..."
echo "Application: $EB_APP_NAME"
echo "Environment: $EB_ENV_NAME"
echo "Version: $VERSION_LABEL"
echo "JAR file: $JAR_FILE"

# Ensure JAR file exists
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file $JAR_FILE not found!"
    echo "Current directory: $(pwd)"
    echo "Directory contents: $(ls -la target/)"
    exit 1
fi

# Create deployment package
echo "Creating deployment package..."
mkdir -p deploy
cp $JAR_FILE deploy/application.jar
cd deploy
zip -r app.zip ./ && echo "ZIP created successfully"
cd ..

# Initialize EB CLI with proper settings
echo "Initializing EB CLI..."
eb init $EB_APP_NAME \
    --region $AWS_REGION \
    --platform "64bit Amazon Linux 2023 v4.4.1 running Corretto 17"

# Use the environment
echo "Selecting environment..."
eb use $EB_ENV_NAME

# Deploy with the new version label
echo "Deploying version $VERSION_LABEL to environment $EB_ENV_NAME..."
eb deploy \
    --label $VERSION_LABEL \
    --timeout 20 \
    --verbose

# Check deployment status
if [ $? -ne 0 ]; then
    echo "Deployment failed!"
    echo "Fetching logs..."
    eb logs --all
    exit 1
fi

echo "Deployment completed successfully!"
echo "Version $VERSION_LABEL is now live on $EB_ENV_NAME"