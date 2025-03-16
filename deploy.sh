#!/bin/bash
# 🚀 Deployment script using Gunicorn for production-ready Flask server

echo "🚀 Deploying application with Gunicorn..."

# 🛑 Stop existing Gunicorn process if running
if pgrep -f "gunicorn" > /dev/null; then
    echo "🛑 Stopping existing Gunicorn process..."
    pkill -f "gunicorn"
    sleep 3
fi

# 🔄 Set correct permissions for venv
sudo chown -R jenkins:jenkins /var/lib/jenkins/workspace/DevOps1/venv
sudo chmod -R 755 /var/lib/jenkins/workspace/DevOps1/venv

# 🔄 Activate virtual environment
echo "🔄 Activating virtual environment..."
export VIRTUAL_ENV="/var/lib/jenkins/workspace/DevOps1/venv"
export PATH="$VIRTUAL_ENV/bin:$PATH"

# 📦 Install required dependencies inside virtual environment
pip install --upgrade pip
pip install flask gunicorn requests pytest

# 📂 Ensure log directory exists
mkdir -p logs

# 🚀 Start Gunicorn
echo "🚀 Starting Gunicorn..."
nohup gunicorn -w 4 -b 0.0.0.0:5000 app:app > logs/gunicorn.log 2>&1 &

# 🔍 Verify that the server is running
sleep 5
if ! pgrep -f "gunicorn" > /dev/null; then
    echo "❌ Gunicorn failed to start!"
    exit 1
fi

echo "✅ Application is running with Gunicorn at http://localhost:5000"
