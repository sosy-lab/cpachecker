#!/usr/bin/env python3

# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2017 Rodrigo Castano
# SPDX-FileCopyrightText: 2017-2020 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

import logging
import sys

import post_processing.coverage.generate_coverage as generate_coverage

if __name__ == "__main__":
    if sys.version_info[0] < 3:
        sys.exit("This script requires Python 3.")

    logging.basicConfig()
    logger = logging.getLogger(__name__)
    logger.setLevel(logging.INFO)
    # Excluding argv[0], otherwise it'll be recognized as the positional
    # argument.
    generate_coverage.main(sys.argv[1:], logger)
