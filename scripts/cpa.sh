#!/bin/bash

# where the eclipse project directory is, relative to the location of this
# script
[ -n "$PATH_TO_CPACHECKER" ] || PATH_TO_CPACHECKER="`dirname $0`/.."

# the location of the java command
JAVA=java

#------------------------------------------------------------------------------
# From here on you should not need to change anything
#------------------------------------------------------------------------------

if [ ! -e $PATH_TO_CPACHECKER/bin/org/sosy_lab/cpachecker/cmdline/CPAMain.class ] ; then
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
  "Power Macintosh-Darwin")
    arch_platform="ppc-macosx"
    ;;
esac
if [ "$arch_platform" = "unknown" ] ; then
  echo "Failed to determine system type" 1>&2
  exit 1
fi
arch_platform_path="$PATH_TO_CPACHECKER/lib/native/$arch_platform/"
# in regression tests we might run old revision in a different directory without
# the arch_platform stuff
if [ "$PATH_TO_CPACHECKER" != "`dirname $0`/../../.." ] ; then
  arch_platform_path="$arch_platform_path:`dirname $0`/../../../lib/native/$arch_platform"
fi

# project files
CLASSPATH="$CLASSPATH:$PATH_TO_CPACHECKER/bin"

# external jars shipped with the project
CLASSPATH="$CLASSPATH:`find $PATH_TO_CPACHECKER/lib -name '*.jar' | tr "[:space:]" ":"`"

export CLASSPATH

# where to find the native binaries
export PATH="$PATH:$arch_platform_path"

if [ ! -z "$JAVA_VM_ARGUMENTS" ]; then
  echo "Running CPAchecker with the following extra VM options: $JAVA_VM_ARGUMENTS"
fi
$JAVA $JAVA_VM_ARGUMENTS -Djava.library.path=$arch_platform_path -Xmx1200m -ea org.sosy_lab.cpachecker.cmdline.CPAMain "$@"
