#!/usr/bin/env bash

matrix_file='bin/build/java-product-matrix.json'

# Get slack plugin major common version
pl_common_version=$(. bin/build/get-plugin-major-version.sh common)
if [ $pl_common_version -gt 1 ]; then
  matrix_key='current'
# TODO: Remove these flags when new major jira (10.x) is released
  echo "skip-jira-its=true" >> $GITHUB_OUTPUT
else
  matrix_key='old'
fi
echo "Matrix key - $matrix_key"

echo "unit-tests-matrix=$(jq --compact-output --arg v "$matrix_key" '.[$v]."unit-tests"' $matrix_file)" >> $GITHUB_OUTPUT
echo "jira-it-matrix=$(jq --compact-output --arg v "$matrix_key" '.[$v]."jira-it"' $matrix_file)" >> $GITHUB_OUTPUT
echo "confluence-it-matrix=$(jq --compact-output --arg v "$matrix_key" '.[$v]."confluence-it"' $matrix_file)" >> $GITHUB_OUTPUT
echo "bitbucket-it-matrix=$(jq --compact-output --arg v "$matrix_key" '.[$v]."bitbucket-it"' $matrix_file)" >> $GITHUB_OUTPUT