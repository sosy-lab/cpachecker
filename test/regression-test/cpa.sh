#!/bin/sh
#
# script to run CPAChecker from the command line. This assumes to be placed in
# a bin/ directory, out of the eclipse workspace.
# If not the case, change the variable RELATIVE_PATH_TO_WORKSPACE

# where the eclipse workspace is
[ -n "$PATH_TO_WORKSPACE" ] || PATH_TO_WORKSPACE=../../

# the location of the jvm
JAVA=java

# installation dir for eclipse
ECLIPSE_DIR=/localhome/tautschnig/eclipse

# installation dir for CDT
CDT_PLUGIN_DIR=$ECLIPSE_DIR/plugins

#------------------------------------------------------------------------------
# From here on you should not need to change anything
#------------------------------------------------------------------------------

# the core jar files for eclipse
ECLIPSE_CORE_JARS=`ls --color=no $ECLIPSE_DIR/plugins/org.eclipse.core*jar $ECLIPSE_DIR/plugins/org.eclipse.equinox*jar $ECLIPSE_DIR/plugins/org.eclipse.osgi*jar | tr "[:space:]" ":"`

# the jar files of the CDT plugin
CDT_PLUGIN_JARS=`ls --color=no $CDT_PLUGIN_DIR/org.eclipse.cdt*jar | tr "[:space:]" ":"`

PROJECT_CLASSPATH=$PATH_TO_WORKSPACE/bin:$PATH_TO_WORKSPACE/lib/mathsat.jar:$PATH_TO_WORKSPACE/lib/minijdd_103.jar:$PATH_TO_WORKSPACE/lib/javabdd-1.0b2.jar

# the classpath is just the concatenation of the above
export CLASSPATH=$ECLIPSE_CORE_JARS:$CDT_PLUGIN_JARS:$PROJECT_CLASSPATH

# where to find the native library for mathsat
export LD_LIBRARY_PATH=$PATH_TO_WORKSPACE/nativeLibs

nice -n15 $JAVA -Xmx500m -ea cmdline.CPAMain $@
