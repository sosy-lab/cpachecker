/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import static org.sosy_lab.cpachecker.cmdline.CPAMain.ERROR_EXIT_CODE;
import static org.sosy_lab.cpachecker.cmdline.CPAMain.ERROR_OUTPUT;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSortedSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.sosy_lab.common.configuration.OptionCollector;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.cpachecker.cmdline.CmdLineArgument.CmdLineArgument1;
import org.sosy_lab.cpachecker.cmdline.CmdLineArgument.PropertyAddingCmdLineArgument;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;

/**
 * This classes parses the CPAchecker command line arguments. To add a new argument, handle it in
 * {@link #processArguments(String[])} and list it in {@link #printHelp(PrintStream)}.
 */
class CmdLineArguments {

  private static final Splitter SETPROP_OPTION_SPLITTER = Splitter.on('=').trimResults().limit(2);

  /**
   * Exception thrown when something invalid is specified on the command line.
   */
  public static class InvalidCmdlineArgumentException extends Exception {

    private static final long serialVersionUID = -6526968677815416436L;

    InvalidCmdlineArgumentException(final String msg) {
      super(msg);
    }

    public InvalidCmdlineArgumentException(String msg, Throwable cause) {
      super(msg, cause);
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

  static final String SECURE_MODE_OPTION = "secureMode";
  static final String PRINT_USED_OPTIONS_OPTION = "log.usedOptions.export";

  private static final ImmutableSortedSet<CmdLineArgument> CMD_LINE_ARGS =
      ImmutableSortedSet.of(
          new PropertyAddingCmdLineArgument("-stats")
              .settingProperty("statistics.print", "true")
              .withDescription("collect statistics during the analysis and print them afterwards"),
          new PropertyAddingCmdLineArgument("-noout")
              .settingProperty("output.disable", "true")
              .withDescription("disable all output (except directly specified files)"),
          new PropertyAddingCmdLineArgument("-java")
              .settingProperty("language", "JAVA")
              .withDescription("language of the sourcefile"),
          new PropertyAddingCmdLineArgument("-32")
              .settingProperty("analysis.machineModel", "Linux32")
              .withDescription("set machine model to LINUX32"),
          new PropertyAddingCmdLineArgument("-64")
              .settingProperty("analysis.machineModel", "Linux64")
              .withDescription("set machine model to LINUX64"),
          new PropertyAddingCmdLineArgument("-preprocess")
              .settingProperty("parser.usePreprocessor", "true")
              .withDescription("execute a preprocessor before starting the analysis"),
          new PropertyAddingCmdLineArgument("-secureMode")
              .settingProperty(SECURE_MODE_OPTION, "true")
              .withDescription("allow to use only secure options"),
          new CmdLineArgument1("-witness", "witness.validation.file")
              .withDescription("the witness to validate"),
          new CmdLineArgument1("-outputpath", "output.path")
              .withDescription("where to store the files with results, statistics, logs"),
          new CmdLineArgument1("-logfile", "log.file").withDescription("set a direct logfile"),
          new CmdLineArgument1("-entryfunction", "analysis.entryFunction")
              .withDescription("set the initial function for the analysis"),
          new CmdLineArgument1("-config", CONFIGURATION_FILE_OPTION)
              .withDescription("set the configuration for the analysis"),
          new CmdLineArgument1("-timelimit", "limits.time.cpu")
              .withDescription("set a timelimit for the analysis"),
          new CmdLineArgument1("-sourcepath", "java.sourcepath")
              .withDescription("set the sourcepath for the analysis of Java programs"),
          new CmdLineArgument1("-cp", "-classpath", "java.classpath")
              .withDescription("set the classpath for the analysis of Java programs"),
          new CmdLineArgument1("-spec", "specification") {
            @Override
            void handleArg(Map<String, String> properties, String arg)
                throws InvalidCmdlineArgumentException {
              if (SPECIFICATION_FILES_PATTERN.matcher(arg).matches()) {
                arg = resolveSpecificationFileOrExit(arg);
              }
              appendOptionValue(properties, getOption(), arg);
            }
          }.withDescription("set the specification for the main analysis"),
          new CmdLineArgument("-cmc") {

            @Override
            void apply0(Map<String, String> properties, String pCurrentArg, Iterator<String> argsIt)
                throws InvalidCmdlineArgumentException {
              handleCmc(argsIt, properties);
            }
          }.withDescription("use conditional model checking"),
          new CmdLineArgument1("-cpas") {

            @Override
            void handleArg(Map<String, String> properties, String arg) {
              properties.put("cpa", CompositeCPA.class.getName());
              properties.put(CompositeCPA.class.getSimpleName() + ".cpas", arg);
            }
          }.withDescription("set CPAs for the analysis"),
          new PropertyAddingCmdLineArgument("-cbmc")
              .settingProperty("analysis.checkCounterexamples", "true")
              .settingProperty("counterexample.checker", "CBMC")
              .withDescription("use CBMC as counterexample checker"),
          new PropertyAddingCmdLineArgument("-nolog")
              .settingProperty("log.level", "off")
              .settingProperty("log.consoleLevel", "off")
              .withDescription("disable logging"),
          new PropertyAddingCmdLineArgument("-skipRecursion")
              .settingProperty("analysis.summaryEdges", "true")
              .settingProperty("cpa.callstack.skipRecursion", "true")
              .withDescription("skip recursive function calls"),
          new PropertyAddingCmdLineArgument("-benchmark")
              .settingProperty("output.disable", "true")
              .settingProperty("coverage.enabled", "false")
              .settingProperty("statistics.memory", "false")
              .withDescription(
                  "disable assertions and optional features such as output files for improved performance"),
          new CmdLineArgument1("-setprop") {

            @Override
            void handleArg(Map<String, String> properties, String arg)
                throws InvalidCmdlineArgumentException {
              List<String> bits = SETPROP_OPTION_SPLITTER.splitToList(arg);
              if (bits.size() != 2) {
                throw new InvalidCmdlineArgumentException(
                    "-setprop argument must be a key=value pair, but \"" + arg + "\" is not.");
              }
              putIfNotExistent(properties, bits.get(0), bits.get(1));
            }
          }.withDescription("set an option directly"),
          new CmdLineArgument("-printOptions") {

            @SuppressFBWarnings("DM_EXIT")
            @Override
            void apply0(Map<String, String> properties, String pCurrentArg, Iterator<String> argsIt)
                throws InvalidCmdlineArgumentException {
              boolean verbose = false;
              if (argsIt.hasNext()) {
                final String nextArg = argsIt.next();
                verbose = ("-v".equals(nextArg) || ("-verbose".equals(nextArg)));
              }
              PrintStream out = System.out;
              OptionCollector.collectOptions(verbose, true, out);
              System.exit(0);
            }
          }.withDescription("print all possible options on StdOut"),
          new PropertyAddingCmdLineArgument("-printUsedOptions")
              .settingProperty(PRINT_USED_OPTIONS_OPTION, "true")
              .settingProperty("analysis.disable", "true")
              .overridingProperty("log.consoleLevel", "SEVERE")
              .withDescription("print all used options"),
          new CmdLineArgument("-h", "-help") {

            @SuppressFBWarnings("DM_EXIT")
            @Override
            void apply0(
                Map<String, String> pProperties, String pCurrentArg, Iterator<String> pArgsIt)
                throws InvalidCmdlineArgumentException {
              printHelp(System.out);
              System.exit(0);
            }
          }.withDescription("print help message"));

  /**
   * Reads the arguments and process them.
   *
   * <p>In some special cases this method may terminate the VM.
   *
   * @param args commandline arguments
   * @return a map with all options found in the command line
   * @throws InvalidCmdlineArgumentException if there is an error in the command line
   */
  static Map<String, String> processArguments(final String[] args)
      throws InvalidCmdlineArgumentException {
    Map<String, String> properties = new HashMap<>();
    List<String> programs = new ArrayList<>();

    Iterator<String> argsIt = Arrays.asList(args).iterator();

    while (argsIt.hasNext()) {
      String arg = argsIt.next();
      boolean foundMatchingArg = false;
      for (CmdLineArgument cmdLineArg : CMD_LINE_ARGS) {
        if (cmdLineArg.apply(properties, arg, argsIt)) {
          foundMatchingArg = true;
          break;
        }
      }
      if (foundMatchingArg) {
        // nothing left to do
      } else if (arg.startsWith("-") && Files.notExists(Paths.get(arg))) {
        String argName = arg.substring(1); // remove "-"
        if (DEFAULT_CONFIG_FILES_PATTERN.matcher(argName).matches()) {
          Path configFile = findFile(DEFAULT_CONFIG_FILES_DIR, argName);

          if (configFile != null) {
            try {
              IO.checkReadableFile(configFile);
              putIfNotExistent(properties, CONFIGURATION_FILE_OPTION, configFile.toString());
            } catch (FileNotFoundException e) {
              ERROR_OUTPUT.println("Invalid configuration " + argName + " (" + e.getMessage() + ")");
              System.exit(ERROR_EXIT_CODE);
            }
          } else {
            ERROR_OUTPUT.println("Invalid option " + arg);
            ERROR_OUTPUT.println("If you meant to specify a configuration file, the file "
                + String.format(DEFAULT_CONFIG_FILES_DIR, argName) + " does not exist.");
            ERROR_OUTPUT.println("");
            printHelp(ERROR_OUTPUT);
            System.exit(ERROR_EXIT_CODE);
          }
        } else {
          ERROR_OUTPUT.println("Invalid option " + arg);
          ERROR_OUTPUT.println("");
          printHelp(ERROR_OUTPUT);
          System.exit(ERROR_EXIT_CODE);
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

  private static void handleCmc(final Iterator<String> argsIt, final Map<String, String> properties)
      throws InvalidCmdlineArgumentException {
    properties.put("analysis.restartAfterUnknown", "true");

    if (argsIt.hasNext()) {
      String newValue = argsIt.next();

      // replace "predicateAnalysis" with config/predicateAnalysis.properties etc.
      if (DEFAULT_CONFIG_FILES_PATTERN.matcher(newValue).matches()
          && Files.notExists(Paths.get(newValue))) {
        Path configFile = findFile(DEFAULT_CONFIG_FILES_DIR, newValue);

        if (configFile != null) {
          newValue = configFile.toString();
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

  private static void printHelp(PrintStream out) {
    out.println("CPAchecker " + CPAchecker.getVersion());
    out.println();
    out.println("OPTIONS:");
    for (CmdLineArgument cmdLineArg : CMD_LINE_ARGS) {
      out.println(" " + cmdLineArg);
    }
    out.println();
    out.println("You can also specify any of the configuration files in the directory config/");
    out.println("with -CONFIG_FILE, e.g., -predicateAnalysis for config/predicateAnalysis.properties.");
    out.println();
    out.println("More information on how to configure CPAchecker can be found in 'doc/Configuration.md'.");
  }

  static void putIfNotExistent(
      final Map<String, String> properties, final String key, final String value)
      throws InvalidCmdlineArgumentException {

    if (properties.containsKey(key) && !properties.get(key).equals(value)) {
      throw new InvalidCmdlineArgumentException(
          String.format(
              "Option %s specified twice on command-line with values '%s' and '%s'.",
              key, properties.get(key), value));
    }

    properties.put(key, value);
  }

  static void appendOptionValue(
      final Map<String, String> options, final String option, String newValue) {
    if (newValue != null) {
      String value = options.get(option);
      if (value != null) {
        value = value + "," + newValue;
      } else {
        value = newValue;
      }
      options.put(option, value);
    }
  }

  static String resolveSpecificationFileOrExit(String pSpecification) {
    Path specFile = findFile(SPECIFICATION_FILES_TEMPLATE, pSpecification);
    if (specFile != null) {
      return specFile.toString();
    }
    ERROR_OUTPUT.println(
        "Checking for property " + pSpecification + " is currently not supported by CPAchecker.");
    System.exit(ERROR_EXIT_CODE);
    return pSpecification;
  }

  /**
   * Try to locate a file whose (partial) name is given by the user,
   * using a file name template which is filled with the user given name.
   *
   * If the path is relative, it is first looked up in the current directory,
   * and (if the file does not exist there), it is looked up in the parent directory
   * of the code base.
   *
   * If the file cannot be found, null is returned.
   *
   * @param template The string template for the path.
   * @param name The value for filling in the template.
   * @return An absolute Path object pointing to an existing file or null.
   */
  private static @Nullable Path findFile(final String template, final String name) {
    final String fileName = String.format(template, name);

    Path file = Paths.get(fileName);

    // look in current directory first
    if (Files.exists(file)) {
      return file;
    }

    // look relative to code location second
    Path codeLocation;
    try {
      codeLocation =
          Paths.get(
              CmdLineArguments.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    } catch (SecurityException | URISyntaxException e) {
      ERROR_OUTPUT.println(
          "Cannot resolve paths relative to project directory of CPAchecker: " + e.getMessage());
      return null;
    }
    file = codeLocation.resolveSibling(fileName);
    if (Files.exists(file)) {
      return file;
    }

    return null;
  }
}
