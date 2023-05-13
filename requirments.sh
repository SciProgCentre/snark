set -e

apt-get update
apt-get install -y sudo

sudo apt-get install -y kotlin

for dir in ./*/
do
    if [[($dir == *'snark'*)]]
    then
        cd "$dir"

        if [[ $(find -type d -name "ci") ]]
        then

            cd ci

            ./requirments.sh

            cd ..
        fi

        cd ..
    fi
done

