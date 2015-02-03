import os

import benchmark.tools.template
import benchmark.result as result

PROGRAM_PATH="../cbmc"

class Tool(benchmark.tools.template.BaseTool):

    def getExecutable(self):
        return './cbmc-wrapper.sh'

    def getVersion(self, executable):
        return '4.9'

    def getName(self):
        return 'CBMC'

    def getWorkingDirectory(self, executable):
        return PROGRAM_PATH

    def getProgrammFiles(self, executable):
        return [PROGRAM_PATH]

    def getCmdline(self, executable, options, sourcefiles, propertyfile=None, rlimits={}):
        assert len(sourcefiles) == 1
        return [executable, '--propertyfile', os.path.relpath(propertyfile, start=PROGRAM_PATH)] + \
            options + [os.path.relpath(s, start=PROGRAM_PATH) for s in sourcefiles]


    def getStatus(self, returncode, returnsignal, output, isTimeout):
        status = "error"
        
        if output:
            lastLine = output[-1] # last line
        else:
            lastLine = ""

        if "FALSE(valid-free)" in lastLine:
            status = result.STATUS_FALSE_FREE
        elif "FALSE(valid-deref)" in lastLine:
            status = result.STATUS_FALSE_DEREF
        elif "FALSE(valid-memtrack)" in lastLine:
            status = result.STATUS_FALSE_MEMTRACK
        elif "FALSE" in lastLine:
            status = result.STATUS_FALSE_REACH
        elif "TRUE" in lastLine:
            status = result.STATUS_TRUE_PROP
        elif "UNKNOWN" in lastLine:
            status = result.STATUS_UNKNOWN

        return status
