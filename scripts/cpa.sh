#!/bin/bash

# the location of the java command
JAVA=java

# the default heap size of the javaVM
DEFAULT_HEAP_SIZE="1200m"

#------------------------------------------------------------------------------
# From here on you should not need to change anything
#------------------------------------------------------------------------------

java_version="`$JAVA -version 2>&1 | grep "^java version" | cut -f2 -d\\\" | sed 's/\.//g' | cut -b1-2`"
if [ -z "$java_version" -o "$java_version" -lt 16 ] ; then
  echo "$JAVA not found or version less than 1.6" 1>&2
  exit 1
fi

# Find out architecture

arch="`uname -m`"
platform="`uname -s`"
case "$arch-$platform" in
  i686-Linux)
    arch_platform="x86-linux"
    ;;
  x86_64-Linux)
    arch_platform="x86_64-linux"
    ;;
  x86_64-Darwin)
    arch_platform="x86_64-macosx"
    ;;
  i386-Darwin)
    arch_platform="x86-macosx"
    ;;
  *)
   echo "Failed to determine system type" 1>&2
   exit 1
   ;;
esac

# where the project directory is, relative to the location of this script
case "$platform" in
  Linux)
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

# where to find the native binaries
arch_platform_path="$PATH_TO_CPACHECKER/lib/native/$arch_platform/"
export PATH="$PATH:$arch_platform_path"

# loop over all input parameters and parse them
declare -a OPTIONS
JAVA_ASSERTIONS=-ea
while [ $# -gt 0 ]; do

  case $1 in
   "-heap")
       shift
       JAVA_HEAP_SIZE=$1
       ;;
   "-debug")
       JAVA_VM_ARGUMENTS="$JAVA_VM_ARGUMENTS -Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=5005,suspend=n"
       ;;
   "-disable-java-assertions")
       JAVA_ASSERTIONS=-da
       ;;
   *) # other params are only for CPAchecker
       OPTIONS+=("$1")
       ;;
  esac

  shift
done

if [ -n "$JAVA_HEAP_SIZE" ]; then
  echo "Running JavaVM with special heap size: $JAVA_HEAP_SIZE"
fi

if [ ! -z "$JAVA_VM_ARGUMENTS" ]; then
  echo "Running CPAchecker with the following extra VM options: $JAVA_VM_ARGUMENTS"
fi

if [ ! -z "$CPACHECKER_ARGUMENTS" ]; then
  echo "Running CPAchecker with the following extra arguments: $CPACHECKER_ARGUMENTS"
fi

# run CPAchecker
exec "$JAVA" $JAVA_VM_ARGUMENTS "-Djava.library.path=$arch_platform_path" -Xmx${JAVA_HEAP_SIZE:-$DEFAULT_HEAP_SIZE} $JAVA_ASSERTIONS org.sosy_lab.cpachecker.cmdline.CPAMain "${OPTIONS[@]}" $CPACHECKER_ARGUMENTS
