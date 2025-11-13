#!/bin/bash
set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 설정
DOCKER_IMAGE="minibrb/motionit-backend"
CONTAINER_NAME="motionit-backend"
APP_PORT=8080

echo -e "${GREEN}=================================${NC}"
echo -e "${GREEN}  MotionIt Backend Deployment${NC}"
echo -e "${GREEN}=================================${NC}"

# 1. 기존 컨테이너 중지 및 제거
echo -e "\n${YELLOW}[1/5] Stopping existing container...${NC}"
if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    docker stop ${CONTAINER_NAME} || true
    docker rm ${CONTAINER_NAME} || true
    echo -e "${GREEN}✓ Container stopped and removed${NC}"
else
    echo -e "${GREEN}✓ No existing container found${NC}"
fi

# 2. 최신 이미지 Pull
echo -e "\n${YELLOW}[2/5] Pulling latest Docker image...${NC}"
docker pull ${DOCKER_IMAGE}:latest
echo -e "${GREEN}✓ Image pulled successfully${NC}"

# 3. 사용하지 않는 이미지 정리
echo -e "\n${YELLOW}[3/5] Cleaning up old images...${NC}"
docker image prune -f
echo -e "${GREEN}✓ Old images removed${NC}"

# 4. 새 컨테이너 실행
echo -e "\n${YELLOW}[4/5] Starting new container...${NC}"
docker run -d \
  --name ${CONTAINER_NAME} \
  --restart unless-stopped \
  -p ${APP_PORT}:8080 \
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
  ${DOCKER_IMAGE}:latest

echo -e "${GREEN}✓ Container started${NC}"

# 컨테이너 로그 확인
echo -e "\n${YELLOW}Initial container logs:${NC}"
sleep 3
docker logs --tail 20 ${CONTAINER_NAME}

# 5. Health Check
echo -e "\n${YELLOW}[5/5] Waiting for application to be healthy...${NC}"
MAX_RETRY=30
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRY ]; do
    if curl -f http://localhost:${APP_PORT}/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Application is healthy!${NC}"

        # 컨테이너 상태 출력
        echo -e "\n${GREEN}Container Status:${NC}"
        docker ps --filter name=${CONTAINER_NAME} --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"

        echo -e "\n${GREEN}=================================${NC}"
        echo -e "${GREEN}  Deployment Completed! 🚀${NC}"
        echo -e "${GREEN}=================================${NC}"
        exit 0
    fi

    RETRY_COUNT=$((RETRY_COUNT+1))
    echo -e "${YELLOW}Waiting... ($RETRY_COUNT/$MAX_RETRY)${NC}"
    sleep 2
done

# Health check 실패
echo -e "\n${RED}✗ Health check failed after ${MAX_RETRY} retries${NC}"
echo -e "${RED}Showing container logs:${NC}"
docker logs --tail 50 ${CONTAINER_NAME}
exit 1
