#!/usr/bin/env bash

# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2007-2024 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

# Build mpfr-java and its dependencies in the Ubuntu20.04 container
# Usage: ./buildForUbuntu2004.sh PATH_TO_THE_WORKSPACE GMP_VERSION MPFR_VERSION MPFRJAVA_VERSION

# Check if CPAchecker is in the workspace
PATH_WORKSPACE=$1
if [ ! -d "${PATH_WORKSPACE}/cpachecker" ]; then
    echo -e "Could not find CPAchecker.\nThe workspace from the first argument needs to contain the CPAchecker directory"
    exit 1
fi

# Check if GMP is in the workspace
GMP="gmp-$2"
if [ ! -d "${PATH_WORKSPACE}/${GMP}" ]; then
    echo -e "Could not find GMP.\nThe workspace from the first argument needs to contain a directory named \"gmp-VERSION\" where VERSION is given by the second argument"
    exit 1
fi

# Check if MPFR is in the workspace
MPFR="mpfr-$3"
if [ ! -d "${PATH_WORKSPACE}/${MPFR}" ]; then
    echo -e "Could not find MPFR.\nThe workspace from the first argument needs to contain a directory named \"mpfr-VERSION\" where VERSION is given by the third argument"
    exit 1
fi

# Check if MPFR is in the workspace
MPFRJAVA="mpfrjava-$4"
if [ ! -d "${PATH_WORKSPACE}/${MPFRJAVA}" ]; then
    echo -e "Could not find mpfr-java.\nThe workspace from the first argument needs to contain a directory named \"mpfrjava-VERSION\" where VERSION is given by the fourth argument"
    exit 1
fi

# Build the container
podman build -t mpfrjava-focal - < ubuntu2004.Dockerfile

# Run the compile script in it
podman run \
       --mount type=bind,source=${PATH_WORKSPACE},target=/workspace \
       --workdir /workspace \
       mpfrjava-focal:latest \
       /workspace/${GMP} \
       /workspace/${MPFR} \
       /workspace/${MPFRJAVA}
