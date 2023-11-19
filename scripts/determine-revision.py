#!/usr/bin/env python3

# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2007-2023 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

import subprocess
import sys

sys.dont_write_bytecode = True  # prevent creation of .pyc files


def determineRevision(dir_path):
    """
    Determine the revision of the given directory in a version control system.
    """
    # Check for SVN repository
    try:
        svnProcess = subprocess.run(
            ["svnversion", "--committed", dir_path],
            env={"LANG": "C"},
            capture_output=True,
            text=True,
        )
        stdout = svnProcess.stdout.strip().split(":")[-1]
        if not (
            svnProcess.returncode
            or svnProcess.stderr
            or (stdout == "exported")
            or (stdout == "Unversioned directory")
        ):
            return stdout
    except OSError:
        pass

    # Check for git-svn repository
    try:
        # This will silently perform the migration from older git-svn directory layout.
        # Otherwise, the migration may be performed by the next git svn invocation,
        # producing nonempty stderr.
        subprocess.call(["git", "svn", "migrate"], stderr=subprocess.DEVNULL)

        gitProcess = subprocess.run(
            ["git", "svn", "find-rev", "HEAD"],
            env={"LANG": "C"},
            cwd=dir_path,
            capture_output=True,
            text=True,
        )
        stdout = gitProcess.stdout.strip()
        if not (gitProcess.returncode or gitProcess.stderr) and stdout:
            return stdout + ("M" if _isGitRepositoryDirty(dir_path) else "")

        # Check for git repository
        gitProcess = subprocess.run(
            ["git", "log", "-1", "--pretty=format:%h", "--abbrev-commit"],
            env={"LANG": "C"},
            cwd=dir_path,
            capture_output=True,
            text=True,
        )
        stdout = gitProcess.stdout.strip()
        if not (gitProcess.returncode or gitProcess.stderr) and stdout:
            return stdout + ("+" if _isGitRepositoryDirty(dir_path) else "")
    except OSError:
        pass
    return None


def _isGitRepositoryDirty(dir_path):
    gitProcess = subprocess.run(
        ["git", "status", "--porcelain"],
        env={"LANG": "C"},
        cwd=dir_path,
        capture_output=True,
    )
    if not (gitProcess.returncode or gitProcess.stderr):
        return bool(gitProcess.stdout)  # Dirty if stdout is non-empty
    return None


if __name__ == "__main__":
    if len(sys.argv) > 2:
        sys.exit("Unsupported command-line parameters.")

    dir_path = sys.argv[1] if len(sys.argv) > 1 else "."

    revision = determineRevision(dir_path)
    if revision:
        print(revision)
    else:
        sys.exit(f"Directory {dir_path} is not a supported version control checkout.")
