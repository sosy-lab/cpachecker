
import subprocess
import os
import benchmark.util as Util
import benchmark.tools.template
import benchmark.result as result

class Tool(benchmark.tools.template.BaseTool):
    """
    This class serves as tool adaptor for Threader (http://www.esbmc.org/)
    """

    def getExecutable(self):
        return Util.findExecutable('threader.sh')


    def getProgrammFiles(self, executable):
        executableDir = os.path.dirname(executable)
        return [executableDir]


    def getWorkingDirectory(self, executable):
        executableDir = os.path.dirname(executable)
        return executableDir


    def getEnvironments(self, executable):
        return {"additionalEnv" : {'PATH' :  ':.'}}


    def getVersion(self, executable):
        exe = 'cream'
        return subprocess.Popen([exe, '--help'], stdout=subprocess.PIPE)\
                              .communicate()[0].splitlines()[2][34:42]


    def getName(self):
        return 'Threader'


    def getCmdline(self, executable, options, sourcefile):
        workingDir = self.getWorkingDirectory(executable)
        return [os.path.relpath(executable, start=workingDir)] + options + [os.path.relpath(sourcefile, start=workingDir)]


    def getStatus(self, returncode, returnsignal, output, isTimeout):
        if 'SSSAFE' in output:
            status = result.STR_TRUE
        elif 'UNSAFE' in output:
            status = result.STR_FALSE_LABEL
        else:
            status = result.STR_UNKNOWN

        if status == result.STR_UNKNOWN and isTimeout:
            status = 'TIMEOUT'

        return status
