#!/bin/bash
# Deployment script using Gunicorn

echo "🚀 Deploying application with Gunicorn..."

# Stop existing Flask process if running
if pgrep -f "gunicorn" > /dev/null
then
    echo "🛑 Stopping existing Gunicorn process..."
    pkill -f "gunicorn"
    sleep 3
fi

# Create virtual environment if it doesn't exist
if [ ! -d "venv" ]; then
    echo "📦 Creating virtual environment..."
    python3 -m venv venv
fi

# Activate virtual environment and install dependencies
bash -c "source venv/bin/activate && pip install --upgrade pip && pip install flask gunicorn requests pytest"

# Start the Flask application using Gunicorn with multiple workers
nohup bash -c "source venv/bin/activate && gunicorn -w 4 -b 0.0.0.0:5000 app:app" > app.log 2>&1 &

echo "✅ Application is running with Gunicorn at http://localhost:5000"
