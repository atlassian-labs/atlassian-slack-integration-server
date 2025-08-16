#!/usr/bin/env bash
set -ex
trap 'set +ex' EXIT

sudo apt-get update
sudo apt-get -y install libgtk2.0-0 libgtk-3-0 libgbm-dev libnotify-dev libnss3 libxss1 libasound2 libxtst6 xauth xvfb

sudo snap remove firefox || true
sudo add-apt-repository -y ppa:mozillateam/ppa
echo '
Package: firefox*
Pin: release o=LP-PPA-mozillateam
Pin-Priority: 501
' | sudo tee -a /etc/apt/preferences.d/mozillateamppa

sudo apt-get update
sudo apt-get -y install firefox-esr