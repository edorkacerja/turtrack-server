#!/bin/bash

set -e

# Variables
EB_APP_NAME="turtrack-server"
EB_ENV_NAME="Turtrack-server-env"
VERSION_LABEL="${VERSION_LABEL:-v$(date +%Y%m%d-%H%M%S)}"
JAR_FILE="target/turtrack-server-0.0.1-SNAPSHOT.jar"

echo "Starting deployment process..."
echo "Application: $EB_APP_NAME"
echo "Environment: $EB_ENV_NAME"
echo "Version: $VERSION_LABEL"
echo "JAR File: $JAR_FILE"

# Ensure JAR file exists
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file $JAR_FILE not found!"
    exit 1
fi

# Clean up any previous deployment files
rm -rf deploy
mkdir -p deploy

# Copy files to deploy directory
cp $JAR_FILE deploy/application.jar

# Create Procfile
echo "web: java -Dserver.port=5000 -Dspring.profiles.active=prod -jar application.jar" > deploy/Procfile

# Create .ebextensions for configuration
mkdir -p deploy/.ebextensions

# Create simple environment configuration
cat > deploy/.ebextensions/options.config << 'EOL'
option_settings:
  aws:elasticbeanstalk:container:java:
    JVM Options: "-Xms256m -Xmx512m"
  aws:elasticbeanstalk:application:environment:
    SERVER_PORT: 5000
    SPRING_PROFILES_ACTIVE: prod
  aws:autoscaling:launchconfiguration:
    IamInstanceProfile: aws-elasticbeanstalk-ec2-role
EOL

# Create deployment package
cd deploy
zip -r ../app.zip .
cd ..

# Initialize Elastic Beanstalk environment
eb init $EB_APP_NAME \
    --region $AWS_REGION \
    --platform "64bit Amazon Linux 2023 v4.4.1 running Corretto 17"

# Deploy to Elastic Beanstalk
echo "Deploying application..."
eb deploy $EB_ENV_NAME \
    --label $VERSION_LABEL \
    --timeout 20

echo "Deployment complete!"