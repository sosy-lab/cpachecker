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
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.ShutdownNotifier.ShutdownRequestListener;
import org.sosy_lab.cpachecker.core.algorithm.ProofGenerator;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;

import com.google.common.base.Strings;
import com.google.common.io.Closer;

public class CPAMain {

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

    // create everything
    ShutdownNotifier shutdownNotifier = ShutdownNotifier.create();
    CPAchecker cpachecker = null;
    ProofGenerator proofGenerator = null;
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

      cpachecker = new CPAchecker(cpaConfig, logManager, shutdownNotifier);
      if (options.doPCC) {
        proofGenerator = new ProofGenerator(cpaConfig, logManager, shutdownNotifier);
      }
    } catch (InvalidConfigurationException e) {
      logManager.logUserException(Level.SEVERE, e, "Invalid configuration");
      System.exit(1);
      return;
    }

    // This is for shutting down when Ctrl+C is caught.
    ShutdownHook shutdownHook = new ShutdownHook(shutdownNotifier);
    Runtime.getRuntime().addShutdownHook(shutdownHook);

    // This is for actually forcing a termination when CPAchecker
    // fails to shutdown within some time.
    ShutdownRequestListener forcedExitOnShutdown =
        ForceTerminationOnShutdown.createShutdownListener(logManager, shutdownHook);
    shutdownNotifier.register(forcedExitOnShutdown);

    // run analysis
    CPAcheckerResult result = cpachecker.run(options.programs);

    // We want to print the statistics completely now that we have come so far,
    // so we disable all the limits, shutdown hooks, etc.
    shutdownHook.disable();
    shutdownNotifier.unregister(forcedExitOnShutdown);
    ForceTerminationOnShutdown.cancelPendingTermination();
    limits.cancel();
    Thread.interrupted(); // clear interrupted flag

    // generated proof (if enabled)
    if (proofGenerator != null) {
      proofGenerator.generateProof(result);
    }

    try {
      printResultAndStatistics(result, outputDirectory, options, logManager);
    } catch (IOException e) {
      logManager.logUserException(Level.WARNING, e, "Could not write statistics to file");
    }

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

    @Option(name = "pcc.proofgen.doPCC", description = "Generate and dump a proof")
    private boolean doPCC = false;
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
  private static void printResultAndStatistics(CPAcheckerResult mResult,
      String outputDirectory, MainOptions options, LogManager logManager) throws IOException {

    // setup output streams
    PrintStream console = options.printStatistics ? System.out : null;
    OutputStream file = null;
    Closer closer = Closer.create();

    if (options.exportStatistics && options.exportStatisticsFile != null) {
      try {
        Files.createParentDirs(options.exportStatisticsFile);
        file = closer.register(options.exportStatisticsFile.asByteSink().openStream());
      } catch (IOException e) {
        logManager.logUserException(Level.WARNING, e, "Could not write statistics to file");
      }
    }

    PrintStream stream = makePrintStream(mergeStreams(console, file));

    try {
      // print statistics
      mResult.printStatistics(stream);
      stream.println();

      // print result
      if (!options.printStatistics) {
        stream = makePrintStream(mergeStreams(System.out, file)); // ensure that result is printed to System.out
      }
      mResult.printResult(stream);

      if (outputDirectory != null) {
        stream.println("More details about the verification run can be found in the directory \"" + outputDirectory + "\".");
      }

      stream.flush();
    } catch (Throwable t) {
      closer.rethrow(t);

    } finally {
      closer.close();
    }
  }

  private static PrintStream makePrintStream(OutputStream stream) {
    if (stream instanceof PrintStream) {
      return (PrintStream)stream;
    } else {
      return new PrintStream(stream);
    }
  }

  private CPAMain() { } // prevent instantiation
}