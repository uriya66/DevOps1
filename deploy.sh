#!/bin/bash
# 🚀 Deployment script using Gunicorn for production-ready Flask server

echo "🚀 Deploying application with Gunicorn..."

# 🛑 Stop existing Gunicorn process if running
if pgrep -f "gunicorn" > /dev/null; then
    echo "🛑 Stopping existing Gunicorn process..."
    pkill -f "gunicorn"
    sleep 3  # Wait for process to stop
fi

# 📦 Create virtual environment if it doesn't exist
if [ ! -d "venv" ]; then
    echo "📦 Creating virtual environment..."
    python3 -m venv venv
fi

# 🔄 Activate virtual environment
echo "🔄 Activating virtual environment..."
source venv/bin/activate

# 📦 Install required dependencies
echo "📦 Installing dependencies..."
pip install --upgrade pip
pip install flask gunicorn requests pytest

# 📂 Ensure log directory exists
mkdir -p logs  # Create logs directory if it doesn't exist

# 🛠 Add venv binaries to PATH to fix Gunicorn issue
export PATH=$PWD/venv/bin:$PATH

# 🚀 Start the Flask application using Gunicorn
echo "🚀 Starting Gunicorn server..."
nohup gunicorn -w 4 -b 0.0.0.0:5000 app:app > logs/gunicorn.log 2>&1 &

# 🔍 Verify that the server is running
sleep 5
if ! pgrep -f "gunicorn" > /dev/null; then
    echo "❌ Gunicorn failed to start!"
    exit 1
fi

echo "✅ Application is running with Gunicorn at http://localhost:5000"

