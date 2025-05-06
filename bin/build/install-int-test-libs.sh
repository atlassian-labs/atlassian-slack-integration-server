#!/usr/bin/env bash
set -ex
trap 'set +ex' EXIT

sudo apt-get update
sudo apt-get -y install libdbus-glib-1-2 libxrender1 libxcomposite-dev libasound2t64 libgtk2.0-0t64 libgtk-3-0t64 xvfb
