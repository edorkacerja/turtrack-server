#!/bin/bash

set -e

# Variables
EB_APP_NAME="turtrack-server"
EB_ENV_NAME="Turtrack-server-env"
VERSION_LABEL="${VERSION_LABEL:-v$(date +%Y%m%d-%H%M%S)}"
JAR_FILE="target/turtrack-server-0.0.1-SNAPSHOT.jar"
AWS_REGION="${AWS_REGION:-us-east-1}"

echo "Starting deployment process..."
echo "Application: $EB_APP_NAME"
echo "Environment: $EB_ENV_NAME"
echo "Version: $VERSION_LABEL"

# Clean and prepare deployment directory
rm -rf deploy
mkdir -p deploy
mkdir -p deploy/.ebextensions

# Copy application jar
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file $JAR_FILE not found."
    exit 1
fi
cp "$JAR_FILE" deploy/application.jar

# Create Procfile
echo "web: java -jar application.jar" > deploy/Procfile

# Create VPC configuration
cat > deploy/.ebextensions/vpc.config << 'EOL'
option_settings:
  aws:autoscaling:launchconfiguration:
    IamInstanceProfile: aws-elasticbeanstalk-ec2-role
    SecurityGroups: eb-ec2-1

  aws:ec2:vpc:
    VPCId: vpc-080b3b2013d730253
    Subnets: subnet-0345499473ca6aadd
    AssociatePublicIpAddress: true
EOL

# Create environment configuration
cat > deploy/.ebextensions/env.config << 'EOL'
option_settings:
  aws:elasticbeanstalk:application:environment:
    SERVER_PORT: 9999
    SPRING_PROFILES_ACTIVE: prod
    SPRING_DATASOURCE_URL: jdbc:postgresql://turtrack-db.cbo24t7isrjl.us-east-1.rds.amazonaws.com:5432/turtrack
    SPRING_DATASOURCE_USERNAME: postgres
    SPRING_DATASOURCE_PASSWORD: postgres

  aws:elasticbeanstalk:container:java:
    JVM Options: "-Xms512m -Xmx1024m"

  aws:elasticbeanstalk:environment:
    EnvironmentType: SingleInstance
    ServiceRole: aws-elasticbeanstalk-service-role
EOL

# Create nginx configuration
cat > deploy/.ebextensions/nginx.config << 'EOL'
files:
  "/etc/nginx/conf.d/proxy.conf":
    mode: "000644"
    owner: root
    group: root
    content: |
      upstream springboot {
        server 127.0.0.1:9999;
        keepalive 256;
      }

      server {
        listen 80;

        location / {
            proxy_pass  http://springboot;
            proxy_set_header   Connection "";
            proxy_http_version 1.1;
            proxy_set_header        Host            $host;
            proxy_set_header        X-Real-IP       $remote_addr;
            proxy_set_header        X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header        X-Forwarded-Proto $scheme;
        }
      }

container_commands:
  01_reload_nginx:
    command: "service nginx reload"
EOL

# Create deployment package
cd deploy
zip -r ../deploy.zip .
cd ..

# Initialize Elastic Beanstalk application
eb init $EB_APP_NAME \
    --region $AWS_REGION \
    --platform "64bit Amazon Linux 2023 v4.4.1 running Corretto 17"

# Check if environment exists
if ! eb status "$EB_ENV_NAME" &>/dev/null; then
    echo "Creating new environment..."
    eb create "$EB_ENV_NAME" \
        --vpc \
        --vpc.id vpc-080b3b2013d730253 \
        --vpc.ec2subnets subnet-0345499473ca6aadd \
        --vpc.securitygroups eb-ec2-1 \
        --platform "64bit Amazon Linux 2023 v4.4.1 running Corretto 17" \
        --version "$VERSION_LABEL"
else
    echo "Deploying to existing environment..."

    # Check if the version already exists
    if aws elasticbeanstalk describe-application-versions \
        --application-name "$EB_APP_NAME" \
        --version-labels "$VERSION_LABEL" \
        --region "$AWS_REGION" &>/dev/null; then
        echo "Version $VERSION_LABEL already exists. Proceeding with deployment..."
    else
        echo "Uploading new version..."
        aws elasticbeanstalk create-application-version \
            --application-name "$EB_APP_NAME" \
            --version-label "$VERSION_LABEL" \
            --source-bundle S3Bucket="elasticbeanstalk-$AWS_REGION",S3Key="deploy.zip" \
            --region "$AWS_REGION"
    fi

    # Deploy the version
    aws elasticbeanstalk update-environment \
        --environment-name "$EB_ENV_NAME" \
        --version-label "$VERSION_LABEL" \
        --use-existing-version-if-available
fi

echo "Deployment initiated!"
