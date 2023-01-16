#!/usr/bin/env bash

# override Maven from Plugin SDK as it is too old to run Confluence 8 and Jira 9.5

BUNDLED_MVN_HOME=$(mvn -v | grep "Maven home" | sed 's/Maven home: //')
echo "ATLAS_MVN=$BUNDLED_MVN_HOME/bin/mvn" >> $GITHUB_ENV