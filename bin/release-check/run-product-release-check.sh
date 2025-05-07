#!/usr/bin/env bash

# SEARCH LATEST RELEASES
latest_releases=$(. ./find-latest-releases.sh)
echo "========================"
echo "LATEST RELEASES"
echo "$latest_releases" | sed -z 's/\n/, /g' | awk '{print substr( $0, 1, length($0)-2)}'
echo "========================"

if [ "$latest_releases" = "" ]; then
  echo "Could not find latest versions"
fi

case $PRODUCT in
  jira)
    version_regex="10\\.[0-9]+\\.[0-9]+"
    ;;
  confluence|bitbucket)
    version_regex="9\\.[0-9]+\\.[0-9]+"
    ;;
  *)
    echo "Invalid product"
    exit 1
    ;;
esac

latest_version=$(echo "$latest_releases" | grep -oE "$version_regex" | tail -1)

if [ "$latest_version" = "" ]; then
  echo "Could not find latest versions"
  echo "Product [$PRODUCT]"
  echo "Compatible version pattern [$version_regex]"
  exit
fi

echo "========================"
echo "LATEST VERSION"
echo "$latest_version"
echo "========================"

# RUN CHECK FOR LATEST VERSION
PRODUCT_VERSION="$latest_version" . ./check-release.sh
