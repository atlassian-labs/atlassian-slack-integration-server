#!/usr/bin/env bash
set -ex
trap 'set +ex' EXIT

sudo apt-get update
sudo apt-get -y install libdbus-glib-1-2 libxrender1 libxcomposite-dev libasound2 libgtk2.0-0 libgtk-3-0 xvfb
