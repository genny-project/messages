#!/bin/bash

export GENNY_SHOW_VALUES=TRUE
export GENNY_KEYCLOAK_URL=https://keycloak.gada.io
export GENNY_API_URL=http://internmatch.genny.life:8280
export GENNY_REALM=internmatch

./mvnw  quarkus:dev -Ddebug=5558

