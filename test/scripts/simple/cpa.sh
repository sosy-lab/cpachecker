#!/bin/bash

# where the eclipse project directory is, relative to the location of this
# script
RELATIVE_PATH_TO_CPACHECKER=../../..

# the location of the java command
JAVA=java

# installation of eclipse
ECLIPSE_SEARCH_PATH="$HOME/eclipse /opt/eclipse $HOME/Desktop/eclipse"

#------------------------------------------------------------------------------
# From here on you should not need to change anything
#------------------------------------------------------------------------------

cd `dirname $0`
scriptdir="`pwd`"
cd -

if [ ! -e $scriptdir/$RELATIVE_PATH_TO_CPACHECKER/bin/cmdline/CPAMain.class ] ; then
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
    $arch_platform="x86-linux"
    ;;
  x86_64-Linux)
    $arch_platform="x86_64-linux"
    ;;
  "Power Macintosh-Darwin")
    $arch_platform="ppc-macosx"
    ;;
esac
if [ "$arch_platform" = "unknown" ] ; then
  echo "Failed to determine system type" 1>&2
  exit 1
fi
arch_platform_path="$scriptdir/$RELATIVE_PATH_TO_CPACHECKER/lib/native/$arch_platform/"

# the core jar files for eclipse
CLASSPATH="`ls --color=no $eclipse_plugins/org.eclipse.core*jar $eclipse_plugins/org.eclipse.equinox*jar $eclipse_plugins/org.eclipse.osgi*jar | tr "[:space:]" ":"`"
# the jar files of the CDT plugin
CLASSPATH="$CLASSPATH:`ls --color=no $eclipse_plugins/org.eclipse.cdt*jar | tr "[:space:]" ":"`"

# project files
CLASSPATH="$CLASSPATH:$scriptdir/$RELATIVE_PATH_TO_CPACHECKER/bin"

# external jars shipped with the project
CLASSPATH="$CLASSPATH:`ls --color=no $scriptdir/$RELATIVE_PATH_TO_CPACHECKER/lib/*.jar | tr "[:space:]" ":"`"

export CLASSPATH

# where to find the native libraries and binaries
export LD_LIBRARY_PATH="$arch_platform_path"
export PATH="$PATH:$arch_platform_path"

$JAVA -Xmx1200m -ea cmdline.CPAMain $@

