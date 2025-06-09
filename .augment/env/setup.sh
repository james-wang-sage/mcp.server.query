#!/bin/bash
set -e

echo "Setting up Spring Boot Maven project environment..."

# Update package lists
sudo apt-get update -y

# Install Java 17 (OpenJDK)
echo "Installing Java 17..."
sudo apt-get install -y openjdk-17-jdk

# Set JAVA_HOME and add to PATH
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
echo "export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64" >> $HOME/.profile
echo "export PATH=\$JAVA_HOME/bin:\$PATH" >> $HOME/.profile

# Install Maven
echo "Installing Maven..."
sudo apt-get install -y maven

# Verify installations
echo "Verifying Java installation..."
java -version
echo "Verifying Maven installation..."
mvn -version

# Navigate to project directory
cd /mnt/persist/workspace

# Clean and compile the project
echo "Cleaning and compiling the project..."
mvn clean compile

# Compile test sources
echo "Compiling test sources..."
mvn test-compile

echo "Setup completed successfully!"