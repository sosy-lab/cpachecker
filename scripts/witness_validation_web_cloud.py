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

if os.path.basename(__file__) == 'witness_validation_web_cloud.py':
    # try looking up additional libraries if not packaged
    for egg in glob.glob(os.path.join(os.path.dirname(__file__), os.pardir, 'lib', 'python-benchmark', '*.whl')):
        sys.path.insert(0, egg)

from benchmark.webclient import *  # @UnusedWildImport

__version__ = '1.0'

DEFAULT_OUTPUT_PATH = "./"


def _create_argument_parser():
    """
    Create a parser for the command-line options.
    @return: an argparse.ArgumentParser instance
    """

    parser = argparse.ArgumentParser(
        description="Validate witness using CPAchecker in the cloud (without local installation).",
        fromfile_prefix_chars='@')

    parser.add_argument("--cloudMaster",
                      dest="cloud_master",
                      default="http://vcloud.sosy-lab.org/webclient/",
                      metavar="HOST",
                      help=argparse.SUPPRESS)

    parser.add_argument("--cloudUser",
                      dest="cloud_user",
                      metavar="USER:PWD",
                      help=argparse.SUPPRESS)

    parser.add_argument("--program",
                      dest="program_file",
                      metavar="FILE",
                      help="The path to the program file.",
                      required=True)

    parser.add_argument("--witness",
                      dest="witness_file",
                      metavar="FILE",
                      help="The path to the witness file.",
                      required=True)

    parser.add_argument("--configuration",
                      dest="configuration",
                      metavar="CONFIG",
                      help="The configuration used for the validation.")

    parser.add_argument("-d", "--debug",
                      action="store_true",
                      help=argparse.SUPPRESS)

    parser.add_argument("-o", "--outputpath",
                      dest="output_path", type=str,
                      default=DEFAULT_OUTPUT_PATH,
                      help="Output prefix for the generated results. "
                            + "If the path is a folder files are put into it,"
                            + "otherwise it is used as a prefix for the resulting files.")

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
    if not config.cloud_master:
        sys.exit("No URL of a VerifierCloud instance is given.")

    webclient = WebInterface(config.cloud_master, config.cloud_user,
                             user_agent='witness_validation_web_cloud.py', version=__version__)

    logging.info('Using %s version %s.', webclient.tool_name(), webclient.tool_revision())
    return webclient

def _submit_run(webclient, config):
    """
    Submits a single run using the web interface of the VerifierCloud.
    @return: the run's result
    """
    run_result_future = webclient.submit_witness_validation(\
          config.witness_file, config.program_file, config.configuration, config.cloud_user)
    webclient.flush_runs()
    return run_result_future.result()

def _execute():
    """
    Executes a single CPAchecker run in the VerifierCloud via the web front end.
    All informations are given by the command line arguments.
    @return: the return value of CPAchecker
    """
    arg_parser = _create_argument_parser()
    config = arg_parser.parse_args()
    _setup_logging(config)
    webclient = _init(config)

    try:
        run_result = _submit_run(webclient, config)
        return handle_result(run_result, config.output_path, config.witness_file,
                             handle_host_info=lambda x : None)

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
