#!/usr/bin/env python3

# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

import glob
import os
import sys

sys.dont_write_bytecode = True  # prevent creation of .pyc files
for egg in glob.glob(
    os.path.join(
        os.path.dirname(__file__), os.pardir, "lib", "python-benchmark", "*.whl"
    )
):
    sys.path.insert(0, egg)

# Add ./benchmark/tools to __path__ of benchexec.tools package
# such that additional tool-wrapper modules can be placed in this directory.
import benchexec.tools

benchexec.tools.__path__ += [
    os.path.join(os.path.dirname(__file__), "benchmark", "tools")
]

import benchexec.tablegenerator

benchexec.tablegenerator.DEFAULT_OUTPUT_PATH = "test/results/"
benchexec.tablegenerator.LIB_URL = "https://www.sosy-lab.org/lib"

try:
    sys.exit(benchexec.tablegenerator.main())
except KeyboardInterrupt:
    print("Interrupted")
