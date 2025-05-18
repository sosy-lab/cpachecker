#!/bin/sh

# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

if [ "$(uname)" = "Darwin" ]; then
    OUT_FILE="libFloatingPoints.dylib"
else
    OUT_FILE="libFloatingPoints.so"
fi

gcc -std='c11' -shared -fpic -o "$OUT_FILE" \
 	-I"$JAVA_HOME/include/" \
	-I"$JAVA_HOME/include/linux/" \
	-I"$JAVA_HOME/include/darwin/" \
	floatingPoints.c
