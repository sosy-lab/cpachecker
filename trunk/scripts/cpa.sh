#!/bin/bash

# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

# the location of the java command
[ -z "$JAVA" ] && JAVA=java

# the default heap and stack sizes of the Java VM
DEFAULT_HEAP_SIZE="1200M"
DEFAULT_STACK_SIZE="1024k"

#------------------------------------------------------------------------------
# From here on you should not need to change anything
#------------------------------------------------------------------------------

java_version="`$JAVA -XX:-UsePerfData -Xmx5m -version 2>&1`"
result=$?
if [ $result -eq 127 ]; then
  echo "Java not found, please install Java 11 or newer." 1>&2
  echo "For Ubuntu: sudo apt-get install openjdk-11-jre" 1>&2
  echo "If you have installed Java 11, but it is not in your PATH," 1>&2
  echo "let the environment variable JAVA point to the \"java\" binary." 1>&2
  exit 1
fi
if [ $result -ne 0 ]; then
  echo "Failed to execute Java VM, return code was $result and output was"
  echo "$java_version"
  echo "Please make sure you are able to execute Java processes by running \"$JAVA\"."
  exit 1
fi
java_version="`echo "$java_version" | grep -e "^\(java\|openjdk\) version" | cut -f2 -d\\\" | cut -f1 -d.`"
if [ -z "$java_version" ] || [ "$java_version" -lt 11 ] ; then
  echo "Your Java version is too old, please install Java 11 or newer." 1>&2
  echo "For Ubuntu: sudo apt-get install openjdk-11-jre" 1>&2
  echo "If you have installed Java 11, but it is not in your PATH," 1>&2
  echo "let the environment variable JAVA point to the \"java\" binary." 1>&2
  exit 1
fi

platform="`uname -s`"

# where the project directory is, relative to the location of this script
case "$platform" in
  Linux|CYGWIN*)
    SCRIPT="$(readlink -f "$0")"
    [ -n "$PATH_TO_CPACHECKER" ] || PATH_TO_CPACHECKER="$(readlink -f "$(dirname "$SCRIPT")/..")"
    ;;
  # other platforms like Mac don't support readlink -f
  *)
    [ -n "$PATH_TO_CPACHECKER" ] || PATH_TO_CPACHECKER="$(dirname "$0")/.."
    ;;
esac

if [ ! -e "$PATH_TO_CPACHECKER/bin/org/sosy_lab/cpachecker/cmdline/CPAMain.class" ] ; then
  if [ ! -e "$PATH_TO_CPACHECKER/cpachecker.jar" ] ; then
    echo "Could not find CPAchecker binary, please check path to project directory" 1>&2
    exit 1
  fi
fi

export CLASSPATH="$CLASSPATH:$PATH_TO_CPACHECKER/bin:$PATH_TO_CPACHECKER/cpachecker.jar:$PATH_TO_CPACHECKER/lib/*:$PATH_TO_CPACHECKER/lib/java/runtime/*"

# loop over all input parameters and parse them
declare -a OPTIONS
JAVA_ASSERTIONS=-ea
while [ $# -gt 0 ]; do

  case $1 in
   "-benchmark")
       JAVA_ASSERTIONS=-da
       unset DEFAULT_HEAP_SIZE  # no default heap size in benchmark mode
       OPTIONS+=("$1")          # pass param to CPAchecker, too
       ;;
   "-heap")
       shift
       JAVA_HEAP_SIZE=$1
       ;;
   "-stack")
       shift
       JAVA_STACK_SIZE=$1
       ;;
   "-debug")
       JAVA_VM_ARGUMENTS="$JAVA_VM_ARGUMENTS -Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=5005,suspend=n"
       ;;
   "-disable-java-assertions")
       JAVA_ASSERTIONS=-da
       ;;
   "-generateReport")
       echo "Option -generateReport is not necessary anymore. Please open the HTML files produced by CPAchecker in the output directory."
       ;;
   -X*) # params starting with "-X" are used for JVM
       JAVA_VM_ARGUMENTS="$JAVA_VM_ARGUMENTS $1"
       ;;
   *) # other params are only for CPAchecker
       OPTIONS+=("$1")
       ;;
  esac

  shift
done

if [ -n "$TMPDIR" ]; then
  JAVA_VM_ARGUMENTS="$JAVA_VM_ARGUMENTS -Djava.io.tmpdir=$TMPDIR"
elif [ -n "$TEMP" ]; then
  JAVA_VM_ARGUMENTS="$JAVA_VM_ARGUMENTS -Djava.io.tmpdir=$TEMP"
elif [ -n "$TMP" ]; then
  JAVA_VM_ARGUMENTS="$JAVA_VM_ARGUMENTS -Djava.io.tmpdir=$TMP"
fi

if [ -n "$JAVA_HEAP_SIZE" ]; then
  echo "Running CPAchecker with Java heap of size ${JAVA_HEAP_SIZE}."
else
  JAVA_HEAP_SIZE="$DEFAULT_HEAP_SIZE"
  if [ -z "$JAVA_HEAP_SIZE" ]; then
    echo "A heap size needs to be specified with -heap if -benchmark is given." 1>&2
    echo "Please see doc/Benchmark.md for further information." 1>&2
    exit 1
  fi
  echo "Running CPAchecker with default heap size (${JAVA_HEAP_SIZE}). Specify a larger value with -heap if you have more RAM."
fi

if [ -n "$JAVA_STACK_SIZE" ]; then
  echo "Running CPAchecker with Java stack of size ${JAVA_STACK_SIZE}."
else
  JAVA_STACK_SIZE="$DEFAULT_STACK_SIZE"
  echo "Running CPAchecker with default stack size (${JAVA_STACK_SIZE}). Specify a larger value with -stack if needed."
fi

if [ ! -z "$JAVA_VM_ARGUMENTS" ]; then
  echo "Running CPAchecker with the following extra VM options: $JAVA_VM_ARGUMENTS"
fi

if [ ! -z "$CPACHECKER_ARGUMENTS" ]; then
  echo "Running CPAchecker with the following extra arguments: $CPACHECKER_ARGUMENTS"
fi

case "$platform" in
  CYGWIN*)
    JAVA_VM_ARGUMENTS="$JAVA_VM_ARGUMENTS -classpath `cygpath -wp $CLASSPATH`"
    ;;
esac

# Run CPAchecker.
# Order of arguments for JVM:
# - options hard-coded in this script (to allow overriding them)
# - options specified in environment variable
# - options specified on command-line via "-X..." (except stack/head/tmpdir)
# - options specified on command-line to this script via direct token (stack/heap) and tmpdir
# - CPAchecker class and options
# Stack size is set because on some systems it is too small for recursive algorithms and very large programs.
# PerfDisableSharedMem avoids hsperfdata in /tmp (disable it to connect easily with VisualConsole and Co.).
exec "$JAVA" \
    -XX:+PerfDisableSharedMem \
    -Djava.awt.headless=true \
    $JAVA_VM_ARGUMENTS \
    -Xss${JAVA_STACK_SIZE} \
    -Xmx${JAVA_HEAP_SIZE} \
    $JAVA_ASSERTIONS \
    org.sosy_lab.cpachecker.cmdline.CPAMain \
    "${OPTIONS[@]}" \
    $CPACHECKER_ARGUMENTS
