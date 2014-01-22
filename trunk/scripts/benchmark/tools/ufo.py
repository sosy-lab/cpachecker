import benchmark.util as Util
import benchmark.tools.template

class Tool(benchmark.tools.template.BaseTool):

    def getExecutable(self):
        return Util.findExecutable('ufo.sh')


    def getName(self):
        return 'Ufo'


    def getStatus(self, returncode, returnsignal, output, isTimeout):
        if returnsignal == 9 or returnsignal == (128+9):
            if isTimeout:
                status = "TIMEOUT"
            else:
                status = "KILLED BY SIGNAL 9"
        elif returncode == 1 and "program correct: ERROR unreachable" in output:
            status = "SAFE"
        elif returncode != 0:
            status = "ERROR ({0})".format(returncode)
        elif "ERROR reachable" in output:
            status = "UNSAFE"
        elif "program correct: ERROR unreachable" in output:
            status = "SAFE"
        else:
            status = "FAILURE"
        return status