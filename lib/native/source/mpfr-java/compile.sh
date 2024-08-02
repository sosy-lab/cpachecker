#!/usr/bin/env bash

# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2007-2024 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

# Build mpfr-java with its dependencies GMP and MPFR and copy the libraries to the CPAchecker /lib directory
# Usage: ./compile.sh PATH_TO_GMP PATH_TO_MPFR PATH_TO_MPFRJAVA

# Get the directory of the script
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

# Get the path to GMP
PATH_GMP=$1
if [ ! -f "${PATH_GMP}/gmpxx.h" ]; then
    echo -e "Could not find GMP.\nYou need to specify the path to GMP as the first argument on the command line"
    exit 1
fi

# Get the path to MPFR
PATH_MPFR=$2
if [ ! -f "${PATH_MPFR}/src/mpfr.h" ]; then
    echo -e "Could not find MPFR.\nYou need to specify the path to MPFR as the second argument on the command line"
    exit 1
fi

# Get the path to mpfr-java
PATH_MPFRJAVA=$3
if [ ! -f "${PATH_MPFRJAVA}/src/main/native-package/src/mpfr_java.h" ]; then
    echo -e "Could not find mpfr-java.\nYou need to specify the path to mpfr-java as the third argument on the command line"
    exit 1
fi

# Get the installation path for the libraries
PATH_INSTALL=$( cd $SCRIPT_DIR && cd ../../x86_64-linux && pwd )
PATH_INSTALL_JAVA=$( cd $PATH_INSTALL && cd ../.. && pwd )

echo "Compiling GMP..."
cd $PATH_GMP \
    && ./configure --with-pic --prefix=$PATH_GMP/install \
    && make \
    && make install \
    && cp install/lib/libgmp.so.10.5.0 $PATH_INSTALL/libgmp.so

echo -e "\n\n"

echo "Compiling MPFR..."
cd $PATH_MPFR \
    && ./configure --with-pic --with-gmp=$PATH_GMP/install --prefix=$PATH_MPFR/install \
    && make \
    && make install \
    && cp install/lib/libmpfr.so.6.2.1 $PATH_INSTALL/libmpfr.so

echo -e "\n\n"

echo "Building mpfr-java..."
cd $PATH_MPFRJAVA \
   && git apply $SCRIPT_DIR/mpfr-java.patch \
   && mvn install -Dmpfr.cppflags="-I${PATH_GMP}/install/include -I${PATH_MPFR}/install/include" -Dmpfr.ldflags="-L${PATH_GMP}/install/lib -L${PATH_MPFR}/install/lib" -DskipTests \
   && cp target/native-build/.libs/libmpfr_java-1.4.so $PATH_INSTALL/libmpfr_java.so \
   && cp target/mpfr_java-1.4.jar $PATH_INSTALL_JAVA/mpfr_java.jar

echo -e "\n\n"

echo "Postprocessing..."
cd $PATH_INSTALL \
   && strip libgmp.so libmpfr.so libmpfr_java.so \
   && patchelf --set-rpath '$ORIGIN' --replace-needed libgmp.so.10 libgmp.so libmpfr.so \
   && patchelf --set-rpath '$ORIGIN' --replace-needed libmpfr.so.6 libmpfr.so --replace-needed libgmp.so.10 libgmp.so libmpfr_java.so

echo "Done!"
