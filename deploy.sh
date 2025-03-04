#!/bin/bash

echo "🚀 Starting deployment..."

# Make sure the server is not already running
if pgrep -f "python3 app.py" > /dev/null
then
    echo "🛑 Stopping existing application..."
    pkill -f "python3 app.py"
    sleep 3
fi

# Create a virtual environment if it doesn't exist
if [ ! -d "venv" ]; then
    echo "🔧 Creating virtual environment..."
    python3 -m venv venv
fi

# Activate the environment
source venv/bin/activate

# Update and install packages
pip install --upgrade pip
pip install flask requests pytest

# Restart the application
nohup python3 app.py > app.log 2>&1 &

echo "✅ Application is running at http://localhost:5000"
