FROM --platform=$BUILDPLATFORM eclipse-temurin:11-jdk-noble

# Install system packages + Node/npm
RUN apt-get update && apt-get install -y --no-install-recommends \
        gdb \
        git \
        graphviz \
        libcanberra-gtk-module \
        libcanberra-gtk3-module \
        libswt-gtk-4-java \
        libswt-gtk-4-jni \
        libwebkit2gtk-4.1-0 \
        maven \
        npm \
        packagekit-gtk3-module \
        sudo \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*
