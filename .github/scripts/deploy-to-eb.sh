#!/bin/bash

set -e

# Variables for Elastic Beanstalk
EB_APP_NAME="turtrack-server"
EB_ENV_NAME="Turtrack-server-env"
VERSION_LABEL="${VERSION_LABEL:-v$(date +%Y%m%d-%H%M%S)}"
JAR_FILE="target/turtrack-server-0.0.1-SNAPSHOT.jar"

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

# Create simple .ebextensions for environment variables
mkdir -p deploy/.ebextensions
cat > deploy/.ebextensions/env.config << EOF
option_settings:
  aws:elasticbeanstalk:application:environment:
    SPRING_PROFILES_ACTIVE: prod
    SERVER_PORT: 5000
EOF

# Create deployment package
cd deploy
zip -r ../app.zip .
cd ..

# Initialize EB CLI
eb init $EB_APP_NAME \
    --region $AWS_REGION \
    --platform "64bit Amazon Linux 2023 v4.4.1 running Corretto 17"

# Deploy
echo "Deploying to environment..."
eb deploy $EB_ENV_NAME \
    --label $VERSION_LABEL \
    --timeout 20

echo "Deployment complete!"