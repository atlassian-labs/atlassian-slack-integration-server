#!/usr/bin/env bash

# SEARCH LATEST RELEASES
latest_releases=$(. ./find-latest-releases.sh)
echo "========================"
echo "LATEST RELEASES"
echo "$latest_releases" | sed -z 's/\n/, /g' | awk '{print substr( $0, 1, length($0)-2)}'
echo "========================"

if [ "$latest_releases" = "" ]; then
  echo "Could not find latest versions"
  exit
fi

# TAKE AND CHECKS LATEST COMPATIBLE VERSION ONLY, MEANING THE GREATEST REVISION VERSION
pl_product_version=$(. ../get-plugin-major-version.sh "$PRODUCT")
product_compat_version_regex=$(jq -r --arg pl "$pl_product_version" --arg p "$PRODUCT" '.[$p].[$pl]' ./plugin-product-compat-matrix.json)
latest_version=$(echo "$latest_releases" | grep -oE "$product_compat_version_regex" | tail -1)

if [ "$latest_version" = "" ]; then
  echo "Could not find latest versions"
  echo "Product [$PRODUCT]"
  echo "Compatible version pattern [$product_compat_version_regex]"
  exit
fi

echo "========================"
echo "LATEST VERSION"
echo "$latest_version"
echo "========================"

# RUN CHECK FOR LATEST VERSION
PRODUCT_VERSION="$latest_version" . ./check-release.sh
