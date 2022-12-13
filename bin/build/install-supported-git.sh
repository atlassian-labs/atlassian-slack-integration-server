#!/usr/bin/env bash

set -ex
trap 'set +ex' EXIT

git --version

dpkg -l | grep git
sudo apt remove git git-man
dpkg -l | grep git

# Instruction: https://www.digitalocean.com/community/tutorials/how-to-install-git-from-source-on-ubuntu-20-04-quickstart
sudo apt update
sudo apt install libz-dev libssl-dev libcurl4-gnutls-dev libexpat1-dev gettext cmake gcc
mkdir git-src
cd git-src
curl -o git.tar.gz https://mirrors.edge.kernel.org/pub/software/scm/git/git-2.37.4.tar.gz
tar -zxf git.tar.gz
cd git-*
make prefix=/usr/local all
sudo make prefix=/usr/local install

echo $PATH
/usr/local/bin/git --version
