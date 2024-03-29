#!/bin/bash

set -e

apt-get update
apt-get install -y sudo

for dir in ./*/
do
    if [[ $dir == *'snark'* ]]
    then
        cd "$dir"

        if [[ $(find -type d -name "ci") ]]
        then

            cd ci
            if [[ -f "requirements.sh" ]]; then
              echo "executing sub"
              bash -c "./requirements.sh"
            fi

            cd ..
        fi

        cd ..
    fi
done

