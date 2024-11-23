#!/bin/bash

set -e

# Variables for Elastic Beanstalk
EB_APP_NAME="turtrack-server"
EB_ENV_NAME="Turtrack-server-env"
VERSION_LABEL="${VERSION_LABEL:-v$(date +%Y%m%d-%H%M%S)}"
JAR_FILE="target/turtrack-server-0.0.1-SNAPSHOT.jar"

# Function to check environment health
check_env_health() {
    health=$(eb health $EB_ENV_NAME --refresh | grep "Overall health:" | awk '{print $3}')
    echo "Current health: $health"
    echo $health
}

# Function to handle degraded state
handle_degraded_state() {
    echo "Environment is degraded. Attempting to recover..."

    # First try: Restart the application
    echo "Attempting application restart..."
    eb restart
    sleep 30

    # Check health after restart
    if [ "$(check_env_health)" == "Green" ]; then
        echo "Environment recovered after restart!"
        return 0
    fi

    # Second try: Force deployment
    echo "Restart didn't fix it. Attempting forced deployment..."
    eb deploy --ignore-health-check
    sleep 30

    # Check health after forced deployment
    if [ "$(check_env_health)" == "Green" ]; then
        echo "Environment recovered after forced deployment!"
        return 0
    fi

    echo "Recovery attempts failed. Manual intervention may be needed."
    return 1
}

# Rest of your existing deployment script...

# Before deploying, check current health
current_health=$(check_env_health)
if [ "$current_health" != "Green" ]; then
    echo "Environment is not healthy. Attempting recovery..."
    handle_degraded_state
fi

# Proceed with deployment
echo "Creating deployment package..."
mkdir -p deploy
cp $JAR_FILE deploy/application.jar

# Create Procfile with healthcheck
echo "Creating Procfile..."
cat > deploy/Procfile << EOF
web: java -Dspring.profiles.active=prod -jar application.jar
EOF

# Create .ebextensions for environment variables and healthcheck
mkdir -p deploy/.ebextensions
cat > deploy/.ebextensions/00-options.config << EOF
option_settings:
  aws:elasticbeanstalk:application:environment:
    SPRING_PROFILES_ACTIVE: prod
    SERVER_PORT: 5000
  aws:elasticbeanstalk:environment:proxy:staticfiles:
    /health: /health
EOF

# Add healthcheck configuration
cat > deploy/.ebextensions/01-healthcheck.config << EOF
files:
    "/etc/nginx/conf.d/proxy.conf":
        mode: "000644"
        owner: root
        group: root
        content: |
            location /health {
                return 200 'OK';
                add_header Content-Type text/plain;
            }
EOF

# Create zip
cd deploy
zip -r app.zip ./ && echo "ZIP created successfully"
cd ..

# Deploy with improved error handling
echo "Deploying version $VERSION_LABEL to environment $EB_ENV_NAME..."
if ! eb deploy --label $VERSION_LABEL --timeout 20; then
    echo "Deployment failed. Checking environment health..."
    if [ "$(check_env_health)" != "Green" ]; then
        handle_degraded_state
    fi
fi

# Final health check
final_health=$(check_env_health)
if [ "$final_health" == "Green" ]; then
    echo "Deployment completed successfully!"
else
    echo "Warning: Environment is not healthy after deployment"
    eb logs --all
    exit 1
fi