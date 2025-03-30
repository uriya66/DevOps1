import requests  # Import the requests library to send HTTP requests
import pytest  # Import pytest for writing test cases
import os  # Import os for system checks

# Define the base URL for the Flask application
BASE_URL = "http://localhost:5000"

# Define headers to ensure the response is returned in JSON format
HEADERS = {"Accept": "application/json"}

def test_health_check():
    """
    Test the /health endpoint to ensure the API is running correctly.
    This test verifies:
    - The API is up and responding with HTTP 200.
    - The response JSON contains expected keys.
    - The status of the service is "ok".
    """
    response = requests.get(f"{BASE_URL}/health", headers=HEADERS)
    json_data = response.json()
    assert response.status_code == 200
    assert "status" in json_data
    assert json_data["status"] == "ok"
    assert "message" in json_data
    assert isinstance(json_data["message"], str)

def test_home_api():
    """
    Test the root (/) endpoint to verify it returns a valid JSON response.
    """
    response = requests.get(BASE_URL, headers=HEADERS)
    json_data = response.json()
    assert response.status_code == 200
    assert "page" in json_data
    assert json_data["page"] == "home"
    assert "message" in json_data
    assert isinstance(json_data["message"], str)

def test_404_page():
    """
    Test a non-existing page to verify that the custom 404 JSON response is returned.
    """
    response = requests.get(f"{BASE_URL}/nonexistentpage", headers=HEADERS)
    json_data = response.json()
    assert response.status_code == 404
    assert "error" in json_data
    assert json_data["error"] == "404 - Page Not Found"

def test_api_test_content():
    """
    Test the /api/test-content endpoint to ensure API data remains consistent.
    This test verifies:
    - The API responds with HTTP 200.
    - The response includes a 'message' key with a string value.
    """
    response = requests.get(f"{BASE_URL}/api/test-content", headers=HEADERS)
    json_data = response.json()
    assert response.status_code == 200
    assert "message" in json_data
    assert isinstance(json_data["message"], str)

def test_gunicorn_running():
    """
    Ensure that Gunicorn is actually running by checking process existence.
    This test verifies:
    - The Gunicorn process is running on the system.
    - If Gunicorn is not running, the test fails.
    """
    result = os.system("pgrep gunicorn > /dev/null")
    assert result == 0, "Gunicorn is not running!"

def test_api_health_check():
    """
    Test the /api/health endpoint to validate it returns a correct response.
    This test verifies:
    - The API responds with HTTP 200.
    - The response JSON contains expected keys.
    - The status of the service is "ok".
    """
    response = requests.get(f"{BASE_URL}/api/health", headers=HEADERS)
    json_data = response.json()
    assert response.status_code == 200
    assert "status" in json_data
    assert json_data["status"] == "ok"
    assert "message" in json_data
    assert isinstance(json_data["message"], str)

def test_invalid_method():
    """
    Test an invalid method request (POST instead of GET) to ensure proper handling.
    This test verifies:
    - A POST request to a GET-only endpoint should return 400 or 405.
    """
    response = requests.post(f"{BASE_URL}/health", headers=HEADERS)
    assert response.status_code in [400, 405], "Expected 400 or 405 for invalid method request"
