#!/usr/bin/python3

import copy
import getopt
import json
import logging
from mpi4py import MPI
import os
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
            logging.debug("Input of user args:", str(argv))
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
                    logging.critical("Input has an invalid type: ", type(arg))
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
            logging.info("executing cmd:", cmdline)
            # Redirect all output from the errorstream in the child CPAchecker
            # instances, such that the output log stays consistent
            process = subprocess.run(cmdline, stderr=sys.stdout.buffer)
            logging.debug("output: ", process.stdout)
            logging.info("Process exited with status code ", process.returncode)

    def prepare_cmdline(self):
        logging.info("Running analysis with number:", self.rank)
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

            logging.debug("Running analysis:", self.analysis_param[ANALYSIS])
            logging.debug("Running cmd:", self.analysis_param[CMDLINE])
            logging.debug("Writing log in file:", self.analysis_param[LOGFILE])
            logging.debug("Storing output in dir:", self.analysis_param[OUTPUT_PATH])


def main():
    mpi = MPIMain(sys.argv[1:])
    mpi.print_self_info()
    mpi.prepare_cmdline()
    mpi.execute_verifier()


if __name__ == "__main__":
    main()
