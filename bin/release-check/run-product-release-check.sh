#!/usr/bin/env bash

# SEARCH LATEST RELEASES
latest_releases=$(. ./find-latest-releases.sh)
echo "========================"
echo "LATEST RELEASES"
echo "$latest_releases" | jq -r 'join(", ")'
echo "========================"

if [ "$latest_releases" = "" ]; then
  echo "Could not find latest versions"
  exit
fi

# TAKE AND CHECKS LATEST VERSION ONLY, MEANING THE GREATEST REVISION VERSION
latest_version=$(echo "$latest_releases" | jq -r '. | first')

# RUN CHECK FOR LATEST VERSION
PRODUCT_VERSION="$latest_version" . ./check-release.sh
