#!/usr/bin/env python3

"""
CPAchecker is a tool for configurable software verification.
This file is part of CPAchecker.

Copyright (C) 2007-2015  Dirk Beyer
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

import sys
sys.dont_write_bytecode = True # prevent creation of .pyc files

import argparse
import glob
import logging
import os
import urllib.request as request

for egg in glob.glob(os.path.join(os.path.dirname(__file__), os.pardir, 'lib', 'python-benchmark', '*.whl')):
    sys.path.insert(0, egg)

from benchexec import util

from benchmark.webclient import *  # @UnusedWildImport

__version__ = '1.0'

DEFAULT_OUTPUT_PATH = "./"


def _create_argument_parser():
    """
    Create a parser for the command-line options.
    @return: an argparse.ArgumentParser instance
    """

    parser = argparse.ArgumentParser(
        description="Execute a CPAchecker run in the VerifierCloud using the web interface." \
         + " Command-line parameters can additionally be read from a file if file name prefixed with '@' is given as argument.",
        fromfile_prefix_chars='@',
        add_help=False) # conflicts with -heap

    parser.add_argument("--help",
                      action='help',
                      help="Prints this help.")

    parser.add_argument("--cloudMaster",
                      dest="cloud_master",
                      default="http://vcloud.sosy-lab.org/webclient/",
                      metavar="HOST",
                      help="Sets the webclient host of the VerifierCloud instance to be used.")

    parser.add_argument("--cloudPriority",
                      dest="cloud_priority",
                      metavar="PRIORITY",
                      help="Sets the priority for this benchmark used in the VerifierCloud. Possible values are IDLE, LOW, HIGH, URGENT.")

    parser.add_argument("--cloudCPUModel",
                      dest="cpu_model", type=str, default=None,
                      metavar="CPU_MODEL",
                      help="Only execute runs in the VerifierCloud on CPU models that contain the given string.")

    parser.add_argument("--cloudUser",
                      dest="cloud_user",
                      metavar="USER:PWD",
                      help="The user and password for the VerifierCloud.")

    parser.add_argument("--revision",
                      dest="revision",
                      metavar="BRANCH:REVISION",
                      help="The svn revision of CPAchecker to use.")

    parser.add_argument("-d", "--debug",
                      action="store_true",
                      help="Enable debug output")

    parser.add_argument("-o", "--outputpath",
                      dest="output_path", type=str,
                      default=DEFAULT_OUTPUT_PATH,
                      help="Output prefix for the generated results. "
                            + "If the path is a folder files are put into it,"
                            + "otherwise it is used as a prefix for the resulting files.")
    parser.add_argument("--resultFilePattern",
                     dest="result_file_pattern", type=str,
                     default="**",
                     help="Only files matching this glob pattern are transported back to the client.")

    parser.add_argument("-T", "--timelimit",
                      dest="timelimit", default=None,
                      type=util.parse_timespan_value,
                      help="Time limit in seconds",
                      metavar="SECONDS")

    parser.add_argument("-M", "--memorylimit",
                      dest="memorylimit", default=None,
                      type=util.parse_memory_value,
                      help="Memory limit",
                      metavar="BYTES")

    parser.add_argument("-c", "--corelimit", dest="corelimit",
                      type=int, default=None,
                      metavar="N",
                      help="Limit the tool to N CPU cores.")

    parser.add_argument("--version",
                        action="version",
                        version="%(prog)s " + __version__)
    return parser

def _setup_logging(config):
    """
    Configure the logging framework.
    """
    if config.debug:
        logging.basicConfig(format="%(asctime)s - %(levelname)s - %(message)s",
                            level=logging.DEBUG)
    else:
        logging.basicConfig(format="%(asctime)s - %(levelname)s - %(message)s",
                            level=logging.INFO)

def _init(config):
    """
    Sets _webclient if it is defined in the given config.
    """
    if not config.cpu_model:
        logging.warning("It is strongly recommended to set a CPU model('--cloudCPUModel'). "\
                        "Otherwise the used machines and CPU models are undefined.")

    if not config.cloud_master:
        sys.exit("No URL of a VerifierCloud instance is given.")

    (svn_branch, svn_revision) = _get_revision(config)

    webclient = WebInterface(config.cloud_master, config.cloud_user, svn_branch, svn_revision,
                             user_agent='cpa_web_cloud.py', version=__version__)

    logging.info('Using %s version %s.', webclient.tool_name(), webclient.tool_revision())
    return webclient

def _get_revision(config):
    """
    Extracts branch and revision number from the given config parameter.
    @return: (branch, revision number)
    """
    if config.revision:
        tokens = config.revision.split(':')
        svn_branch = tokens[0]
        if len(tokens) > 1:
            revision = config.revision.split(':')[1]
        else:
            revision = 'HEAD'
        return (svn_branch, revision)
    else:
        return ('trunk', 'HEAD')


def _submit_run(webclient, config, cpachecker_args, counter=0):
    """
    Submits a single run using the web interface of the VerifierCloud.
    @return: the run's result
    """
    limits = {}
    if config.memorylimit:
        limits['memlimit'] = config.memorylimit
    if config.timelimit:
        limits['timelimit'] = config.timelimit
    if config.corelimit:
        limits['corelimit'] = config.corelimit

    run = _parse_cpachecker_args(cpachecker_args)

    run_result_future = webclient.submit(run, limits, config.cpu_model, \
                              config.result_file_pattern, config.cloud_priority )
    webclient.flush_runs()
    return run_result_future.result()

def _parse_cpachecker_args(cpachecker_args):
    """
    Parses the given CPAchecker arguments.
    @return:  Run object with options, identifier and list of source files
    """
    class Run:
        options = []
        identifier = None
        sourcefiles = []
        propertyfile = None

    run = Run()
    run.identifier = cpachecker_args

    i = iter(cpachecker_args)
    while True:
        try:
            option=next(i)
            if len(option) == 0:
                continue # ignore empty arguments

            if option in ["-heap", "-timelimit", "-entryfunction", "-spec", "-config", "-setprop"]:
                run.options.append(option)
                run.options.append(next(i))

            elif option[0] == '-':
                run.options.append(option)

            else:
                run.sourcefiles.append(option)

        except StopIteration:
            break

    return run

def _execute():
    """
    Executes a single CPAchecker run in the VerifierCloud via the web front end.
    All informations are given by the command line arguments.
    @return: the return value of CPAchecker
    """
    arg_parser = _create_argument_parser()
    (config, cpachecker_args) = arg_parser.parse_known_args()
    _setup_logging(config)
    webclient = _init(config)

    try:
        run_result = _submit_run(webclient, config, cpachecker_args)
        return handle_result(run_result, config.output_path, cpachecker_args)

    except request.HTTPError as e:
        logging.warning(e.reason)
    except WebClientError as e:
        logging.warning(str(e))

    finally:
        webclient.shutdown()


if __name__ == "__main__":
    try:
        sys.exit(_execute())
    except KeyboardInterrupt:
        sys.exit(1)
