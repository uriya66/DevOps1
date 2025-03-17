from flask import Flask, jsonify, request, render_template  # Import Flask modules
import logging  # Import Python's built-in logging module

# Configure logging to suppress HTTP request logs
log = logging.getLogger('werkzeug')  # Get Flask's built-in logger
log.setLevel(logging.ERROR)  # Only log errors, not HTTP requests

# Initialize the Flask application
app = Flask(__name__)

def wants_json_response():
    """
    Determines if the client expects a JSON response based on the 'Accept' header.
    
    Returns:
        bool: True if client expects JSON, otherwise False.
    """
    return request.headers.get("Accept") == "application/json"

@app.route('/')
def home():
    """
    Home page - Returns either an HTML page or JSON response.
    """
    if wants_json_response():
        return jsonify({"page": "home", "message": "Welcome to My Flask App!"}), 200
    return render_template('home.html')  # Render home page for browser users

@app.route('/health')
def health_check():
    """
    Health check - Returns either JSON status or an HTML page.
    """
    if wants_json_response():
        return jsonify({"status": "ok", "message": "Application is running!"}), 200
    return render_template("health.html")  # Render the health.html file from the "templates/" folder for browser users

@app.route('/api/test-content')
def test_content():
    """
    API that provides structured test data for validation.
    """
    return jsonify({"message": "This is test content for API validation."}), 200

@app.errorhandler(404)
def not_found(e):
    """
    Custom 404 error handler that returns either JSON or HTML.
    """
    if wants_json_response():
        return jsonify({"error": "404 - Page Not Found"}), 404
    return render_template('404.html'), 404  # Render 404 page for browser users


if __name__ == '__main__':
    # Run Flask application on all network interfaces (0.0.0.0) at port 5000
    app.run(host='0.0.0.0', port=5000, debug=True)

