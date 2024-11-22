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

# Initialize EB CLI with the correct platform
eb init -p "Corretto 17 running on 64bit Amazon Linux 2" --region $AWS_REGION $EB_APP_NAME

# Function to wait for environment to be ready
wait_for_environment() {
    echo "Waiting for environment to be ready..."
    while true; do
        status=$(aws elasticbeanstalk describe-environments \
            --environment-names ${EB_ENV_NAME} \
            --query "Environments[0].Status" \
            --output text)

        echo "Current environment status: $status"

        if [ "$status" = "Ready" ]; then
            break
        elif [ "$status" = "Failed" ]; then
            echo "Environment is in Failed state. Please check AWS Console for details."
            exit 1
        fi

        echo "Waiting 30 seconds before next check..."
        sleep 30
    done
}

# Check environment status before proceeding
echo "Checking environment status..."
current_status=$(aws elasticbeanstalk describe-environments \
    --environment-names ${EB_ENV_NAME} \
    --query "Environments[0].Status" \
    --output text)

echo "Environment status: $current_status"

if [ "$current_status" != "Ready" ]; then
    echo "Environment is not in Ready state. Waiting for it to become ready..."
    wait_for_environment
fi

# Now proceed with the deployment
echo "Environment is ready. Starting deployment..."
eb use ${EB_ENV_NAME}
eb deploy --label $VERSION_LABEL --timeout 20

echo "Deployment complete!"