#!/usr/bin/python3

# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

import ast
import copy
import getopt
import json
import logging
from mpi4py import MPI
import os
import socket
import subprocess
import sys

ANALYSIS = "analysis"
CMDLINE = "cmd"
OUTPUT_PATH = "output"
LOGFILE = "logfile"

logger = None


class MPIMain:
    """
    A wrapper class used by the MPIPortfolioAlgorithm in CPAchecker. It uses MPI to
    start additional CPAchecker instances in which the actual analyses are concurrently
    executed on different processes. The command line for the subprocess is decided by
    the number of the MPI rank.
    """

    comm = MPI.COMM_WORLD
    input_args = {}
    analysis_param = {}

    run_subanalysis = False

    main_node_network_config = None

    def __init__(self, argv):
        self.setup_mpi()
        self.setup_logger()
        self.parse_input_args(argv)

    def setup_mpi(self):
        # Name of the processor
        self.name = MPI.Get_processor_name()

        # Number of total processes
        self.size = self.comm.Get_size()
        # Rank of the this process
        self.rank = self.comm.Get_rank()

    def setup_logger(self):
        """
        This sets up a logger such that debug and info messages are logged to
        stdout, whereas messages of level warning and higher (critical, error, etc.)
        get logged to stderr. The default log-level is set to normal.
        """
        global logger

        class InfoFilter(logging.Filter):
            def filter(self, rec):  # noqa: A003
                return rec.levelno in (logging.DEBUG, logging.INFO)

        if logger is None:
            logger = logging.getLogger(__name__)

        logging_format = logging.Formatter(
            fmt="Rank {} - %(levelname)s:  %(message)s".format(self.rank),
        )

        log_handler_stdout = logging.StreamHandler(stream=sys.stdout)
        log_handler_stdout.setLevel(logging.DEBUG)
        log_handler_stdout.addFilter(InfoFilter())
        log_handler_stdout.setFormatter(logging_format)

        log_handler_stderr = logging.StreamHandler(stream=sys.stderr)
        log_handler_stderr.setLevel(logging.WARNING)
        log_handler_stderr.setFormatter(logging_format)

        logger.addHandler(log_handler_stdout)
        logger.addHandler(log_handler_stderr)
        logger.setLevel(logging.INFO)

    def parse_input_args(self, argv):
        # TODO: use argsparse for parsing input
        try:
            logger.debug("Input of user args: %s", str(argv))
            opts, args = getopt.getopt(argv, "di:w", ["input="])
        except getopt.GetoptError:
            logger.error(
                "Unable to parse user input. Usage: %s -d -i <input>",
                __file__,
                exc_info=True,
            )
            sys.exit(2)

        for opt, arg in opts:
            if opt == "-d":
                logger.setLevel(logging.DEBUG)
            elif opt in ("-i", "--input"):
                if isinstance(arg, str):
                    self.input_args = ast.literal_eval(arg)
                elif isinstance(arg, dict):
                    self.input_args = arg
                else:
                    logger.critical("Input has an invalid type: %s", type(arg))
                    sys.exit(2)
            elif opt in ("-w"):
                logger.setLevel(logging.WARNING)

        self.main_node_network_config = self.input_args.get("network_settings")
        if self.main_node_network_config is not None:
            aws_main_ip = os.environ.get("AWS_BATCH_JOB_MAIN_NODE_PRIVATE_IPV4_ADDRESS")
            if (
                aws_main_ip is not None
                and self.main_node_network_config["main_node_ipv4_address"]
                != aws_main_ip
            ):
                # Two different ip addresses received for the main node
                logger.critical("Inconsistent ip addresses for main node received.")
                logger.debug(
                    "aws_main_ip: '%s', main_node_network_...: '%s'",
                    aws_main_ip,
                    self.main_node_network_config["main_node_ipv4_address"],
                )
                sys.exit(2)

            self.replace_escape_chars(self.main_node_network_config)

        logger.debug(json.dumps(self.input_args, sort_keys=True, indent=4))

    def print_self_info(self):
        """Print an info about the proccesor name, the rank of the executed process, and
        the number of total processes."""
        logger.info(
            "Executing process on '{}' (rank {} of {})".format(
                self.name, self.rank, self.size
            )
        )

    def execute_verifier(self):
        logger.debug("Running script %s from dir '%s'", __file__, os.getcwd())
        cmdline = self.analysis_param[CMDLINE]
        if cmdline is None:
            logger.warning(
                "Cmdline does not contain any input; nothing to do. ",
                "This is probably because there are more processors available ",
                "than analyses to perform.",
                "Exiting with status 0",
            )
            sys.exit(0)
        else:
            logger.warning(
                "Starting new CPAchecker instance performing the analysis: %s",
                self.analysis_param[ANALYSIS],
            )
            logger.info("Executing cmd: %s", cmdline)
            # Redirect all output from the errorstream in the child CPAchecker
            # instances, such that the output log stays consistent
            if not os.path.exists(self.analysis_param[OUTPUT_PATH]):
                os.makedirs(self.analysis_param[OUTPUT_PATH])

            outputfile = open(self.analysis_param[LOGFILE], "w+")
            process = subprocess.run(
                cmdline, stdout=outputfile, stderr=subprocess.STDOUT,
            )
            outputfile.close()
            logger.warning("Process returned with status code %d", process.returncode)

    def prepare_cmdline(self):
        logger.info("Running analysis with number: %d", self.rank)
        analysis_args = None
        if self.rank <= len(self.input_args) - 1:
            analysis_args = self.input_args.get("Analysis_{}".format(self.rank))
        if analysis_args is None:
            logger.warning("No arguments for the analysis found.")
        else:
            self.analysis_param = copy.deepcopy(analysis_args)
            self.replace_escape_chars(self.analysis_param)
            self.run_subanalysis = True

            logger.debug("Running analysis: %s", self.analysis_param[ANALYSIS])
            logger.debug("Running cmd: %s", self.analysis_param[CMDLINE])
            logger.debug("Writing log in file: %s", self.analysis_param[LOGFILE])
            logger.debug("Storing output in dir: %s", self.analysis_param[OUTPUT_PATH])

    def replace_escape_chars(self, d):
        for key, value in d.items():
            if isinstance(value, str):
                d[key] = value.replace("\\", "")
            elif isinstance(value, list):
                d[key] = [w.replace("\\", "") for w in value]

    def push_results_to_master(self):
        if self.main_node_network_config is None:
            logger.warning(
                "No network parameters received. The process was executed "
                "on the main node only. The result files are hence "
                "already in their correct location."
            )
        else:
            # Get the local ip adress and compare it with the address from the main node
            # If they differ, create an ssh-connection and push all result files to the
            # main node. Otherwise, do nothing.
            hostname = socket.gethostname()
            local_ip_address = socket.gethostbyname(hostname)
            if (
                local_ip_address
                != self.main_node_network_config["main_node_ipv4_address"]
            ):
                output = os.path.abspath(self.analysis_param[OUTPUT_PATH])
                if not os.path.exists(output):
                    logger.warning(
                        "Found no output that can be copied to the main node"
                        "Exiting with code 0."
                    )
                    sys.exit(0)
                else:
                    logger.info("Copying result files via scp to the main-node")
                    scp_cmd = [
                        "scp",
                        "-r",
                        output,
                        "{}@{}:{}".format(
                            self.main_node_network_config["user_name_main_node"],
                            self.main_node_network_config["main_node_ipv4_address"],
                            os.path.join(
                                self.main_node_network_config[
                                    "project_location_main_node"
                                ],
                                self.analysis_param[OUTPUT_PATH],
                            ),
                        ),
                    ]
                    logger.warning("Command for scp: %s", scp_cmd)
                    scp_proc = subprocess.run(scp_cmd)
                    logger.warning(
                        "Process for copying the output back to the main node "
                        "completed with status code %d",
                        scp_proc.returncode,
                    )

            else:
                logger.warning(
                    "The current process is executed on the main node. The result "
                    "files are already in the correct place."
                )


def main():
    mpi = MPIMain(sys.argv[1:])
    mpi.print_self_info()
    if len(mpi.input_args) == 0:
        logger.warning("No input received. Aborting with status code 0")
        sys.exit(0)

    mpi.prepare_cmdline()
    if mpi.run_subanalysis is True:
        mpi.execute_verifier()
        mpi.push_results_to_master()
    else:
        logger.warning("Nothing to run. Exiting with status 0")
    sys.exit(0)


if __name__ == "__main__":
    main()
