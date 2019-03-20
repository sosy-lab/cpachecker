#!/bin/bash
#/home/yinboyu/workspace/codebase/csurf-results/OAI-UE
#/home/yinboyu/workspace/CFAtest/lock
#cd ../

###Compile###
#ant


###Run####

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
  echo "Java not found, please install Java 1.8 or newer." 1>&2
  echo "For Ubuntu: sudo apt-get install openjdk-8-jre" 1>&2
  echo "If you have installed Java 8, but it is not in your PATH," 1>&2
  echo "let the environment variable JAVA point to the \"java\" binary." 1>&2
  exit 1
fi
if [ $result -ne 0 ]; then
  echo "Failed to execute Java VM, return code was $result and output was"
  echo "$java_version"
  echo "Please make sure you are able to execute Java processes by running \"$JAVA\"."
  exit 1
fi
java_version="`echo "$java_version" | grep -e "^\(java\|openjdk\) version" | cut -f2 -d\\\" | sed 's/\.//g' | cut -b1-2`"
if [ -z "$java_version" ] || [ "$java_version" -lt 18 -a "$java_version" -gt 13 ] ; then
  echo "Your Java version is too old, please install Java 1.8 or newer." 1>&2
  echo "For Ubuntu: sudo apt-get install openjdk-8-jre" 1>&2
  echo "If you have installed Java 8, but it is not in your PATH," 1>&2
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

if [ -n "$TMPDIR" ]; then
  JAVA_TOOL_OPTIONS="-Djava.io.tmpdir=$TMPDIR"
elif [ -n "$TEMP" ]; then
  JAVA_TOOL_OPTIONS="-Djava.io.tmpdir=$TEMP"
elif [ -n "$TMP" ]; then
  JAVA_TOOL_OPTIONS="-Djava.io.tmpdir=$TMP"
fi


# read input parameters
declare -a OPTIONS


until [ -z "$1" ]
  do
    case "$1" in
        -heap)
            JAVA_HEAP_SIZE=$2
            shift 2;;
        -stack)
            JAVA_STACK_SIZE=$2
            shift 2;;
        -D | --debug)
            JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
            shift;;
        -B | --build)
            ant
            shift;;
        -P | --project)
            PROJECT=$2
            shift 2;;
        -C | --clean)
            rm -rf $PATH_TO_CPACHECKER/.output
            shift;;
	*)# other params are only for CPAchecker
       OPTIONS+=($1)
       shift;;
   esac
  done

# print Java heap and stack size
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

# set Java environment options
JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS  -Xss${JAVA_STACK_SIZE} -Xmx${JAVA_HEAP_SIZE}"

export JAVA_TOOL_OPTIONS

# CodeSurfer STK
CODESURFER_STK="$PATH_TO_CPACHECKER/scripts/csurfJava_plugin.stk"

# Check the path of the target project
if [  -e "$PROJECT" ];then
    echo "Please input the project you want to model check!"
    exit 1
else
    echo "Performing model checking on the project: ${PROJECT}"
fi


#CPAChecker agruments
CPACHECKER_ARGUMENTS=$(echo ${OPTIONS[@]})

sed "5c \    \"${PATH_TO_CPACHECKER}\"" -i scripts/csurfJava_plugin.stk
sed "6c \    \"${CPACHECKER_ARGUMENTS}\"" -i scripts/csurfJava_plugin.stk

# Perform the plugin
csurf -nogui $PROJECT -l $CODESURFER_STK
