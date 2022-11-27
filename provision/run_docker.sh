#!/bin/sh

# Project root directory
pushd $(dirname $0)/.. > /dev/null

# Run the docker container
docker run -it --rm -v $PWD:/app -w /app \
       --detach-keys=ctrl-q,ctrl-q \
       -e ENTRYPOINT_UID=${SUDO_UID:-$(id -u)} \
       -e ENTRYPOINT_GID=${SUDO_GID:-$(id -g)} \
       -e ENTRYPOINT_NAME=kschwab-phd-computing-artifact \
       ghcr.io/kschwab/jpf-core/phd-computing-artifact:0.1 "$@"

popd > /dev/null
