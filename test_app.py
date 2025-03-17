import requests
import pytest

# Define the base URL for the Flask application
BASE_URL = "http://localhost:5000"
HEADERS = {"Accept": "application/json"}  # Ensure JSON response

def test_health_check():
    """
    Test the /api/health endpoint to ensure the API is running correctly.
    """
    response = requests.get(f"{BASE_URL}/api/health", headers=HEADERS)  # Send GET request
    assert response.status_code == 200  # Ensure HTTP status 200 OK

    json_data = response.json()  # Parse response as JSON
    assert "status" in json_data  # Ensure 'status' key exists
    assert json_data["status"] == "ok"  # Verify service status
    assert "message" in json_data  # Ensure 'message' key exists
    assert isinstance(json_data["message"], str)  # Ensure 'message' is a string

def test_home_api():
    """
    Test the /api/home endpoint to verify content is returned correctly in JSON.
    """
    response = requests.get(f"{BASE_URL}/api/home", headers=HEADERS)  # Send GET request
    assert response.status_code == 200  # Ensure HTTP status 200 OK

    json_data = response.json()  # Parse response as JSON
    assert "page" in json_data  # Ensure 'page' key exists
    assert json_data["page"] == "home"  # Validate 'page' content
    assert "message" in json_data  # Ensure 'message' key exists
    assert isinstance(json_data["message"], str)  # Ensure 'message' is a string

def test_404_page():
    """
    Test a non-existing page to verify the custom 404 page is returned in JSON format.
    """
    response = requests.get(f"{BASE_URL}/nonexistentpage", headers=HEADERS)  # Request a non-existent page
    assert response.status_code == 404  # Ensure HTTP status 404 Not Found

    json_data = response.json()  # Parse response as JSON
    assert "error" in json_data  # Ensure 'error' key exists
    assert json_data["error"] == "404 - Page Not Found"  # Validate error message

def test_api_test_content():
    """
    Test the /api/test-content endpoint to ensure API data remains consistent.
    """
    response = requests.get(f"{BASE_URL}/api/test-content", headers=HEADERS)  # Send GET request
    assert response.status_code == 200  # Ensure HTTP status 200 OK

    json_data = response.json()  # Parse response as JSON
    assert "message" in json_data  # Ensure 'message' key exists
    assert isinstance(json_data["message"], str)  # Ensure 'message' is a string

