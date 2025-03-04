"""
This script contains automated tests for the Flask application.
It verifies the availability of key API endpoints.
"""
import requests
import pytest

# Base URL of the Flask application
BASE_URL = "http://localhost:5000"

def test_health_check():
    """
    Test the /health endpoint to ensure the application is running correctly.
    """
    response = requests.get(f"{BASE_URL}/health")  # Send a request to the API
    assert response.status_code == 200  # Ensure the server returns HTTP 200 OK
    assert response.json() == {"status": "ok"}  # Verify the response JSON content

def test_home_page():
    """
    Test the root (/) endpoint to ensure it returns a valid response.
    """
    response = requests.get(BASE_URL)  # Send a request to the root endpoint
    assert response.status_code == 200  # Ensure the response is HTTP 200 OK
    assert "Welcome to the Home Page" in response.text  # Verify the expected output in response
