#!/usr/bin/env python3

# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

import getpass
import glob
import logging
import os
import subprocess
import sys

sys.dont_write_bytecode = True  # prevent creation of .pyc files
cpachecker_dir = os.path.join(os.path.dirname(__file__), os.pardir)
for egg in glob.glob(os.path.join(cpachecker_dir, "lib", "python-benchmark", "*.whl")):
    sys.path.insert(0, egg)

from benchmark.vcloudbenchmarkbase import VcloudBenchmarkBase  # noqa E402
from benchexec import __version__
import benchexec.benchexec
import benchexec.model
import benchexec.tooladapter
import benchexec.tools
import benchexec.util
from benchmark import vcloudutil

# Add ./benchmark/tools to __path__ of benchexec.tools package
# such that additional tool-wrapper modules can be placed in this directory.
benchexec.tools.__path__ = [
    os.path.join(os.path.dirname(__file__), "benchmark", "tools")
] + benchexec.tools.__path__

_ROOT_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), os.pardir))


def download_required_jars():
    # install cloud and dependencies
    subprocess.run(
        ["ant", "resolve-benchmark-dependencies"],
        cwd=_ROOT_DIR,
        shell=vcloudutil.is_windows(),  # noqa: S602
    )


class Benchmark(VcloudBenchmarkBase):
    """
    An extension of BenchExec for use with CPAchecker
    that supports executing the benchmarks in the VerifierCloud.
    """

    DEFAULT_OUTPUT_PATH = "test/results/"

    def add_vcloud_args(self, vcloud_args):
        vcloud_args.add_argument(
            "--cloud",
            dest="cloud",
            action="store_true",
            help="Use VerifierCloud to execute benchmarks.",
        )

        vcloud_args.add_argument(
            "--cloudUser",
            dest="cloudUser",
            metavar="USER[:PWD]",
            help="The user (and password) for the VerifierCloud (if using the web interface).",
        )

        vcloud_args.add_argument(
            "--revision",
            dest="revision",
            metavar="(tags/<tag name>|branch_name)[:(HEAD|head|<revision number>)]",
            help="The revision of CPAchecker to use (if using the web interface of the VerifierCloud).",
        )

        vcloud_args.add_argument(
            "--cloudSubmissionThreads",
            dest="cloud_threads",
            default=5,
            type=int,
            help="The number of threads used for parallel run submission (if using the web interface of the VerifierCloud).",
        )

        vcloud_args.add_argument(
            "--cloudPollInterval",
            dest="cloud_poll_interval",
            metavar="SECONDS",
            default=5,
            type=int,
            help="The interval in seconds for polling results from the server (if using the web interface of the VerifierCloud).",
        )
        # add arguments from the base class.
        super(Benchmark, self).add_vcloud_args(vcloud_args)

    def get_param_name(self, pname):
        return "--" + pname

    def validate_local_checkout(self):
        """
        Validates the local Git checkout to ensure it's clean and has no unpushed changes.
        If valid, retrieves the latest commit hash.
        """
        try:
            result = subprocess.run(
                ["git", "status", "--porcelain", "--untracked-files=no"],
                cwd=_ROOT_DIR,
                text=True,
                capture_output=True,
            )
            if result.returncode != 0:
                sys.exit(
                    f"Error: Failed to check the Git status of the repository. "
                    f"Git output:\n{result.stderr}")
            if result.stdout.strip():
                sys.exit(
                    "Error: Only revisions in the CPAchecker repository are supported when "
                    "benchmarking CPAchecker remotely via Benchcloud. "
                    "Please either specify an existing revision to be benchmarked or commit and push your changes."
                )

            branch_result = subprocess.run(
                ["git", "rev-parse", "--abbrev-ref", "HEAD"],
                cwd=_ROOT_DIR,
                text=True,
                capture_output=True,
            )
            if branch_result.returncode != 0:
                sys.exit(
                    f"Error: Failed to determine the current Git branch. "
                    f"Git output:\n{branch_result.stderr}")

            upstream_check = subprocess.run(
                ["git", "rev-parse", "HEAD", "HEAD@{u}"],
                cwd=_ROOT_DIR,
                text=True,
                capture_output=True,
            )

            if upstream_check.returncode != 0:
                logging.warning(
                    "Warning: No upstream branch configured for the current branch. "
                    "Benchmarking will proceed using the current revision hash, "
                    "but ensure this revision is accessible in your repository."
                )
            else:
                local_commit, upstream_commit = upstream_check.stdout.strip().splitlines()
                if local_commit != upstream_commit:
                    logging.warning(
                        "Warning: Current branch and upstream branch differ. "
                        "Benchmarking may fail if the local revision has not been pushed."
                    )

            revision_result = subprocess.run(
                ["git", "rev-parse", "HEAD"],
                cwd=_ROOT_DIR,
                text=True,
                capture_output=True,
            )
            if revision_result.returncode != 0 or not revision_result.stdout.strip():
                sys.exit(
                    f"Error: Could not determine the latest Git revision. "
                    f"Please explicitly pass --revision to specify the revision you want to benchmark."
                    f"Git output:\n{revision_result.stderr}"
                )

            return revision_result.stdout.strip()

        except FileNotFoundError:
            sys.exit("Error: Git is not installed or unavailable on this system.")

    def load_executor(self):
        webclient = False
        if getpass.getuser() == "root":
            logging.warning(
                "Benchmarking as root user is not advisable! Please execute this script as normal user!"
            )

        if not self.config.revision:
            latest_commit = self.validate_local_checkout()
            self.config.revision = latest_commit

        if self.config.cloud:
            if self.config.cloudMaster and "http" in self.config.cloudMaster:
                webclient = True
                import benchmark.webclient_executor as executor
            else:
                download_required_jars()

                import benchmark.benchmarkclient_executor as executor

                executor.set_vcloud_jar_path(
                    os.path.join(_ROOT_DIR, "lib", "java-benchmark", "vcloud.jar")
                )

            logging.debug(
                "This is CPAchecker's benchmark.py (based on benchexec %s) "
                "using the VerifierCloud %s API.",
                __version__,
                "HTTP" if webclient else "internal",
            )
        else:
            executor = super(Benchmark, self).load_executor()

        if not webclient:
            original_load_function = benchexec.model.load_tool_info

            def build_cpachecker_before_load(tool_name, *args, **kwargs):
                if tool_name == "cpachecker":
                    # This duplicates the logic from our tool-info module,
                    # but we cannot call it here.
                    # Note that base_dir can be different from cpachecker_dir!
                    tool_locator = benchexec.tooladapter.create_tool_locator(
                        self.config
                    )
                    script = tool_locator.find_executable("cpachecker", subdir="bin")
                    base_dir = os.path.join(os.path.dirname(script), os.path.pardir)
                    build_file = os.path.join(base_dir, "build.xml")
                    if (
                        os.path.exists(build_file)
                        and subprocess.run(
                            ["ant", "-q", "jar"],
                            cwd=base_dir,
                            shell=vcloudutil.is_windows(),  # noqa: S602
                        ).returncode
                    ):
                        sys.exit(
                            "Failed to build CPAchecker, please fix the build first."
                        )

                return original_load_function(tool_name, *args, **kwargs)

            # Monkey-patch BenchExec to build CPAchecker before loading the tool-info
            # module (https://gitlab.com/sosy-lab/software/cpachecker/issues/549)
            benchexec.model.load_tool_info = build_cpachecker_before_load

        return executor

    def check_existing_results(self, benchmark):
        if not self.config.reprocessResults:
            super(Benchmark, self).check_existing_results(benchmark)


if __name__ == "__main__":
    benchexec.benchexec.main(Benchmark())
