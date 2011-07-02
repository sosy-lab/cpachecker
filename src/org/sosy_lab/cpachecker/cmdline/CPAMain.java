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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.sosy_lab.common.DuplicateOutputStream;
import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.OptionCollector;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;

import com.google.common.base.Joiner;

public class CPAMain {

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

    @Option(name="file", type=Option.Type.OUTPUT_FILE,
        description="write some statistics to disk")
    private File exportStatisticsFile = new File("Statistics.txt");

    private final LogManager logManager;
    private final Thread mainThread;

    // if still null when run() is executed, analysis has been interrupted by user
    private CPAcheckerResult mResult = null;

    public ShutdownHook(Configuration config, LogManager logger) throws InvalidConfigurationException {
      config.inject(this);
      this.logManager = logger;
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
      if (mainThread.isAlive()) {
        // probably the user pressed Ctrl+C
        mainThread.interrupt();
        logManager.log(Level.INFO, "Stop signal received, waiting 2s for analysis to stop cleanly...");
        try {
          mainThread.join(2000);
        } catch (InterruptedException e) {}
        if (mainThread.isAlive()) {
          logManager.log(Level.WARNING, "Analysis did not stop fast enough, forcing it. This might prevent the statistics from being generated.");
          mainThread.stop();
        }
      }

      logManager.flush();
      System.out.flush();
      System.err.flush();
      if (mResult != null) {
        PrintStream stream = System.out;
        if (exportStatistics && exportStatisticsFile != null) {
          try {
            com.google.common.io.Files.createParentDirs(exportStatisticsFile);
            FileOutputStream file = new FileOutputStream(exportStatisticsFile);
            stream = new PrintStream(new DuplicateOutputStream(stream, file));
          } catch (IOException e) {
            logManager.log(Level.WARNING, "Could not write statistics to file ("
                + e.getMessage() + ")");
          }
        }
        mResult.printStatistics(stream);
      }
      logManager.flush();
    }
  }


  public static void main(String[] args) {
    // initialize various components
    //for(String s: args){
    //  System.out.println(s);
    //}
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

    final String[] cFilePaths = getCodeFilePaths(cpaConfig, logManager);

    // create everything
    CPAchecker cpachecker = null;
    ShutdownHook shutdownHook = null;
    try {
      shutdownHook = new ShutdownHook(cpaConfig, logManager);
      cpachecker = new CPAchecker(cpaConfig, logManager);
    } catch (InvalidConfigurationException e) {
      logManager.log(Level.SEVERE, "Invalid configuration:", e.getMessage());
      System.exit(1);
    }

    // this is for catching Ctrl+C and printing statistics even in that
    // case. It might be useful to understand what's going on when
    // the analysis takes a lot of time...
    Runtime.getRuntime().addShutdownHook(shutdownHook);

    // run analysis
    CPAcheckerResult result = cpachecker.run(cFilePaths);

    shutdownHook.setResult(result);

    // statistics are displayed by shutdown hook
  }

  static String[] getCodeFilePaths(final Configuration cpaConfig, final LogManager logManager){
    String[] names = cpaConfig.getPropertiesArray("analysis.programNames");
    String[] concurrentOption = cpaConfig.getPropertiesArray("analysis.concurrent");
    String paths[] = new String[names.length];
    boolean isConcurrent = false;

    // with option "-concurrent" multiple input files are allowed
    if (concurrentOption.length>0  && concurrentOption[0].equals("true")){
      isConcurrent = true;
    }

    if (!isConcurrent & names.length != 1) {
      logManager.log(Level.SEVERE, "Exactly one code file has to be given!");
      System.exit(1);
    }

    for(int i=0; i<names.length; i++){
      String name = names[i];
      File cFile = new File(name);
      if (!cFile.isAbsolute()) {
        cFile = new File(cpaConfig.getRootDirectory(), cFile.getPath());
      }

      try {
        Files.checkReadableFile(cFile);
      } catch (FileNotFoundException e) {
        logManager.log(Level.SEVERE, e.getMessage());
        System.exit(1);
      }

      paths[i] = cFile.getPath();
    }

    return paths;
  }

  static Configuration createConfiguration(String[] args)
          throws InvalidCmdlineArgumentException, IOException, InvalidConfigurationException {
    if (args == null || args.length < 1) {
      throw new InvalidCmdlineArgumentException("Need to specify at least configuration file or list of CPAs! Use -help for more information.");
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
      if (   handleArgument1("-outputpath", "output.path", arg, argsIt, properties)
          || handleArgument1("-logfile", "log.file", arg, argsIt, properties)
          || handleArgument1("-entryfunction", "analysis.entryFunction", arg, argsIt, properties)
          || handleArgument1("-config", CONFIGURATION_FILE_OPTION, arg, argsIt, properties)
          || handleArgument1("-spec", SPECIFICATION_FILE_OPTION, arg, argsIt, properties)
      ) {
        // nothing left to do

      } else if (arg.equals("-cpas")) {
        if (argsIt.hasNext()) {
          properties.put("cpa", CompositeCPA.class.getName());
          properties.put(CompositeCPA.class.getSimpleName() + ".cpas", argsIt.next());
        } else {
          throw new InvalidCmdlineArgumentException("-cpas argument missing!");
        }
      } else if (arg.equals("-dfs")) {
        properties.put("analysis.traversal.order", "dfs");
      } else if (arg.equals("-bfs")) {
        properties.put("analysis.traversal.order", "bfs");
      } else if (arg.equals("-topsort")) {
        properties.put("analysis.traversal.order", "topsort");
      } else if (arg.equals("-cbmc")) {
        properties.put("analysis.useCBMC", "true");
      } else if (arg.equals("-nolog")) {
        properties.put("log.level", "off");
        properties.put("log.consoleLevel", "off");
      }
      // option  "-concurrent" means that there are several program files, one per thread
      else if (arg.equals("-concurrent")) {
        properties.put("analysis.concurrent", "true");
      } else if (arg.equals("-setprop")) {
        if (argsIt.hasNext()) {
          String[] bits = argsIt.next().split("=");
          if (bits.length != 2) {
            throw new InvalidCmdlineArgumentException(
            "-setprop argument must be a key=value pair!");
          }
          properties.put(bits[0], bits[1]);
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
        properties.put("log.usedOptions.export", "true");
        // interrupt thread before CPAchecker is run
        // this will stop CPAchecker before the actual analysis (hack)
        Thread.currentThread().interrupt();

      } else if (arg.equals("-help")) {
        printHelp();

      } else if (arg.startsWith("-") && !(new File(arg).exists())) {
        System.out.println("Invalid option " + arg);
        System.out.println("");
        printHelp();

      } else {
        programs.add(arg);
      }
    }

    // arguments with non-specified options are considered as file names
    if (!programs.isEmpty()) {
      properties.put("analysis.programNames", Joiner.on(", ").join(programs));
    }
    System.out.println(properties.toString());
    return properties;
  }

  private static void printHelp() {
    System.out.println("OPTIONS:");
    System.out.println(" -config");
    System.out.println(" -cpas");
    System.out.println(" -spec");
    System.out.println(" -outputpath");
    System.out.println(" -logfile");
    System.out.println(" -entryfunction");
    System.out.println(" -dfs");
    System.out.println(" -bfs");
    System.out.println(" -cbmc");
    System.out.println(" -nolog");
    System.out.println(" -setprop");
    System.out.println(" -printOptions [-v|-verbose]");
    System.out.println(" -printUsedOptions");
    System.out.println(" -concurrent");
    System.out.println(" -help");
    System.exit(0);
  }

  /**
   * Handle a command line argument with one value.
   */
  private static boolean handleArgument1(String arg, String option, String currentArg,
        Iterator<String> args, Map<String, String> properties)
        throws InvalidCmdlineArgumentException {
    if (currentArg.equals(arg)) {
      if (args.hasNext()) {
        properties.put(option, args.next());
      } else {
        throw new InvalidCmdlineArgumentException(currentArg + " argument missing!");
      }
      return true;
    } else {
      return false;
    }
  }

// TODO implement this when you get really bored
//  private void normalizeValues() {
//    for (Enumeration<?> keys = propertyNames(); keys.hasMoreElements();) {
//      String k = (String) keys.nextElement();
//      String v = getProperty(k);
//
//      // trim heading and trailing blanks (at least Java 1.4.2 does not take care of trailing blanks)
//      String v0 = v;
//      v = v.trim();
//      if (!v.equals(v0)) {
//        put(k, v);
//      }
//
//      if ("true".equalsIgnoreCase(v) || "t".equalsIgnoreCase(v)
//            || "yes".equalsIgnoreCase(v) || "y".equalsIgnoreCase(v)) {
//        put(k, "true");
//      } else if ("false".equalsIgnoreCase(v) || "f".equalsIgnoreCase(v)
//            || "no".equalsIgnoreCase(v) || "n".equalsIgnoreCase(v)) {
//        put(k, "false");
//      }
//    }
//  }
}
