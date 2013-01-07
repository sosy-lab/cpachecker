from random import random

class Tool:
    """
    This tool is an imaginary tool that randomly returns SAFE and UNSAFE.
    To use it you need a normal benchmark-xml-file
    with the tool and sourcefiles, however options are ignored.
    """

    @staticmethod
    def getExecutable():
        return '/bin/true'

    @staticmethod
    def getVersion(executable):
        return ''

    @staticmethod
    def getName():
        return 'Random'

    @staticmethod
    def getCmdline(executable, options, sourcefile):
        return [executable]

    @staticmethod
    def getStatus(returncode, returnsignal, output, isTimeout):
        return 'SAFE' if random() < 0.5 else 'UNSAFE'

    @staticmethod
    def addColumnValues(output, columns):
        pass