from flask import Flask, render_template, jsonify  # Import Flask modules
import logging  # Import Python's built-in logging module

# Configure logging to suppress HTTP request logs
log = logging.getLogger('werkzeug')  # Get Flask's built-in logger
log.setLevel(logging.ERROR)  # Only log errors, not HTTP requests

# Initialize the Flask application
app = Flask(__name__)

# Function to determine if the client wants JSON or HTML response
def wants_json_response():
    """
    ✅ Determines if the client expects a JSON response based on the 'Accept' header.
    """
    return request.headers.get("Accept") == "application/json"
    
# Home page - returns an HTML page
@app.route('/')
def home():
    if wants_json_response():
        return jsonify({"page": "home", "message": "Welcome to the home page!"}) # Returns JSON response
    return render_template('home.html')  # Renders the home.html file from the "templates/" folder

# Health check - returns a JSON response
@app.route('/health')
def health_check():
    if wants_json_response():
        return jsonify({"status": "ok", "message": "Application is running!"}) # Returns JSON response
    return render_template("health.html")  # Renders the health.html file from the "templates/" folder
    
# Custom 404 page (returns JSON)
@app.errorhandler(404)
def not_found(e):
    if wants_json_response():
        return jsonify({"error": "404 - Page Not Found"}), 404  # Returns a JSON error message for unknown routes
    return render_template('404.html'), 404  # Renders the 404.html file from the "templates/" folder


# Run the application on all network interfaces (0.0.0.0) at port 5000
if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)  # Runs the Flask server in debug mode
