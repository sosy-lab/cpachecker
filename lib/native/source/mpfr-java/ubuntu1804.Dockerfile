# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2007-2024 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

FROM ubuntu:bionic

# set default locale
RUN apt-get update \
 && apt-get install -y \
        locales locales-all
ENV LC_ALL en_US.UTF-8
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US.UTF-8

RUN    apt-get update            \
    && apt-get install --yes     \
        ant                      \
        autogen                  \
        automake1.11             \
        autoconf                 \
        autotools-dev            \
        build-essential          \
        curl                     \
        gcc                      \
        git                      \
        libtool                  \
        shtool                   \
        patchelf                 \
        openjdk-11-jdk           \
        python3                  \
        curl

WORKDIR /dependencies
RUN curl -O https://gmplib.org/download/gmp/gmp-6.3.0.tar.xz \
 && tar xf gmp-6.3.0.tar.xz \
 && cd gmp-6.3.0 \
 && ./configure \
 && make \
 && make install \
 && cd --
RUN curl -O https://www.mpfr.org/mpfr-current/mpfr-4.2.1.tar.bz2 \
 && tar xf mpfr-4.2.1.tar.bz2 \
 && cd mpfr-4.2.1 \
 && ./configure \
 && make \
 && make install \
 && cd --
RUN curl -O https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz \
 && tar xf apache-maven-3.9.6-bin.tar.gz
ENV PATH="/dependencies/apache-maven-3.9.6/bin:${PATH}"

# Add the user "developer" with UID:1000, GID:1000, home at /developer.
# This allows to map the docker-internal user to the local user 1000:1000 outside of the container.
# This avoids to have new files created with root-rights.
RUN groupadd -r developer -g 1000 \
 && useradd -u 1000 -r -g developer -m -d /developer -s /sbin/nologin -c "JavaSMT Development User" developer \
 && chmod 755 /developer

USER developer

# JNI is not found when compiling mpfr-java in the image, so we need to set JAVA_HOME
ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64/
