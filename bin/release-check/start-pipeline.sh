#!/usr/bin/env bash

# INPUT ENV VARS
pipeline_name=${PIPELINE_NAME:?Please set PIPELINE_NAME.}
version="${VERSION:?Please set VERSION.}"
atl_bb_app_user="${ATL_BB_APP_USER:?Please set ATL_BB_APP_USER.}"
atl_bb_app_password="${ATL_BB_APP_PASSWORD:?Please set ATL_BB_APP_PASSWORD. Creatae yours at https://bitbucket.org/account/ > App passwords}"

# TRIGGER A CUSTOM PIPELINE

# PREPARE PAYLOAD BODY
start_pipeline_payload=$(cat <<EOF
{
  "target": {
    "type": "pipeline_ref_target",
    "ref_type": "branch",
    "ref_name": "master",
    "selector": {
      "type": "custom",
      "pattern": "$pipeline_name"
    }
  },
  "variables": [
    {
      "key": "VERSION",
      "value": "$version"
    }
  ]
}
EOF
)

# CALL BB TO START PIPELINE
start_pipeline_response=$(curl -X POST -s -u "${atl_bb_app_user}:${atl_bb_app_password}" \
  -H 'Content-Type: application/json' -d "$start_pipeline_payload" \
  "https://api.bitbucket.org/2.0/repositories/atlassianlabs/atlassian-slack-integration-server/pipelines/")

# FIND A BUILD NUMBER IN THE RESPONSE
build_number=$(echo "$start_pipeline_response" | jq -r '.build_number')

# RETURN LINK TO PIPELINE, IF ANY, OR AN ERROR MESSAGE
if [ "$build_number" = "" ]; then
  echo "Unexpected response: ${start_pipeline_response}"
else
  echo "https://bitbucket.org/atlassianlabs/atlassian-slack-integration-server/addon/pipelines/home#!/results/$build_number"
fi
