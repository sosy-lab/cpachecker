#!/usr/bin/env python3

# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

import sys

sys.dont_write_bytecode = True  # prevent creation of .pyc files

if __name__ == "__main__":
    sys.exit(
        "This script is not necessary anymore because CPAchecker now directly generates HTML reports. Please look for HTML files in the output directory."
    )
