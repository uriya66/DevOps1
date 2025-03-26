#!/bin/bash
set -e  # Exit if any command fails

# Service name for systemd
SERVICE_NAME="gunicorn"

# Start deployment
echo "Deploying application using Gunicorn..."

# Activate virtualenv or create it
if [ -d "venv" ]; then
    echo "Activating virtual environment..."
    . venv/bin/activate
else
    echo "Virtual environment not found. Creating one..."
    python3 -m venv venv
    . venv/bin/activate
fi

# Install packages only if missing
pip show flask > /dev/null || pip install flask
pip show gunicorn > /dev/null || pip install gunicorn
pip show requests > /dev/null || pip install requests
pip show pytest > /dev/null || pip install pytest

# Restart Gunicorn
echo "Restarting Gunicorn service..."
sudo -n systemctl restart $SERVICE_NAME

# Verify it's active
if ! systemctl is-active --quiet $SERVICE_NAME; then
    echo "ERROR: Gunicorn failed to start!"
    exit 1
fi

# Inform success
echo "Deployment completed successfully. App running at http://localhost:5000 or http://<public-ip>:5000"

