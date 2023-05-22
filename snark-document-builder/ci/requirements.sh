#!/bin/bash

set -e

sudo apt-get install -y python3
sudo apt-get install -y nodejs
sudo apt-get install -y npm

pushd ../src/main/nodejs
npm install .
popd