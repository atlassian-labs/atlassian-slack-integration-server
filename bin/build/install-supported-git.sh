#!/usr/bin/env bash

set -ex
trap 'set +ex' EXIT

git --version

dpkg -l | grep git
sudo apt-get remove -y git git-man
dpkg -l | grep git

# Instruction: https://www.digitalocean.com/community/tutorials/how-to-install-git-from-source-on-ubuntu-20-04-quickstart
sudo apt-get update
sudo apt-get install libz-dev libssl-dev libcurl4-gnutls-dev libexpat1-dev gettext cmake gcc
mkdir git-src
cd git-src
curl -L https://www.kernel.org/pub/software/scm/git/git-2.50.1.tar.gz | tar xz
cd git-*
make prefix=/usr/local all
sudo make prefix=/usr/local install

echo $PATH
/usr/local/bin/git --version
