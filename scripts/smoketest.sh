#!/bin/bash

# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0


set -e  # Exit immediately if any command fails

echo "==============================================="
echo "Starting smoke test for SV-COMP..."
echo "==============================================="

# Determine the absolute path to the CPAchecker root directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CPACHECKER_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Run CPAchecker using absolute paths
"$CPACHECKER_ROOT/bin/cpachecker" \
  --svcomp26 \
  --no-output-files \
  --spec "$CPACHECKER_ROOT/config/properties/unreach-call.prp" \
  "$CPACHECKER_ROOT/doc/examples/example-safe.c"

echo "The smoke test finished successfully!"