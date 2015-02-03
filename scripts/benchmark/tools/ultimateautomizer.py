import os

import benchmark.tools.template
import benchmark.result as result

PROGRAM_PATH="../UltimateAutomizer"
Z3_PATH="../z3-4.3.3.f50a8b0a59ff-x64-ubuntu-14.04"

class Tool(benchmark.tools.template.BaseTool):

    def getExecutable(self):
        return './Ultimate.py'

    def getVersion(self, executable):
        return 'r12950'

    def getName(self):
        return 'Ultimate Automizer'

    def getWorkingDirectory(self, executable):
        return PROGRAM_PATH

    def getProgrammFiles(self, executable):
        return [PROGRAM_PATH, Z3_PATH]

    def getCmdline(self, executable, options, sourcefiles, propertyfile=None, rlimits={}):
        assert len(sourcefiles) == 1
        return ['python', executable, os.path.relpath(propertyfile, start=PROGRAM_PATH)] + \
            [os.path.relpath(s, start=PROGRAM_PATH) for s in sourcefiles] + options

    def getEnvironments(self, executable):
        return {'additionalEnv' : {'PATH' : ':' + os.path.relpath(Z3_PATH + '/bin', start=PROGRAM_PATH)}}

    def getStatus(self, returncode, returnsignal, output, isTimeout):
        content = '\n'.join(output)

        if "FALSE(valid-free)" in content:
            status = result.STATUS_FALSE_FREE
        elif "FALSE(valid-deref)" in content:
            status = result.STATUS_FALSE_DEREF
        elif "FALSE(valid-memtrack)" in content:
            status = result.STATUS_FALSE_MEMTRACK
        elif "FALSE" in content and "Checking for termination" in content:
            status = result.STATUS_FALSE_TERMINATION
        elif "FALSE" in content:
            status = result.STATUS_FALSE_REACH
        elif "TRUE" in content:
            status = result.STATUS_TRUE_PROP
        else:
            status = result.STATUS_UNKNOWN

        return status
