#!/bin/bash

set -e

# Variables
EB_APP_NAME="turtrack-server"
EB_ENV_NAME="Turtrack-server-env"
VERSION_LABEL="${VERSION_LABEL:-v$(date +%Y%m%d-%H%M%S)}"
JAR_FILE="target/turtrack-server-0.0.1-SNAPSHOT.jar"

echo "Starting deployment process..."

# Clean up any previous deployment files
rm -rf deploy
mkdir -p deploy

# Copy files to deploy directory
cp $JAR_FILE deploy/application.jar

# Create Procfile with explicit Java command
echo "web: java -Dspring.profiles.active=prod -Dserver.port=9999 -Dspring.config.location=classpath:/application.yml,classpath:/application-prod.yml -jar application.jar" > deploy/Procfile

# Create .ebextensions for configuration
mkdir -p deploy/.ebextensions

# Create environment configuration
cat > deploy/.ebextensions/options.config << 'EOL'
option_settings:
  aws:elasticbeanstalk:container:java:
    JVM Options: "-Xms256m -Xmx512m"
  aws:elasticbeanstalk:application:environment:
    SPRING_PROFILES_ACTIVE: prod
    SPRING_CONFIG_LOCATION: classpath:/application.yml,classpath:/application-prod.yml
    SERVER_PORT: 5000
  aws:elasticbeanstalk:environment:proxy:staticfiles:
    /static: static
EOL

# Create healthcheck configuration
cat > deploy/.ebextensions/healthcheck.config << 'EOL'
option_settings:
  aws:elasticbeanstalk:application:
    Application Healthcheck URL: /actuator/health
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