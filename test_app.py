import requests  # Used to send HTTP requests to the Flask API
import pytest  # Used for structuring and running test cases
import os  # Used for interacting with the system (e.g., checking Gunicorn)

# Define the base URL for the application (localhost only to avoid external IP dependency)
BASE_URL = "http://localhost:5000"

# Define request headers for JSON responses
HEADERS = {"Accept": "application/json"}

# ------------------- Tests -------------------

def test_health_check():
    # Test /health endpoint and validate expected JSON structure and content
    response = requests.get(f"{BASE_URL}/health", headers=HEADERS)
    json_data = response.json()
    assert response.status_code == 200  # Ensure HTTP response is 200
    assert "status" in json_data  # Ensure 'status' key exists
    assert json_data["status"] == "ok"  # Ensure status value is "ok"
    assert "message" in json_data  # Ensure 'message' key exists
    assert isinstance(json_data["message"], str)  # Ensure message is a string

def test_home_api():
    # Test root (/) endpoint and validate structure for JSON response
    response = requests.get(BASE_URL, headers=HEADERS)
    json_data = response.json()
    assert response.status_code == 200  # Ensure HTTP response is 200
    assert "page" in json_data  # Ensure 'page' key exists
    assert json_data["page"] == "home"  # Ensure page value is "home"
    assert "message" in json_data  # Ensure 'message' key exists
    assert isinstance(json_data["message"], str)  # Ensure message is a string

def test_404_page():
    # Test for invalid route and validate custom 404 JSON response
    response = requests.get(f"{BASE_URL}/nonexistent", headers=HEADERS)
    json_data = response.json()
    assert response.status_code == 404  # Ensure HTTP response is 404
    assert "error" in json_data  # Ensure 'error' key exists
    assert json_data["error"] == "404 - Page Not Found"  # Ensure correct error message

def test_api_test_content():
    # Test /api/test-content and verify structure of the response
    response = requests.get(f"{BASE_URL}/api/test-content", headers=HEADERS)
    json_data = response.json()
    assert response.status_code == 200  # Ensure HTTP response is 200
    assert "message" in json_data  # Ensure 'message' key exists
    assert isinstance(json_data["message"], str)  # Ensure message is a string

def test_gunicorn_running():
    # Verify that Gunicorn process is running using pgrep
    result = os.system("pgrep gunicorn > /dev/null")
    assert result == 0, "Gunicorn is not running!"  # Fail if Gunicorn is not active

def test_api_health_check():
    # Test /api/health endpoint to validate status and response structure
    response = requests.get(f"{BASE_URL}/api/health", headers=HEADERS)
    json_data = response.json()
    assert response.status_code == 200  # Ensure HTTP response is 200
    assert "status" in json_data  # Ensure 'status' key exists
    assert json_data["status"] == "ok"  # Ensure status is "ok"
    assert "message" in json_data  # Ensure 'message' key exists
    assert isinstance(json_data["message"], str)  # Ensure message is a string

def test_invalid_method():
    # Send a POST to a GET-only endpoint to ensure proper error response
    response = requests.post(f"{BASE_URL}/health", headers=HEADERS)
    assert response.status_code in [400, 405]  # Validate appropriate method error code
