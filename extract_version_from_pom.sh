#!/bin/bash

v="v"$(mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | egrep "^[0-9]+\.")
echo $v > src/main/resources/VERSION

v=$(cat src/main/resources/VERSION)
echo "src/main/resources/VERSION -> $v"

