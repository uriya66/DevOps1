#!/bin/bash
set -e  # Exit the script immediately if any command fails

# Print a starting message to indicate the beginning of the deployment
echo "Deploying Flask application using Gunicorn..."

# Check if the Python virtual environment exists
if [ -d "venv" ]; then
    echo "Activating existing virtual environment..."  # Inform that venv will be used
    . venv/bin/activate  # Activate the virtual environment
else
    echo "Creating new virtual environment..."  # Inform that venv will be created
    python3 -m venv venv  # Create a new virtual environment using Python 3
    . venv/bin/activate  # Activate the newly created virtual environment
fi

# Loop through required Python packages and install them if not already installed
for package in flask gunicorn requests pytest; do
    if ! pip show $package > /dev/null; then
        echo "Installing $package..."  # Inform which package is being installed
        pip install $package  # Install the package using pip
    fi
done

# Stop any previously running Gunicorn processes to avoid port conflicts
echo "Stopping previous Gunicorn processes (if any)..."  # Inform about cleanup
pkill gunicorn || true  # Attempt to kill Gunicorn; ignore error if it's not running

# Start the Gunicorn server in the background, listening on all interfaces (0.0.0.0:5000)
echo "Starting new Gunicorn server..."  # Inform about starting the app
nohup venv/bin/gunicorn -w 1 -b 0.0.0.0:5000 app:app > gunicorn.log 2>&1 &  # Start Flask app using Gunicorn in background, log output

# Wait for a few seconds to allow Gunicorn to start fully
sleep 2  # Sleep to give Gunicorn time to initialize

# Verify that Gunicorn started successfully by checking for its process
if pgrep gunicorn > /dev/null; then
    echo "Gunicorn started successfully."  # Print success message
else
    echo "ERROR: Gunicorn failed to start!"  # Print failure message
    exit 1  # Exit script with error code
fi

# Fetch the public IP address dynamically from an external service
PUBLIC_IP=$(curl -s http://checkip.amazonaws.com)  # Get the server's public IP using AWS metadata service

# Display both localhost and public access URLs for the running application
echo "Deployment complete. Application is running at:"  # Final confirmation message
echo " - http://localhost:5000  # Localhost access for testing within the EC2 instance"
echo " - http://${PUBLIC_IP}:5000  # Public access for external users using dynamic IP"

