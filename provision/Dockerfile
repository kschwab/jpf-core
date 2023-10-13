FROM --platform=linux/amd64 openjdk:8

# Install packages
RUN curl -LO https://github.com/kaitai-io/kaitai_struct_compiler/releases/download/0.10/kaitai-struct-compiler_0.10_all.deb \
    && apt-get update && apt-get install -y \
        graphviz \
        ./kaitai-struct-compiler_0.10_all.deb \
        ruby \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* ./kaitai-struct-compiler_0.10_all.deb \
    && gem install kaitai-struct-visualizer

# Install Kaitai runtime library for Java
RUN git clone https://github.com/kaitai-io/kaitai_struct_java_runtime.git /opt/kaitai \
    && cd /opt/kaitai/src/main/java \
    && find . -name *.java -exec javac {} \; \
    && jar cvf kaitai.jar $(find . -name *.class)

ENV CLASSPATH=".:/opt/kaitai/src/main/java/kaitai.jar"
LABEL org.opencontainers.image.source=https://github.com/kschwab/jpf-core
LABEL org.opencontainers.image.description="Kyle Schwab PhD Computing Artifact"
