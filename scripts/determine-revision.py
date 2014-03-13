#!/usr/bin/env python

"""
CPAchecker is a tool for configurable software verification.
This file is part of CPAchecker.

Copyright (C) 2007-2014  Dirk Beyer
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

# prepare for Python 3
from __future__ import absolute_import, print_function, unicode_literals

import subprocess
import sys

sys.dont_write_bytecode = True # prevent creation of .pyc files

import benchmark.util as Util

def determineRevision(dir):
    """
    Determine the revision of the given directory in a version control system.
    """
    # Check for SVN repository
    try:
        svnProcess = subprocess.Popen(['svnversion', dir], env={'LANG': 'C'}, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        (stdout, stderr) = svnProcess.communicate()
        stdout = Util.decodeToString(stdout).strip()
        if not (svnProcess.returncode or stderr or (stdout == 'exported')):
            return stdout
    except OSError:
        pass

    # Check for git-svn repository
    try:
        gitProcess = subprocess.Popen(['git', 'svn', 'find-rev', 'HEAD'], env={'LANG': 'C'}, cwd=dir, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        (stdout, stderr) = gitProcess.communicate()
        stdout = Util.decodeToString(stdout).strip()
        if not (gitProcess.returncode or stderr) and stdout:
            return stdout + ('M' if _isGitRepositoryDirty(dir) else '')

        # Check for git repository
        gitProcess = subprocess.Popen(['git', 'log', '-1', '--pretty=format:%h', '--abbrev-commit'], env={'LANG': 'C'}, cwd=dir, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        (stdout, stderr) = gitProcess.communicate()
        stdout = Util.decodeToString(stdout).strip()
        if not (gitProcess.returncode or stderr) and stdout:
            return stdout + ('+' if _isGitRepositoryDirty(dir) else '')
    except OSError:
        pass
    return None


def _isGitRepositoryDirty(dir):
    gitProcess = subprocess.Popen(['git', 'status', '--porcelain'], env={'LANG': 'C'}, cwd=dir, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    (stdout, stderr) = gitProcess.communicate()
    if not (gitProcess.returncode or stderr):
        return True if stdout else False  # True if stdout is non-empty
    return None


if __name__ == "__main__":
    if len(sys.argv) > 2:
        sys.exit('Unsupported command-line parameters.')

    dir = '.'
    if len(sys.argv) > 1:
        dir = sys.argv[1]

    revision = determineRevision(dir)
    if revision:
        print(revision)
    else:
        sys.exit('Directory {0} is not a supported version control checkout.'.format(dir))