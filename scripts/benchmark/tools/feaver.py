import os

import benchmark.filewriter as filewriter
import benchmark.util as Util

class Tool:
    @staticmethod
    def getExecutable():
        return Util.findExecutable('feaver_cmd')

    @staticmethod
    def getVersion(executable):
        return ''

    @staticmethod
    def getName():
        return 'Feaver'

    @staticmethod
    def getCmdline(executable, options, sourcefile):
        # create tmp-files for feaver, feaver needs special error-labels
        Tool.prepSourcefile = _prepareSourcefile(sourcefile)

        return [executable] + ["--file"] + [Tool.prepSourcefile] + options

    @staticmethod
    def _prepareSourcefile(sourcefile):
        content = open(sourcefile, "r").read()
        content = content.replace("goto ERROR;", "assert(0);")
        newFilename = "tmp_benchmark_feaver.c"
        filewriter.writeFile(newFilename, content)
        return newFilename

    @staticmethod
    def getStatus(returncode, returnsignal, output, isTimeout):
        if "collect2: ld returned 1 exit status" in output:
            status = "COMPILE ERROR"

        elif "Error (parse error" in output:
            status = "PARSE ERROR"

        elif "error: (\"model\":" in output:
            status = "MODEL ERROR"

        elif "Error: syntax error" in output:
            status = "SYNTAX ERROR"

        elif "error: " in output or "Error: " in output:
            status = "ERROR"

        elif "Error Found:" in output:
            status = "UNSAFE"

        elif "No Errors Found" in output:
            status = "SAFE"

        else:
            status = "UNKNOWN"

        # delete tmp-files
        for tmpfile in [Tool.prepSourcefile, Tool.prepSourcefile[0:-1] + "M",
                     "_modex_main.spn", "_modex_.h", "_modex_.cln", "_modex_.drv",
                     "model", "pan.b", "pan.c", "pan.h", "pan.m", "pan.t"]:
            try:
                os.remove(tmpfile)
            except OSError:
                pass

        return status

    @staticmethod
    def addColumnValues(output, columns):
        pass