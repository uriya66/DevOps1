#!/bin/bash
# Deployment script using Gunicorn for a production-ready Flask server

echo "Deploying application with Gunicorn..."

# Activate virtual environment
echo "Activating virtual environment..."
. venv/bin/activate  # Activate venv

# Stop existing Gunicorn service
echo "Stopping existing Gunicorn service..."
sudo -n systemctl stop gunicorn || true  # Stop Gunicorn without requiring a password

# Install required dependencies inside the virtual environment
echo "Installing dependencies..."
pip install --upgrade pip --break-system-packages  # Upgrade pip
pip install flask gunicorn requests pytest --break-system-packages  # Install necessary dependencies

# Ensure log directory exists
echo "Creating logs directory if it doesn't exist..."
mkdir -p logs  # Ensure logs directory is created

# Set Jenkins user environment (Fix sudo issue)
export SUDO_USER=jenkins

# Reload systemd and restart Gunicorn
echo "Reloading systemd daemon..."
sudo -n systemctl daemon-reload  # Reload systemd without password

echo "Restarting Gunicorn service..."
sudo -n systemctl start gunicorn  # Start Gunicorn without password

echo "Enabling Gunicorn service to start on boot..."
sudo -n systemctl enable gunicorn  # Ensure Gunicorn starts on reboot

# Verify that the server status
sleep 5
if ! systemctl is-active --quiet gunicorn; then
    echo "ERROR: Gunicorn service failed to start!"
    exit 1
fi

echo "Deployment completed successfully. Application is running at http://localhost:5000"
