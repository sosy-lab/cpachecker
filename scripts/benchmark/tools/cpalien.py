# prepare for Python 3
from __future__ import absolute_import, print_function, unicode_literals

import logging
import subprocess
import sys
import string
import os
import re
import benchmark.result as result

sys.dont_write_bytecode = True # prevent creation of .pyc files

if __name__ == "__main__":
    sys.path.append(os.path.join(os.path.dirname(__file__), os.path.pardir, os.path.pardir))

import benchmark.util as Util
import benchmark.tools.cpachecker

class Tool(benchmark.tools.cpachecker.Tool):

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

        bad_free = False
        memory_leak = False
        bad_deref = False
        undef = False
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
                elif 'Unknown function' in line:
                    status = 'ERROR (unknown function)'
            elif line.startswith("Invalid free found"):
              bad_free = True
            elif line.startswith("Memory leak found"):
              memory_leak = True
            elif line.startswith("Invalid read found"):
              bad_deref = True
            elif line.startswith("Invalid write found"):
              bad_deref = True
            elif line.startswith("Non-target undefined behavior detected."):
              status = "ERROR (undefined behavior)"
              undef = True;

            elif line.startswith('Verification result: '):
                line = line[21:].strip()
                if line.startswith('TRUE'):
                    newStatus = result.STR_TRUE
                elif line.startswith('FALSE'):
                  newStatus = result.STR_FALSE_LABEL
                  match = re.match('.* Violation of propert[a-z]* (.*) found by chosen configuration.*', line)
                  if match:
                      newStatus = result.STR_FALSE + '(' + match.group(1) + ')'

                else:
                    newStatus = result.STR_UNKNOWN if not status.startswith('ERROR') else None
                if newStatus and not status:
                    status = newStatus

        if status == 'KILLED (UNKNOWN)':
            status = 'KILLED'
        if not status or undef:
            status = result.STR_UNKNOWN
        return status


if __name__ == "__main__":
    tool = Tool()
    executable = tool.getExecutable()
    print('Executable: {0}'.format(os.path.abspath(executable)))
    print('Version: {0}'.format(tool.getVersion(executable)))
