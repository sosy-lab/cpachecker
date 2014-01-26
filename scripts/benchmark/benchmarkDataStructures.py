"""
CPAchecker is a tool for configurable software verification.
This file is part of CPAchecker.

Copyright (C) 2007-2013  Dirk Beyer
All rights reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.


CPAchecker web page:
  http://cpachecker.sosy-lab.org
"""

# prepare for Python 3
from __future__ import absolute_import, print_function, unicode_literals

import logging
import os
import time
import xml.etree.ElementTree as ET
import sys

from datetime import date

from . import result
from . import runexecutor
from . import util as Util

MEMLIMIT = runexecutor.MEMLIMIT
TIMELIMIT = runexecutor.TIMELIMIT
CORELIMIT = runexecutor.CORELIMIT

SOFTTIMELIMIT = 'softtimelimit'
HARDTIMELIMIT = 'hardtimelimit'


def getOptionsFromXML(optionsTag):
    '''
    This function searches for options in a tag
    and returns a list with command-line arguments.
    '''
    return Util.toSimpleList((option.get("name"), option.text)
               for option in optionsTag.findall("option"))


def substituteVars(oldList, runSet, sourcefile=None):
    """
    This method replaces special substrings from a list of string
    and return a new list.
    """

    benchmark = runSet.benchmark

    # list with tuples (key, value): 'key' is replaced by 'value'
    keyValueList = [('${benchmark_name}', benchmark.name),
                    ('${benchmark_date}', benchmark.date),
                    ('${benchmark_path}', os.path.dirname(benchmark.benchmarkFile)),
                    ('${benchmark_path_abs}', os.path.abspath(os.path.dirname(benchmark.benchmarkFile))),
                    ('${benchmark_file}', os.path.basename(benchmark.benchmarkFile)),
                    ('${benchmark_file_abs}', os.path.abspath(os.path.basename(benchmark.benchmarkFile))),
                    ('${logfile_path}',   os.path.dirname(runSet.logFolder)),
                    ('${logfile_path_abs}', os.path.abspath(runSet.logFolder)),
                    ('${rundefinition_name}', runSet.realName if runSet.realName else ''),
                    ('${test_name}',      runSet.realName if runSet.realName else '')]

    if sourcefile:
        keyValueList.append(('${sourcefile_name}', os.path.basename(sourcefile)))
        keyValueList.append(('${sourcefile_path}', os.path.dirname(sourcefile)))
        keyValueList.append(('${sourcefile_path_abs}', os.path.dirname(os.path.abspath(sourcefile))))

    # do not use keys twice
    assert len(set((key for (key, value) in keyValueList))) == len(keyValueList)

    newList = []

    for oldStr in oldList:
        newStr = oldStr
        for (key, value) in keyValueList:
            newStr = newStr.replace(key, value)
        if '${' in newStr:
            logging.warn("a variable was not replaced in '{0}'".format(newStr))
        newList.append(newStr)

    return newList



class Benchmark:
    """
    The class Benchmark manages the import of source files, options, columns and
    the tool from a benchmarkFile.
    This class represents the <benchmark> tag.
    """

    def __init__(self, benchmarkFile, config, OUTPUT_PATH):
        """
        The constructor of Benchmark reads the source files, options, columns and the tool
        from the XML in the benchmarkFile..
        """
        logging.debug("I'm loading the benchmark {0}.".format(benchmarkFile))

        self.config = config        
        self.benchmarkFile = benchmarkFile

        # get benchmark-name
        self.name = os.path.basename(benchmarkFile)[:-4] # remove ending ".xml"
        if config.name:
            self.name += "."+config.name

        # get current date as String to avoid problems, if script runs over midnight
        currentTime = time.localtime()
        self.date = time.strftime("%y-%m-%d_%H%M", currentTime)
        self.dateISO = time.strftime("%y-%m-%d %H:%M", currentTime)

        # parse XML
        rootTag = ET.ElementTree().parse(benchmarkFile)

        # get tool
        toolName = rootTag.get('tool')
        if not toolName:
            sys.exit('A tool needs to be specified in the benchmark definition file.')
        toolModule = "benchmark.tools." + toolName
        try:
            self.tool = __import__(toolModule, fromlist=['Tool']).Tool()
        except ImportError as ie:
            sys.exit('Unsupported tool "{0}" specified. ImportError: {1}'.format(toolName, ie))
        except AttributeError:
            sys.exit('The module for "{0}" does not define the necessary class.'.format(toolName))

        self.toolName = self.tool.getName()
        self.executable = self.tool.getExecutable()
        self.toolVersion = self.tool.getVersion(self.executable)

        logging.debug("The tool to be benchmarked is {0}.".format(str(self.toolName)))

        self.rlimits = {}
        keys = list(rootTag.keys())
        for limit in [MEMLIMIT, TIMELIMIT, CORELIMIT]:
            if limit in keys:
                self.rlimits[limit] = int(rootTag.get(limit))

        # override limits from XML with values from command line
        def overrideLimit(configVal, limit):
            if configVal != None:
                val = int(configVal)
                if val == -1: # infinity
                    if limit in self.rlimits:
                        self.rlimits.pop(limit)
                else:
                    self.rlimits[limit] = val

        overrideLimit(config.memorylimit, MEMLIMIT)
        overrideLimit(config.timelimit, TIMELIMIT)
        overrideLimit(config.corelimit, CORELIMIT)

        if HARDTIMELIMIT in keys:
            hardtimelimit = int(rootTag.get(HARDTIMELIMIT))
            if TIMELIMIT in self.rlimits:
                if hardtimelimit < self.rlimits[TIMELIMIT]:
                    logging.warning('Hard timelimit %d is smaller than timelimit %d, ignoring the former.'
                                    % (hardtimelimit, self.rlimits[TIMELIMIT]))
                else:
                    self.rlimits[SOFTTIMELIMIT] = self.rlimits[TIMELIMIT]
                    self.rlimits[TIMELIMIT] = hardtimelimit
            else:
                self.rlimits[TIMELIMIT] = hardtimelimit

        # get number of threads, default value is 1
        self.numOfThreads = int(rootTag.get("threads")) if ("threads" in keys) else 1
        if config.numOfThreads != None:
            self.numOfThreads = config.numOfThreads
        if self.numOfThreads < 1:
            logging.error("At least ONE thread must be given!")
            sys.exit()

        # create folder for file-specific log-files.
        # existing files (with the same name) will be OVERWRITTEN!
        self.outputBase = OUTPUT_PATH + self.name + "." + self.date
        self.logFolder = self.outputBase + ".logfiles/"
        if not os.path.isdir(self.logFolder):
            os.makedirs(self.logFolder)

        # get global options
        self.options = getOptionsFromXML(rootTag)

        # get columns
        self.columns = Benchmark.loadColumns(rootTag.find("columns"))

        # get global source files, they are used in all run sets
        globalSourcefilesTags = rootTag.findall("sourcefiles")

        # get required files
        self._requiredFiles = []
        baseDir = os.path.dirname(self.benchmarkFile)
        for requiredFilesTag in rootTag.findall('requiredfiles'):
            requiredFiles = Util.expandFileNamePattern(requiredFilesTag.text, baseDir)
            if not requiredFiles:
                logging.warning('Pattern {0} in requiredfiles tag did not match any file.'.format(requiredFilesTag.text))
            self._requiredFiles.extend(requiredFiles)

        # get requirements
        self.requirements = Requirements(rootTag.findall("require"), self.rlimits, config.cloudCPUModel)

        self.resultFilesPattern = None
        resultFilesTags = rootTag.findall("resultfiles")
        if resultFilesTags:
            if len(resultFilesTags) > 1:
                logger.warning("Benchmark file {0} has multiple <resultfiles> tags, ignoring all but the first.")
            self.resultFilesPattern = resultFilesTags[0].text

        # get benchmarks
        self.runSets = []
        for (i, rundefinitionTag) in enumerate(rootTag.findall("rundefinition")):
            self.runSets.append(RunSet(rundefinitionTag, self, i+1, globalSourcefilesTags))

        if not self.runSets:
            for (i, rundefinitionTag) in enumerate(rootTag.findall("test")):
                self.runSets.append(RunSet(rundefinitionTag, self, i+1, globalSourcefilesTags))
            if self.runSets:
                logging.warning("Benchmark file {0} uses deprecated <test> tags. Please rename them to <rundefinition>.".format(benchmarkFile))
            else:
                logging.warning("Benchmark file {0} specifies no runs to execute (no <rundefinition> tags found).".format(benchmarkFile))


    def requiredFiles(self):
        return self._requiredFiles + self.tool.getProgrammFiles(self.executable)


    def workingDirectory(self):
        return self.tool.getWorkingDirectory(self.executable)


    def getEnvironments(self):
        return self.tool.getEnvironments(self.executable)


    @staticmethod
    def loadColumns(columnsTag):
        """
        @param columnsTag: the columnsTag from the XML file
        @return: a list of Columns()
        """

        logging.debug("I'm loading some columns for the outputfile.")
        columns = []
        if columnsTag != None: # columnsTag is optional in XML file
            for columnTag in columnsTag.findall("column"):
                pattern = columnTag.text
                title = columnTag.get("title", pattern)
                numberOfDigits = columnTag.get("numberOfDigits") # digits behind comma
                column = Column(pattern, title, numberOfDigits)
                columns.append(column)
                logging.debug('Column "{0}" with title "{1}" loaded from XML file.'
                          .format(column.text, column.title))
        return columns


class RunSet:
    """
    The class RunSet manages the import of files and options of a run set.
    """

    def __init__(self, rundefinitionTag, benchmark, index, globalSourcefilesTags=[]):
        """
        The constructor of RunSet reads run-set name and the source files from rundefinitionTag.
        Source files can be included or excluded, and imported from a list of
        names in another file. Wildcards and variables are expanded.
        @param rundefinitionTag: a rundefinitionTag from the XML file
        """

        self.benchmark = benchmark

        # get name of run set, name is optional, the result can be "None"
        self.realName = rundefinitionTag.get("name")

        # index is the number of the run set
        self.index = index

        self.logFolder = benchmark.logFolder
        if self.realName:
            self.logFolder += self.realName + "."

        # get all run-set-specific options from rundefinitionTag
        self.options = benchmark.options + getOptionsFromXML(rundefinitionTag)

        # get all runs, a run contains one sourcefile with options
        self.blocks = self.extractRunsFromXML(globalSourcefilesTags + rundefinitionTag.findall("sourcefiles"))
        self.runs = [run for block in self.blocks for run in block.runs]

        names = [self.realName]
        if len(self.blocks) == 1:
            # there is exactly one source-file set to run, append its name to run-set name
            names.append(self.blocks[0].realName)
        self.name = '.'.join(filter(None, names))
        self.fullName = self.benchmark.name + (("." + self.name) if self.name else "")

        # Currently we store logfiles as "basename.log",
        # so we cannot distinguish sourcefiles in different folder with same basename.
        # For a 'local benchmark' this causes overriding of logfiles after reading them,
        # so the result is correct, only the logfile is gone.
        # For 'cloud-mode' the logfile is overridden before reading it,
        # so the result will be wrong and every measured value will be missing.
        if self.shouldBeExecuted():
            sourcefilesSet = set()
            for run in self.runs:
                base = os.path.basename(run.sourcefile)
                if base in sourcefilesSet:
                    logging.warning("sourcefile with basename '" + base + 
                    "' appears twice in runset. This could cause problems with equal logfile-names.")
                else:
                    sourcefilesSet.add(base)
            del sourcefilesSet


    def shouldBeExecuted(self):
        return not self.benchmark.config.selectedRunDefinitions \
            or self.realName in self.benchmark.config.selectedRunDefinitions


    def extractRunsFromXML(self, sourcefilesTagList):
        '''
        This function builds a list of SourcefileSets (containing filename with options).
        The files and their options are taken from the list of sourcefilesTags.
        '''
        # runs are structured as sourcefile sets, one set represents one sourcefiles tag
        blocks = []
        baseDir = os.path.dirname(self.benchmark.benchmarkFile)

        for index, sourcefilesTag in enumerate(sourcefilesTagList):
            sourcefileSetName = sourcefilesTag.get("name")
            matchName = sourcefileSetName or str(index)
            if self.benchmark.config.selectedSourcefileSets \
                and matchName not in self.benchmark.config.selectedSourcefileSets:
                    continue

            # get list of filenames
            sourcefiles = self.getSourcefilesFromXML(sourcefilesTag, baseDir)

            # get file-specific options for filenames
            fileOptions = getOptionsFromXML(sourcefilesTag)

            currentRuns = []
            for sourcefile in sourcefiles:
                currentRuns.append(Run(sourcefile, fileOptions, self))

            blocks.append(SourcefileSet(sourcefileSetName, index, currentRuns))
        return blocks


    def getSourcefilesFromXML(self, sourcefilesTag, baseDir):
        sourcefiles = []

        # get included sourcefiles
        for includedFiles in sourcefilesTag.findall("include"):
            sourcefiles += self.expandFileNamePattern(includedFiles.text, baseDir)

        # get sourcefiles from list in file
        for includesFilesFile in sourcefilesTag.findall("includesfile"):

            for file in self.expandFileNamePattern(includesFilesFile.text, baseDir):

                # check for code (if somebody confuses 'include' and 'includesfile')
                if Util.isCode(file):
                    logging.error("'" + file + "' seems to contain code instead of a set of source file names.\n" + \
                        "Please check your benchmark definition file or remove bracket '{' from this file.")
                    sys.exit()

                # read files from list
                fileWithList = open(file, 'rt')
                for line in fileWithList:

                    # strip() removes 'newline' behind the line
                    line = line.strip()

                    # ignore comments and empty lines
                    if not Util.isComment(line):
                        sourcefiles += self.expandFileNamePattern(line, os.path.dirname(file))

                fileWithList.close()

        # remove excluded sourcefiles
        for excludedFiles in sourcefilesTag.findall("exclude"):
            excludedFilesList = self.expandFileNamePattern(excludedFiles.text, baseDir)
            for excludedFile in excludedFilesList:
                sourcefiles = Util.removeAll(sourcefiles, excludedFile)

        for excludesFilesFile in sourcefilesTag.findall("excludesfile"):
            for file in self.expandFileNamePattern(excludesFilesFile.text, baseDir):
                # read files from list
                fileWithList = open(file, 'rt')
                for line in fileWithList:

                    # strip() removes 'newline' behind the line
                    line = line.strip()

                    # ignore comments and empty lines
                    if not Util.isComment(line):
                        excludedFilesList = self.expandFileNamePattern(line, os.path.dirname(file))
                        for excludedFile in excludedFilesList:
                            sourcefiles = Util.removeAll(sourcefiles, excludedFile)

                fileWithList.close()

        return sourcefiles


    def expandFileNamePattern(self, pattern, baseDir):
        """
        The function expandFileNamePattern expands a filename pattern to a sorted list
        of filenames. The pattern can contain variables and wildcards.
        If baseDir is given and pattern is not absolute, baseDir and pattern are joined.
        """

        # store pattern for fallback
        shortFileFallback = pattern

        # replace vars like ${benchmark_path},
        # with converting to list and back, we can use the function 'substituteVars()'
        expandedPattern = substituteVars([pattern], self)
        assert len(expandedPattern) == 1
        expandedPattern = expandedPattern[0]

        if expandedPattern != pattern:
            logging.debug("Expanded variables in expression {0} to {1}."
                .format(repr(pattern), repr(expandedPattern)))

        fileList = Util.expandFileNamePattern(expandedPattern, baseDir)

        # sort alphabetical,
        fileList.sort()

        if not fileList:
                logging.warning("No files found matching {0}."
                            .format(repr(pattern)))

        return fileList


class SourcefileSet():
    """
    A SourcefileSet contains a list of runs and a name.
    """
    def __init__(self, name, index, runs):
        self.realName = name # this name is optional
        self.name = name or str(index) # this name is always non-empty
        self.runs = runs


class Run():
    """
    A Run contains one sourcefile and options.
    """

    def __init__(self, sourcefile, fileOptions, runSet):
        self.sourcefile = sourcefile
        self.runSet = runSet
        self.benchmark = runSet.benchmark
        self.specificOptions = fileOptions # options that are specific for this run
        self.logFile = runSet.logFolder + os.path.basename(sourcefile) + ".log"
        
        # lets reduce memory-consumption: if 2 lists are equal, do not use the second one
        self.options = runSet.options + fileOptions if fileOptions else runSet.options # all options to be used when executing this run
        substitutedOptions = substituteVars(self.options, runSet, sourcefile)
        if substitutedOptions != self.options: self.options = substitutedOptions # for less memory again

        # Copy columns for having own objects in run
        # (we need this for storing the results in them).
        self.columns = [Column(c.text, c.title, c.numberOfDigits) for c in self.benchmark.columns]

        # dummy values, for output in case of interrupt
        self.status = ""
        self.cpuTime = 0
        self.wallTime = 0
        self.memUsage = None
        self.host = None
        self.category = result.CATEGORY_UNKNOWN


    def getCmdline(self):
        args = self.benchmark.tool.getCmdline(self.benchmark.executable, self.options, self.sourcefile)
        args = [os.path.expandvars(arg) for arg in args]
        args = [os.path.expanduser(arg) for arg in args]
        return args;


    def getPropFile(self):
        # get prp-file from options
        prpfile = None
        for option in self.options:
            if option.endswith('.prp'):
                assert prpfile == None
                prpfile = option
                break

        #if prpfile is None and not logPrpfileOnlyOnce:
        #    logging.warn("Could not find propertyfile in options. This will be logged only once!")

        # prpfile is relative to toolWorkingDir, we need it relativ to currentWorkingDir
        if prpfile is not None:
            prpfile = os.path.join(self.benchmark.tool.getWorkingDirectory(self.benchmark.executable), prpfile)
            assert os.path.isfile(prpfile)

        return prpfile


    def afterExecution(self, returnvalue, output, forceTimeout=False):

        rlimits = self.benchmark.rlimits
        isTimeout = forceTimeout or self._isTimeout()

        # calculation: returnvalue == (returncode * 256) + returnsignal
        # highest bit of returnsignal shows only whether a core file was produced, we clear it
        returnsignal = returnvalue & 0x7F
        returncode = returnvalue >> 8
        logging.debug("My subprocess returned {0}, code {1}, signal {2}.".format(returnvalue, returncode, returnsignal))
        self.status = self.benchmark.tool.getStatus(returncode, returnsignal, output, isTimeout)
        self.category = result.getResultCategory(self.sourcefile, self.status, self.getPropFile())
        self.benchmark.tool.addColumnValues(output, self.columns)

        # Tools sometimes produce a result even after a timeout.
        # This should not be counted, so we overwrite the result with TIMEOUT
        # here. if this is the case.
        # However, we don't want to forget more specific results like SEGFAULT,
        # so we do this only if the result is a "normal" one like TRUE.
        if self.status in result.STR_LIST and isTimeout:
            self.status = "TIMEOUT"
            self.category = result.CATEGORY_ERROR
        if returnsignal == 9 \
                and MEMLIMIT in rlimits \
                and self.memUsage \
                and int(self.memUsage) >= (rlimits[MEMLIMIT] * 1024 * 1024):
            self.status = 'OUT OF MEMORY'
            self.category = result.CATEGORY_ERROR


    def _isTimeout(self):
        ''' try to find out whether the tool terminated because of a timeout '''
        rlimits = self.benchmark.rlimits
        if SOFTTIMELIMIT in rlimits:
            limit = rlimits[SOFTTIMELIMIT]
        elif TIMELIMIT in rlimits:
            limit = rlimits[TIMELIMIT]
        else:
            limit = float('inf')

        return self.cpuTime > limit*0.99


class Column:
    """
    The class Column contains text, title and numberOfDigits of a column.
    """

    def __init__(self, text, title, numOfDigits):
        self.text = text
        self.title = title
        self.numberOfDigits = numOfDigits
        self.value = ""


class Requirements:
    '''
    This class wrappes the values for the requirements.
    It parses the tags from XML to get those values.
    If no values are found, at least the limits are used as requirements.
    If the user gives a cpuModel, it overrides the previous cpuModel.
    '''
    def __init__(self, tags, rlimits, cloudCPUModel):
        
        self.cpuModel = None
        self.memory   = None
        self.cpuCores = None
        
        for requireTag in tags:
            
            cpuModel = requireTag.get('cpuModel', None)
            if self.cpuModel is None:
                self.cpuModel = cpuModel
            else:
                raise Exception('Double specification of required CPU model.')

            cpuCores = requireTag.get('cpuCores', None)
            if self.cpuCores is None:
                if cpuCores is not None: self.cpuCores = int(cpuCores)
            else:
                raise Exception('Double specification of required CPU cores.')

            memory = requireTag.get('memory',   None)
            if self.memory is None:
                if memory is not None: self.memory = int(memory)
            else:
                raise Exception('Double specification of required memory.')

        # TODO check, if we have enough requirements to reach the limits        
        # TODO is this really enough? we need some overhead!
        if self.cpuCores is None:
            self.cpuCores = rlimits.get(CORELIMIT, None)

        if self.memory is None:
            self.memory = rlimits.get(MEMLIMIT, None)

        if cloudCPUModel is not None: # user-given model -> override value
            self.cpuModel = cloudCPUModel

        if self.cpuCores is not None and self.cpuCores <= 0:
            raise Exception('Invalid value {} for required CPU cores.'.format(self.cpuCores))

        if self.memory is not None and self.memory <= 0:
            raise Exception('Invalid value {} for required memory.'.format(self.memory))


    def __str__(self):
        s = ""
        if self.cpuModel:
            s += " CPU='" + self.cpuModel + "'"
        if self.cpuCores:
            s += " Cores=" + str(self.cpuCores)
        if self.memory:
            s += " Memory=" + str(self.memory) + "MB"

        return "Requirements:" + (s if s else " None")


