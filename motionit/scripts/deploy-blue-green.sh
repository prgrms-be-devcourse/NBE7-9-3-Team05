#!/bin/bash
set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

DOCKER_IMAGE="minibrb/motionit-backend"
BLUE_CONTAINER="motionit-blue"
GREEN_CONTAINER="motionit-green"
BLUE_PORT=8080
GREEN_PORT=8081
NGINX_CONF="/etc/nginx/sites-available/motionit"
HEALTH_CHECK_ENDPOINT="/actuator/health"

echo -e "${BLUE}=================================${NC}"
echo -e "${BLUE}  Blue-Green Deployment${NC}"
echo -e "${BLUE}=================================${NC}"

echo -e "\n${YELLOW}[0/7] Checking Docker network...${NC}"
if ! docker network ls | grep -q "motionit-network"; then
    docker network create motionit-network
    echo -e "${GREEN}Docker network created${NC}"
else
    echo -e "${GREEN}Docker network exists${NC}"
fi

if ! docker network inspect motionit-network | grep -q "motionit-mysql"; then
    docker network connect motionit-network motionit-mysql || true
    echo -e "${GREEN}MySQL connected to network${NC}"
fi

echo -e "\n${YELLOW}[1/7] Checking current active container...${NC}"

if docker ps --format '{{.Names}}' | grep -q "^${BLUE_CONTAINER}$"; then
    CURRENT_CONTAINER=$BLUE_CONTAINER
    CURRENT_PORT=$BLUE_PORT
    NEW_CONTAINER=$GREEN_CONTAINER
    NEW_PORT=$GREEN_PORT
    CURRENT_COLOR="${BLUE}BLUE${NC}"
    NEW_COLOR="${GREEN}GREEN${NC}"
elif docker ps --format '{{.Names}}' | grep -q "^${GREEN_CONTAINER}$"; then
    CURRENT_CONTAINER=$GREEN_CONTAINER
    CURRENT_PORT=$GREEN_PORT
    NEW_CONTAINER=$BLUE_CONTAINER
    NEW_PORT=$BLUE_PORT
    CURRENT_COLOR="${GREEN}GREEN${NC}"
    NEW_COLOR="${BLUE}BLUE${NC}"
else
    CURRENT_CONTAINER=""
    NEW_CONTAINER=$BLUE_CONTAINER
    NEW_PORT=$BLUE_PORT
    CURRENT_COLOR="NONE"
    NEW_COLOR="${BLUE}BLUE${NC}"
    echo -e "${YELLOW}No active container. Starting first deployment...${NC}"
fi

if [ -n "$CURRENT_CONTAINER" ]; then
    echo -e "${GREEN}Current active: ${CURRENT_COLOR} (${CURRENT_CONTAINER}:${CURRENT_PORT})${NC}"
    echo -e "${GREEN}Deploying to: ${NEW_COLOR} (${NEW_CONTAINER}:${NEW_PORT})${NC}"
else
    echo -e "${GREEN}Deploying to: ${NEW_COLOR} (${NEW_CONTAINER}:${NEW_PORT})${NC}"
fi

echo -e "\n${YELLOW}[2/7] Pulling latest Docker image...${NC}"
docker pull ${DOCKER_IMAGE}:latest
echo -e "${GREEN}Image pulled successfully${NC}"

echo -e "\n${YELLOW}[3/7] Cleaning up old ${NEW_COLOR} container...${NC}"
if docker ps -a --format '{{.Names}}' | grep -q "^${NEW_CONTAINER}$"; then
    docker stop ${NEW_CONTAINER} || true
    docker rm ${NEW_CONTAINER} || true
    echo -e "${GREEN}Old container removed${NC}"
else
    echo -e "${GREEN}No cleanup needed${NC}"
fi

echo -e "\n${YELLOW}[4/7] Starting ${NEW_COLOR} container...${NC}"
docker run -d \
  --name ${NEW_CONTAINER} \
  --restart unless-stopped \
  --network motionit-network \
  -p ${NEW_PORT}:8080 \
  -v /home/ubuntu/aws_motionit_private_key.pem:/app/config/aws_motionit_private_key.pem:ro \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e SPRING_CONFIG_ADDITIONAL_LOCATION=file:/app/application-prod.yml \
  -e DATABASE_URL="${DATABASE_URL}" \
  -e DB_USERNAME="${DB_USERNAME}" \
  -e DB_PASSWORD="${DB_PASSWORD}" \
  -e AWS_ACCESS_KEY="${AWS_ACCESS_KEY}" \
  -e AWS_SECRET_KEY="${AWS_SECRET_KEY}" \
  -e AWS_S3_BUCKET_NAME="${AWS_S3_BUCKET_NAME}" \
  -e AWS_CLOUDFRONT_DOMAIN="${AWS_CLOUDFRONT_DOMAIN}" \
  -e AWS_CLOUDFRONT_KEY_ID="${AWS_CLOUDFRONT_KEY_ID}" \
  -e AWS_CLOUDFRONT_PRIVATE_KEY_PATH="/app/config/aws_motionit_private_key.pem" \
  -e JWT_SECRET="${JWT_SECRET}" \
  -e JWT_ACCESS_TOKEN_EXPIRATION="${JWT_ACCESS_TOKEN_EXPIRATION}" \
  -e JWT_REFRESH_TOKEN_EXPIRATION="${JWT_REFRESH_TOKEN_EXPIRATION}" \
  -e OPENAI_API_KEY="${OPENAI_API_KEY}" \
  -e YOUTUBE_API_KEY="${YOUTUBE_API_KEY}" \
  -e KAKAO_CLIENT_ID="${KAKAO_CLIENT_ID}" \
  ${DOCKER_IMAGE}:latest

echo -e "${GREEN}${NEW_COLOR} container started${NC}"

echo -e "\n${YELLOW}[5/7] Waiting for ${NEW_COLOR} container to be healthy...${NC}"
MAX_RETRY=30
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRY ]; do
    if curl -f http://localhost:${NEW_PORT}${HEALTH_CHECK_ENDPOINT} > /dev/null 2>&1; then
        echo -e "${GREEN}${NEW_COLOR} container is healthy!${NC}"
        break
    fi

    RETRY_COUNT=$((RETRY_COUNT+1))
    echo -e "${YELLOW}Waiting... ($RETRY_COUNT/$MAX_RETRY)${NC}"
    sleep 2
done

if [ $RETRY_COUNT -eq $MAX_RETRY ]; then
    echo -e "\n${RED}Health check failed after ${MAX_RETRY} retries${NC}"
    echo -e "${RED}Showing ${NEW_COLOR} container logs:${NC}"
    docker logs --tail 50 ${NEW_CONTAINER}
    echo -e "${RED}Rolling back...${NC}"
    docker stop ${NEW_CONTAINER} || true
    docker rm ${NEW_CONTAINER} || true
    exit 1
fi

echo -e "\n${YELLOW}[6/7] Switching Nginx to ${NEW_COLOR}...${NC}"

sudo tee ${NGINX_CONF} > /dev/null <<EOF
server {
    listen 80;
    server_name _;

    location / {
        proxy_pass http://localhost:${NEW_PORT};
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;

        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";

        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    location /actuator/health {
        proxy_pass http://localhost:${NEW_PORT}/actuator/health;
        access_log off;
    }
}
EOF

if [ ! -L /etc/nginx/sites-enabled/motionit ]; then
    sudo ln -sf ${NGINX_CONF} /etc/nginx/sites-enabled/motionit
fi

if sudo nginx -t; then
    sudo systemctl reload nginx
    echo -e "${GREEN}Nginx switched to ${NEW_COLOR} (port ${NEW_PORT})${NC}"
else
    echo -e "${RED}Nginx configuration test failed${NC}"
    exit 1
fi

if [ -n "$CURRENT_CONTAINER" ]; then
    echo -e "\n${YELLOW}[7/7] Stopping old ${CURRENT_COLOR} container...${NC}"
    sleep 5
    docker stop ${CURRENT_CONTAINER} || true
    docker rm ${CURRENT_CONTAINER} || true
    echo -e "${GREEN}Old ${CURRENT_COLOR} container stopped${NC}"
else
    echo -e "\n${YELLOW}[7/7] No old container to stop${NC}"
fi

echo -e "\n${YELLOW}Cleaning up old images...${NC}"
docker image prune -f

echo -e "\n${GREEN}=================================${NC}"
echo -e "${GREEN}Current Running Containers:${NC}"
docker ps --filter "name=motionit-" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

echo -e "\n${GREEN}=================================${NC}"
echo -e "${GREEN}  Deployment Completed! 🚀${NC}"
echo -e "${GREEN}  Active: ${NEW_COLOR}${NC}"
echo -e "${GREEN}=================================${NC}"
