#!/bin/bash

echo " Starting deployment..."
nohup python3 app.py &  
echo " Application is running at http://localhost:5000"
