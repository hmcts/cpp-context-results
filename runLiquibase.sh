#!/usr/bin/env bash

declare -rx FRAMEWORK_VERSION=6.4.0
declare -rx EVENT_STORE_VERSION=2.4.5

. functions.sh

CONTEXT_NAME=results

runLiquibase
