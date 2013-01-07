import benchmark.util as Util

class Tool:
    @staticmethod
    def getExecutable():
        return Util.findExecutable('ufo.sh')

    @staticmethod
    def getVersion(executable):
        return ''

    @staticmethod
    def getName():
        return 'Ufo'

    @staticmethod
    def getCmdline(executable, options, sourcefile):
        return [executable] + options + [sourcefile]

    @staticmethod
    def getStatus(returncode, returnsignal, output, isTimeout):
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

    @staticmethod
    def addColumnValues(output, columns):
        pass