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

echo -e "${RED}=================================${NC}"
echo -e "${RED}  Rollback Deployment${NC}"
echo -e "${RED}=================================${NC}"

if [ $# -eq 0 ]; then
    echo -e "\n${YELLOW}Usage:${NC}"
    echo -e "  $0 <image-tag>"
    echo -e "\n${YELLOW}Examples:${NC}"
    echo -e "  $0 20241114-abc1234"
    echo -e "  $0 latest"
    echo -e "\n${YELLOW}Available tags from Docker Hub:${NC}"
    docker search ${DOCKER_IMAGE} --limit 5 2>/dev/null || echo "  (Run 'docker images ${DOCKER_IMAGE}' on EC2 to see local tags)"
    exit 1
fi

TARGET_TAG=$1
TARGET_IMAGE="${DOCKER_IMAGE}:${TARGET_TAG}"

echo -e "\n${YELLOW}[1/8] Verifying target image...${NC}"
if ! docker pull ${TARGET_IMAGE}; then
    echo -e "${RED}✗ Failed to pull ${TARGET_IMAGE}${NC}"
    echo -e "${RED}  Please check if the tag exists in Docker Hub${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Image ${TARGET_IMAGE} verified${NC}"

echo -e "\n${YELLOW}[2/8] Checking current active container...${NC}"
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
    echo -e "${RED}✗ No active container found${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Current: ${CURRENT_COLOR} (port ${CURRENT_PORT})${NC}"
echo -e "${GREEN}✓ Rolling back to: ${NEW_COLOR} (port ${NEW_PORT})${NC}"

echo -e "\n${YELLOW}[3/8] Cleaning up old ${NEW_COLOR} container...${NC}"
if docker ps -a --format '{{.Names}}' | grep -q "^${NEW_CONTAINER}$"; then
    docker stop ${NEW_CONTAINER} || true
    docker rm ${NEW_CONTAINER} || true
fi
echo -e "${GREEN}✓ Cleanup completed${NC}"

echo -e "\n${YELLOW}[4/8] Starting ${NEW_COLOR} container with ${TARGET_TAG}...${NC}"
docker run -d \
  --name ${NEW_CONTAINER} \
  --restart unless-stopped \
  -p ${NEW_PORT}:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL="${DATABASE_URL}" \
  -e DB_USERNAME="${DB_USERNAME}" \
  -e DB_PASSWORD="${DB_PASSWORD}" \
  -e AWS_ACCESS_KEY="${AWS_ACCESS_KEY}" \
  -e AWS_SECRET_KEY="${AWS_SECRET_KEY}" \
  -e AWS_S3_BUCKET_NAME="${AWS_S3_BUCKET_NAME}" \
  -e AWS_CLOUDFRONT_DOMAIN="${AWS_CLOUDFRONT_DOMAIN}" \
  -e AWS_CLOUDFRONT_KEY_ID="${AWS_CLOUDFRONT_KEY_ID}" \
  -e AWS_CLOUDFRONT_PRIVATE_KEY_PATH="${AWS_CLOUDFRONT_PRIVATE_KEY_PATH}" \
  -e JWT_SECRET="${JWT_SECRET}" \
  -e JWT_ACCESS_TOKEN_EXPIRATION="${JWT_ACCESS_TOKEN_EXPIRATION}" \
  -e JWT_REFRESH_TOKEN_EXPIRATION="${JWT_REFRESH_TOKEN_EXPIRATION}" \
  -e OPENAI_API_KEY="${OPENAI_API_KEY}" \
  -e YOUTUBE_API_KEY="${YOUTUBE_API_KEY}" \
  -e KAKAO_CLIENT_ID="${KAKAO_CLIENT_ID}" \
  ${TARGET_IMAGE}

echo -e "${GREEN}✓ ${NEW_COLOR} container started${NC}"

echo -e "\n${YELLOW}[5/8] Health checking...${NC}"
MAX_RETRY=30
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRY ]; do
    if curl -f http://localhost:${NEW_PORT}${HEALTH_CHECK_ENDPOINT} > /dev/null 2>&1; then
        echo -e "${GREEN}✓ ${NEW_COLOR} container is healthy!${NC}"
        break
    fi
    RETRY_COUNT=$((RETRY_COUNT+1))
    echo -e "${YELLOW}Waiting... ($RETRY_COUNT/$MAX_RETRY)${NC}"
    sleep 2
done

if [ $RETRY_COUNT -eq $MAX_RETRY ]; then
    echo -e "\n${RED}✗ Health check failed${NC}"
    docker logs --tail 50 ${NEW_CONTAINER}
    echo -e "${RED}Rolling back failed. Keeping current ${CURRENT_COLOR} container.${NC}"
    docker stop ${NEW_CONTAINER} || true
    docker rm ${NEW_CONTAINER} || true
    exit 1
fi

echo -e "\n${YELLOW}[6/8] Switching Nginx to ${NEW_COLOR}...${NC}"
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
    echo -e "${GREEN}✓ Nginx switched to ${NEW_COLOR} (port ${NEW_PORT})${NC}"
else
    echo -e "${RED}✗ Nginx configuration test failed${NC}"
    exit 1
fi

echo -e "\n${YELLOW}[7/8] Stopping old ${CURRENT_COLOR} container...${NC}"
sleep 5
docker stop ${CURRENT_CONTAINER} || true
docker rm ${CURRENT_CONTAINER} || true
echo -e "${GREEN}✓ Old container stopped${NC}"

echo -e "\n${YELLOW}[8/8] Cleanup...${NC}"
docker image prune -f

echo -e "\n${GREEN}=================================${NC}"
echo -e "${GREEN}  Rollback Completed!${NC}"
echo -e "${GREEN}  Rolled back to: ${TARGET_TAG}${NC}"
echo -e "${GREEN}  Active: ${NEW_COLOR} (port ${NEW_PORT})${NC}"
echo -e "${GREEN}=================================${NC}"
