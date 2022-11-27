#!/bin/sh

# Project provision directory
pushd $(dirname $0) > /dev/null

# Run the docker build command
docker build -t ghcr.io/kschwab/jpf-core/phd-computing-artifact:0.1 .

popd > /dev/null
