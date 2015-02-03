import os

import benchmark.tools.template
import benchmark.result as result

PROGRAM_PATH="../smack-corral"

class Tool(benchmark.tools.template.BaseTool):

    def getExecutable(self):
        return './smack-svcomp.sh'

    def getVersion(self, executable):
        return ''

    def getName(self):
        return 'SMACK'

    def getWorkingDirectory(self, executable):
        return PROGRAM_PATH

    def getProgrammFiles(self, executable):
        return [PROGRAM_PATH]

    def getCmdline(self, executable, options, sourcefiles, propertyfile=None, rlimits={}):
        assert len(sourcefiles) == 1
        return [executable] + \
               [os.path.relpath(s, start=PROGRAM_PATH) for s in sourcefiles] + \
               options

    def getStatus(self, returncode, returnsignal, output, isTimeout):
        content = '\n'.join(output)

        if "FALSE(REACH)" in content:
            status = result.STATUS_FALSE_REACH
        elif "TRUE" in content:
            status = result.STATUS_TRUE_PROP
        else:
            status = result.STATUS_UNKNOWN

        return status
