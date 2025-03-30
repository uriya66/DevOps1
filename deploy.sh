#!/bin/bash
set -e  # Exit on command failure

SERVICE_NAME="gunicorn"

echo "Deploying application with Gunicorn..."

# Ensure venv exists
if [ ! -d "venv" ]; then
    echo "Creating virtual environment..."
    python3 -m venv venv
fi

. venv/bin/activate

# Install dependencies if missing
pip install --upgrade pip
for package in flask gunicorn requests pytest; do
    pip show $package || pip install $package
done

# Reload systemd units if needed
sudo -n systemctl daemon-reload

# Stop Gunicorn if running
if systemctl is-active --quiet $SERVICE_NAME; then
    echo "Stopping Gunicorn service..."
    sudo -n systemctl stop $SERVICE_NAME
fi

# Restart Gunicorn service
echo "Restarting Gunicorn service..."
sudo -n systemctl restart $SERVICE_NAME

# Check service status
if ! systemctl is-active --quiet $SERVICE_NAME; then
    echo "ERROR: Gunicorn failed to start!"
    exit 1
fi

echo "Deployment completed successfully. App is running on public IP."

