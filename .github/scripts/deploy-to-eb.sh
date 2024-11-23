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
cp $JAR_FILE deploy/application.jar  # Renamed to application.jar for auto-detection
cd deploy
zip -r app.zip ./ && echo "ZIP created successfully"
cd ..

# Initialize EB CLI with correct platform and use existing environment
echo "Initializing Elastic Beanstalk CLI..."
eb init -p "64bit Amazon Linux 2023 v4.4.1 running Corretto 17" --region $AWS_REGION $EB_APP_NAME
eb use ${EB_ENV_NAME}

# Deploy to existing environment
echo "Starting deployment..."
eb deploy --label $VERSION_LABEL --timeout 20 --verbose

# Check deployment status
DEPLOY_STATUS=$?
if [ $DEPLOY_STATUS -ne 0 ]; then
    echo "Deployment failed. Fetching recent logs..."
    eb logs --all
    exit $DEPLOY_STATUS
fi

echo "Deployment complete!"