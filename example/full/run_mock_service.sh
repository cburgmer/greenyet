#!/bin/bash
set -e

SCRIPT_DIR=$(cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd)

cd "${SCRIPT_DIR}/.."
if [ ! -d node_modules ]; then
    mkdir -p node_modules # Don't add to ancestor node_modules dir if exists
    npm install express
fi
node mock_server.js "$SCRIPT_DIR/status_responses"
