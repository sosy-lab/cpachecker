import os
import subprocess

import benchmark.util as Util
import benchmark.tools.template
import benchmark.result as result

class Tool(benchmark.tools.template.BaseTool):
    """
    This class serves as tool adaptor for LLBMC
    """

    def getExecutable(self):
        return Util.findExecutable('lib/native/x86_64-linux/llbmc')


    def getVersion(self, executable):
        return subprocess.Popen([executable, '--version'],
                                stdout=subprocess.PIPE).communicate()[0].splitlines()[2][8:18]


    def getName(self):
        return 'LLBMC'


    def getCmdline(self, executable, options, sourcefile, propertyfile):
        # compile sourcefile with clang
        self.prepSourcefile = self._prepareSourcefile(sourcefile)

        return [executable] + options + [self.prepSourcefile]


    def _prepareSourcefile(self, sourcefile):
        clangExecutable = Util.findExecutable('clang')
        newFilename     = sourcefile + ".o"

        subprocess.Popen([clangExecutable,
                            '-c',
                            '-emit-llvm',
                            '-std=gnu89',
                            '-m32',
                            sourcefile,
                            '-O0',
                            '-o',
                            newFilename,
                            '-w'],
                          stdout=subprocess.PIPE).wait()

        return newFilename


    def getStatus(self, returncode, returnsignal, output, isTimeout):
        status = result.STR_UNKNOWN

        for line in output.splitlines():
            if 'Error detected.' in line:
                status = result.STR_FALSE_LABEL
            elif 'No error detected.' in line:
                status = result.STR_TRUE

        # delete tmp-files
        try:
          os.remove(self.prepSourcefile)
        except OSError, e:
            print "Could not remove file " + self.prepSourcefile + "! Maybe clang call failed"
            pass

        return status


    def addColumnValues(self, output, columns):
        """
        This method adds the values that the user requested to the column objects.
        If a value is not found, it should be set to '-'.
        If not supported, this method does not need to get overridden.
        """
        pass
