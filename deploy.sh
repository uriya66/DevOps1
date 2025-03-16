#!/bin/bash
# Deployment script using Gunicorn for a production-ready Flask server

echo "Deploying application with Gunicorn..."

# Activate virtual environment
echo "Activating virtual environment..."
. venv/bin/activate  # Using '.' instead of 'source' for compatibility

# Stop existing Gunicorn process if running
echo "Stopping existing Gunicorn process..."
sudo pkill -f "gunicorn" || true  # Try killing Gunicorn if running
sudo pkill -9 -f "gunicorn" || true  # Force kill if necessary
sudo pkill -9 -f "python" || true  # Ensure any rogue Python processes are terminated

# Free port 5000 if it's still in use
echo "Ensuring port 5000 is free..."
sudo fuser -k 5000/tcp || true  # Free the port if occupied

# Install required dependencies inside the virtual environment
echo "Installing dependencies..."
pip install --upgrade pip  # Upgrade pip
pip install flask gunicorn requests pytest  # Install necessary dependencies

# Ensure log directory exists
echo "Creating logs directory if it doesn't exist..."
mkdir -p logs  # Create logs directory if it does not exist

# Add Gunicorn to PATH
export PATH=$(pwd)/venv/bin:$PATH  # Ensure virtual environment binaries are in PATH

# Ensure systemd service is properly configured
echo "Configuring Gunicorn systemd service..."
cat <<EOF | sudo tee /etc/systemd/system/gunicorn.service
[Unit]
Description=Gunicorn instance to serve Flask app
After=network.target

[Service]
User=ubuntu
Group=ubuntu
WorkingDirectory=$(pwd)
Environment="PATH=$(pwd)/venv/bin"
ExecStart=$(pwd)/venv/bin/gunicorn -w 4 -b 0.0.0.0:5000 app:app

[Install]
WantedBy=multi-user.target
EOF

# Reload systemd and restart Gunicorn
echo "Reloading systemd daemon..."
sudo systemctl daemon-reload  # Reload systemd daemon

echo "Restarting Gunicorn service..."
sudo systemctl restart gunicorn  # Restart Gunicorn service

echo "Enabling Gunicorn service to start on boot..."
sudo systemctl enable gunicorn  # Ensure Gunicorn starts on reboot

# Check Gunicorn status
echo "Checking Gunicorn status..."
sudo systemctl status gunicorn --no-pager  # Display Gunicorn status without pausing

# Start Gunicorn manually (as a fallback)
echo "Starting Gunicorn manually..."
nohup venv/bin/gunicorn -w 4 -b 0.0.0.0:5000 app:app > logs/gunicorn.log 2>&1 &  # Start Gunicorn in the background

# Verify that the server is running
sleep 5
if ! pgrep -f "gunicorn" > /dev/null; then
    echo "Gunicorn failed to start!"
    exit 1
fi

echo "Application is running with Gunicorn at http://localhost:5000"

