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

# Initialize EB CLI with correct platform
echo "Initializing Elastic Beanstalk CLI..."
eb init -p "64bit Amazon Linux 2023 v4.4.1 running Corretto 17" --region $AWS_REGION $EB_APP_NAME

# Function to wait for environment to be ready
wait_for_environment() {
    echo "Waiting for environment to be ready..."
    TIMEOUT=300  # 5 minutes timeout
    ELAPSED=0

    while [ $ELAPSED -lt $TIMEOUT ]; do
        status=$(aws elasticbeanstalk describe-environments \
            --environment-names ${EB_ENV_NAME} \
            --query "Environments[0].Status" \
            --output text)

        echo "Current environment status: $status"

        if [ "$status" = "Ready" ]; then
            return 0
        elif [ "$status" = "Failed" ]; then
            echo "Environment is in Failed state. Getting health details..."
            aws elasticbeanstalk describe-environments \
                --environment-names ${EB_ENV_NAME} \
                --query "Environments[0].Health*" \
                --output text
            return 1
        fi

        sleep 30
        ELAPSED=$((ELAPSED+30))
    done

    echo "Timeout waiting for environment to be ready"
    return 1
}

# Check environment status before proceeding
echo "Checking environment status..."
wait_for_environment

if [ $? -ne 0 ]; then
    echo "Environment is not in a deployable state"
    exit 1
fi

# Proceed with deployment
echo "Starting deployment..."
eb use ${EB_ENV_NAME}
eb deploy --label $VERSION_LABEL --timeout 20 --verbose

# Check deployment status
DEPLOY_STATUS=$?
if [ $DEPLOY_STATUS -ne 0 ]; then
    echo "Deployment failed. Fetching recent logs..."
    eb logs --all
    exit $DEPLOY_STATUS
fi

echo "Deployment complete!"