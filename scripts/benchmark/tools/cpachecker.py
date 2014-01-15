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
import benchmark.tools.template

class Tool(benchmark.tools.template.BaseTool):

    def getExecutable(self):
        executable = Util.findExecutable('cpa.sh', 'scripts/cpa.sh')
        executableDir = os.path.join(os.path.dirname(executable),"../")
        if os.path.isdir(os.path.join(executableDir, 'src')):
            self._buildCPAchecker(executableDir)
        if not os.path.isfile(os.path.join(executableDir, "cpachecker.jar")):
            logging.warning("Required JAR file for CPAchecker not found in {0}.".format(executableDir))
        return executable


    def _buildCPAchecker(self, executableDir):
        logging.info('Building CPAchecker in directory {0}.'.format(executableDir))
        ant = subprocess.Popen(['ant', '-q', 'jar'], cwd=executableDir)
        (stdout, stderr) = ant.communicate()
        if ant.returncode:
            sys.exit('Failed to build CPAchecker, please fix the build first.')


    def getProgrammFiles(self, executable):
        executableDir = os.path.join(os.path.dirname(executable),"../")
        return [os.path.join(executableDir, path) for path in ["lib", "scripts", "cpachecker.jar", "config"]]


    def getWorkingDirectory(self, executable):
        return os.curdir


    def getVersion(self, executable):
        process = subprocess.Popen([executable, '-help'], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        (stdout, stderr) = process.communicate()
        if stderr:
            sys.exit(Util.decodeToString(stderr))
        if process.returncode:
            sys.exit('CPAchecker returned exit code {0}'.format(process.returncode))
        stdout = Util.decodeToString(stdout)
        version = ' '.join(stdout.splitlines()[0].split()[1:])  # first word is 'CPAchecker'

        # CPAchecker might be within a SVN repository
        # Determine the revision and add it to the version.
        cpaShDir = os.path.dirname(os.path.realpath(executable))
        cpacheckerDir = os.path.join(cpaShDir, os.path.pardir)
        try:
            svnProcess = subprocess.Popen(['svnversion', cpacheckerDir], env={'LANG': 'C'}, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            (stdout, stderr) = svnProcess.communicate()
            stdout = Util.decodeToString(stdout).strip()
            if not (svnProcess.returncode or stderr or (stdout == 'exported')):
                return version + ' ' + stdout
        except OSError:
            pass

        # CPAchecker might be within a git-svn repository
        try:
            gitProcess = subprocess.Popen(['git', 'svn', 'find-rev', 'HEAD'], env={'LANG': 'C'}, cwd=cpacheckerDir, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            (stdout, stderr) = gitProcess.communicate()
            stdout = Util.decodeToString(stdout).strip()
            if not (gitProcess.returncode or stderr) and stdout:
                return version + ' ' + stdout + ('M' if self._isGitRepositoryDirty(cpacheckerDir) else '')
    
            # CPAchecker might be within a git repository
            gitProcess = subprocess.Popen(['git', 'log', '-1', '--pretty=format:%h', '--abbrev-commit'], env={'LANG': 'C'}, cwd=cpacheckerDir, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            (stdout, stderr) = gitProcess.communicate()
            stdout = Util.decodeToString(stdout).strip()
            if not (gitProcess.returncode or stderr) and stdout:
                return version + ' ' + stdout + ('+' if self._isGitRepositoryDirty(cpacheckerDir) else '')
        except OSError:
            pass

        return version


    def _isGitRepositoryDirty(self, dir):
        gitProcess = subprocess.Popen(['git', 'status', '--porcelain'], env={'LANG': 'C'}, cwd=dir, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        (stdout, stderr) = gitProcess.communicate()
        if not (gitProcess.returncode or stderr):
            return True if stdout else False  # True if stdout is non-empty
        return None


    def getName(self):
        return 'CPAchecker'


    def getCmdline(self, executable, options, sourcefile):
        if ("-stats" not in options):
            options = options + ["-stats"]
        return [executable] + options + [sourcefile]


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

        property = None
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

            elif line.startswith('Found violation of property '):
                property = re.match('Found violation of property ([^ ]*) .*', line).group(1)

            elif line.startswith('Verification result: '):
                line = line[21:].strip()
                if line.startswith('TRUE'):
                    newStatus = result.STR_TRUE
                elif line.startswith('FALSE'):
                    newStatus = result.STR_FALSE_LABEL
                    if property:
                        newStatus = newStatus + '(' + property + ')'
                else:
                    newStatus = result.STR_UNKNOWN if not status.startswith('ERROR') else None
                if newStatus:
                    status = newStatus if not status else "{0} ({1})".format(status, newStatus)

        if status == 'KILLED (UNKNOWN)':
            status = 'KILLED'
        if not status:
            status = result.STR_UNKNOWN
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
