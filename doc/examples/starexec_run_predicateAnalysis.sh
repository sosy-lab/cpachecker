#!/bin/bash

# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

#Starexec solver configuration filenames need to start with "starexec_run_"
#Call cpa.sh script to run CPAchecker and add configurations according to
#your requirements. Only make a single cpa.sh call per file. Use "$1" 
#instead of a specific filename as those will be passed by the starexec job.
./cpa.sh -predicateAnalysis "$1"
