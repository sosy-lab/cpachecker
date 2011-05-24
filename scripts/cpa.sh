#!/bin/bash

# where the eclipse project directory is, relative to the location of this
# script
SCRIPT="$(readlink -f "$0")"
[ -n "$PATH_TO_CPACHECKER" ] || PATH_TO_CPACHECKER="$(readlink -f "$(dirname "$SCRIPT")/..")"

# the location of the java command
JAVA=java

# the default heap size of the javaVM
DEFAULT_HEAP_SIZE="1200m"

#------------------------------------------------------------------------------
# From here on you should not need to change anything
#------------------------------------------------------------------------------

if [ ! -e "$PATH_TO_CPACHECKER/bin/org/sosy_lab/cpachecker/cmdline/CPAMain.class" ] ; then
  echo "bin/org/sosy_lab/cpachecker/cmdline/CPAMain.class not found, please check path to project directory" 1>&2
  exit 1
fi

java_version="`$JAVA -version 2>&1 | grep "^java version" | cut -f2 -d\\\" | sed 's/\.//g' | cut -b1-2`"
if [ -z "$java_version" -o "$java_version" -lt 16 ] ; then
  echo "$JAVA not found or version less than 1.6" 1>&2
  exit 1
fi

arch_platform="unknown"
arch="`uname -m`"
platform="`uname -s`"
case "$arch-$platform" in
  i686-Linux)
    arch_platform="x86-linux"
    ;;
  x86_64-Linux)
    arch_platform="x86_64-linux"
    ;;
  i386-Darwin)
    arch_platform="x86-macosx"
    ;;
  "Power Macintosh-Darwin")
    arch_platform="ppc-macosx"
    ;;
esac
if [ "$arch_platform" = "unknown" ] ; then
  echo "Failed to determine system type" 1>&2
  exit 1
fi
arch_platform_path="$PATH_TO_CPACHECKER/lib/native/$arch_platform/"

# project files
CLASSPATH="$CLASSPATH:$PATH_TO_CPACHECKER/bin"

# external jars shipped with the project
CLASSPATH="$CLASSPATH$(find "$PATH_TO_CPACHECKER/lib" -maxdepth 1 -name '*.jar' -printf ':%p' )"
CLASSPATH="$CLASSPATH$(find "$PATH_TO_CPACHECKER/lib/eclipse" -maxdepth 1 -name '*.jar' -printf ':%p' )"

export CLASSPATH

# where to find the native binaries
export PATH="$PATH:$arch_platform_path"

# loop over all input parameters and parse them
declare -a OPTIONS
while [ $# -gt 0 ]; do 

  case $1 in
   "-heap")
       shift
       JAVA_HEAP_SIZE=$1
       ;;
   "-debug")  
       JAVA_VM_ARGUMENTS="$JAVA_VM_ARGUMENTS -Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=5005,suspend=n"
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

# run CPAchecker
"$JAVA" $JAVA_VM_ARGUMENTS "-Djava.library.path=$arch_platform_path" -Xmx${JAVA_HEAP_SIZE:-$DEFAULT_HEAP_SIZE} -ea org.sosy_lab.cpachecker.cmdline.CPAMain "${OPTIONS[@]}"
