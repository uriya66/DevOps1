#!/bin/bash
# Deployment script using Gunicorn for a production-ready Flask server

set -e  # Stop script on error

echo "Deploying application with Gunicorn..."

# Activate virtual environment
echo "Activating virtual environment..."
. venv/bin/activate

# Stop existing Gunicorn service if running
if systemctl is-active --quiet gunicorn; then
    echo "Stopping existing Gunicorn service..."
    sudo -n systemctl stop gunicorn
fi

# Install dependencies
echo "Installing dependencies..."
pip install --upgrade pip --break-system-packages
pip install flask gunicorn requests pytest --break-system-packages

# Restart Gunicorn
echo "Restarting Gunicorn service..."
sudo -n systemctl restart gunicorn

echo "Deployment completed successfully. Application is running at http://localhost:5000"

