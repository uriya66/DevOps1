#!/bin/bash
set -e  # Exit immediately if any command fails

# Define service name
SERVICE_NAME="gunicorn"

# Print deployment start message
echo "Deploying application with Gunicorn..."

# Ensure the virtual environment exists
if [ -d "venv" ]; then
    echo "Activating virtual environment..."
    . venv/bin/activate
else
    echo "Virtual environment not found! Creating one..."
    python3 -m venv venv
    . venv/bin/activate
fi

# Stop existing Gunicorn service if running
if systemctl is-active --quiet $SERVICE_NAME; then
    echo "Stopping existing Gunicorn service..."
    sudo -n systemctl stop $SERVICE_NAME
fi

# Install required dependencies only if they are missing
if ! pip show flask > /dev/null; then
    echo "Installing Flask..."
    pip install flask
fi

if ! pip show gunicorn > /dev/null; then
    echo "Installing Gunicorn..."
    pip install gunicorn
fi

if ! pip show requests > /dev/null; then
    echo "Installing Requests..."
    pip install requests
fi

if ! pip show pytest > /dev/null; then
    echo "Installing Pytest..."
    pip install pytest
fi

# Restart Gunicorn service
echo "Restarting Gunicorn service..."
sudo -n systemctl restart $SERVICE_NAME

# Verify Gunicorn status
if ! systemctl is-active --quiet $SERVICE_NAME; then
    echo "ERROR: Gunicorn service failed to start!"
    exit 1
fi

# Print successful deployment message
echo "Deployment completed successfully. Application is running at http://localhost:5000"
