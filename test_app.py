import requests
import pytest

# 🌍 Define the base URL for the Flask application
BASE_URL = "http://localhost:5000"

def test_health_check():
    """
    ✅ Test the /health endpoint to ensure the application is running correctly.
    """
    response = requests.get(f"{BASE_URL}/health")  # Send a GET request to /health
    json_data = response.json()  # Parse response as JSON

    assert response.status_code == 200  # Ensure the response status is 200 OK
    assert "status" in json_data  # Check if "status" key exists in the response
    assert json_data["status"] == "ok"  # Ensure the status is "ok"
    assert "message" in json_data  # Check if "message" key exists
    assert json_data["message"] == "Application is running!"  # Validate message content

def test_home_page():
    """
    ✅ Test the root (/) endpoint to ensure it returns a valid response.
    """
    response = requests.get(BASE_URL)  # Send a GET request to the root endpoint
    assert response.status_code == 200  # Ensure the response status is 200 OK
    assert "🚀 Welcome to My Flask App!" in response.text  # Validate response content

def test_404_page():
    """
    ❌ Test a non-existing page to verify the custom 404 page is returned.
    """
    response = requests.get(f"{BASE_URL}/nonexistentpage")  # Request a non-existent page
    json_data = response.json()  # Parse response as JSON

    assert response.status_code == 404  # Ensure the response status is 404 Not Found
    assert "error" in json_data  # Check if "error" key exists in response
    assert json_data["error"] == "404 - Page Not Found"  # Validate error message

def test_api_response_format():
    """
    ✅ Test the response format to ensure it is a valid JSON structure.
    """
    response = requests.get(f"{BASE_URL}/health")  # Send a GET request to /health
    json_data = response.json()  # Parse response as JSON

    assert isinstance(json_data, dict)  # Ensure the response is a dictionary (JSON object)
    assert "status" in json_data  # Check if "status" key exists
    assert json_data["status"] == "ok"  # Ensure the status is "ok"
    assert "message" in json_data  # Check if "message" key exists
    assert isinstance(json_data["message"], str)  # Ensure the message is a string
