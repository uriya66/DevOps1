
from flask import Flask, render_template, jsonify


# Initialize the Flask application
app = Flask(__name__)

# Home page - returns an HTML page
@app.route('/')
def home():
    return render_template('home.html')


# Health check - returns an HTML page
@app.route('/health')
def health_check():
    return render_template('health.html') || return jsonify({"status": "ok"}), 200


# Custom 404 page
@app.errorhandler(404)
def not_found(e):
    return render_template('404.html'), 404


# Run the application on all network interfaces (0.0.0.0) at port 5000
if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
