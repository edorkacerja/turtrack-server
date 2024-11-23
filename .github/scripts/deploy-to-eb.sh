#!/bin/bash

set -e

# Variables
EB_APP_NAME="turtrack-server"
EB_ENV_NAME="Turtrack-server-env"
VERSION_LABEL="${VERSION_LABEL:-v$(date +%Y%m%d-%H%M%S)}"
JAR_FILE="target/turtrack-server-0.0.1-SNAPSHOT.jar"
SPRING_PROFILES_ACTIVE=prod

echo "Starting deployment process..."
echo "Application: $EB_APP_NAME"
echo "Environment: $EB_ENV_NAME"
echo "Version: $VERSION_LABEL"

# Ensure JAR file exists
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file $JAR_FILE not found!"
    exit 1
fi

# Clean up any previous deployment files
rm -rf deploy
mkdir -p deploy

# Copy the JAR
cp $JAR_FILE deploy/application.jar

# Create deployment package
cd deploy
zip -r ../app.zip .
cd ..

## Initialize Elastic Beanstalk environment if not already initialized
#eb init $EB_APP_NAME \
#    --region $AWS_REGION \
#    --platform "64bit Amazon Linux 2023 v4.4.1 running Corretto 17"

# Set environment variables
eb setenv SPRING_PROFILES_ACTIVE=prod SERVER_PORT=9999

# Deploy to Elastic Beanstalk
echo "Deploying application..."
eb deploy $EB_ENV_NAME \
    --label $VERSION_LABEL \
    --timeout 20

echo "Deployment complete!"
