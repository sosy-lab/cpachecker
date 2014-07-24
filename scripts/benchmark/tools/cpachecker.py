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

import logging
import subprocess
import sys
import string
import os
import re

sys.dont_write_bytecode = True # prevent creation of .pyc files

if __name__ == "__main__":
    sys.path.append(os.path.join(os.path.dirname(__file__), os.path.pardir, os.path.pardir))

import benchmark.result as result
import benchmark.util as Util
import benchmark.tools.template

REQUIRED_PATHS = [
                  "lib/java/runtime",
                  "lib/*.jar",
                  "lib/native/x86_64-linux",
                  "scripts",
                  "cpachecker.jar",
                  "config",
                  ]

class Tool(benchmark.tools.template.BaseTool):

    def getExecutable(self):
        executable = Util.findExecutable('cpa.sh', 'scripts/cpa.sh')
        executableDir = os.path.join(os.path.dirname(executable), os.path.pardir)
        if os.path.isdir(os.path.join(executableDir, 'src')):
            self._buildCPAchecker(executableDir)
        if not os.path.isfile(os.path.join(executableDir, "cpachecker.jar")):
            logging.warning("Required JAR file for CPAchecker not found in {0}.".format(executableDir))
        return executable


    def _buildCPAchecker(self, executableDir):
        logging.debug('Building CPAchecker in directory {0}.'.format(executableDir))
        ant = subprocess.Popen(['ant', '-lib', 'lib/java/build', '-q', 'jar'], cwd=executableDir, shell=Util.isWindows())
        (stdout, stderr) = ant.communicate()
        if ant.returncode:
            sys.exit('Failed to build CPAchecker, please fix the build first.')


    def getProgrammFiles(self, executable):
        executableDir = os.path.join(os.path.dirname(executable), os.path.pardir)
        return Util.flatten(Util.expandFileNamePattern(path, executableDir) for path in REQUIRED_PATHS)


    def getWorkingDirectory(self, executable):
        return os.curdir


    def getVersion(self, executable):
        try:
            process = subprocess.Popen([executable, '-help'], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            (stdout, stderr) = process.communicate()
        except OSError as e:
            logging.warning('Cannot run CPAchecker to determine version: {0}'.format(e.strerror))
            return ''
        if stderr:
            logging.warning('Cannot determine CPAchecker version, error output: {0}'.format(Util.decodeToString(stderr)))
            return ''
        if process.returncode:
            logging.warning('Cannot determine CPAchecker version, exit code {0}'.format(process.returncode))
            return ''
        stdout = Util.decodeToString(stdout)
        line = stdout.splitlines()[0]
        line = line.replace('CPAchecker' , '')
        line = line.split('(')[0]
        return line.strip()

    def getName(self):
        return 'CPAchecker'


    def getCmdline(self, executable, options, sourcefiles, propertyfile=None):
        if ("-stats" not in options):
            options = options + ["-stats"]
        spec = ["-spec", propertyfile] if propertyfile is not None else []
        return [executable] + options + spec + sourcefiles


    def getStatus(self, returncode, returnsignal, output, isTimeout):
        """
        @param returncode: code returned by CPAchecker
        @param returnsignal: signal, which terminated CPAchecker
        @param output: the output of CPAchecker
        @return: status of CPAchecker after executing a run
        """

        def isOutOfNativeMemory(line):
            return ('std::bad_alloc'             in line # C++ out of memory exception (MathSAT)
                 or 'Cannot allocate memory'     in line
                 or 'Native memory allocation (malloc) failed to allocate' in line # JNI
                 or line.startswith('out of memory')     # CuDD
                 )

        if returnsignal == 0 and returncode > 128:
            # shells sets return code to 128+signal when a signal is received
            returnsignal = returncode - 128

        if returnsignal != 0:
            if returnsignal == 6:
                status = 'ABORTED'
            elif returnsignal == 9 and isTimeout:
                status = 'TIMEOUT'
            elif returnsignal == 11:
                status = 'SEGMENTATION FAULT'
            elif returnsignal == 15:
                status = 'KILLED'
            else:
                status = 'KILLED BY SIGNAL '+str(returnsignal)

        elif returncode != 0:
            status = 'ERROR ({0})'.format(returncode)

        else:
            status = ''

        for line in output.splitlines():
            if 'java.lang.OutOfMemoryError' in line:
                status = 'OUT OF JAVA MEMORY'
            elif isOutOfNativeMemory(line):
                status = 'OUT OF NATIVE MEMORY'
            elif 'There is insufficient memory for the Java Runtime Environment to continue.' in line \
                    or 'cannot allocate memory for thread-local data: ABORT' in line:
                status = 'OUT OF MEMORY'
            elif 'SIGSEGV' in line:
                status = 'SEGMENTATION FAULT'
            elif ((returncode == 0 or returncode == 1)
                    and ('Exception' in line or 'java.lang.AssertionError' in line)
                    and not line.startswith('cbmc')): # ignore "cbmc error output: ... Minisat::OutOfMemoryException"
                status = 'ASSERTION' if 'java.lang.AssertionError' in line else 'EXCEPTION'
            elif 'Could not reserve enough space for object heap' in line:
                status = 'JAVA HEAP ERROR'
            elif line.startswith('Error: ') and not status.startswith('ERROR'):
                status = 'ERROR'
                if 'Unsupported C feature (recursion)' in line:
                    status = 'ERROR (recursion)'
                elif 'Unsupported C feature (threads)' in line:
                    status = 'ERROR (threads)'
                elif 'Parsing failed' in line:
                    status = 'ERROR (parsing failed)'

            elif line.startswith('Verification result: '):
                line = line[21:].strip()
                if line.startswith('TRUE'):
                    newStatus = result.STATUS_TRUE_PROP
                elif line.startswith('FALSE'):
                    newStatus = result.STATUS_FALSE_REACH
                    match = re.match('.* Violation of propert[a-z]* (.*) found by chosen configuration.*', line)
                    if match:
                        newStatus = result.STR_FALSE + '(' + match.group(1) + ')'
                else:
                    newStatus = result.STATUS_UNKNOWN if not status.startswith('ERROR') else None
                if newStatus:
                    status = newStatus if not status else "{0} ({1})".format(status, newStatus)

        if status == 'KILLED (UNKNOWN)':
            status = 'KILLED'
        if not status:
            status = result.STATUS_UNKNOWN
        return status


    def addColumnValues(self, output, columns):
        for column in columns:

            # search for the text in output and get its value,
            # stop after the first line, that contains the searched text
            column.value = "-" # default value
            for line in output.splitlines():
                if column.text in line:
                    startPosition = line.find(':') + 1
                    endPosition = line.find('(', startPosition) # bracket maybe not found -> (-1)
                    if (endPosition == -1):
                        column.value = line[startPosition:].strip()
                    else:
                        column.value = line[startPosition: endPosition].strip()
                    break


if __name__ == "__main__":
    tool = Tool()
    executable = tool.getExecutable()
    print('Executable: {0}'.format(os.path.abspath(executable)))
    print('Version: {0}'.format(tool.getVersion(executable)))
