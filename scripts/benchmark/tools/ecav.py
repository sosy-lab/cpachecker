import benchmark.util as Util
import benchmark.tools.template
import benchmark.result as result

class Tool(benchmark.tools.template.BaseTool):

    def getExecutable(self):
        return Util.findExecutable('ecaverifier')


    def getName(self):
        return 'EcaVerifier'


    def getStatus(self, returncode, returnsignal, output, isTimeout):
        status = result.STATUS_UNKNOWN
        for line in output.splitlines():
            if line.startswith('0 safe, 1 unsafe'):
                status = result.STATUS_FALSE_REACH
            elif line.startswith('1 safe, 0 unsafe'):
                status = result.STATUS_TRUE_PROP
            elif returnsignal == 9:
                if isTimeout:
                    status = 'TIMEOUT'
                else:
                    status = "KILLED BY SIGNAL 9"

        return status