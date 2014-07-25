from random import random
import benchmark.tools.template
import benchmark.result as result

class Tool(benchmark.tools.template.BaseTool):
    """
    This tool is an imaginary tool that randomly returns SAFE and UNSAFE.
    To use it you need a normal benchmark-xml-file
    with the tool and sourcefiles, however options are ignored.
    """

    def getExecutable(self):
        return '/bin/true'

    def getName(self):
        return 'Random'

    def getCmdline(self, executable, options, sourcefile, propertyfile, rlimits):
        return [executable]

    def getStatus(self, returncode, returnsignal, output, isTimeout):
        return result.STATUS_TRUE_PROP if random() < 0.5 else result.STATUS_FALSE_REACH