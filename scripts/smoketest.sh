#!/bin/bash

# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

set -e

echo "==============================================="
echo "Starting smoke test for SV-COMP..."
echo "==============================================="

# Move up directories until we find the CPAchecker root
CPACHECKER_DIR="$(dirname "$0")"
while [ ! -x "$CPACHECKER_DIR/bin/cpachecker" ]; do
  if [ "$CPACHECKER_DIR" = "/" ] || [ -z "$CPACHECKER_DIR" ]; then
    echo "Could not locate CPAchecker root (missing bin/cpachecker)."
    exit 1
  fi
  CPACHECKER_DIR="$CPACHECKER_DIR/.."
done

# Run CPAchecker using relative paths
$CPACHECKER_DIR/bin/cpachecker \
  --svcomp26 \
  --spec $CPACHECKER_DIR/config/properties/unreach-call.prp \
  $CPACHECKER_DIR/doc/examples/example-safe.c

echo "==============================================="
echo "WARNING: The output/ folder has been created in the current directory!"
echo "The smoke test finished successfully!"
echo "==============================================="