#!/usr/bin/python3

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
LOGFILE = "logfile"
OUTPUT_PATH = "results"


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

    main_node_network_config = None

    def __init__(self, argv):
        logging.basicConfig(
            format="%(asctime)s - %(levelname)s:  %(message)s",
            datefmt="%Y-%d-%m %I:%M:%S",
            level=logging.DEBUG,
        )  # TODO change logging level to INFO

        # Name of the processor
        self.name = MPI.Get_processor_name()

        # Number of total processes
        self.size = self.comm.Get_size()
        # Rank of the this process
        self.rank = self.comm.Get_rank()

        try:
            logging.debug("Input of user args: %s", str(argv))
            opts, args = getopt.getopt(argv, "di:", ["input="])
        except getopt.GetoptError:
            logging.critical(
                "Unable to parse user input. Usage: %s -d -i <input>", __file__
            )
            sys.exit(2)

        for opt, arg in opts:
            if opt == "-d":
                logging.basicConfig(level=logging.DEBUG)
            elif opt in ("-i", "--input"):
                if isinstance(arg, str):
                    self.input_args = eval(arg)
                elif isinstance(arg, dict):
                    self.input_args = arg
                else:
                    logging.critical("Input has an invalid type: %s", type(arg))
                    sys.exit(2)

        self.main_node_network_config = self.input_args.get(
            "main_node_network_settings"
        )
        if self.main_node_network_config is not None:
            aws_main_ip = os.environ.get("AWS_BATCH_JOB_MAIN_NODE_PRIVATE_IPV4_ADDRESS")
            if (
                aws_main_ip is not None
                and self.main_node_network_config["main_node_ipv4_address"]
                == aws_main_ip
            ):
                logging.critical("Inconsistent ip addresses for main node received.\n")
                sys.exit(2)

        logging.debug(json.dumps(self.input_args, sort_keys=True, indent=4))

    def print_self_info(self):
        """Print an info about the proccesor name, the rank of the executed process, and
        the number of total processes."""
        logging.info(
            "Executing process on '{}' (rank {} of {})".format(
                self.name, self.rank, self.size
            )
        )

    def execute_verifier(self):
        logging.debug("Running script %s from dir '%s'", __file__, os.getcwd())
        cmdline = self.analysis_param[CMDLINE]
        if cmdline is None:
            logging.info(
                "Cmdline does not contain any input. Will not do anything (Rank %d).",
                self.rank,
            )
        else:
            logging.info("executing cmd: %s", cmdline)
            # Redirect all output from the errorstream in the child CPAchecker
            # instances, such that the output log stays consistent
            process = subprocess.run(cmdline, stderr=sys.stdout.buffer)
            logging.info("Process exited with status code %d", process.returncode)

    def prepare_cmdline(self):
        logging.info("Running analysis with number: %d", self.rank)
        if self.rank <= len(self.input_args) - 1:
            analysis_args = self.input_args.get("Analysis_{}".format(self.rank))
        if analysis_args is None:
            logging.warning("No arguments for the analysis found.")
        else:

            def replace_escape_chars(d):
                for key, value in d.items():
                    if isinstance(value, str):
                        d[key] = value.replace("\\", "")
                    elif isinstance(value, list):
                        d[key] = [w.replace("\\", "") for w in value]

            self.analysis_param = copy.deepcopy(analysis_args)
            replace_escape_chars(self.analysis_param)

            logging.debug("Running analysis: %s", self.analysis_param[ANALYSIS])
            logging.debug("Running cmd: %s", self.analysis_param[CMDLINE])
            logging.debug("Writing log in file: %s", self.analysis_param[LOGFILE])
            logging.debug("Storing output in dir: %s", self.analysis_param[OUTPUT_PATH])

    def push_results_to_master(self):
        if self.main_node_network_config is None:
            logging.info(
                "Already on main node. Result files are already "
                "in the correct location."
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
                logging.info("Copy result files via scp to the main-node")
                scp_cmd = [
                    "scp",
                    "-r",
                    self.analysis_param[OUTPUT_PATH],
                    "{}@{}:{}/{}".format(
                        self.main_node_network_config["user_name_main_node"],
                        self.main_node_network_config["main_node_ipv4_address"],
                        self.main_node_network_config["project_location_main_node"],
                        self.analysis_param[OUTPUT_PATH],
                    ),
                ]
                subprocess.run(scp_cmd)


def main():
    mpi = MPIMain(sys.argv[1:])
    mpi.print_self_info()
    mpi.prepare_cmdline()
    mpi.execute_verifier()


if __name__ == "__main__":
    main()
