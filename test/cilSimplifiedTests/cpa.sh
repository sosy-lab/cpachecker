#!/bin/sh
#
# script to run CPAChecker from the command line. This assumes to be placed in
# a bin/ directory, out of the eclipse workspace.
# If not the case, change the variable RELATIVE_PATH_TO_WORKSPACE

# where the eclipse workspace is, relative to the location of this script
RELATIVE_PATH_TO_WORKSPACE=../../..

# the location of the jvm
JAVA=/localhome/erkan/jdk1.6.0_07/bin/java

# installation dir for eclipse
ECLIPSE_DIR=/localhome/erkan/eclipse

# installation dir for CDT
CDT_PLUGIN_DIR=/localhome/erkan/eclipse/plugins

#------------------------------------------------------------------------------
# From here on you should not need to change anything
#------------------------------------------------------------------------------

# the location of this script
MYDIR=`dirname $0`

# the core jar files for eclipse
ECLIPSE_CORE_JARS=`ls --color=no $ECLIPSE_DIR/plugins/org.eclipse.core*jar $ECLIPSE_DIR/plugins/org.eclipse.equinox*jar $ECLIPSE_DIR/plugins/org.eclipse.osgi*jar | tr "[:space:]" ":"`

# the jar files of the CDT plugin
CDT_PLUGIN_JARS=`ls --color=no $CDT_PLUGIN_DIR/org.eclipse.cdt*jar | tr "[:space:]" ":"`

PROJECT_CLASSPATH=$MYDIR/$RELATIVE_PATH_TO_WORKSPACE/CPAchecker/bin:$MYDIR/$RELATIVE_PATH_TO_WORKSPACE/CPAchecker/lib/mathsat.jar:$MYDIR/$RELATIVE_PATH_TO_WORKSPACE/CPAchecker/lib/minijdd_103.jar:$MYDIR/$RELATIVE_PATH_TO_WORKSPACE/CPAchecker/lib/javabdd-1.0b2.jar

# the classpath is just the concatenation of the above
export CLASSPATH=$ECLIPSE_CORE_JARS:$CDT_PLUGIN_JARS:$PROJECT_CLASSPATH

# where to find the native library for mathsat
export LD_LIBRARY_PATH=$MYDIR/$RELATIVE_PATH_TO_WORKSPACE/CPAchecker/nativeLibs

$JAVA -Xmx2000m -ea cmdline.CPAMain $@
