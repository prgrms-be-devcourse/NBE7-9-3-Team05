#!/bin/bash
set -e

echo "================================="
echo "  Nginx Setup for Blue-Green"
echo "================================="

if ! command -v nginx &> /dev/null; then
    echo "Installing Nginx..."
    sudo apt-get update
    sudo apt-get install -y nginx
    echo "✓ Nginx installed"
else
    echo "✓ Nginx already installed"
fi

if [ -L /etc/nginx/sites-enabled/default ] || [ -f /etc/nginx/sites-enabled/default ]; then
    sudo rm -f /etc/nginx/sites-enabled/default
    echo "✓ Default site disabled"
fi

if ! groups $USER | grep -q '\bdocker\b'; then
    echo "Adding $USER to docker group..."
    sudo usermod -aG docker $USER
    echo "⚠ Please logout and login again for docker group to take effect"
    echo "  Or run: newgrp docker"
else
    echo "✓ User already in docker group"
fi

sudo systemctl enable nginx
sudo systemctl start nginx || sudo systemctl restart nginx
echo "✓ Nginx started"

echo ""
echo "================================="
echo "  Setup Completed!"
echo "================================="
echo ""
echo "Next steps:"
echo "1. If docker group was added, run: newgrp docker"
echo "2. Run deploy-blue-green.sh to deploy"
echo "3. Access via http://<server-ip>"
