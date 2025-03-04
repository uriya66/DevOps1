from flask import Flask, jsonify

app = Flask(__name__)

@app.route('/')
def home():
    return "Welcome to the Home Page", 200

@app.route('/health')
def health_check():
    return jsonify({"status": "ok"}), 200

@app.errorhandler(404)
def not_found(e):
    return jsonify({"error": "Not Found"}), 404

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
