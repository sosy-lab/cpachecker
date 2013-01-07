import os
import platform
import subprocess
import xml.etree.ElementTree as ET

from benchmark.filewriter import FileWriter
from benchmark.util import Util

class Tool:
    @staticmethod
    def getExecutable():
        fallback = "lib/native/x86_64-linux/cbmc" if platform.machine() == "x86_64" else \
                   "lib/native/x86-linux/cbmc"    if platform.machine() == "i386" else None
        return Util.findExecutable('cbmc', fallback)

    @staticmethod
    def getVersion(executable):
        return subprocess.Popen([executable, '--version'],
                                stdout=subprocess.PIPE).communicate()[0].strip()

    @staticmethod
    def getName():
        return 'CBMC'

    @staticmethod
    def getCmdline(executable, options, sourcefile):
        if ("--xml-ui" not in options):
            options = options + ["--xml-ui"]
        return [executable] + options + [sourcefile]

    @staticmethod
    def getStatus(returncode, returnsignal, output, isTimeout):
        #an empty tag cannot be parsed into a tree
        output = output.replace("<>", "<emptyTag>")
        output = output.replace("</>", "</emptyTag>")

        if ((returncode == 0) or (returncode == 10)):
            try:
                tree = ET.fromstring(output)
                status = tree.findtext('cprover-status')

                if status is None:
                    def isErrorMessage(msg):
                        return msg.get('type', None) == 'ERROR'

                    messages = list(filter(isErrorMessage, tree.getiterator('message')))
                    if messages:
                        # for now, use only the first error message if there are several
                        msg = messages[0].findtext('text')
                        if msg == 'Out of memory':
                            status = 'OUT OF MEMORY'
                        elif msg:
                            status = 'ERROR (%s)'.format(msg)
                        else:
                            status = 'ERROR'
                    else:
                        status = 'INVALID OUTPUT'

                elif status == "FAILURE":
                    assert returncode == 10
                    reason = tree.find('goto_trace').find('failure').findtext('reason')
                    if 'unwinding assertion' in reason:
                        status = "UNKNOWN"
                    else:
                        status = "UNSAFE"

                elif status == "SUCCESS":
                    assert returncode == 0
                    if "--no-unwinding-assertions" in options:
                        status = "UNKNOWN"
                    else:
                        status = "SAFE"

            except Exception as e: # catch all exceptions
                if isTimeout:
                    # in this case an exception is expected as the XML is invaliddd
                    status = 'TIMEOUT'
                elif 'Minisat::OutOfMemoryException' in output:
                    status = 'OUT OF MEMORY'
                else:
                    status = 'INVALID OUTPUT'
                    logging.warning("Error parsing CBMC output for returncode %d: %s" % (returncode, e))

        elif returncode == 6:
            # parser error or something similar
            status = 'ERROR'

        elif returnsignal == 9 or returncode == (128+9):
            if isTimeout:
                status = 'TIMEOUT'
            else:
                status = "KILLED BY SIGNAL 9"

        elif returnsignal == 6:
            status = "ABORTED"
        elif returnsignal == 15 or returncode == (128+15):
            status = "KILLED"
        else:
            status = "ERROR ({0})".format(returncode)

        return status

    @staticmethod
    def addColumnValues(output, columns):
        pass