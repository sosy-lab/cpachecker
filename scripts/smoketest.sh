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
SEARCH_DIR="$(dirname "$0")"
while [ ! -x "$SEARCH_DIR/bin/cpachecker" ]; do
  if [ "$SEARCH_DIR" = "/" ] || [ -z "$SEARCH_DIR" ]; then
    echo "Could not locate CPAchecker root (missing bin/cpachecker)."
    exit 1
  fi
  SEARCH_DIR="$SEARCH_DIR/.."
done

# Change to the CPAchecker root directory (relative traversal only)
cd "$SEARCH_DIR"

# Run CPAchecker using relative paths
bin/cpachecker \
  --svcomp26 \
  --no-output-files \
  --spec config/properties/unreach-call.prp \
  doc/examples/example-safe.c

echo "==============================================="
echo "The smoke test finished successfully!"
echo "==============================================="