#!/usr/bin/env bash
set -ex
trap 'set +ex' EXIT

# With ngrok running, obtain public https URL for selected port via ngrok API (https://ngrok.com/docs#list-tunnels)
# JSON parsing requires https://stedolan.github.io/jq/
BB_NGROK="$(curl -s "http://127.0.0.1:4040/api/tunnels" | \
  jq -r '.tunnels[] | select(.proto == "https") | select (.config.addr|endswith("'"7990"'")) | .public_url[8:]')"

(
    cd "$( dirname "${BASH_SOURCE[0]}")/.." ;

    # set env var BB_NGROK to enable HTTPS

    mvn bitbucket:debug \
        -Datlassian.dev.mode=true \
        -Dmaven.test.skip=true \
        -Dlogging.level.com.atlassian.bitbucket.plugins.slack=TRACE \
        -Dlogging.level.com.atlassian.plugins.slack=TRACE \
        -Dlogging.level.com.github.seratach.jslack.maintainer.json=DEBUG \
        "$@" \
        $([[ "${BB_NGROK}" != "" ]] && echo "-Dserver.proxy-name=${BB_NGROK} -Dserver.proxy-port=443 -Dserver.scheme=https -Dserver.secure=true" || echo "") \
        --projects bitbucket-slack-server-integration-plugin \
        --activate-profiles include-common \
        | tee bitbucket.log
)

# With proxy:
#        -Dhttps.proxyHost=localhost -Dhttps.proxyPort=4567 \
#        -Dhttps.proxyUser=user -Dhttps.proxyPassword=pass \
