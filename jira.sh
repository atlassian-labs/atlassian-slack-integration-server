#!/usr/bin/env bash

HELP_TEXT="This is a shortcut script to run *everything* you need related to the Jira plugin.

Arguments:
  - run       compile the common module, the Jira plugin, and start up Jira for development
  - pack      compile the Jira plugin ('pack' is skipped if 'run' is passed)
  - common    compile common module
  - compat    compile Jira 8 and JSD compatibility modules
  - clean     clear compiled files, except Jira home directory ('clean' is skipped if 'purge' is passed)
  - purge     clear all compiled files, including Jira home directory with database and everything
  - deps      clean the plugin's embedded dependencies, forcing them to be rebuild in the next compilation
              (use when strange errors about missing classes occur - IntelliJ removes some files when rebuilding)
  - ngrok     with ngrok and Jira running, it updates Jira base URL with the current ngrok HTTPS URL

You pass any combination of arguments in any order. Calling the script without arguments defaults to: 'common jira'

Examples:
  ./jira.sh run          -> compiles the Jira plugin and starts up Jira for development

  ./jira.sh              -> compiles the common module and the Jira plugin; good for quick reloading
  ./jira.sh common pack  -> same as above

  ./jira.sh clean run    -> cleans all compiled files but the Jira home directory, compiles everything, and runs Jira in development mode
"

# check java 8
javaVersion=`java -version 2>&1 | head -n 1 | cut -d\" -f 2`
javaCompilerVersion=`javac -version 2>&1 | head -n 1 | cut -d\" -f 2`
[[ "$javaVersion" != "1.8."* || "$javaCompilerVersion" != *"1.8."* ]] && echo "Java 8 expected" && exit 1

# compute parameters
purge=$([[ "$*" == *"purge"* ]] && echo "yes" || echo "no")
clean=$([[ "$*" == *"clean"* ]] && ([[ "$purge" == "yes" ]] && echo "skip" || echo "yes") || echo "no")

empty=$([[ "$*" == "" ]] && echo "yes" || echo "no")

run=$([[ "$*" == *"run"* ]] && echo "yes" || echo "no")
pack=$([[ "$*" == *"pack"* || "$empty" == "yes" ]] && ([[ "$run" == "yes" ]] && echo "skip" || echo "yes") || echo "no") # enabled by default, skips if "run" is passed

common=$([[ "$*" == *"common"* || "$empty" == "yes" || "$run" == "yes" ]] && echo "yes" || echo "no") # enabled by default or when run is passed

compat=$([[ "$*" == *"compat"* ]] && echo "yes" || echo "no")

deps=$([[ "$*" == *"deps"* ]] && echo "yes" || echo "no") # it cleans dependencies when building the plugin

ngrok=$([[ "$*" == *"ngrok"* ]] && echo "yes" || echo "no") # update jira with ngrok url

help=$([[ "$*" == *"help"* ]] && echo "yes" || echo "no")

# display plan
[[ "$help" != "yes" ]] && echo "==== Build ==> clean: $clean, purge: $purge, common: $common, refresh dependencies: $deps, pack: $pack, run jira: $run, ngrok: $ngrok , compat: $compat ===="

export PLUGIN="jira-slack-server-integration/jira-slack-server-integration-plugin"

# do tasks
(
    cd "$( dirname "${BASH_SOURCE[0]}")" ;
    [[ "$help" == "yes" ]] && echo "${HELP_TEXT}" ;
    ([[ "$clean" != "yes" ]] || mvn clean) && \
    ([[ "$purge" != "yes" ]] || (rm -rf ${PLUGIN}/target && mvn clean)) && \
    ([[ "$common" != "yes" ]] || ./bin/pack-common.sh) && \
    ([[ "$compat" != "yes" ]] || ./bin/pack-compat-jira.sh) && \
    ([[ "$deps" != "yes" ]] || (rm -f ${PLUGIN}/target/dependency-maven-plugin-markers/*.marker && rm -rf ${PLUGIN}/target/classes)) && \
    ([[ "$pack" != "yes" ]] || ./bin/pack-plugin.sh) && \
    ([[ "$run" != "yes" ]] || ./bin/run-jira.sh) && \
    ([[ "$ngrok" != "yes" ]] || bin/set-jira-ngrok-baseurl.sh)
)
