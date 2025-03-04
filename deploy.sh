#!/bin/bash

echo "Starting deployment..."

# Make sure the server is not already running
if pgrep -f "python3 app.py" > /dev/null
then
    echo "Stopping existing application..."
    pkill -f "python3 app.py"
    sleep 3
fi

# Create a virtual workspace if it does not exist
if [ ! -d "venv" ]; then
    echo "Creating virtual environment..."
    python3 -m venv venv
fi

# Activate the workspace
bash -c "source venv/bin/activate && pip install --upgrade pip && pip install flask requests pytest"

# Restart the application
nohup bash -c "source venv/bin/activate && python3 app.py" > app.log 2>&1 &

echo "Application is running at http://localhost:5000"
