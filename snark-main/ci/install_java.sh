#!/bin/bash

JAVA_VERSION="$1"

source ~/.sdkman/bin/sdkman-init.sh
sdk install java "$JAVA_VERSION"
