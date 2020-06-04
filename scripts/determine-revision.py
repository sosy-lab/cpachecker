#!/usr/bin/env python3

# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

import subprocess
import sys
import os

sys.dont_write_bytecode = True  # prevent creation of .pyc files


def determineRevision(dir_path):
    """
    Determine the revision of the given directory in a version control system.
    """
    # Check for SVN repository
    try:
        svnProcess = subprocess.Popen(
            ["svnversion", "--committed", dir_path],
            env={"LANG": "C"},
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        (stdout, stderr) = svnProcess.communicate()
        stdout = _decode_to_string(stdout).strip()
        stdout = stdout.split(":")[-1]
        if not (
            svnProcess.returncode
            or stderr
            or (stdout == "exported")
            or (stdout == "Unversioned directory")
        ):
            return stdout
    except OSError:
        pass

    # Check for git-svn repository
    try:
        with open(os.devnull, "wb") as DEVNULL:
            # This will silently perform the migration from older git-svn directory layout.
            # Otherwise, the migration may be performed by the next git svn invocation,
            # producing nonempty stderr.
            subprocess.call(["git", "svn", "migrate"], stderr=DEVNULL)

        gitProcess = subprocess.Popen(
            ["git", "svn", "find-rev", "HEAD"],
            env={"LANG": "C"},
            cwd=dir_path,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        (stdout, stderr) = gitProcess.communicate()
        stdout = _decode_to_string(stdout).strip()
        if not (gitProcess.returncode or stderr) and stdout:
            return stdout + ("M" if _isGitRepositoryDirty(dir_path) else "")

        # Check for git repository
        gitProcess = subprocess.Popen(
            ["git", "log", "-1", "--pretty=format:%h", "--abbrev-commit"],
            env={"LANG": "C"},
            cwd=dir_path,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        (stdout, stderr) = gitProcess.communicate()
        stdout = _decode_to_string(stdout).strip()
        if not (gitProcess.returncode or stderr) and stdout:
            return stdout + ("+" if _isGitRepositoryDirty(dir_path) else "")
    except OSError:
        pass
    return None


def _isGitRepositoryDirty(dir_path):
    gitProcess = subprocess.Popen(
        ["git", "status", "--porcelain"],
        env={"LANG": "C"},
        cwd=dir_path,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    (stdout, stderr) = gitProcess.communicate()
    if not (gitProcess.returncode or stderr):
        return True if stdout else False  # True if stdout is non-empty
    return None


def _decode_to_string(to_decode):
    """
    This function is needed for Python 3,
    because a subprocess can return bytes instead of a string.
    """
    try:
        return to_decode.decode("utf-8")
    except AttributeError:  # bytesToDecode was of type string before
        return to_decode


if __name__ == "__main__":
    if len(sys.argv) > 2:
        sys.exit("Unsupported command-line parameters.")

    dir_path = "."
    if len(sys.argv) > 1:
        dir_path = sys.argv[1]

    revision = determineRevision(dir_path)
    if revision:
        print(revision)
    else:
        sys.exit(
            "Directory {0} is not a supported version control checkout.".format(
                dir_path
            )
        )
