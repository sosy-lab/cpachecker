from benchmark.util import Util

class Tool:
    @staticmethod
    def getExecutable():
        return Util.findExecutable('tool')

    @staticmethod
    def getVersion(executable):
        return ''

    @staticmethod
    def getName():
        return 'UNKOWN'

    @staticmethod
    def getCmdline(executable, options, sourcefile):
        return [executable] + options + [sourcefile]

    @staticmethod
    def getStatus(returncode, returnsignal, output, isTimeout):
        return 'UNKNOWN'

    @staticmethod
    def addColumnValues(output, columns):
        """
        This method adds the values that the user requested to the column objects.
        If a value is not found, it should be set to '-'.
        """
        pass