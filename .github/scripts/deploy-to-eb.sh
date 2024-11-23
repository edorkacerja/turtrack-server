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

# Clean up any previous deployment files
rm -rf deploy
mkdir -p deploy
mkdir -p deploy/.ebextensions

# Copy files to deploy directory
cp $JAR_FILE deploy/application.jar

# Create Procfile
echo "web: java -jar application.jar" > deploy/Procfile

# Create VPC configuration
cat > deploy/.ebextensions/vpc.config << 'EOL'
option_settings:
  aws:ec2:vpc:
    VPCId: vpc-080b3b2013d730253
    Subnets: subnet-0345499473ca6aadd
    AssociatePublicIpAddress: true

  aws:autoscaling:launchconfiguration:
    SecurityGroups: ec2-rds-1,rds-ec2-1
    IamInstanceProfile: aws-elasticbeanstalk-ec2-role
EOL

# Create environment configuration
cat > deploy/.ebextensions/env.config << 'EOL'
option_settings:
  aws:elasticbeanstalk:application:environment:
    SERVER_PORT: 9999
    SPRING_PROFILES_ACTIVE: prod
    SPRING_DATASOURCE_TURTRACK_URL: jdbc:postgresql://turtrack-db.cbo24t7isrjl.us-east-1.rds.amazonaws.com:5432/turtrack
    SPRING_DATASOURCE_TURTRACK_USERNAME: postgres
    SPRING_DATASOURCE_TURTRACK_PASSWORD: postgres
    SPRING_DATASOURCE_TURTRACK_DRIVER_CLASS_NAME: org.postgresql.Driver

  aws:elasticbeanstalk:container:java:
    JVM Options: "-Xms512m -Xmx1024m"
EOL

# Create database security configuration
cat > deploy/.ebextensions/db-security.config << 'EOL'
Resources:
  AWSEBSecurityGroup:
    Type: AWS::EC2::SecurityGroupIngress
    Properties:
      GroupId: sg-0dd2066c2023ed882
      IpProtocol: tcp
      FromPort: 5432
      ToPort: 5432
      SourceSecurityGroupId: {"Fn::GetAtt" : ["AWSEBSecurityGroup", "GroupId"]}
EOL

# Create nginx configuration for proper proxy setup
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
            proxy_buffer_size       128k;
            proxy_buffers          4 256k;
            proxy_busy_buffers_size 256k;
        }
      }

container_commands:
  01_reload_nginx:
    command: "sudo service nginx reload"
EOL

# Create health check configuration
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