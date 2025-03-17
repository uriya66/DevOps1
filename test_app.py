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
    assert json_data["status"] == "ok"  # Verify service status

def test_home_api():
    """
    Test the /api/home endpoint to verify content is returned correctly in JSON.
    """
    response = requests.get(f"{BASE_URL}/api/home", headers=HEADERS)  # Send GET request
    assert response.status_code == 200  # Ensure HTTP status 200 OK
    json_data = response.json()  # Parse response as JSON
    assert json_data["page"] == "home"  # Validate 'page' content

def test_404_page():
    """
    Test a non-existing page to verify the custom 404 page is returned in JSON format.
    """
    response = requests.get(f"{BASE_URL}/nonexistentpage", headers=HEADERS)  # Request a non-existent page
    assert response.status_code == 404  # Ensure HTTP status 404 Not Found

def test_api_test_content():
    """
    Test the /api/test-content endpoint to ensure API data remains consistent.
    """
    response = requests.get(f"{BASE_URL}/api/test-content", headers=HEADERS)  # Send GET request
    assert response.status_code == 200  # Ensure HTTP status 200 OK
    json_data = response.json()  # Parse response as JSON
    assert isinstance(json_data["message"], str)  # Ensure message is a string

