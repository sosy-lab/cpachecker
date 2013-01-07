import subprocess

import benchmark.util as Util
import benchmark.tools.template

class Tool(benchmark.tools.template.BaseTool):

    def getExecutable(self):
        return Util.findExecutable('cpa.sh', 'scripts/cpa.sh')


    def getVersion(self, executable):
        version = ''
        try:
            versionHelpStr = subprocess.Popen([executable, '-help'],
                stdout=subprocess.PIPE).communicate()[0]
            versionHelpStr = Util.decodeToString(versionHelpStr)
            version = ' '.join(versionHelpStr.splitlines()[0].split()[1:])  # first word is 'CPAchecker'
        except IndexError:
            logging.critical('IndexError! Have you built CPAchecker?\n') # TODO better message
            sys.exit()
        return Util.decodeToString(version)


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
                 or line.startswith('out of memory')     # CuDD
                 )

        if returnsignal == 0 and returncode > 128:
            # shells sets return code to 128+signal when a signal is received
            returnsignal = returncode - 128

        if returnsignal != 0:
            if returnsignal == 6:
                status = 'ABORTED (probably by Mathsat)'
            elif returnsignal == 9 and isTimeout:
                status = 'TIMEOUT'
            elif returnsignal == 15:
                status = 'KILLED'
            else:
                status = 'KILLED BY SIGNAL '+str(returnsignal)

        elif returncode != 0:
            status = 'ERROR ({0})'.format(returncode)

        else:
            status = None

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

            elif line.startswith('Verification result: '):
                line = line[21:].strip()
                if line.startswith('SAFE'):
                    newStatus = 'SAFE'
                elif line.startswith('UNSAFE'):
                    newStatus = 'UNSAFE'
                else:
                    newStatus = 'UNKNOWN'
                status = newStatus if status is None else "{0} ({1})".format(status, newStatus)

            elif (status is None) and line.startswith('#Test cases computed:'):
                status = 'OK'

        if status == 'KILLED (UNKNOWN)':
            status = 'KILLED'
        if status is None:
            status = "UNKNOWN"
        return status


    def addColumnValues(self, output, columns):
        for column in columns:

            # search for the text in output and get its value,
            # stop after the first line, that contains the searched text
            column.value = "-" # default value
            for line in output.splitlines():
                if column.text in line:
                    startPosition = line.find(':') + 1
                    endPosition = line.find('(') # bracket maybe not found -> (-1)
                    if (endPosition == -1):
                        column.value = line[startPosition:].strip()
                    else:
                        column.value = line[startPosition: endPosition].strip()
                    break