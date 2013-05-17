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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.configuration.OptionCollector;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

/**
 * This classes parses the CPAchecker command line arguments.
 * To add a new argument, handle it in {@link #processArguments(String[])}
 * and list it in {@link #printHelp()}.
 */
class CmdLineArguments {

  private static final Splitter SETPROP_OPTION_SPLITTER = Splitter.on('=').trimResults().limit(2);

  /**
   * Exception thrown when something invalid is specified on the command line.
   */
  static class InvalidCmdlineArgumentException extends Exception {

    private static final long serialVersionUID = -6526968677815416436L;

    private InvalidCmdlineArgumentException(String msg) {
      super(msg);
    }
  }

  private CmdLineArguments() { } // prevent instantiation, this is a static helper class

  private static final Pattern DEFAULT_CONFIG_FILES_PATTERN = Pattern.compile("^[a-zA-Z0-9-+]+$");

  /**
   * The directory where to look for configuration files for options like
   * "-predicateAbstraction" that get translated into a config file name.
   */
  private static final String DEFAULT_CONFIG_FILES_DIR = "config/%s.properties";

  static final String CONFIGURATION_FILE_OPTION = "configuration.file";

  private static final String CMC_CONFIGURATION_FILES_OPTION = "restartAlgorithm.configFiles";

  private static final Pattern SPECIFICATION_FILES_PATTERN = DEFAULT_CONFIG_FILES_PATTERN;
  private static final String SPECIFICATION_FILES_TEMPLATE = "config/specification/%s.spc";

  /**
   * Reads the arguments and process them.
   *
   * In some special cases this method may terminate the VM.
   *
   * @param args commandline arguments
   * @return a map with all options found in the command line
   * @throws InvalidCmdlineArgumentException if there is an error in the command line
   */
  static Map<String, String> processArguments(String[] args) throws InvalidCmdlineArgumentException {
    Map<String, String> properties = new HashMap<>();
    List<String> programs = new ArrayList<>();

    Iterator<String> argsIt = Arrays.asList(args).iterator();

    while (argsIt.hasNext()) {
      String arg = argsIt.next();
      if (   handleArgument0("-cbmc",    "analysis.useCBMC", "true",            arg, properties)
          || handleArgument0("-stats",   "statistics.print", "true",            arg, properties)
          || handleArgument0("-noout",   "output.disable",   "true",            arg, properties)
          || handleArgument0("-java",    "language",         "JAVA",            arg, properties)
          || handleArgument0("-32",      "analysis.machineModel", "Linux32",    arg, properties)
          || handleArgument0("-64",      "analysis.machineModel", "Linux64",    arg, properties)
          || handleArgument0("-preprocess",    "parser.usePreprocessor", "true", arg, properties)
          || handleArgument1("-outputpath",    "output.path",             arg, argsIt, properties)
          || handleArgument1("-logfile",       "log.file",                arg, argsIt, properties)
          || handleArgument1("-entryfunction", "analysis.entryFunction",  arg, argsIt, properties)
          || handleArgument1("-config",        CONFIGURATION_FILE_OPTION, arg, argsIt, properties)
          || handleArgument1("-timelimit",     "cpa.conditions.global.time.wall", arg, argsIt, properties)
          || handleArgument1("-sourcepath",    "java.sourcepath",         arg, argsIt, properties)
          || handleArgument1("-cp",            "java.classpath",          arg, argsIt, properties)
          || handleArgument1("-classpath",     "java.classpath",          arg, argsIt, properties)
          || handleMultipleArgument1("-spec",  "specification",           arg, argsIt, properties)
      ) {
        // nothing left to do
      } else if (arg.equals("-cmc")) {
        handleCmc(argsIt, properties);

      } else if (arg.equals("-cpas")) {
        if (argsIt.hasNext()) {
          properties.put("cpa", CompositeCPA.class.getName());
          properties.put(CompositeCPA.class.getSimpleName() + ".cpas", argsIt.next());
        } else {
          throw new InvalidCmdlineArgumentException("-cpas argument missing.");
        }

      } else if (arg.equals("-nolog")) {
        putIfNotExistent(properties, "log.level", "off");
        putIfNotExistent(properties, "log.consoleLevel", "off");

      } else if (arg.equals("-setprop")) {
        if (argsIt.hasNext()) {
          String s = argsIt.next();
          List<String> bits = Lists.newArrayList(SETPROP_OPTION_SPLITTER.split(s));
          if (bits.size() != 2) {
            throw new InvalidCmdlineArgumentException("-setprop argument must be a key=value pair, but \"" + s + "\" is not.");
          }
          putIfNotExistent(properties, bits.get(0), bits.get(1));
        } else {
          throw new InvalidCmdlineArgumentException("-setprop argument missing.");
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
        putIfNotExistent(properties, "analysis.disable", "true");

        // this will disable all other output
        properties.put("log.consoleLevel", "SEVERE");

      } else if (arg.equals("-help") || arg.equals("-h")) {
        printHelp();

      } else if (arg.startsWith("-") && !(new File(arg).exists())) {
        String argName = arg.substring(1); // remove "-"
        File f = new File(String.format(DEFAULT_CONFIG_FILES_DIR, argName));

        if (DEFAULT_CONFIG_FILES_PATTERN.matcher(argName).matches() && f.exists()) {
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

  private static void handleCmc(Iterator<String> argsIt, Map<String, String> properties)
      throws InvalidCmdlineArgumentException {
    properties.put("analysis.restartAfterUnknown", "true");

    if (argsIt.hasNext()) {
      String newValue = argsIt.next();

      // replace "predicateAnalysis" with config/predicateAnalysis.properties etc.
      if (DEFAULT_CONFIG_FILES_PATTERN.matcher(newValue).matches() && !(new File(newValue).exists())) {
        File f = new File(String.format(DEFAULT_CONFIG_FILES_DIR, newValue));

        if (f.exists()) {
          newValue = f.getPath();
        }
      }

      String value = properties.get(CMC_CONFIGURATION_FILES_OPTION);
      if (value != null) {
        value += "," + newValue;
      } else {
        value = newValue;
      }
      properties.put(CMC_CONFIGURATION_FILES_OPTION, value);

    } else {
      throw new InvalidCmdlineArgumentException("-cmc argument missing.");
    }
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
    System.out.println(" -java");
    System.out.println(" -32");
    System.out.println(" -64");
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
      throw new InvalidCmdlineArgumentException("Duplicate option " + key + " specified on command-line.");
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
        throw new InvalidCmdlineArgumentException(currentArg + " argument missing.");
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

        String newValue = args.next();
        if (arg.equals("-spec")
            && SPECIFICATION_FILES_PATTERN.matcher(newValue).matches()) {

          File specFile = new File(String.format(SPECIFICATION_FILES_TEMPLATE, newValue));
          if (specFile.exists()) {
            newValue = specFile.getAbsolutePath();
          } else {
            System.err.println("Checking for property " + newValue + " is currently not supported by CPAchecker.");
            System.exit(0);
          }
        }

        String value = properties.get(option);
        if (value != null) {
          value = value + "," + newValue;
        } else {
          value = newValue;
        }
        properties.put(option, value);

      } else {
        throw new InvalidCmdlineArgumentException(currentArg + " argument missing.");
      }
      return true;
    } else {
      return false;
    }
  }
}
