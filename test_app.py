import requests  # Import the requests library to send HTTP requests
import pytest  # Import pytest for writing test cases

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
    # Send a GET request to the /health endpoint
    response = requests.get(f"{BASE_URL}/health", headers=HEADERS)

    # Parse the response as JSON
    json_data = response.json()

    # Validate HTTP status
    assert response.status_code == 200, f"Expected status 200, got {response.status_code}"

    # Validate that the response JSON contains the expected keys
    assert "status" in json_data, "Missing 'status' key in response JSON"
    assert json_data["status"] == "ok", f"Unexpected API status: {json_data['status']}"

    # Validate that 'message' exists and is a string
    assert "message" in json_data, "Missing 'message' key in response JSON"
    assert isinstance(json_data["message"], str), "Expected 'message' to be a string"

def test_home_api():
    """
    Test the root (/) endpoint to verify it returns a valid JSON response.
    This test checks:
    - The API returns HTTP 200.
    - The response contains the correct page name ("home").
    - The response includes a valid message.
    """
    # Send a GET request to the home ("/") endpoint
    response = requests.get(BASE_URL, headers=HEADERS)

    # Parse the response as JSON
    json_data = response.json()

    # Validate HTTP status
    assert response.status_code == 200, f"Expected status 200, got {response.status_code}"

    # Validate that the response JSON contains the expected keys
    assert "page" in json_data, "Missing 'page' key in response JSON"
    assert json_data["page"] == "home", f"Unexpected page name: {json_data['page']}"

    # Validate that 'message' exists and is a string
    assert "message" in json_data, "Missing 'message' key in response JSON"
    assert isinstance(json_data["message"], str), "Expected 'message' to be a string"

def test_404_page():
    """
    Test a non-existing page to verify that the custom 404 JSON response is returned.
    This test ensures:
    - A non-existent endpoint returns HTTP 404.
    - The response JSON contains the correct error message.
    """
    # Send a GET request to a non-existing page
    response = requests.get(f"{BASE_URL}/nonexistentpage", headers=HEADERS)

    # Parse the response as JSON
    json_data = response.json()

    # Validate HTTP status
    assert response.status_code == 404, f"Expected status 404, got {response.status_code}"

    # Validate that the response JSON contains the expected error message
    assert "error" in json_data, "Missing 'error' key in response JSON"
    assert json_data["error"] == "404 - Page Not Found", f"Unexpected error message: {json_data['error']}"

def test_api_test_content():
    """
    Test the /api/test-content endpoint to ensure API data remains consistent.
    This test verifies:
    - The API responds with HTTP 200.
    - The response includes a 'message' key with a string value.
    """
    # Send a GET request to the /api/test-content endpoint
    response = requests.get(f"{BASE_URL}/api/test-content", headers=HEADERS)

    # Parse the response as JSON
    json_data = response.json()

    # Validate HTTP status
    assert response.status_code == 200, f"Expected status 200, got {response.status_code}"

    # Validate that the response JSON contains the expected 'message' key
    assert "message" in json_data, "Missing 'message' key in response JSON"
    assert isinstance(json_data["message"], str), "Expected 'message' to be a string"

