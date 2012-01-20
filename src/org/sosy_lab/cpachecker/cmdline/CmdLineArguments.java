/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import org.sosy_lab.common.Files;
import org.sosy_lab.common.configuration.OptionCollector;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;

import com.google.common.base.Joiner;

/**
 * This classes parses the CPAchecker command line arguments.
 * To add a new argument, handle it in {@link #processArguments(String[])}
 * and list it in {@link #printHelp()}.
 */
class CmdLineArguments {

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

  /**
   * The directory where to look for configuration files for options like
   * "-predicateAbstraction" that get translated into a config file name.
   */
  private static final String DEFAULT_CONFIG_FILES_DIR = "config/%s.properties";

  static final String CONFIGURATION_FILE_OPTION = "configuration.file";

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
    if (args == null || args.length < 1) {
      throw new InvalidCmdlineArgumentException("Configuration file or list of CPAs needed. Use -help for more information.");
    }

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
          || handleMultipleArgument1("-spec",  "specification",           arg, argsIt, properties)
      ) {
        // nothing left to do

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
          String[] bits = s.split("=");
          if (bits.length != 2) {
            throw new InvalidCmdlineArgumentException("-setprop argument must be a key=value pair, but \"" + s + "\" is not.");
          }
          putIfNotExistent(properties, bits[0], bits[1]);
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

        String value = properties.get(option);
        if (value != null) {
          value = value + "," + args.next();
        } else {
          value = args.next();
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
