#!/usr/bin/env bash
set -ex
trap 'set +ex' EXIT

########################################################################################################################
# Update running Jira with a base url based on running ngrok on port 2990 or specified as parameter
# Example using another port: ./scripts/set-ngrok-baseurl.sh 2991
########################################################################################################################

# Port selection
jira_port=${1:-2990}

echo "Looking for local ngrok instance at port $jira_port"
# With ngrok running, obtain public https URL for selected port via ngrok API (https://ngrok.com/docs#list-tunnels)
# JSON parsing requires https://stedolan.github.io/jq/
ngrok_host="$(curl -s "http://127.0.0.1:4040/api/tunnels" | \
  jq -r '.tunnels[] | select(.proto == "https") | select (.config.addr|endswith("'"$jira_port"'")) | .public_url')"

if [ "$ngrok_host" = "" ]; then
    echo "ngrok not found for port $jira_port. Run: ngrok http $jira_port"
    exit
fi

# Update Jira base URL according to running ngrok
echo "Updating local Jira base URL with $ngrok_host..."
curl -s -w "%{http_code}" \
  -u admin:admin -X PUT  \
  -H "Content-Type: application/json" \
  -d "${ngrok_host}/jira" \
  "http://localhost:$jira_port/jira/rest/api/2/settings/baseUrl"



