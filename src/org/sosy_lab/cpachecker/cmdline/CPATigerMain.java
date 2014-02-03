/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cmdline;

import static org.sosy_lab.common.DuplicateOutputStream.mergeStreams;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.converters.FileTypeConverter;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.cpachecker.cmdline.CmdLineArguments.InvalidCmdlineArgumentException;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.ShutdownNotifier.ShutdownRequestListener;
import org.sosy_lab.cpachecker.tiger.core.CPAtiger;
import org.sosy_lab.cpachecker.tiger.core.CPAtiger.AnalysisType;
import org.sosy_lab.cpachecker.tiger.core.CPAtigerResult;
import org.sosy_lab.cpachecker.tiger.fql.PredefinedCoverageCriteria;
import org.sosy_lab.cpachecker.tiger.util.NullOutputStream;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;

import com.google.common.base.Strings;
import com.google.common.io.Closeables;

public class CPATigerMain {

  @SuppressWarnings("resource") // We don't close LogManager
  public static void main(String[] args) {
    // initialize various components
    Configuration cpaConfig = null;
    LogManager logManager = null;
    String outputDirectory = null;
    try {
      try {
        Pair<Configuration, String> p = createConfiguration(args);
        cpaConfig = p.getFirst();
        outputDirectory = p.getSecond();
      } catch (InvalidCmdlineArgumentException e) {
        System.err.println("Could not process command line arguments: " + e.getMessage());
        System.exit(1);
      } catch (IOException e) {
        System.err.println("Could not read config file " + e.getMessage());
        System.exit(1);
      }

      logManager = new BasicLogManager(cpaConfig);

    } catch (InvalidConfigurationException e) {
      System.err.println("Invalid configuration: " + e.getMessage());
      System.exit(1);
      return;
    }

    // set analysis type: predicate, explicit, ...
    AnalysisType aType = getAnalysisType(cpaConfig);

    long timelimit = 0;
    String timelimitStr = cpaConfig.getProperty("cpatiger.timelimit");
    if (!Strings.isNullOrEmpty(timelimitStr)){
      timelimit = Long.parseLong(timelimitStr);
    }

    // create everything
    ShutdownNotifier shutdownNotifier = ShutdownNotifier.create();

    // This is for shutting down when Ctrl+C is caught.
    ShutdownHook shutdownHook = new ShutdownHook(shutdownNotifier);
    Runtime.getRuntime().addShutdownHook(shutdownHook);

    // This is for actually forcing a termination when CPAchecker
    // fails to shutdown within some time.
    ShutdownRequestListener forcedExitOnShutdown =
        ForceTerminationOnShutdown.createShutdownListener(logManager, shutdownHook);
    shutdownNotifier.register(forcedExitOnShutdown);


    CPAtiger cpatiger = null;
    ResourceLimitChecker limits = null;
    MainOptions options = new MainOptions();
    try {
      cpaConfig.inject(options);
      if (Strings.isNullOrEmpty(options.programs)) {
        throw new InvalidConfigurationException("Please specify a program to analyze on the command line.");
      }
      dumpConfiguration(options, cpaConfig, logManager);

      limits = ResourceLimitChecker.fromConfiguration(cpaConfig, logManager, shutdownNotifier);
      limits.start();

      String entryFunction = cpaConfig.getProperty("analysis.entryFunction");

      if (Strings.isNullOrEmpty(entryFunction)) {
        entryFunction = "main";
      }

      PrintStream lPrintStream = System.out;
      if (!Strings.isNullOrEmpty(cpaConfig.getProperty("output.disable"))) {
        lPrintStream = new PrintStream(NullOutputStream.getInstance());
      }

      boolean lStopOnImpreciseExecution = false;
      if (!Strings.isNullOrEmpty(cpaConfig.getProperty("cpatiger.simulation.stopOnImpreciseExecution"))) {
        lStopOnImpreciseExecution = true;
      }

      cpatiger = new CPAtiger(options.programs, entryFunction, shutdownNotifier, lPrintStream, aType, timelimit, lStopOnImpreciseExecution);
    } catch (InvalidConfigurationException e) {
      logManager.logUserException(Level.SEVERE, e, "Invalid configuration");
      System.exit(1);
      return;
    }



    // run analysis

    String fqlQuery = cpaConfig.getProperty("cpatiger.fqlquery");

    if (Strings.isNullOrEmpty(fqlQuery)) {

      String fqlBBQuery = cpaConfig.getProperty("cpatiger.fqlquery.bb");

      if (Strings.isNullOrEmpty(fqlBBQuery)) {

        String fqlBB2Query = cpaConfig.getProperty("cpatiger.fqlquery.bb2");

        if (Strings.isNullOrEmpty(fqlBB2Query)) {

          String fqlBB3Query = cpaConfig.getProperty("cpatiger.fqlquery.bb3");

          if (Strings.isNullOrEmpty(fqlBB3Query)) {
            fqlQuery = PredefinedCoverageCriteria.BASIC_BLOCK_COVERAGE;
          }
          else {
            fqlQuery = PredefinedCoverageCriteria.BASIC_BLOCK_3_COVERAGE;
          }
        }
        else {
          fqlQuery = PredefinedCoverageCriteria.BASIC_BLOCK_2_COVERAGE;
        }
      }
      else {
        fqlQuery = PredefinedCoverageCriteria.BASIC_BLOCK_COVERAGE;
      }
    }

    CPAtigerResult result;

    String lGoalIndex = cpaConfig.getProperty("cpatiger.goal");
    if (Strings.isNullOrEmpty(lGoalIndex)) {
      result = cpatiger.run(fqlQuery);
    }
    else {
      result = cpatiger.run(fqlQuery, Integer.parseInt(lGoalIndex));
    }

    // We want to print the statistics completely now that we have come so far,
    // so we disable all the limits, shutdown hooks, etc.
    shutdownHook.disable();
    shutdownNotifier.unregister(forcedExitOnShutdown);
    ForceTerminationOnShutdown.cancelPendingTermination();
    limits.cancel();
    Thread.interrupted(); // clear interrupted flag

    printResultAndStatistics(result, outputDirectory, options, logManager);

    System.out.flush();
    System.err.flush();
    logManager.flush();
  }



  @Options
  private static class BootstrapOptions {
    @Option(name="memorysafety.check",
        description="Whether to check for memory safety properties "
            + "(this can be specified by passing an appropriate .prp file to the -spec parameter).")
    private boolean checkMemsafety = false;

    @Option(name="memorysafety.config",
        description="When checking for memory safety properties, "
            + "use this configuration file instead of the current one.")
    @FileOption(Type.OPTIONAL_INPUT_FILE)
    private Path memsafetyConfig = null;
  }

  @Options
  private static class MainOptions {
    @Option(name="analysis.programNames",
        //required=true, NOT required because we want to give a nicer user message ourselves
        description="A String, denoting the programs to be analyzed")
    private String programs;

    @Option(name="configuration.dumpFile",
        description="Dump the complete configuration to a file.")
    @FileOption(FileOption.Type.OUTPUT_FILE)
    private Path configurationOutputFile = Paths.get("UsedConfiguration.properties");

    @Option(name="statistics.export", description="write some statistics to disk")
    private boolean exportStatistics = true;

    @Option(name="statistics.file",
        description="write some statistics to disk")
    @FileOption(FileOption.Type.OUTPUT_FILE)
    private Path exportStatisticsFile = Paths.get("Statistics.txt");

    @Option(name="statistics.print", description="print statistics to console")
    private boolean printStatistics = false;
  }

  private static void dumpConfiguration(MainOptions options, Configuration config,
      LogManager logManager) {
    if (options.configurationOutputFile != null) {
      try {
        Files.writeFile(options.configurationOutputFile, config.asPropertiesString());
      } catch (IOException e) {
        logManager.logUserException(Level.WARNING, e, "Could not dump configuration to file");
      }
    }
  }

  /**
   * Parse the command line, read the configuration file,
   * and setup the program-wide base paths.
   * @return A Configuration object and the output directory.
   */
  private static Pair<Configuration, String> createConfiguration(String[] args) throws InvalidConfigurationException, InvalidCmdlineArgumentException, IOException {
    // if there are some command line arguments, process them
    Map<String, String> cmdLineOptions = CmdLineArguments.processArguments(args);

    // get name of config file (may be null)
    // and remove this from the list of options (it's not a real option)
    String configFile = cmdLineOptions.remove(CmdLineArguments.CONFIGURATION_FILE_OPTION);

    // create initial configuration from config file and command-line arguments
    Configuration.Builder configBuilder = Configuration.builder();
    if (configFile != null) {
      configBuilder.loadFromFile(configFile);
    }
    configBuilder.setOptions(cmdLineOptions);
    Configuration config = configBuilder.build();

    // Get output directory and setup paths.
    Pair<Configuration, String> p = setupPaths(config);
    config = p.getFirst();
    String outputDirectory = p.getSecond();

    // Check if we should switch to another config because we are analyzing memsafety properties.
    BootstrapOptions options = new BootstrapOptions();
    config.inject(options);
    if (options.checkMemsafety) {
      if (options.memsafetyConfig == null) {
        throw new InvalidConfigurationException("Verifying memory safety is not supported if option memorysafety.config is not specified.");
      }
      config = Configuration.builder()
          .loadFromFile(options.memsafetyConfig)
          .setOptions(cmdLineOptions)
          .clearOption("memorysafety.check")
          .clearOption("memorysafety.config")
          .clearOption("output.disable")
          .clearOption("output.path")
          .clearOption("rootDirectory")
          .build();
    }

    return Pair.of(config, outputDirectory);
  }

  private static Pair<Configuration, String> setupPaths(Configuration pConfig) throws InvalidConfigurationException {
    // We want to be able to use options of type "File" with some additional
    // logic provided by FileTypeConverter, so we create such a converter,
    // add it to our Configuration object and to the the map of default converters.
    // The latter will ensure that it is used whenever a Configuration object
    // is created.
    FileTypeConverter fileTypeConverter = new FileTypeConverter(pConfig);
    String outputDirectory = fileTypeConverter.getOutputDirectory();

    Configuration config = Configuration.builder()
        .copyFrom(pConfig)
        .addConverter(FileOption.class, fileTypeConverter)
        .build();

    Configuration.getDefaultConverters()
    .put(FileOption.class, fileTypeConverter);

    return Pair.of(config, outputDirectory);
  }

  @SuppressWarnings("deprecation")
  private static void printResultAndStatistics(CPAtigerResult mResult,
      String outputDirectory, MainOptions options, LogManager logManager) {

    // setup output streams
    PrintStream console = options.printStatistics ? System.out : null;
    OutputStream file = null;

    if (options.exportStatistics && options.exportStatisticsFile != null) {
      try {
        Files.createParentDirs(options.exportStatisticsFile);
        file = options.exportStatisticsFile.asByteSink().openStream();
      } catch (IOException e) {
        logManager.logUserException(Level.WARNING, e, "Could not write statistics to file");
      }
    }

    PrintStream stream = makePrintStream(mergeStreams(console, file));

    try {
      // print statistics
      // TODO implement
      //mResult.printStatistics(stream);
      stream.println();

      // print result
      if (!options.printStatistics) {
        stream = makePrintStream(mergeStreams(System.out, file)); // ensure that result is printed to System.out
      }
      // TODO implement
      //mResult.printResult(stream);

      if (outputDirectory != null) {
        stream.println("More details about the verification run can be found in the directory \"" + outputDirectory + "\".");
      }

      stream.flush();

    } finally {
      // close only file, not System.out
      if (file != null) {
        Closeables.closeQuietly(file);
      }
    }
  }

  /**
   * Finds the analysis type from cmd arguments;
   * @param cpaConfig
   * @return
   */
  private static AnalysisType getAnalysisType(Configuration cpaConfig) {
    AnalysisType aType = AnalysisType.PREDICATE;
    String predStr = cpaConfig.getProperty("cpatiger.predicate");
    String expsimStr = cpaConfig.getProperty("cpatiger.explicit_simple");
    String exprefStr = cpaConfig.getProperty("cpatiger.explicit_ref");
    String exppredStr = cpaConfig.getProperty("cpatiger.explicit_predicate");

    boolean pred = !Strings.isNullOrEmpty(predStr);
    boolean expsim = !Strings.isNullOrEmpty(expsimStr);
    boolean expref = !Strings.isNullOrEmpty(exprefStr);
    boolean expred = !Strings.isNullOrEmpty(exppredStr);

    if (!pred && !expsim && !expref && !expred) {
      pred = true;
    }

    if (pred) {
      aType = AnalysisType.PREDICATE;
    }

    if (expsim) {
      aType = AnalysisType.EXPLICIT_SIMPLE;
    }

    if (expref) {
      aType = AnalysisType.EXPLICIT_REF;
    }

    if (expred) {
      aType = AnalysisType.EXPLICIT_PRED;
    }

    return aType;
  }

  private static PrintStream makePrintStream(OutputStream stream) {
    if (stream instanceof PrintStream) {
      return (PrintStream)stream;
    } else {
      return new PrintStream(stream);
    }
  }

  private CPATigerMain() { } // prevent instantiation
}