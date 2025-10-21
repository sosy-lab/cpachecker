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

bin/cpachecker \
  --svcomp26 \
  --spec config/properties/unreach-call.prp \
  doc/examples/example-safe.c

echo "The smoke test finished successfully!"