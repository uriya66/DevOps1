#!/bin/bash
# 🚀 Deployment script using Gunicorn for production-ready Flask server

echo "🚀 Deploying application with Gunicorn..."

# 🛑 Stop existing Gunicorn process if running
if pgrep -f "gunicorn" > /dev/null; then
    echo "🛑 Stopping existing Gunicorn process..."
    pkill -f "gunicorn"
    sleep 3
fi

# 📦 Create virtual environment if it doesn't exist
if [ ! -d "venv" ]; then
    echo "📦 Creating virtual environment..."
    python3 -m venv --system-site-packages venv
fi

# 🔄 Activate virtual environment and install dependencies
echo "🔄 Activating virtual environment..."
source venv/bin/activate

# ⚠️ Fix for PEP 668 (externally-managed-environment error)
echo "📦 Installing dependencies..."
pip install --upgrade pip --break-system-packages
pip install flask gunicorn requests pytest --break-system-packages

# 📂 Ensure log directory exists
mkdir -p logs

# 🚀 Start the Flask application using Gunicorn with multiple workers
echo "🚀 Starting Gunicorn server..."
nohup gunicorn -w 4 -b 0.0.0.0:5000 app:app > logs/gunicorn.log 2>&1 &

# 🔍 Verify that the server is running
sleep 5
if ! pgrep -f "gunicorn" > /dev/null; then
    echo "❌ Gunicorn failed to start!"
    exit 1
fi

echo "✅ Application is running with Gunicorn at http://localhost:5000"
