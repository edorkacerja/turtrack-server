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
echo "web: java -Dserver.port=9999 -Dspring.profiles.active=prod -jar application.jar" > deploy/Procfile

# Create .ebextensions for configuration
mkdir -p deploy/.ebextensions

# Create environment configuration
cat > deploy/.ebextensions/00-options.config << 'EOL'
option_settings:
  aws:elasticbeanstalk:application:environment:
    SERVER_PORT: 9999
    SPRING_PROFILES_ACTIVE: prod
  aws:elasticbeanstalk:container:java:
    JVM Options: "-Xms512m -Xmx1024m"
  aws:autoscaling:launchconfiguration:
    InstanceType: t2.micro
    SecurityGroups: default
  aws:ec2:vpc:
    VPCId: null
    Subnets: null
  aws:elasticbeanstalk:environment:
    EnvironmentType: SingleInstance
EOL

# Create nginx configuration
cat > deploy/.ebextensions/01-nginx.config << 'EOL'
files:
  "/etc/nginx/conf.d/proxy.conf":
    mode: "000644"
    owner: root
    group: root
    content: |
      client_max_body_size 20M;

      server {
        listen 80;

        location / {
          proxy_pass http://127.0.0.1:9999;
          proxy_set_header Connection "";
          proxy_http_version 1.1;
          proxy_set_header Host $host;
          proxy_set_header X-Real-IP $remote_addr;
          proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
          proxy_set_header X-Forwarded-Proto $scheme;
          proxy_buffer_size 128k;
          proxy_buffers 4 256k;
          proxy_busy_buffers_size 256k;
        }
      }

container_commands:
  01_reload_nginx:
    command: "sudo service nginx reload"
EOL

# Create JVM configuration
cat > deploy/.ebextensions/02-java.config << 'EOL'
commands:
  01_set_java_home:
    command: echo 'export JAVA_HOME=/usr/lib/jvm/java-17' >> /etc/profile.d/java.sh
EOL

# Create deployment package
cd deploy
zip -r ../app.zip .
cd ..

# Initialize Elastic Beanstalk environment if not already initialized
eb init $EB_APP_NAME \
    --region $AWS_REGION \
    --platform "64bit Amazon Linux 2023 v4.4.1 running Corretto 17"

# Deploy to Elastic Beanstalk
echo "Deploying application..."
eb deploy $EB_ENV_NAME \
    --label $VERSION_LABEL \
    --timeout 20

echo "Deployment complete!"