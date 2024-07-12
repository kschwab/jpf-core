#!/bin/bash

VERSION=0.4
CREATED=$(date --rfc-3339 seconds)
PROVISION_DIR=$(dirname "$0")

# Enter provision directory
if ! pushd "${PROVISION_DIR}" > /dev/null; then
    echo "Error entering provision directory."
    exit 1
fi

# Run the docker build command
docker build -t ghcr.io/kschwab/jpf-core/phd-computing-artifact:${VERSION} . \
    --label org.opencontainers.image.source=https://github.com/kschwab/jpf-core \
    --label org.opencontainers.image.description="Kyle Schwab PhD Computing Artifact" \
    --label "org.opencontainers.image.version=${VERSION}" \
    --label "org.opencontainers.image.created=${CREATED}"

# Exit provision directory
if ! popd > /dev/null; then
    echo "Error exiting provision directory."
    exit 1
fi
