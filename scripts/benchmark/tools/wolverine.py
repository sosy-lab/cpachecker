import subprocess

import benchmark.util as Util
import benchmark.tools.template
import benchmark.result as result

class Tool(benchmark.tools.template.BaseTool):

    def getExecutable(self):
        return Util.findExecutable('wolverine')


    def getVersion(self, executable):
        return subprocess.Popen([executable, '--version'],
                                stdout=subprocess.PIPE).communicate()[0].split()[1].strip()


    def getName(self):
        return 'Wolverine'


    def getStatus(self, returncode, returnsignal, output, isTimeout):
        if "VERIFICATION SUCCESSFUL" in output:
            assert returncode == 0
            status = result.STR_TRUE_PROP
        elif "VERIFICATION FAILED" in output:
            assert returncode == 10
            status = result.STR_FALSE_REACH
        elif returnsignal == 9:
            status = "TIMEOUT"
        elif returnsignal == 6 or (returncode == 6 and "Out of memory" in output):
            status = "OUT OF MEMORY"
        elif returncode == 6 and "PARSING ERROR" in output:
            status = "PARSING ERROR"
        else:
            status = "FAILURE"
        return status