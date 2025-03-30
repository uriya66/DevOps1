#!/bin/bash

# === deploy.sh ===
# Hybrid deployment script using systemd + venv + smart dependency handling

set -e  # Exit on error

SERVICE_NAME="gunicorn"

echo "Starting deployment..."

# Check Python3
if ! command -v python3 > /dev/null; then
    echo " Python3 is not installed!"
    exit 1
fi

# Create or activate venv
if [ -d "venv" ]; then
    echo " Activating existing virtual environment..."
    source venv/bin/activate
else
    echo "Creating new virtual environment..."
    python3 -m venv venv
    source venv/bin/activate
fi

# Upgrade pip
echo "pgrading pip..."
pip install --upgrade pip

# Install dependencies: prefer requirements.txt
if [ -f requirements.txt ]; then
    echo "Installing dependencies from requirements.txt..."
    pip install -r requirements.txt
else
    echo "requirements.txt not found, installing common packages manually..."
    for package in flask gunicorn requests pytest; do
        if ! pip show $package > /dev/null; then
            echo "Installing $package..."
            pip install $package
        else
            echo "$package already installed"
        fi
    done
fi

# Restart Gunicorn service using systemd
echo "estarting Gunicorn systemd service..."
sudo systemctl daemon-reexec
sudo systemctl daemon-reload
sudo systemctl restart $SERVICE_NAME
sudo systemctl enable $SERVICE_NAME

# Wait and verify
sleep 2

if systemctl is-active --quiet $SERVICE_NAME; then
    echo " Gunicorn started successfully via systemd."
else
    echo " ERROR: Gunicorn failed to start!"
    echo "Recent logs from journalctl:"
    journalctl -u $SERVICE_NAME --no-pager | tail -20
    exit 1
fi

# Show public IP for external access
PUBLIC_IP=$(curl -s http://checkip.amazonaws.com)
echo "App is available at:"
echo " - http://localhost:5000"
echo " - http://${PUBLIC_IP}:5000"

