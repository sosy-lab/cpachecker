#!/usr/bin/env bash

# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

# ##############################################################################
# This script is a helper for automated bisecting of CPAchecker.
# To use it:
# 0. Make sure you are in CPAchecker project directory.
# 1. Make temporary copy of this script, e.g., "cp scripts/bisect.sh bisect.sh".
# 2. Fill out variables below in copy of script
#    (at least CPACHECKER_ARGS and one outcome for "good" and "bad").
# 2. Start bisecting: "git bisect start <bad> <good>"
# 3. Execute "git bisect run ./bisect.sh" (optionally append "-q")
# ##############################################################################

# Enter CPAchecker arguments here
CPACHECKER_ARGS=""
# Example:
# CPACHECKER_ARGS="--default doc/examples/example-safe.c"

# Choose how to build CPAchecker:
# fastest (no Error Prone checks)
BUILD_ARGS="build -Derrorprone.disable=true"
# regular build
#BUILD_ARGS="build"
# slowest, most precise
#BUILD_ARGS="clean build"

# Define how to treat each outcome for git bisect.
# Valid values are "good", "bad", "skip" (git bisect skip) and "stop" (stops bisecting).
# Change at least one variable to "good" and "bad", respectively.
BUILD_FAILURE="skip"
CPACHECKER_FAILURE="stop"
TRUE_RESULT="stop"
FALSE_RESULT="stop"
UNKNOWN_RESULT="stop"
OTHER="stop"

# Configuration ends, actual script follows
# ######################################################################

# Argument handling
case "$1" in
  -q)
    QUIET=true
    BUILD_ARGS="$BUILD_ARGS -q"
    ;;
  "")
    QUIET=false
    ;;
  *)
    echo "Invalid argument: $1" >&2
    exit 128
  ;;
esac

if [[ -z "$CPACHECKER_ARGS" ]]; then
  cat <<EOF
This script is a helper for bisecting CPAchecker.
To use it, please edit the variables inside it and run

git bisect run $0 $*
EOF
  echo "" >&2
  exit 128
fi


exit_for_bisect() {
  # Convert good/bad/skip/stop into appropriate exit codes for git bisect.
  case "$1" in
    skip)
      echo "Skipping because of $2" >&2
      exit 125
      ;;
    stop)
      echo "Stopping because requested for $2" >&2
      exit 128
      ;;
    good)
      echo "$2: mark as good" >&2
      exit 0
      ;;
    bad)
      echo "$2: mark as bad" >&2
      exit 1
      ;;
    *)
      echo "Stopping because of invalid value '$1' for $2" >&2
      exit 128
      ;;
  esac
}


echo "Building CPAchecker" >&2
ant $BUILD_ARGS
BUILD_CODE="$?"

# cleanup
git checkout doc/ConfigurationOptions.txt

[[ $BUILD_CODE == 0 ]] \
  || exit_for_bisect $BUILD_FAILURE "build failure"


echo "Executing CPAchecker" >&2
CPACHECKER_OUT="$(bin/cpachecker $CPACHECKER_ARGS 2>&1)"
CPACHECKER_CODE="$?"

[[ $QUIET == true ]] || echo "$CPACHECKER_OUT"

[[ $CPACHECKER_CODE == 0 ]] \
  || exit_for_bisect $CPACHECKER_FAILURE "CPAchecker failure"

[[ "$CPACHECKER_OUT" == *"Verification result: TRUE"* ]] \
  && exit_for_bisect $TRUE_RESULT "result TRUE"

[[ "$CPACHECKER_OUT" == *"Verification result: FALSE"* ]] \
  && exit_for_bisect $FALSE_RESULT "result FALSE"

[[ "$CPACHECKER_OUT" == *"Verification result: UNKNOWN"* ]] \
  && exit_for_bisect $UNKNOWN_RESULT "result UNKNOWN"

exit_for_bisect $OTHER "other output"
