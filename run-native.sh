#!/bin/bash

export GENNY_SHOW_VALUES=TRUE
export GENNY_SERVICE_USERNAME=service
export GENNY_KEYCLOAK_URL=https://keycloak.gada.io
export GENNY_API_URL=http://internmatch.genny.life:8280

./target/messages-9.10.0-runner
