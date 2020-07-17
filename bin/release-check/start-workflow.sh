#!/usr/bin/env bash

# INPUT ENV VARS
workflow_name=${WORKFLOW_NAME:?Please set WORKFLOW_NAME.}
java_version=${JAVA_VERSION:?Please set JAVA_VERSION.}
product_version=${PRODUCT_VERSION:?Please set PRODUCT_VERSION.}

# TRIGGER A CUSTOM PIPELINE

# PREPARE PAYLOAD BODY
if [[ -z "$GITHUB_REF" ]]; then
  ref=master
else
  # TRANSFORM 'refs/heads/feature/issue-1' to 'feature/issue-1'
  ref=$(echo "$GITHUB_REF" | cut -d / -f 3-)
fi
start_workflow_payload=$(cat <<EOF
{
    "ref": "$ref",
    "inputs": {
        "ref": "$ref",
        "java-version": "$java_version",
        "product-version": "$product_version"
    }
}
EOF
)

# CALL GITHUB TO START WORKFLOW
echo "Triggering workflow=$workflow_name on ref=$ref, java-version=$java_version, product-version=$product_version, token=$GITHUB_TOKEN, payload=[$start_workflow_payload]" 1>&2
curl -X POST -s -i -u "$GH_USER:$GH_TOKEN" \
  -H 'Content-Type: application/json' -d "$start_workflow_payload" \
  "https://api.github.com/repos/atlassian-labs/atlassian-slack-integration-server/actions/workflows/$workflow_name/dispatches" 1>&2

# WAIT A BIT AND REQUEST a RUN ID
sleep 20s
runs_response=$(curl -s "https://api.github.com/repos/atlassian-labs/atlassian-slack-integration-server/actions/workflows/$workflow_name/runs?branch=$ref&event=workflow_dispatch")
#echo "Received runs response: $runs_response" 1>&2
run_url=$(echo "$runs_response" | jq -r '.workflow_runs[0].html_url')

# RETURN LINK TO WORKFLOW, IF ANY, OR AN ERROR MESSAGE
if [ "$run_url" = "" ]; then
  echo "Cannot find runs: $runs_response" 1>&2
  echo ''
else
  echo "$run_url"
fi
