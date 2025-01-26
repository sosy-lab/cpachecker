#!/bin/sh

# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

# See README.md for instruction on how to run this file

# Compile the library with gcc
# The -I options are needed to set the include path for the JNI header.
# We expect the user to set $JAVA_HOME to the right location before running this script.
gcc -std='c11' \
  -shared -fpic \
  -O3 \
  -o 'libFloatingPoints.so' \
 	-I"$JAVA_HOME/include/" \
	-I"$JAVA_HOME/include/linux/" \
	-I"$JAVA_HOME/include/darwin/" \
	floatingPoints.c

chmod 755 libFloatingPoints.so
