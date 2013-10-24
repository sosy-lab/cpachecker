import benchmark.tools.template

class Tool(benchmark.tools.template.BaseTool):
    """
    This tool is an imaginary tool that returns always SAFE.
    To use it you need a normal benchmark-xml-file
    with the tool and sourcefiles, however options are ignored.
    """
    def getExecutable(self):
        return '/bin/true'

    def getName(self):
        return 'AlwaysSafe'

    def getCmdline(self, executable, options, sourcefile):
        return [executable]

    def getStatus(self, returncode, returnsignal, output, isTimeout):
        return 'SAFE'