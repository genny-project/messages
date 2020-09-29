#!/bin/bash
echo "RUNNING DOCKER-ENTRYPOINT"
#set -e;

command="$1";
if [ "$command" != "java" ]; then
   echo "ERROR: command must start with: java";
   exit 1;
fi;

exec "$@"
