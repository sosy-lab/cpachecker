import os

import benchmark.tools.template
import benchmark.result as result

PROGRAM_PATH="../CPArec"
#PYTHON_PATH="../python"

class Tool(benchmark.tools.template.BaseTool):

    def getExecutable(self):
        return './main.py'

    def getVersion(self, executable):
        return 'v0.1-alpha'

    def getName(self):
        return 'CPArec'

    def getWorkingDirectory(self, executable):
        return PROGRAM_PATH

    def getProgrammFiles(self, executable):
        return [PROGRAM_PATH]#, PYTHON_PATH]
    
#    def getEnvironments(self, executable):
#        return {'additionalEnv' : {'PYTHONPATH' : ':' + os.path.relpath(PYTHON_PATH, start=PROGRAM_PATH)}}

    def getCmdline(self, executable, options, sourcefiles, propertyfile=None, rlimits={}):
        assert len(sourcefiles) == 1
        return ['python', executable] + options + [os.path.relpath(s, start=PROGRAM_PATH) for s in sourcefiles]

    def getStatus(self, returncode, returnsignal, output, isTimeout):
        content = '\n'.join(output)

        if 'Proof for "Error" can be found at' in content and 'Verification Result: FALSE' in content:
            status = result.STATUS_FALSE_REACH
        elif 'Proof for "Pass" can be found at' in content and 'Verification Result: TRUE' in content:
            status = result.STATUS_TRUE_PROP
        else:
            status = result.STATUS_UNKNOWN

        return status
