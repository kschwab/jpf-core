FROM --platform=linux/amd64 openjdk:8 AS build-image

# Install packages
RUN apt-get update \
    && apt-get install graphviz -y \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Install gradle
RUN git clone https://github.com/kschwab/jpf-core.git /tmp/jpf-core \
    && GRADLE_USER_HOME=/opt/.gradle /tmp/jpf-core/gradlew --version \
    && chmod -R a+rwx /opt/.gradle

# Install entrypoint.py and pysu.py scripts
COPY entrypoint.py pysu.py /usr/local/bin/

FROM openjdk:8 AS runtime-image
LABEL org.opencontainers.image.source=https://github.com/kschwab/jpf-core
LABEL org.opencontainers.image.description="Kyle Schwab PhD Computing Artifact"
COPY --from=build-image / /

RUN rm -rf /tmp/*
ENV GRADLE_USER_HOME=/opt/.gradle

ENTRYPOINT ["/usr/local/bin/entrypoint.py"]
