from flask import Flask, jsonify

# Initialize the Flask application
app = Flask(__name__)

# Define the home route
@app.route('/')
def home():
    return "Welcome to the Home Page", 200

# Define a health check route
@app.route('/health')
def health_check():
    return jsonify({"status": "ok"}), 200

# Handle 404 errors gracefully
@app.errorhandler(404)
def not_found(e):
    return jsonify({"error": "Not Found"}), 404

# Run the application on all network interfaces (0.0.0.0) at port 5000
if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
