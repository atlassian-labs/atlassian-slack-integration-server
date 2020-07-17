#!/usr/bin/env bash
set -ex
trap 'set +ex' EXIT

RELEASE_VERSION_ARG=$([[ -z $RELEASE_VERSION ]] && echo "" || echo "-DreleaseVersion=$RELEASE_VERSION")
DEVELOPMENT_VERSION_ARG=$([[ -z $DEVELOPMENT_VERSION ]] && echo "" || echo "-DdevelopmentVersion=$DEVELOPMENT_VERSION")

MAVEN_HOME=$(atlas-version | grep 'ATLAS Maven Home' | grep -oE '/.+$')

# atlas-mvn or atlas-release fails with "Unknown lifecycle phase ci]" error
$MAVEN_HOME/bin/mvn -gs ${MAVEN_HOME}/conf/settings.xml release:prepare release:perform \
    --show-version \
    --batch-mode \
    -Dmaven.test.skip=true \
    -Darguments="-Dmaven.test.skip=true --activate-profiles include-common" \
    -DscmCommentPrefix="[skip ci] " \
    -Dusername=$GITHUB_ACTOR \
    -Dpassword=$GITHUB_TOKEN \
    --activate-profiles include-common \
    --projects "${PLUGIN}" ${RELEASE_VERSION_ARG} ${DEVELOPMENT_VERSION_ARG}
