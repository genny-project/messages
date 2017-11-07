#!/bin/bash

if [ -z "${1}" ]; then
   version="latest"
else
   version="${1}"
fi


docker push gennyproject/messages:"${version}"
docker tag  gennyproject/messages:"${version}"  gennyproject/messages:latest
docker push gennyproject/messages:latest

