# This file is part of BenchExec, a framework for reliable benchmarking:
# https://github.com/sosy-lab/benchexec
#
# SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

import collections
import logging
import os
import sys

import benchexec.util

sys.dont_write_bytecode = True  # prevent creation of .pyc files

RESULT_FILES_COUNT = "resultFilesCount"
RESULT_FILE_NAMES = "resultFileNames"


def check_result_files(
    values, actual_files, run_identifier, *, key_prefix="", include_file=None
):
    """Check received files against run metadata and log count mismatches.

    key_prefix supports the executor's prefixed metadata keys. include_file
    filters expected names to exclude files handled separately by the caller.
    """
    count_key = key_prefix + RESULT_FILES_COUNT
    names_key = key_prefix + RESULT_FILE_NAMES
    if count_key not in values:
        logging.debug(
            "'%s' not found in run information for run %s.",
            count_key,
            run_identifier,
        )
        return

    expected_count = int(values[count_key])
    actual_files = set(actual_files)
    actual_count = len(actual_files)
    expected_files = None
    if names_key in values:
        expected_files = {
            name
            for name in values[names_key].split(",")
            if include_file is None or include_file(name)
        }
        expected_count = len(expected_files)

    if expected_count != actual_count:
        if expected_files is not None:
            logging.warning(
                "Number of result files received (%d) does not match the expected count (%d) for run %s. "
                "Missing files: %s",
                actual_count,
                expected_count,
                run_identifier,
                sorted(expected_files - actual_files),
            )
        else:
            logging.warning(
                "Number of result files received (%d) does not match the expected count (%d) for run %s.",
                actual_count,
                expected_count,
                run_identifier,
            )
    else:
        logging.debug(
            "Number of result files received (%d) matches the expected count (%d) for run %s.",
            actual_count,
            expected_count,
            run_identifier,
        )


def parse_vcloud_run_result(values):
    result_values = collections.OrderedDict()

    def parse_time_value(s):
        if s[-1] != "s":
            raise ValueError(f'Cannot parse "{s}" as a time value.')
        return float(s[:-1])

    def set_exitcode(new):
        if "exitcode" in result_values:
            old = result_values["exitcode"]
            assert old == new, (
                f"Inconsistent exit codes {old} and {new} from VerifierCloud"
            )
        else:
            result_values["exitcode"] = new

    for key, value in values:
        value = value.strip()
        if key in ["cputime", "walltime"]:
            result_values[key] = parse_time_value(value)
        elif key == "memory":
            result_values["memory"] = int(value.strip("B"))
        elif key == "exitcode":
            set_exitcode(benchexec.util.ProcessExitCode.from_raw(int(value)))
        elif key == "returnvalue":
            set_exitcode(benchexec.util.ProcessExitCode.create(value=int(value)))
        elif key == "exitsignal":
            set_exitcode(benchexec.util.ProcessExitCode.create(signal=int(value)))
        elif key in [
            "host",
            "terminationreason",
            "cpuCores",
            "memoryNodes",
            "starttime",
        ] or key.startswith(("blkio-", "cpuenergy", "energy-", "cputime-cpu")):
            result_values[key] = value
        elif key not in ["command", "timeLimit", "coreLimit", "memoryLimit"]:
            result_values["vcloud-" + key] = value

    return result_values


def parse_frequency_value(s):
    # Contrary to benchexec.util.parse_frequency_value, this handles float values.
    if not s:
        return s
    s = s.strip()
    pos = len(s)
    while pos and not s[pos - 1].isdigit():
        pos -= 1
    number = float(s[:pos])
    unit = s[pos:].strip()
    if not unit or unit == "Hz":
        return int(number)
    elif unit == "kHz":
        return int(number * 1000)
    elif unit == "MHz":
        return int(number * 1000 * 1000)
    elif unit == "GHz":
        return int(number * 1000 * 1000 * 1000)
    else:
        raise ValueError(f"unknown unit: {unit} (allowed are Hz, kHz, MHz, and GHz)")


def is_windows():
    return os.name == "nt"


def force_linux_path(path):
    if is_windows():
        return path.replace("\\", "/")
    return path
