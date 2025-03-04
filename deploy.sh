#!/bin/bash
# Deployment script for the Flask application

echo "Starting deployment..."

# Check if the application is already running and terminate it if needed
if pgrep -f "python3 app.py" > /dev/null
then
    echo "Stopping existing application..."
    pkill -f "python3 app.py"
    sleep 3
fi

# Check if the virtual environment exists, if not, create it
if [ ! -d "venv" ]; then
    echo "Creating virtual environment..."
    python3 -m venv venv
fi

# Activate the virtual environment and install dependencies
bash -c "source venv/bin/activate && pip install --upgrade pip && pip install flask requests pytest"

# Start the Flask application in the background using nohup
nohup bash -c "source venv/bin/activate && python3 app.py" > app.log 2>&1 &

echo "Application is running at http://localhost:5000"
