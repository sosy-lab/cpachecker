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
from enum import Enum
import getopt
import json
import logging
from mpi4py import MPI
import os
import signal
import socket
import subprocess
import sys
import threading

# MPI message tags
tags = Enum("Status", "READY DONE")

results = Enum("Status", "SUCCESS UNKNOWN EXCEPTION")

MPI_PROBE_INTERVAL = 1

ANALYSIS = "analysis"
CMDLINE = "cmd"
OUTPUT_PATH = "output"
LOGFILE = "logfile"

SUBANALYSIS_RESULT = "subanalysis_result"

logger = None
mpi = None


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
    process = None
    shutdown_requested = False
    mpi_listener_thread = None
    event_listener = None

    main_node_network_config = None

    def __init__(self, argv):
        self.setup_mpi()
        self.setup_logger()
        self.parse_input_args(argv)
        self.setup_mpi_listener_thread()

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
            fmt="Rank {} - %(asctime)s - %(levelname)-9s %(message)s".format(self.rank),
            datefmt="%H:%M:%S",
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
        # TODO: use argparse for parsing input
        try:
            opts, args = getopt.getopt(argv, "di:w", ["input="])
        except getopt.GetoptError:
            logger.exception(
                "Unable to parse user input. Usage: %s -d -i <input>", __file__
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

        logger.debug("Input of user args: %s", str(argv))

        self.main_node_network_config = self.input_args.get("network_settings")
        if self.main_node_network_config is not None:
            main_node_ip = self.main_node_network_config.get("main_node_ipv4_address")
            logger.debug("main node ip address: %s", main_node_ip)

            aws_env_ip = os.environ.get("AWS_BATCH_JOB_MAIN_NODE_PRIVATE_IPV4_ADDRESS")
            if aws_env_ip is not None and aws_env_ip != main_node_ip:
                # Two different ip addresses received for the main node
                raise ValueError("Inconsistent ip addresses for main node received.")

            self.replace_escape_chars(self.main_node_network_config)

        logger.debug(
            "Printing the formatted input: \n%s)",
            json.dumps(self.input_args, sort_keys=True, indent=4),
        )

    def setup_mpi_listener_thread(self):
        # TODO: Use multiprocessing instead of threading
        # In python, Threads apparently do not run in parallel. Instead, only
        # one thread is being executed in a ThreadPool at any given time t.
        # For more information, see
        # https://medium.com/contentsquare-engineering-blog/multithreading-vs-multiprocessing-in-python-ece023ad55a
        self.event_listener = threading.Event()
        self.mpi_listener_thread = threading.Thread(
            name="mpi_listener",
            target=self.mpi_broadcast_listener,
            args=(self.event_listener, MPI_PROBE_INTERVAL),
        )
        self.mpi_listener_thread.setDaemon(True)
        self.mpi_listener_thread.start()

    def mpi_broadcast_listener(self, event_listener, wait_time):
        logger.debug("Setting up mpi_broadcast_listener")
        while not self.event_listener.isSet():
            self.event_listener.wait(timeout=wait_time)
            # TODO: I have not found a proper way to interrupt an MPI-recv command,
            # hence the current implemenatation polls periodically for new
            # messages
            probe = False
            probe = self.comm.iprobe(source=MPI.ANY_SOURCE, tag=MPI.ANY_TAG)

            if probe:
                status = MPI.Status()
                data = self.comm.recv(
                    source=MPI.ANY_SOURCE, tag=MPI.ANY_TAG, status=status
                )
                tag = status.Get_tag()
                source = status.Get_source()
                logger.info(
                    "RECEIVING: Process %d received msg from process %d with tag '%s': %s",
                    self.rank,
                    source,
                    tags(tag).name,
                    data,
                )

                if isinstance(data, dict):
                    result = data.get(SUBANALYSIS_RESULT)
                    # only shut this process down if another subanalysis reported
                    # a TRUE or FALSE result.
                    if result is not None and result == results.SUCCESS.name:
                        logger.info(
                            "RECEIVING: received signal for shutting this process "
                            "down."
                        )
                        self.interrupt_mpi_listener()
                        self.shutdown_processes()

            logger.debug("event set? %s", event_listener.isSet())

    def broadcast_except_to_self(self, data, tag):
        for i in range(0, self.size):
            if self.rank != i:
                self.comm.send(data, dest=i, tag=tag)

    def parse_data(self, data):
        status = None

        result = [x for x in data if x.startswith("Verification result: ")]
        if len(result) == 1:
            result = result[0][21:].strip()
            if result.startswith("TRUE") or result.startswith("FALSE"):
                status = results.SUCCESS
            else:
                status = results.UNKNOWN
        elif len(result) > 1:
            raise ValueError("Unexpected number of result lines received")

        if status is None:
            status = results.EXCEPTION

        return {SUBANALYSIS_RESULT: status.name}

    def publish_completion_of_subprocess(self, proc_output):
        data = self.parse_data(proc_output)
        logger.info("SENDING: Process %d broadcasts msg: %s", self.rank, data)
        self.broadcast_except_to_self(data, tags.DONE.value)
        self.interrupt_mpi_listener()
        self.mpi_listener_thread.join()

    def interrupt_mpi_listener(self):
        self.event_listener.set()

    def shutdown_processes(self):
        self.shutdown_requested = True
        logger.debug("Sending signal to shutdown processes")
        if self.process is not None and self.process.poll() is None:
            self.process.send_signal(signal.SIGINT)

    def print_self_info(self):
        """Print an info about the proccesor name, the rank of the executed process, and
        the number of total processes."""
        logger.info(
            "Executing process on '%s' (rank %d of %d)",
            self.name,
            self.rank,
            self.size,
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
            logger.info(
                "Starting new CPAchecker instance performing the analysis: %s",
                self.analysis_param[ANALYSIS],
            )

            if not os.path.isdir(self.analysis_param[OUTPUT_PATH]):
                os.makedirs(self.analysis_param[OUTPUT_PATH])

            logger.info("Executing cmd: %s", cmdline)
            with open(self.analysis_param[LOGFILE], "w+", buffering=1) as outputfile:
                with subprocess.Popen(
                    cmdline,
                    stdout=subprocess.PIPE,
                    stderr=subprocess.STDOUT,
                    universal_newlines=True,
                ) as self.process:

                    try:
                        proc_stdout, _ = self.process.communicate()
                    except KeyboardInterrupt:
                        mpi.interrupt_mpi_listener()

                    if proc_stdout is not None:
                        outputfile.write(proc_stdout)
                        proc_output = proc_stdout.split("\n")

            logger.info("Process returned with status code %d", self.process.returncode)
            if not self.shutdown_requested:
                self.shutdown_requested = True
                self.publish_completion_of_subprocess(proc_output)
                logger.debug("Rank %d is about to shut itself down", self.rank)

    def prepare_cmdline(self):
        logger.debug("Running analysis with number: %d", self.rank)
        analysis_args = None
        if self.rank <= len(self.input_args) - 1:
            analysis_args = self.input_args.get("Analysis_{}".format(self.rank))
        if analysis_args is None:
            logger.info("No arguments for the analysis found.")
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
            logger.info(
                "No network parameters received. The process was executed "
                "on the main node only. The result files are hence "
                "already in their correct location."
            )
        else:
            # Compare the local ip address with the main node ip.
            # If they differ, create an ssh-connection and push all result files to the
            # main node. Otherwise, the process is already executed on the main node, so
            # the result files are already in the correct place.
            main_node_ip_address = self.main_node_network_config.get(
                "main_node_ipv4_address"
            )
            try:
                local_ip_address = socket.gethostbyname(os.environ.get("USER"))
            except socket.gaierror:
                # hostfile was provided, but ip address could not be found
                # process is most likely running on localhost
                local_ip_address = socket.gethostbyname(socket.gethostname())

            logger.debug("Local ip address: %s ", local_ip_address)

            if (
                main_node_ip_address is not None
                and main_node_ip_address != local_ip_address
            ):
                output = os.path.abspath(self.analysis_param[OUTPUT_PATH])
                if not os.path.exists(output):
                    logger.warning(
                        "Found no output that can be copied to the main node"
                        "Skipping this step."
                    )
                else:
                    logger.info("Copying result files via scp to the main-node")
                    scp_cmd = [
                        "scp",
                        "-r",
                        output,
                        "{}@{}:{}".format(
                            self.main_node_network_config["user_name_main_node"],
                            main_node_ip_address,
                            os.path.join(
                                self.main_node_network_config[
                                    "project_location_main_node"
                                ],
                                self.analysis_param[OUTPUT_PATH],
                            ),
                        ),
                    ]
                    logger.debug("Command for scp: %s", scp_cmd)
                    scp_proc = subprocess.run(scp_cmd)
                    logger.debug(
                        "Process for copying the output back to the main node "
                        "completed with status code %d",
                        scp_proc.returncode,
                    )

            else:
                logger.info(
                    "The current process is executed on the main node. The result "
                    "files are already in the correct place."
                )


def handle_signal(signum, frame):
    mpi.interrupt_mpi_listener()
    mpi.shutdown_processes()


signal.signal(signal.SIGINT, handle_signal)
signal.signal(signal.SIGTERM, handle_signal)


def main():
    mpi = MPIMain(sys.argv[1:])
    mpi.print_self_info()
    if len(mpi.input_args) == 0:
        logger.warning("No input received. Exiting with status code 0")
        sys.exit(0)

    mpi.prepare_cmdline()
    if mpi.run_subanalysis is True:
        mpi.execute_verifier()
        mpi.push_results_to_master()
    else:
        logger.info("Nothing to run. Exiting with status code 0")
    sys.exit(0)


if __name__ == "__main__":
    main()
