#!/bin/bash
# Deployment script using Gunicorn for a production-ready Flask server

echo "Deploying application with Gunicorn..."

# Activate virtual environment
echo "Activating virtual environment..."
. venv/bin/activate  # Activate venv

# Stop existing Gunicorn service
echo "Stopping existing Gunicorn service..."
sudo systemctl stop gunicorn || true

# Install required dependencies inside the virtual environment
echo "Installing dependencies..."
pip install --upgrade pip  # Upgrade pip
pip install flask gunicorn requests pytest  # Install necessary dependencies

# Ensure log directory exists
echo "Creating logs directory if it doesn't exist..."
mkdir -p logs  # Ensure logs directory is created

# Reload systemd and restart Gunicorn
echo "Reloading systemd daemon..."
sudo systemctl daemon-reload

echo "Restarting Gunicorn service..."
sudo systemctl start gunicorn  # Start Gunicorn

echo "Enabling Gunicorn service to start on boot..."
sudo systemctl enable gunicorn  # Ensure Gunicorn starts on reboot

# Verify that the server is running
sleep 5
if ! systemctl is-active --quiet gunicorn; then
    echo "Gunicorn service failed to start!"
    exit 1
fi

echo "Application is running with Gunicorn at http://localhost:5000"

