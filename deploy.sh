#!/bin/bash
set -e  # Exit immediately if any command fails

# Define the name of the Gunicorn systemd service
SERVICE_NAME="gunicorn"

# Notify start of deployment
echo "Deploying application with Gunicorn..."

# Check if virtual environment exists
if [ -d "venv" ]; then
    echo "Activating virtual environment..."
    . venv/bin/activate  # Activate venv if exists
else
    echo "Virtual environment not found! Creating one..."
    python3 -m venv venv  # Create venv
    . venv/bin/activate
fi

# Stop Gunicorn if already running
if systemctl is-active --quiet $SERVICE_NAME; then
    echo "Stopping existing Gunicorn service..."
    sudo -n systemctl stop $SERVICE_NAME
fi

# Install missing Python dependencies
for package in flask gunicorn requests pytest; do
    if ! pip show $package > /dev/null; then
        echo "Installing $package..."
        pip install $package
    fi
done

# Restart Gunicorn service
echo "Restarting Gunicorn service..."
sudo -n systemctl restart $SERVICE_NAME

# Confirm service is running
if ! systemctl is-active --quiet $SERVICE_NAME; then
    echo "ERROR: Gunicorn failed to start!"
    exit 1
fi

# Output deployment success
echo "Deployment completed successfully. App is running on localhost and public IP."

