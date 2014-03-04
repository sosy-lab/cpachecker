import benchmark.util as Util

class BaseTool(object):
    """
    This class serves both as a template for tool adaptor implementations,
    and as an abstract super class for them.
    For writing a new tool adaptor, inherit from this class and override
    the necessary methods.
    """

    def getExecutable(self):
        """
        Find the path to the executable file that will get executed.
        This method always needs to be overridden,
        and most implementations will look similar to this one.
        """
        return Util.findExecutable('tool')


    def getVersion(self, executable):
        """
        Determine a version string for this tool, if available.
        """
        return ''


    def getName(self):
        """
        Return the name of the tool, formatted for humans.
        """
        return 'UNKOWN'


    def getCmdline(self, executable, options, sourcefile, propertyfile=None):
        """
        Compose the command line to execute from the name of the executable,
        the user-specified options, and the sourcefile to analyze.
        This method can get overridden, if, for example, some options should
        """
        return [executable] + options + [sourcefile]


    def getStatus(self, returncode, returnsignal, output, isTimeout):
        """
        Parse the output of the tool and extract the verification result.
        This method always needs to be overridden.
        """
        return 'UNKNOWN'


    def addColumnValues(self, output, columns):
        """
        This method adds the values that the user requested to the column objects.
        If a value is not found, it should be set to '-'.
        If not supported, this method does not need to get overridden.
        """
        pass


    def getProgrammFiles(self, executable):
        """
        OPTIONAL, this method is only necessary for "cloud-mode".
        Returns a list of files or directories, 
        that are necessary to run the tool in "cloud-mode".
        """
        return []


    def getWorkingDirectory(self, executable):
        """
        OPTIONAL, this method is only necessary for "cloud-mode".
        Returns a working directory, that is used to run the tool in "cloud-mode".
        """
        return "."


    def getEnvironments(self, executable):
        """
        OPTIONAL, this method is only necessary for special tools.
        It contains some info, how the environment has to be changed, so that the tool can run. 
        Returns a map, that contains identifiers for several submaps.
        All keys and values have to be Strings!
        
        Currently we support 2 identifiers:
        
        "newEnv": Before the execution, the values are assigned to the real environment-identifiers.
                  This will override existing values.
        "additionalEnv": Before the execution, the values are appended to the real environment-identifiers.
                  The seperator for the appending must be given in this method,
                  so that the operation "realValue + additionalValue" is a valid value.
                  For example in the PATH-variable the additionalValue starts with a ":".
        """
        return {}
