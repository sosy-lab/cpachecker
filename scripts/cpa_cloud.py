#!/usr/bin/env python

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
import logging
import subprocess
import sys
sys.dont_write_bytecode = True # prevent creation of .pyc files
for egg in glob.glob(os.path.join(os.path.dirname(__file__), os.pardir, 'lib', 'python-benchmark', '*.whl')):
    sys.path.insert(0, egg)

# Add ./benchmark/tools to __path__ of benchexec.tools package
# such that additional tool-wrapper modules can be placed in this directory.
import benchexec.tools
benchexec.tools.__path__ = [os.path.join(os.path.dirname(__file__), 'benchmark', 'tools')] + benchexec.tools.__path__

import benchexec.tools.cpachecker
cpachecker = benchexec.tools.cpachecker.Tool()
executable = cpachecker.executable()
required_files = cpachecker.program_files(executable)

# install cloud and dependencies
ant = subprocess.Popen(["ant", "resolve-benchmark-dependencies"])
ant.communicate()
ant.wait()

# assume that last parameter is the input file
argv = sys.argv
parameters = argv[1:-1]
in_file = argv[-1]

# start cloud and wait for exit
logging.debug("Starting cloud.")

logLevel = "FINER"

cpachecker_dir = os.path.normpath(os.path.join(os.path.dirname(argv[0]), os.pardir)) # directory above script directory
lib_dir = os.path.abspath(os.path.join("lib", "java-benchmark"))
cmd_line = ["java", "-jar", os.path.join(lib_dir, "vcloud.jar"), "cpachecker",
            "--loglevel", logLevel,
            "--input", in_file,
            "--requiredFiles", ','.join(required_files),
            "--cpachecker-dir", cpachecker_dir,
            "--", executable
            ]
cmd_line.extend(parameters)

logging.debug("CPAchecker command: ", cmd_line)
cloud = subprocess.Popen(cmd_line)
cloud.communicate()
cloud.wait()


# vim:sts=4:sw=4:expandtab:
