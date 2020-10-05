#!/usr/bin/env bash

declare -rx FRAMEWORK_VERSION=7.0.10
declare -rx EVENT_STORE_VERSION=7.0.8

. functions.sh

CONTEXT_NAME=results

runLiquibase
