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
import platform
import sys
sys.dont_write_bytecode = True # prevent creation of .pyc files
for egg in glob.glob(os.path.join(os.path.dirname(__file__), os.pardir, 'lib', 'python-benchmark', '*.egg')):
    sys.path.insert(0, egg)

import benchexec

# Add ./benchmark/tools to __path__ of benchexec.tools package
# such that additional tool-wrapper modules can be placed in this directory.
import benchexec.tools
benchexec.tools.__path__ = [os.path.join(os.path.dirname(__file__), 'benchmark', 'tools')] + benchexec.tools.__path__

class Benchmark(benchexec.BenchExec):
    """
    An extension of BenchExec for use with CPAchecker
    that supports executor modules for executing the benchmarks
    in the VerifierCloud or in the Google App Engine.
    """

    DEFAULT_OUTPUT_PATH = "test/results/"

    def create_argument_parser(self):
        parser = super(Benchmark, self).create_argument_parser()
        vcloud_args = parser.add_argument_group('Options for using VerifierCloud')
        vcloud_args.add_argument("--cloud",
                          dest="cloud",
                          action="store_true",
                          help="Use VerifierCloud to execute benchmarks.")

        vcloud_args.add_argument("--cloudMaster",
                          dest="cloudMaster",
                          metavar="HOST",
                          help="Sets the master host of the VerifierCloud instance to be used. If this is a HTTP URL, the web interface is used.")

        vcloud_args.add_argument("--cloudPriority",
                          dest="cloudPriority",
                          metavar="PRIORITY",
                          help="Sets the priority for this benchmark used in the VerifierCloud. Possible values are IDLE, LOW, HIGH, URGENT.")

        vcloud_args.add_argument("--cloudCPUModel",
                          dest="cpu_model", type=str, default=None,
                          metavar="CPU_MODEL",
                          help="Only execute runs in the VerifierCloud on CPU models that contain the given string.")

        vcloud_args.add_argument("--cloudUser",
                          dest="cloudUser",
                          metavar="USER:PWD",
                          help="The user and password for the VerifierCloud (if using the web interface).")

        vcloud_args.add_argument("--revision",
                          dest="revision",
                          metavar="BRANCH:REVISION",
                          help="The svn revision of CPAchecker to use  (if using the web interface of the VerifierCloud).")

        vcloud_args.add_argument("--justReprocessResults",
                          dest="reprocessResults",
                          action="store_true",
                          help="Do not run the benchmarks. Assume that the benchmarks were already executed in the VerifierCloud and the log files are stored (use --startTime to point the script to the results).")

        vcloud_args.add_argument("--cloudClientHeap",
                          dest="cloudClientHeap",
                          metavar="MB",
                          default=100,
                          type=int,
                          help="The heap-size (in MB) used by the VerifierCloudClient. A too small heap-size may terminate the client without any results.")

        appengine_args = parser.add_argument_group('Options for using CPAchecker in the AppEngine')
        appengine_args.add_argument("--appengine",
                          dest="appengine",
                          action="store_true",
                          help="Use Google App Engine to execute benchmarks.")

        appengine_args.add_argument("--appengineURI",
                          dest="appengineURI",
                          metavar="URI",
                          default='http://cpachecker.appspot.com',
                          type=str,
                          help="Sets the URI to use when submitting tasks to App Engine.")

        appengine_args.add_argument("--appenginePollInterval",
                          dest="appenginePollInterval",
                          metavar="SECONDS",
                          default=60,
                          type=int,
                          help="Sets the interval in seconds after which App Engine is polled for results.")

        appengine_args.add_argument("--appengineKeep",
                            dest="appengineDeleteWhenDone",
                            action="store_false",
                            help="If set a task will NOT be deleted from App Engine after it has successfully been executed.")

        return parser


    def load_executor(self):
        if self.config.cloud:
            if self.config.cloudMaster and "http" in self.config.cloudMaster:
                import benchmark.webclient as executor
            else:
                import benchmark.vcloud as executor
        elif self.config.appengine:
            import benchmark.appengine as executor
        else:
            executor = super(Benchmark, self).load_executor()
        return executor


    def check_existing_results(self, benchmark):
        if not self.config.reprocessResults:
            super(Benchmark, self).check_existing_results(benchmark)


if __name__ == "__main__":
    # Add directory with binaries to path.
    bin_dir = "lib/native/x86_64-linux" if platform.machine() == "x86_64" else \
              "lib/native/x86-linux"    if platform.machine() == "i386" else None
    if bin_dir:
        bin_dir = os.path.join(os.path.dirname(__file__), os.pardir, bin_dir)
        os.environ['PATH'] += os.pathsep + bin_dir

    benchexec.main(Benchmark())
