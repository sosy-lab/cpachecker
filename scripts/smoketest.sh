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
PATH_TO_CPACHECKER="$(dirname "$0")"
while [ ! -x "$PATH_TO_CPACHECKER/bin/cpachecker" ]; do
  if [ "$PATH_TO_CPACHECKER" = "/" ] || [ -z "$PATH_TO_CPACHECKER" ]; then
    echo "Could not locate CPAchecker root (missing bin/cpachecker)."
    exit 1
  fi
  PATH_TO_CPACHECKER="$PATH_TO_CPACHECKER/.."
done

# Run CPAchecker using relative paths
$PATH_TO_CPACHECKER/bin/cpachecker \
  --svcomp26 \
  --output-path $PATH_TO_CPACHECKER/output/ \
  --spec $PATH_TO_CPACHECKER/config/properties/unreach-call.prp \
  $PATH_TO_CPACHECKER/doc/examples/example-safe.c

echo "==============================================="
echo "The smoke test finished successfully!"
echo "==============================================="