#!/usr/bin/env bash
set -ex
trap 'set +ex' EXIT

sudo sh -c 'echo "deb https://packages.atlassian.com/debian/atlassian-sdk-deb/ stable contrib" >>/etc/apt/sources.list'
wget https://packages.atlassian.com/api/gpg/key/public
sudo apt-key add public
sudo apt-get update
sudo apt-get install atlassian-plugin-sdk
