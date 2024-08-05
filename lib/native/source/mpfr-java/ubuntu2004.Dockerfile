# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2007-2024 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

FROM ubuntu:focal

# set default locale
RUN apt-get update && apt-get install -y \
        locales \
        locales-all
ENV LC_ALL en_US.UTF-8
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US.UTF-8

ARG DEBIAN_FRONTEND=noninteractive
ENV TZ=Etc/UTC
RUN apt-get update && apt-get install --yes \
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
        openjdk-17-jdk           \
        python3                  \
        curl                     \
        maven

# JNI is not found when compiling mpfr-java in the image, so we need to set JAVA_HOME
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64/

# Call the mpfr-java build script
ENTRYPOINT ["/cpachecker/lib/native/source/mpfr-java/compile.sh"]
