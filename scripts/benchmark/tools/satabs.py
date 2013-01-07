import subprocess

import benchmark.util as Util

class Tool:
    @staticmethod
    def getExecutable():
        return Util.findExecutable('satabs')

    @staticmethod
    def getVersion(executable):
        return subprocess.Popen([executable, '--version'],
                                stdout=subprocess.PIPE).communicate()[0].strip()

    @staticmethod
    def getName():
        return 'SatAbs'

    @staticmethod
    def getCmdline(executable, options, sourcefile):
        return [executable] + options + [sourcefile]

    @staticmethod
    def getStatus(returncode, returnsignal, output, isTimeout):
        if "VERIFICATION SUCCESSFUL" in output:
            assert returncode == 0
            status = "SAFE"
        elif "VERIFICATION FAILED" in output:
            assert returncode == 10
            status = "UNSAFE"
        elif returnsignal == 9:
            status = "TIMEOUT"
        elif returnsignal == 6:
            if "Assertion `!counterexample.steps.empty()' failed" in output:
                status = 'COUNTEREXAMPLE FAILED' # TODO: other status?
            else:
                status = "OUT OF MEMORY"
        elif returncode == 1 and "PARSING ERROR" in output:
            status = "PARSING ERROR"
        else:
            status = "FAILURE"
        return status

    @staticmethod
    def addColumnValues(output, columns):
        pass