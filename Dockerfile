FROM openjdk:11-jdk-slim AS baseimage

WORKDIR /usr/app
ENV PATH="/etc/ant/bin:${PATH}"
ENV ANT_HOME="/etc/ant"
ENV ANT_VERSION="1.10.7"

RUN apt-get update && apt-get install -y \
  graphviz=2.40.1-6 \
  libgomp1=8.3.0-6 \
  python3=3.7.3-1 \
  wget=1.20.1-1.1 \
  git \
  procps

# RUN apt-get update && (apt-get install -y ant || true)
RUN cd /tmp; \
    wget http://mirror.23media.de/apache/ant/binaries/apache-ant-${ANT_VERSION}-bin.tar.gz && \
    tar -C /etc/ -xzvf apache-ant-${ANT_VERSION}-bin.tar.gz apache-ant-${ANT_VERSION} && \
    mv /etc/apache-ant-${ANT_VERSION} /etc/ant && \
    cd ${ANT_HOME} && \
    ant -f fetch.xml -Ddest=system && \
    rm -rf /tmp/apache-ant*

COPY build /usr/app/build
COPY lib /usr/app/lib
COPY build*.xml /usr/app/
RUN ant -lib lib/java/build resolve-dependencies

COPY . /usr/app

# RUN ant -lib lib/java/build build -Dcompile.warn=true -Derrorprone.disable=true