FROM --platform=linux/amd64 openjdk:11

# Install packages
RUN apt-get update && apt-get install -y \
        gdb \
        graphviz \
        libcanberra-gtk-module \
        libcanberra-gtk3-module \
        libswt-gtk-4-java \
        libswt-gtk-4-jni \
        libwebkit2gtk-4.0-37 \
        maven \
        packagekit-gtk3-module \
        sudo \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Install MAT
RUN cd /opt \
    && wget https://download.eclipse.org/mat/1.14.0/rcp/MemoryAnalyzer-1.14.0.20230315-linux.gtk.x86_64.zip \
    && unzip MemoryAnalyzer-1.14.0.20230315-linux.gtk.x86_64.zip \
    && rm MemoryAnalyzer-1.14.0.20230315-linux.gtk.x86_64.zip
