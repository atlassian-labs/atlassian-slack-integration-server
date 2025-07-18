#!/usr/bin/env bash

atlas-mvn install --batch-mode \
    --projects \
com.atlassian.plugins:atlassian-slack-server-integration-parent,\
slack-server-integration-common,\
confluence-slack-integration,\
slack-server-integration-test-common \
    -Dmaven.test.skip=true
