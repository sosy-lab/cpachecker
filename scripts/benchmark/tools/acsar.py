import os
import subprocess

import benchmark.filewriter as filewriter
import benchmark.util as Util

class Tool:
    @staticmethod
    def getExecutable():
        return Util.findExecutable('acsar')

    @staticmethod
    def getVersion(executable):
        return ''

    @staticmethod
    def getName():
        return 'Acsar'

    @staticmethod
    def getCmdline(executable, options, sourcefile):
        # create tmp-files for acsar, acsar needs special error-labels
        Tool.prepSourcefile = _prepareSourcefile(sourcefile)

        return [executable] + ["--file"] + [Tool.prepSourcefile] + options

    @staticmethod
    def _prepareSourcefile(sourcefile):
        content = open(sourcefile, "r").read()
        content = content.replace(
            "ERROR;", "ERROR_LOCATION;").replace(
            "ERROR:", "ERROR_LOCATION:").replace(
            "errorFn();", "goto ERROR_LOCATION; ERROR_LOCATION:;")
        newFilename = sourcefile + "_acsar.c"
        filewriter.writeFile(newFilename, content)
        return newFilename

    @staticmethod
    def getStatus(returncode, returnsignal, output, isTimeout):
        if "syntax error" in output:
            status = "SYNTAX ERROR"

        elif "runtime error" in output:
            status = "RUNTIME ERROR"

        elif "error while loading shared libraries:" in output:
            status = "LIBRARY ERROR"

        elif "can not be used as a root procedure because it is not defined" in output:
            status = "NO MAIN"

        elif "For Error Location <<ERROR_LOCATION>>: I don't Know " in output:
            status = "TIMEOUT"

        elif "received signal 6" in output:
            status = "ABORT"

        elif "received signal 11" in output:
            status = "SEGFAULT"

        elif "received signal 15" in output:
            status = "KILLED"

        elif "Error Location <<ERROR_LOCATION>> is not reachable" in output:
            status = "SAFE"

        elif "Error Location <<ERROR_LOCATION>> is reachable via the following path" in output:
            status = "UNSAFE"

        else:
            status = "UNKNOWN"

        # delete tmp-files
        os.remove(Tool.prepSourcefile)

        return status

    @staticmethod
    def addColumnValues(output, columns):
        pass