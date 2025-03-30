#!/bin/bash

# === deploy.sh ===
# Hybrid deployment script using systemd + venv + smart dependency handling

set -e  # Exit immediately if any command fails

SERVICE_NAME="gunicorn"  # Gunicorn systemd service name

echo "Starting deployment..."

# Verify Python3 installation
if ! command -v python3 > /dev/null; then
    echo "Python3 is not installed!"
    exit 1
fi

# Create or activate virtual environment
if [ -d "venv" ]; then
    echo "Activating existing virtual environment..."
    source venv/bin/activate
else
    echo "Creating new virtual environment..."
    python3 -m venv venv
    source venv/bin/activate
fi

# Upgrade pip to latest version
echo "Upgrading pip..."
pip install --upgrade pip

# Install project dependencies using requirements.txt if available
if [ -f requirements.txt ]; then
    echo "Installing dependencies from requirements.txt..."
    pip install -r requirements.txt
else
    # If requirements.txt is missing, install essential packages manually
    echo "requirements.txt not found, installing common packages manually..."
    for package in flask gunicorn requests pytest; do
        if ! pip show $package > /dev/null; then
            echo "Installing $package..."
            pip install $package
        else
            echo "$package already installed."
        fi
    done
fi

# Restart Gunicorn service using systemd (recommended for production)
echo "Restarting Gunicorn systemd service..."
sudo systemctl daemon-reexec
sudo systemctl daemon-reload
sudo systemctl restart $SERVICE_NAME
sudo systemctl enable $SERVICE_NAME

# Wait briefly and verify the service status
sleep 3

if systemctl is-active --quiet $SERVICE_NAME; then
    echo "Gunicorn started successfully via systemd."
else
    echo "ERROR: Gunicorn failed to start!"
    echo "Recent logs from journalctl:"
    journalctl -u $SERVICE_NAME --no-pager | tail -20
    exit 1
fi

# Fetch public IP for external access
PUBLIC_IP=$(curl -s http://checkip.amazonaws.com)
echo "App is available at:"
echo " - http://localhost:5000"
echo " - http://${PUBLIC_IP}:5000"

