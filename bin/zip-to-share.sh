#!/usr/bin/env bash

current_time=$(date "+%Y.%m.%d-%H.%M.%S")
filename=atlassian-slack-server-integration-${current_time}.zip

echo "Compressing project files to share with customers, if we're asked..."
(
    cd "$( dirname "${BASH_SOURCE[0]}")/.." ;
        mkdir target;
        zip -vr target/${filename} \
            bin/ \
            bitbucket-slack-server-integration-plugin/ \
            confluence-slack-integration/ \
            jira-slack-server-integration/ \
            slack-server-integration-common/ \
            slack-server-integration-test-common/ \
            *.sh \
            LICENSE.txt \
            pom.xml \
            README.md \
            -x "*.DS_Store" "*.iml" "*/target/*" "bin/build/*"
)
