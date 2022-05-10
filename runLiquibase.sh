#!/usr/bin/env bash

declare -rx FRAMEWORK_VERSION=8.0.4
declare -rx EVENT_STORE_VERSION=8.2.0

. functions.sh

CONTEXT_NAME=results

runLiquibase
