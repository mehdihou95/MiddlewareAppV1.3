#!/bin/bash

echo "Building parent POM..."
mvn clean install -N

echo "Building shared-config module..."
cd shared-config
mvn clean install

echo "Building processor module..."
cd ../processor
mvn clean install

echo "Building listener module..."
cd ../listener
mvn clean install

echo "Build complete!" 