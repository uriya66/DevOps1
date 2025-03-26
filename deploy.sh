#!/bin/bash
set -e  # Exit immediately if a command exits with a non-zero status

# Define the name of the Gunicorn systemd service
SERVICE_NAME="gunicorn"

echo "Starting deployment process..."

# Check if virtual environment exists
if [ -d "venv" ]; then
    echo "Activating existing virtual environment"
    . venv/bin/activate
else
    echo "Creating new virtual environment"
    python3 -m venv venv
    . venv/bin/activate
fi

# Stop Gunicorn if already running
if systemctl is-active --quiet $SERVICE_NAME; then
    echo "Stopping Gunicorn..."
    sudo -n systemctl stop $SERVICE_NAME
fi

# Install Python packages if not installed
for package in flask gunicorn requests pytest; do
    if ! pip show $package > /dev/null; then
        echo "Installing missing package: $package"
        pip install $package
    fi
done

# Restart Gunicorn service
echo "Starting Gunicorn..."
sudo -n systemctl restart $SERVICE_NAME

# Confirm Gunicorn is running
if ! systemctl is-active --quiet $SERVICE_NAME; then
    echo "ERROR: Gunicorn failed to start"
    exit 1
fi

echo "Deployment finished successfully. Flask app is running."

