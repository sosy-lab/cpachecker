#!/bin/bash

# where the eclipse project directory is, relative to the location of this
# script
[ -n "$PATH_TO_CPACHECKER" ] || PATH_TO_CPACHECKER="`dirname $0`/.."

# the location of the java command
JAVA=java

# installation of eclipse
ECLIPSE_SEARCH_PATH="$ECLIPSE_HOME $HOME/eclipse /opt/eclipse $HOME/Desktop/eclipse $HOME/progs/eclipse"

#------------------------------------------------------------------------------
# From here on you should not need to change anything
#------------------------------------------------------------------------------

if [ ! -e $PATH_TO_CPACHECKER/bin/cmdline/CPAMain.class ] ; then
  echo "bin/cmdline/CPAMain.class not found, please check path to project directory" 1>&2
  exit 1
fi

java_version="`$JAVA -version 2>&1 | grep "^java version" | cut -f2 -d\\\" | sed 's/\.//g' | cut -b1-2`"
if [ -z "$java_version" -o "$java_version" -lt 16 ] ; then
  echo "$JAVA not found or version less than 1.6" 1>&2
  exit 1
fi

eclipse_plugins=""
for d in $ECLIPSE_SEARCH_PATH ; do
  if [ -d $d/plugins ] ; then
    eclipse_plugins="$d/plugins/"
    break
  fi
done
if [ -z "$eclipse_plugins" ] ; then
  echo "Eclipse plugin directory not found" 1>&2
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

# the core jar files for eclipse
CLASSPATH="`ls --color=no $eclipse_plugins/org.eclipse.core*jar $eclipse_plugins/org.eclipse.equinox*jar $eclipse_plugins/org.eclipse.osgi*jar | tr "[:space:]" ":"`"
# the jar files of the CDT plugin
CLASSPATH="$CLASSPATH:`ls --color=no $eclipse_plugins/org.eclipse.cdt*jar | tr "[:space:]" ":"`"

# project files
CLASSPATH="$CLASSPATH:$PATH_TO_CPACHECKER/bin"

# external jars shipped with the project
CLASSPATH="$CLASSPATH:`ls --color=no $PATH_TO_CPACHECKER/lib/*.jar | tr "[:space:]" ":"`"

export CLASSPATH

# where to find the native binaries
export PATH="$PATH:$arch_platform_path"

$JAVA -Djava.library.path=$arch_platform_path -Xmx1200m -ea cmdline.CPAMain $@

