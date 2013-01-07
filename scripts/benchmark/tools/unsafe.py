class Tool:
    """
    This tool is an imaginary tool that returns always UNSAFE.
    To use it you need a normal benchmark-xml-file
    with the tool and sourcefiles, however options are ignored.
    """

    @staticmethod
    def getExecutable():
        return '/bin/false'

    @staticmethod
    def getVersion(executable):
        return ''

    @staticmethod
    def getName():
        return 'AlwaysUnsafe'

    @staticmethod
    def getCmdline(executable, options, sourcefile):
        return [executable]

    @staticmethod
    def getStatus(returncode, returnsignal, output, isTimeout):
        return 'UNSAFE'

    @staticmethod
    def addColumnValues(output, columns):
        pass