# This file is part of BenchExec, a framework for reliable benchmarking:
# https://github.com/sosy-lab/benchexec
#
# SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

import collections
import os
import sys

import benchexec.util

sys.dont_write_bytecode = True  # prevent creation of .pyc files

RESULT_FILE_NAMES = "resultFileNames"


def find_missing_result_files(expected_names, actual_files):
    """Determine which of the expected result files of a run were not received.

    @param expected_names: the value of the run attribute RESULT_FILE_NAMES, i.e.,
        the comma-separated names of the result files that the worker collected,
        or None if the worker did not report it
    @param actual_files: the names of the received result files (any iterable)
    @return: the set of missing file names (empty if all files were received),
        or None if the check is not possible.
    """
    if expected_names is None:
        return None
    expected_files = set(expected_names.split(","))
    expected_files.discard("")
    return expected_files.difference(actual_files)


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
