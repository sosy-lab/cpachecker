#!/usr/bin/env python3

"""
CPAchecker is a tool for configurable software verification.
This file is part of CPAchecker.

Copyright (C) 2007-2014  Dirk Beyer
All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


CPAchecker web page:
  http://cpachecker.sosy-lab.org
"""

# prepare for Python 3
from __future__ import absolute_import, division, print_function, unicode_literals

import glob
import os
import sys
sys.dont_write_bytecode = True # prevent creation of .pyc files
for egg in glob.glob(os.path.join(os.path.dirname(__file__), os.pardir, 'lib', 'python-benchmark', '*.whl')):
    sys.path.insert(0, egg)

# Add ./benchmark/tools to __path__ of benchexec.tools package
# such that additional tool-wrapper modules can be placed in this directory.
import benchexec.tools
benchexec.tools.__path__ += [os.path.join(os.path.dirname(__file__), 'benchmark', 'tools')]

import benchexec.tablegenerator

benchexec.tablegenerator.DEFAULT_OUTPUT_PATH = 'test/results/'
benchexec.tablegenerator.LIB_URL = 'https://www.sosy-lab.org/lib'

try:
    sys.exit(benchexec.tablegenerator.main())
except KeyboardInterrupt:
    print ('Interrupted')
