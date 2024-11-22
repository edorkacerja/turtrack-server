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

# Initialize EB CLI
eb init -p "Corretto 17 running on 64bit Amazon Linux 2" --region $AWS_REGION $EB_APP_NAME

# Check if environment exists and create if it doesn't
if ! eb list | grep -q "^${EB_ENV_NAME}$"; then
    echo "Environment ${EB_ENV_NAME} not found. Creating..."
    eb create ${EB_ENV_NAME} --platform "Corretto 17 running on 64bit Amazon Linux 2"
else
    # Check environment health and status
    echo "Checking environment status..."
    status=$(aws elasticbeanstalk describe-environments --environment-names ${EB_ENV_NAME} --query "Environments[0].Status" --output text)
    health=$(aws elasticbeanstalk describe-environments --environment-names ${EB_ENV_NAME} --query "Environments[0].Health" --output text)

    echo "Environment status: $status"
    echo "Environment health: $health"

    if [ "$status" != "Ready" ]; then
        echo "Environment is not ready (Status: $status). Waiting for it to be ready..."
        while [ "$status" != "Ready" ]; do
            sleep 30
            status=$(aws elasticbeanstalk describe-environments --environment-names ${EB_ENV_NAME} --query "Environments[0].Status" --output text)
            echo "Current status: $status"
        done
    fi
fi

# Use the environment
eb use ${EB_ENV_NAME}

# Deploy the application
echo "Deploying application..."
eb deploy --label $VERSION_LABEL

echo "Deployment complete!"