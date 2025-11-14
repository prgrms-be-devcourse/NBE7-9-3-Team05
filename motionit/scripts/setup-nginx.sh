#!/bin/bash
set -e

echo "================================="
echo "  Nginx Setup for Blue-Green"
echo "================================="

# Nginx 설치 확인
if ! command -v nginx &> /dev/null; then
    echo "Installing Nginx..."
    sudo apt-get update
    sudo apt-get install -y nginx
    echo "✓ Nginx installed"
else
    echo "✓ Nginx already installed"
fi

# 기본 설정 비활성화
if [ -L /etc/nginx/sites-enabled/default ]; then
    sudo rm /etc/nginx/sites-enabled/default
    echo "✓ Default site disabled"
fi

# Nginx 시작
sudo systemctl enable nginx
sudo systemctl start nginx
echo "✓ Nginx started"

echo ""
echo "================================="
echo "  Setup Completed!"
echo "================================="
echo ""
echo "Next steps:"
echo "1. Run deploy-blue-green.sh to deploy"
echo "2. Access via http://<server-ip>"
