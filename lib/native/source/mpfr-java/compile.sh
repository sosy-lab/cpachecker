#!/usr/bin/env bash

# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2007-2024 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

# Download and build mpfr-java with its dependencies GMP and MPFR and copy the libraries to the CPAchecker /lib directory
# Usage: ./compile.sh

# Versions for GMP, MPFR and mpfr-java that will be used for the build
VERSION_GMP="6.3.0"
VERSION_MPFR="4.2.1"
VERSION_MPFRJAVA="b7f2e4a61f45cab28f5792dc834c5f20802a11cc"

# Versions for the .so files from GMP, MPFR and mpfr-java
VERSION_LIBGMP="10.5.0"
VERSION_LIBMPFR="6.2.1"
VERSION_LIBMPFRJAVA="1.4"

MAJOR_VERSION_LIBGMP="${VERSION_LIBGMP%%.*}"
MAJOR_VERSION_LIBMPFR="${VERSION_LIBMPFR%%.*}"

# Fail if there is an error
set -euo pipefail
IFS=$'\n\t'

# Get the directory of the script
SCRIPT_DIR="$(dirname "$0")"

# Create a temporary volume for the build
mkdir /build
cd /build

echo -e "Downloading source..."

# Download GMP source
curl -O https://gmplib.org/download/gmp/gmp-$VERSION_GMP.tar.xz
tar xf gmp-$VERSION_GMP.tar.xz
PATH_GMP="/build/gmp-${VERSION_GMP}"

# Download MPFR source
curl -O https://www.mpfr.org/mpfr-current/mpfr-$VERSION_MPFR.tar.bz2
tar xf mpfr-$VERSION_MPFR.tar.bz2
PATH_MPFR="/build/mpfr-${VERSION_MPFR}"

# Download mpfr-java source
git clone https://github.com/runtimeverification/mpfr-java.git
cd mpfr-java
git checkout $VERSION_MPFRJAVA
PATH_MPFRJAVA="/build/mpfr-java"

# Get the installation path for the libraries
PATH_INSTALL=$( cd $SCRIPT_DIR && cd ../../x86_64-linux && pwd )
PATH_INSTALL_JAVA=$( cd $PATH_INSTALL && cd ../.. && pwd )

echo "Compiling GMP..."
cd $PATH_GMP
./configure --with-pic --prefix=$PATH_GMP/install
make
make install
cp install/lib/libgmp.so.$VERSION_LIBGMP $PATH_INSTALL/libgmp.so

echo -e "\n\n"

echo "Compiling MPFR..."
cd $PATH_MPFR
./configure --with-pic --with-gmp=$PATH_GMP/install --prefix=$PATH_MPFR/install
make
make install
cp install/lib/libmpfr.so.$VERSION_LIBMPFR $PATH_INSTALL/libmpfr.so

echo -e "\n\n"

echo "Building mpfr-java..."
cd $PATH_MPFRJAVA
git apply $SCRIPT_DIR/mpfr-java.patch
mvn install -Dmpfr.cppflags="-I${PATH_GMP}/install/include -I${PATH_MPFR}/install/include" -Dmpfr.ldflags="-L${PATH_GMP}/install/lib -L${PATH_MPFR}/install/lib" -DskipTests
cp target/native-build/.libs/libmpfr_java-$VERSION_LIBMPFRJAVA.so $PATH_INSTALL/libmpfr_java.so
cp target/mpfr_java-$VERSION_LIBMPFRJAVA.jar $PATH_INSTALL_JAVA/mpfr_java.jar

echo -e "\n\n"

echo "Postprocessing..."
cd $PATH_INSTALL
strip libgmp.so libmpfr.so libmpfr_java.so
patchelf --set-rpath '$ORIGIN' --replace-needed libgmp.so.$MAJOR_VERSION_LIBGMP libgmp.so libmpfr.so
patchelf --set-rpath '$ORIGIN' --replace-needed libmpfr.so.$MAJOR_VERSION_LIBMPFR libmpfr.so --replace-needed libgmp.so.10 libgmp.so libmpfr_java.so

echo "Done!"
