#!/usr/bin/env bash
set -ex
trap 'set +ex' EXIT

RELEASE_VERSION_ARG=$([[ -z $RELEASE_VERSION ]] && echo "" || echo "-DreleaseVersion=$RELEASE_VERSION")
DEVELOPMENT_VERSION_ARG=$([[ -z $DEVELOPMENT_VERSION ]] && echo "" || echo "-DdevelopmentVersion=$DEVELOPMENT_VERSION")

mvn release:prepare release:perform --show-version --batch-mode \
    -Dmaven.test.skip=true \
    -Darguments=-Dmaven.test.skip=true --activate-profiles include-common \
    -DscmCommentPrefix="[skip ci] " \
    --activate-profiles include-common \
    --projects "${PLUGIN}" ${RELEASE_VERSION_ARG} ${DEVELOPMENT_VERSION_ARG}

echo "
       ___  ______   _______   ___________
      / _ \/ __/ /  / __/ _ | / __/ __/ _ \\
     / , _/ _// /__/ _// __ |_\ \/ _// // /
    /_/|_/___/____/___/_/ |_/___/___/____/

    https://packages.atlassian.com/maven-public-local/com/atlassian/${NAME}/plugins/${NAME}-slack-server-integration-plugin/${RELEASE_VERSION}/${NAME}-slack-server-integration-plugin-${RELEASE_VERSION}.jar

"

