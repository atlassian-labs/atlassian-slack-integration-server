#!/usr/bin/env bash

HELP_TEXT="This is a shortcut script to run *everything* you need related to the Bitbucket plugin.

Arguments:
  - run       compile the common module, the Bitbucket plugin, and start up Bitbucket for development
  - pack      compile the Bitbucket plugin ('pack' is skipped if 'run' is passed)
  - common    compile common module
  - int       run integration tests against a running Bitbucket instance
  - clean     clear compiled files, except Bitbucket home directory ('clean' is skipped if 'purge' is passed)
  - purge     clear all compiled files, including Bitbucket home directory with database and everything
  - deps      clean the plugin's embedded dependencies, forcing them to be rebuild in the next compilation
              (use when strange errors about missing classes occur - IntelliJ removes some files when rebuilding)

You pass any combination of arguments in any order. Calling the script without arguments defaults to: 'common pack'

Examples:
  ./bitbucket.sh run          -> compiles the Bitbucket plugin and starts up Bitbucket for development

  ./bitbucket.sh              -> compiles the common module and the Bitbucket plugin; good for quick reloading
  ./bitbucket.sh common pack  -> same as above

  ./bitbucket.sh clean run    -> cleans all compiled files but the Bitbucket home directory, compiles everything, and runs Bitbucket in development mode
"

# check java 8
javaVersion=`java -version 2>&1 | head -n 1 | cut -d\" -f 2`
javaCompilerVersion=`javac -version 2>&1 | head -n 1 | cut -d\" -f 2`
[[ "$javaVersion" != "1.8."* && "$javaVersion" != "11."* || "$javaCompilerVersion" != *"1.8."* && "$javaCompilerVersion" != *"11."* ]] && echo "Java 8 expected" && exit 1

# compute parameters
purge=$([[ "$*" == *"purge"* ]] && echo "yes" || echo "no")
clean=$([[ "$*" == *"clean"* ]] && ([[ "$purge" == "yes" ]] && echo "skip" || echo "yes") || echo "no")

empty=$([[ "$*" == "" ]] && echo "yes" || echo "no")

run=$([[ "$*" == *"run"* ]] && echo "yes" || echo "no")
pack=$([[ "$*" == *"pack"* || "$empty" == "yes" ]] && ([[ "$run" == "yes" ]] && echo "skip" || echo "yes") || echo "no") # enabled by default, skips if "run" is passed

common=$([[ "$*" == *"common"* || "$empty" == "yes" || "$run" == "yes" ]] && echo "yes" || echo "no") # enabled by default or when run is passed

deps=$([[ "$*" == *"deps"* ]] && echo "yes" || echo "no") # it cleans dependencies when building the plugin

int=$([[ "$*" == *"int"* ]] && echo "yes" || echo "no")

help=$([[ "$*" == *"help"* ]] && echo "yes" || echo "no")

# display plan
[[ "$help" != "yes" ]] && echo "==== Build ==> clean: $clean, purge: $purge, common: $common, refresh dependencies: $deps, bitbucket: $pack, run bitbucket: $run, integration tests: $int ===="

export PLUGIN="bitbucket-slack-server-integration-plugin"

# do tasks
(
    cd "$( dirname "${BASH_SOURCE[0]}")" ;
    [[ "$help" == "yes" ]] && echo "${HELP_TEXT}" ;
    ([[ "$clean" != "yes" ]] || mvn clean) && \
    ([[ "$purge" != "yes" ]] || (rm -rf ${PLUGIN}/target && mvn clean)) && \
    ([[ "$common" != "yes" ]] || ./bin/pack-common.sh) && \
    ([[ "$deps" != "yes" ]] || (rm -f ${PLUGIN}/target/dependency-maven-plugin-markers/*.marker && rm -rf ${PLUGIN}/target/classes)) && \
    ([[ "$pack" != "yes" ]] || ./bin/pack-plugin.sh) && \
    ([[ "$int" != "yes" ]] || XVFB_ENABLE=false ./bin/run-bitbucket-its.sh) && \
    ([[ "$run" != "yes" ]] || ./bin/run-bitbucket.sh)
)
