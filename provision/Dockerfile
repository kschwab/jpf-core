FROM --platform=linux/amd64 openjdk:8

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
COPY 0001-IBM-DTFJ-https.patch /opt/
RUN git clone --depth=1 https://git.eclipse.org/r/mat/org.eclipse.mat --branch R_1.11.0 /opt/org.eclipse.mat \
    && cd /opt/org.eclipse.mat \
    && mv /opt/0001-IBM-DTFJ-https.patch . \
    && git apply 0001-IBM-DTFJ-https.patch \
    && cd parent \
    && mvn clean install -P build-release-rcp -Dmat-release-repo-url=https://archive.eclipse.org/mat/1.11.0/update-site -DskipTests
