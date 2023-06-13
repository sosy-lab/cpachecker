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

import benchexec.runexecutor

sys.exit(benchexec.runexecutor.main())
