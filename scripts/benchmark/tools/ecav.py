import benchmark.util as Util
import benchmark.tools.template

class Tool(benchmark.tools.template.BaseTool):

    def getExecutable(self):
        return Util.findExecutable('ecaverifier')


    def getName(self):
        return 'EcaVerifier'


    def getStatus(self, returncode, returnsignal, output, isTimeout):
        status = "UNKNOWN"
        for line in output.splitlines():
            if line.startswith('0 safe, 1 unsafe'):
                status = 'UNSAFE'
            elif line.startswith('1 safe, 0 unsafe'):
                status = 'SAFE'
            elif returnsignal == 9:
                if isTimeout:
                    status = 'TIMEOUT'
                else:
                    status = "KILLED BY SIGNAL 9"

        return status