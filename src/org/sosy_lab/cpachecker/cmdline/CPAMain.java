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
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.converters.FileTypeConverter;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.cpachecker.cmdline.CmdLineArguments.InvalidCmdlineArgumentException;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.algorithm.ProofGenerator;

import com.google.common.base.Strings;

public class CPAMain {

  @SuppressWarnings("resource") // We don't close LogManager because it will be used from ShutdownHook.
  public static void main(String[] args) {
    // initialize various components
    Configuration cpaConfig = null;
    LogManager logManager = null;
    String outputDirectory = null;
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

      {
        // We want to be able to use options of type "File" with some additional
        // logic provided by FileTypeConverter, so we create such a converter,
        // add it to our Configuration object and to the the map of default converters.
        // The latter will ensure that it is used whenever a Configuration object
        // is created.
        FileTypeConverter fileTypeConverter = new FileTypeConverter(cpaConfig);
        outputDirectory = fileTypeConverter.getOutputDirectory();

        cpaConfig = Configuration.builder()
                            .copyFrom(cpaConfig)
                            .addConverter(FileOption.class, fileTypeConverter)
                            .build();

        Configuration.getDefaultConverters()
                     .put(FileOption.class, fileTypeConverter);
      }

      logManager = new BasicLogManager(cpaConfig);

    } catch (InvalidConfigurationException e) {
      System.err.println("Invalid configuration: " + e.getMessage());
      System.exit(1);
      return;
    }

    // create everything
    CPAchecker cpachecker = null;
    ShutdownHook shutdownHook = null;
    String programDenotation = null;
    ProofGenerator proofGenerator = null;
    try {
      MainOptions options = new MainOptions();
      cpaConfig.inject(options);
      if (Strings.isNullOrEmpty(options.programs)) {
        throw new InvalidConfigurationException("Please specify a program to analyze on the command line.");
      }
      dumpConfiguration(options, cpaConfig, logManager);
      programDenotation = getProgramDenotation(options);

      shutdownHook = new ShutdownHook(cpaConfig, logManager, outputDirectory);
      cpachecker = new CPAchecker(cpaConfig, logManager);
      proofGenerator = new ProofGenerator(cpaConfig, logManager);
    } catch (InvalidConfigurationException e) {
      logManager.logUserException(Level.SEVERE, e, "Invalid configuration");
      System.exit(1);
      return;
    }

    // this is for catching Ctrl+C and printing statistics even in that
    // case. It might be useful to understand what's going on when
    // the analysis takes a lot of time...
    Runtime.getRuntime().addShutdownHook(shutdownHook);

    // run analysis
    CPAcheckerResult result = cpachecker.run(programDenotation);

    shutdownHook.setResult(result);

    // statistics are displayed by shutdown hook

    // generated proof (if enabled)
    proofGenerator.generateProof(result);
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
    private File configurationOutputFile = new File("UsedConfiguration.properties");
  }

  static String getProgramDenotation(final MainOptions options) throws InvalidConfigurationException {
    return options.programs;
  }

  static void dumpConfiguration(MainOptions options, Configuration config,
      LogManager logManager) {
    if (options.configurationOutputFile != null) {
      try {
        Files.writeFile(options.configurationOutputFile, config.asPropertiesString());
      } catch (IOException e) {
        logManager.logUserException(Level.WARNING, e, "Could not dump configuration to file");
      }
    }
  }

  static Configuration createConfiguration(String[] args)
          throws InvalidCmdlineArgumentException, IOException, InvalidConfigurationException {
    // if there are some command line arguments, process them
    Map<String, String> cmdLineOptions = CmdLineArguments.processArguments(args);


    // get name of config file (may be null)
    // and remove this from the list of options (it's not a real option)
    String configFile = cmdLineOptions.remove(CmdLineArguments.CONFIGURATION_FILE_OPTION);

    Configuration.Builder config = Configuration.builder();
    if (configFile != null) {
      config.loadFromFile(configFile);
    }
    config.setOptions(cmdLineOptions);

    return config.build();
  }

  private CPAMain() { } // prevent instantiation
}