#!/bin/bash
set -e  # Exit immediately if any command fails

SERVICE_NAME="gunicorn"

echo "Deploying application with Gunicorn..."

# Activate or create venv
if [ -d "venv" ]; then
    source venv/bin/activate
else
    python3 -m venv venv
    source venv/bin/activate
fi

pip install -U pip
pip install -r requirements.txt

# Reload systemd services to avoid warnings
sudo systemctl daemon-reload

# Restart Gunicorn safely
if systemctl is-active --quiet $SERVICE_NAME; then
    sudo systemctl stop $SERVICE_NAME
fi

sudo systemctl start $SERVICE_NAME
sudo systemctl enable $SERVICE_NAME

sleep 3  # Allow service startup time

if systemctl is-active --quiet $SERVICE_NAME; then
    echo "Deployment successful! Gunicorn is running."
else
    echo "Deployment failed! Check service logs."
    journalctl -u $SERVICE_NAME --no-pager | tail -20
    exit 1
fi

PUBLIC_IP=$(curl -s http://checkip.amazonaws.com)
echo "App available at http://${PUBLIC_IP}:5000"

