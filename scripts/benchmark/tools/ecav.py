import benchmark.util as Util

class Tool:
    @staticmethod
    def getExecutable():
        return Util.findExecutable('ecaverifier')

    @staticmethod
    def getVersion(executable):
        return ''

    @staticmethod
    def getName():
        return 'EcaVerifier'

    @staticmethod
    def getCmdline(executable, options, sourcefile):
        return [executable] + options + [sourcefile]

    @staticmethod
    def getStatus(returncode, returnsignal, output, isTimeout):
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

    @staticmethod
    def addColumnValues(output, columns):
        pass