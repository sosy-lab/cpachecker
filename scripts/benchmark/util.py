"""
CPAchecker is a tool for configurable software verification.
This file is part of CPAchecker.

Copyright (C) 2007-2019  Dirk Beyer
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

import collections
import os
import sys

import benchexec.util

sys.dont_write_bytecode = True  # prevent creation of .pyc files


def parse_vcloud_run_result(values):
    result_values = collections.OrderedDict()

    def parse_time_value(s):
        if s[-1] != "s":
            raise ValueError('Cannot parse "{0}" as a time value.'.format(s))
        return float(s[:-1])

    def set_exitcode(new):
        if "exitcode" in result_values:
            old = result_values["exitcode"]
            assert (
                old == new
            ), "Inconsistent exit codes {} and {} from VerifierCloud".format(old, new)
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
        elif (
            key in ["host", "terminationreason", "cpuCores", "memoryNodes", "starttime"]
            or key.startswith("blkio-")
            or key.startswith("cpuenergy")
            or key.startswith("energy-")
            or key.startswith("cputime-cpu")
        ):
            result_values[key] = value
        elif key not in ["command", "timeLimit", "coreLimit", "memoryLimit"]:
            result_values["vcloud-" + key] = value

    return result_values


def parse_frequency_value(s):
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
    elif unit == "KHz":
        return int(number * 1000)
    elif unit == "MHz":
        return int(number * 1000 * 1000)
    elif unit == "GHz":
        return int(number * 1000 * 1000 * 1000)
    else:
        raise ValueError(
            "unknown unit: {} (allowed are Hz, KHz, MHz, and GHz)".format(unit)
        )


def is_windows():
    return os.name == "nt"


def force_linux_path(path):
    if is_windows():
        return path.replace("\\", "/")
    return path
