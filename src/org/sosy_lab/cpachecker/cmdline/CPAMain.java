/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.OptionCollector;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.pcc.proof_gen.ProofGenerator;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.io.Closeables;

public class CPAMain {

  /**
   * The directory where to look for configuration files for options like
   * "-predicateAbstraction" that get translated into a config file name.
   */
  private static final String DEFAULT_CONFIG_FILES_DIR = "config/%s.properties";

  private static final String CONFIGURATION_FILE_OPTION = "configuration.file";
  private static final String SPECIFICATION_FILE_OPTION = "specification";

  static class InvalidCmdlineArgumentException extends Exception {

    private static final long serialVersionUID = -6526968677815416436L;

    private InvalidCmdlineArgumentException(String msg) {
      super(msg);
    }
  }

  @Options(prefix="statistics")
  private static class ShutdownHook extends Thread {

    @Option(name="export", description="write some statistics to disk")
    private boolean exportStatistics = true;

    @Option(name="file",
        description="write some statistics to disk")
    @FileOption(FileOption.Type.OUTPUT_FILE)
    private File exportStatisticsFile = new File("Statistics.txt");

    @Option(name="print", description="print statistics to console")
    private boolean printStatistics = false;

    private final Configuration config;
    private final LogManager logManager;
    private final Thread mainThread;

    // if still null when run() is executed, analysis has been interrupted by user
    private CPAcheckerResult mResult = null;

    public ShutdownHook(Configuration pConfig, LogManager logger) throws InvalidConfigurationException {
      config = pConfig;
      logManager = logger;

      config.inject(this);
      mainThread = Thread.currentThread();
    }

    public void setResult(CPAcheckerResult pResult) {
      assert mResult == null;
      mResult = pResult;
    }

    // We want to use Thread.stop() to force the main thread to stop
    // when interrupted by the user.
    @SuppressWarnings("deprecation")
    @Override
    public void run() {
      boolean cancelled = false;

      if (mainThread.isAlive()) {
        // probably the user pressed Ctrl+C
        mainThread.interrupt();
        logManager.log(Level.INFO, "Stop signal received, waiting 2s for analysis to stop cleanly...");
        cancelled = true;

        try {
          mainThread.join(2000);
        } catch (InterruptedException e) {}
        if (mainThread.isAlive()) {
          logManager.log(Level.WARNING, "Analysis did not stop fast enough, forcing immediate termination now. This might prevent the statistics from being generated.");
          mainThread.stop();
        }
      }

      logManager.flush();
      System.out.flush();
      System.err.flush();
      if (mResult != null) {

        // setup output streams
        PrintStream console = printStatistics ? System.out : null;
        FileOutputStream file = null;

        if (exportStatistics && exportStatisticsFile != null) {
          try {
            com.google.common.io.Files.createParentDirs(exportStatisticsFile);
            file = new FileOutputStream(exportStatisticsFile);
          } catch (IOException e) {
            logManager.logUserException(Level.WARNING, e, "Could not write statistics to file");
          }
        }

        PrintStream stream = makePrintStream(mergeStreams(console, file));

        try {
          // print statistics
          mResult.printStatistics(stream);
          stream.println();

          if (cancelled) {
            stream.println(
                "***********************************************************************\n" +
                "* WARNING: Analysis interrupted!! The statistics might be unreliable! *\n" +
                "***********************************************************************\n");
          }

          // print result
          if (!printStatistics) {
            stream = makePrintStream(mergeStreams(System.out, file)); // ensure that result is printed to System.out
          }
          mResult.printResult(stream);

          String outputDirectory = config.getOutputDirectory();
          if (outputDirectory != null && mResult.getResult() != Result.NOT_YET_STARTED) {
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
      logManager.flush();
    }

    private static PrintStream makePrintStream(OutputStream stream) {
      if (stream instanceof PrintStream) {
        return (PrintStream)stream;
      } else {
        return new PrintStream(stream);
      }
    }
  }

  public static void main(String[] args) {
    // initialize various components
    Configuration cpaConfig = null;
    LogManager logManager = null;
    try {
      try {
        cpaConfig = createConfiguration(args);
      } catch (InvalidCmdlineArgumentException e) {
        System.err.println("Could not parse command line arguments: " + e.getMessage());
        System.exit(1);
      } catch (IOException e) {
        System.err.println("Could not read config file " + e.getMessage());
        System.exit(1);
      }

      logManager = new LogManager(cpaConfig);

    } catch (InvalidConfigurationException e) {
      System.err.println("Invalid configuration: " + e.getMessage());
      System.exit(1);
    }

    // create everything
    CPAchecker cpachecker = null;
    ShutdownHook shutdownHook = null;
    File cFile = null;
    try {
      shutdownHook = new ShutdownHook(cpaConfig, logManager);
      cpachecker = new CPAchecker(cpaConfig, logManager);
      cFile = getCodeFile(cpaConfig);
    } catch (InvalidConfigurationException e) {
      logManager.logUserException(Level.SEVERE, e, "Invalid configuration");
      System.exit(1);
    }

    // this is for catching Ctrl+C and printing statistics even in that
    // case. It might be useful to understand what's going on when
    // the analysis takes a lot of time...
    Runtime.getRuntime().addShutdownHook(shutdownHook);

    // run analysis
    CPAcheckerResult result = cpachecker.run(cFile.getPath());

    shutdownHook.setResult(result);

    // statistics are displayed by shutdown hook

    // create PCC proof (if enabled)
    try {
      ProofGenerator pccProofGenerator = new ProofGenerator(cpaConfig, logManager);
      // generate PCC proof
      pccProofGenerator.generateProof(result);
    } catch (InvalidConfigurationException e) {
      logManager.logUserException(Level.SEVERE, e, "Invalid configuration");
      System.exit(1);
    }
  }

  @Options
  private static class MainOptions {
    @Option(name="analysis.programNames",
        required=true,
        description="C programs to analyze (currently only one file is supported)")
    @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
    private List<File> programs;
  }

  static File getCodeFile(final Configuration cpaConfig) throws InvalidConfigurationException {
    MainOptions options = new MainOptions();
    cpaConfig.inject(options);

    if (options.programs.size() != 1) {
      throw new InvalidConfigurationException("Exactly one code file has to be given.");
    }

    return Iterables.getOnlyElement(options.programs);
  }

  static Configuration createConfiguration(String[] args)
          throws InvalidCmdlineArgumentException, IOException, InvalidConfigurationException {
    if (args == null || args.length < 1) {
      throw new InvalidCmdlineArgumentException("Configuration file or list of CPAs needed. Use -help for more information.");
    }

    // if there are some command line arguments, process them
    Map<String, String> cmdLineOptions = processArguments(args);

    // get name of config file (may be null)
    // and remove this from the list of options (it's not a real option)
    String configFile = cmdLineOptions.remove(CONFIGURATION_FILE_OPTION);

    Configuration.Builder config = Configuration.builder();
    if (configFile != null) {
      config.loadFromFile(configFile);
    }
    config.setOptions(cmdLineOptions);

    //normalizeValues();
    return config.build();
  }

  /**
   * Reads the arguments and process them.
   * @param args commandline arguments
   * @return a map with all options found in the command line
   * @throws InvalidCmdlineArgumentException if there is an error in the command line
   */
  private static Map<String, String> processArguments(String[] args) throws InvalidCmdlineArgumentException {
    Map<String, String> properties = new HashMap<String, String>();
    List<String> programs = new ArrayList<String>();

    Iterator<String> argsIt = Arrays.asList(args).iterator();

    while (argsIt.hasNext()) {
      String arg = argsIt.next();
      if (   handleArgument0("-cbmc",    "analysis.useCBMC", "true",            arg, properties)
          || handleArgument0("-stats",   "statistics.print", "true",            arg, properties)
          || handleArgument0("-noout",   "output.disable",   "true",            arg, properties)
          || handleArgument1("-outputpath",    "output.path",             arg, argsIt, properties)
          || handleArgument1("-logfile",       "log.file",                arg, argsIt, properties)
          || handleArgument1("-entryfunction", "analysis.entryFunction",  arg, argsIt, properties)
          || handleArgument1("-config",        CONFIGURATION_FILE_OPTION, arg, argsIt, properties)
          || handleArgument1("-timelimit",     "cpa.conditions.global.time.wall", arg, argsIt, properties)
          || handleMultipleArgument1("-spec",  SPECIFICATION_FILE_OPTION, arg, argsIt, properties)
      ) {
        // nothing left to do

      } else if (arg.equals("-cpas")) {
        if (argsIt.hasNext()) {
          properties.put("cpa", CompositeCPA.class.getName());
          properties.put(CompositeCPA.class.getSimpleName() + ".cpas", argsIt.next());
        } else {
          throw new InvalidCmdlineArgumentException("-cpas argument missing!");
        }

      } else if (arg.equals("-nolog")) {
        putIfNotExistent(properties, "log.level", "off");
        putIfNotExistent(properties, "log.consoleLevel", "off");

      } else if (arg.equals("-setprop")) {
        if (argsIt.hasNext()) {
          String[] bits = argsIt.next().split("=");
          if (bits.length != 2) {
            throw new InvalidCmdlineArgumentException("-setprop argument must be a key=value pair!");
          }
          putIfNotExistent(properties, bits[0], bits[1]);
        } else {
          throw new InvalidCmdlineArgumentException("-setprop argument missing!");
        }

      } else if ("-printOptions".equals(arg)) {
        boolean verbose = false;
        if (argsIt.hasNext()) {
          final String nextArg = argsIt.next();
          verbose = ("-v".equals(nextArg) || ("-verbose".equals(nextArg)));
        }
        System.out.println(OptionCollector.getCollectedOptions(verbose));
        System.exit(0);

      } else if ("-printUsedOptions".equals(arg)) {
        putIfNotExistent(properties, "log.usedOptions.export", "true");
        // interrupt thread before CPAchecker is run
        // this will stop CPAchecker before the actual analysis (hack)
        Thread.currentThread().interrupt();

        // this will disable all other output
        properties.put("log.consoleLevel", "OFF");

      } else if (arg.equals("-help") || arg.equals("-h")) {
        printHelp();

      } else if (arg.startsWith("-") && !(new File(arg).exists())) {
        String argName = arg.substring(1); // remove "-"
        File f = new File(String.format(DEFAULT_CONFIG_FILES_DIR, argName));

        if (argName.matches("^[a-zA-Z0-9-]+$") && f.exists()) {
          try {
            Files.checkReadableFile(f);
            putIfNotExistent(properties, CONFIGURATION_FILE_OPTION, f.getPath());
          } catch (FileNotFoundException e) {
            System.out.println("Invalid configuration " + argName + " (" + e.getMessage() + ")");
            System.exit(0);
          }

        } else {
          System.out.println("Invalid option " + arg);
          System.out.println("");
          printHelp();
        }

      } else {
        programs.add(arg);
      }
    }

    // arguments with non-specified options are considered as file names
    if (!programs.isEmpty()) {
      putIfNotExistent(properties, "analysis.programNames", Joiner.on(", ").join(programs));
    }
    return properties;
  }

  private static void printHelp() {
    System.out.println("CPAchecker " + CPAchecker.getVersion());
    System.out.println();
    System.out.println("OPTIONS:");
    System.out.println(" -config");
    System.out.println(" -cpas");
    System.out.println(" -spec");
    System.out.println(" -outputpath");
    System.out.println(" -logfile");
    System.out.println(" -entryfunction");
    System.out.println(" -timelimit");
    System.out.println(" -cbmc");
    System.out.println(" -stats");
    System.out.println(" -nolog");
    System.out.println(" -noout");
    System.out.println(" -setprop");
    System.out.println(" -printOptions [-v|-verbose]");
    System.out.println(" -printUsedOptions");
    System.out.println(" -help");
    System.out.println();
    System.out.println("More information on how to configure CPAchecker can be found in 'doc/Configuration.txt'.");
    System.exit(0);
  }

  private static void putIfNotExistent(Map<String, String> properties, String key, String value)
      throws InvalidCmdlineArgumentException {

    if (properties.containsKey(key)) {
      throw new InvalidCmdlineArgumentException("Duplicate option " + key + " specified on command-line");
    }

    properties.put(key, value);
  }

  /**
   * Handle a command line argument with no value.
   */
  private static boolean handleArgument0(String arg, String option, String value, String currentArg,
        Map<String, String> properties) throws InvalidCmdlineArgumentException {
    if (currentArg.equals(arg)) {
      putIfNotExistent(properties, option, value);
      return true;
    } else {
      return false;
    }
  }

  /**
   * Handle a command line argument with one value.
   */
  private static boolean handleArgument1(String arg, String option, String currentArg,
        Iterator<String> args, Map<String, String> properties)
        throws InvalidCmdlineArgumentException {
    if (currentArg.equals(arg)) {
      if (args.hasNext()) {
        putIfNotExistent(properties, option, args.next());
      } else {
        throw new InvalidCmdlineArgumentException(currentArg + " argument missing!");
      }
      return true;
    } else {
      return false;
    }
  }

  /**
   * Handle a command line argument with one value that may appear several times.
   */
  private static boolean handleMultipleArgument1(String arg, String option, String currentArg,
      Iterator<String> args, Map<String, String> properties)
      throws InvalidCmdlineArgumentException {
    if (currentArg.equals(arg)) {
      if (args.hasNext()) {

        String value = properties.get(option);
        if (value != null) {
          value = value + "," + args.next();
        } else {
          value = args.next();
        }
        properties.put(option, value);

      } else {
        throw new InvalidCmdlineArgumentException(currentArg + " argument missing!");
      }
      return true;
    } else {
      return false;
    }
  }
}
