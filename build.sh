#!/bin/bash
# Build script for deployment

echo "Building application..."
./mvnw clean package -DskipTests

echo "Build completed successfully!"
