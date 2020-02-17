/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.stream.Collectors.toList;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;
import static org.sosy_lab.common.io.DuplicateOutputStream.mergeStreams;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.io.Closer;
import com.google.common.io.MoreFiles;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.matheclipse.core.util.WriterOutputStream;
import org.sosy_lab.common.Optionals;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.ShutdownNotifier.ShutdownRequestListener;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.converters.FileTypeConverter;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LoggingOptions;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cmdline.CmdLineArguments.InvalidCmdlineArgumentException;
import org.sosy_lab.cpachecker.core.CPAchecker;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.pcc.ProofGenerator;
import org.sosy_lab.cpachecker.core.counterexample.ReportGenerator;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser;
import org.sosy_lab.cpachecker.cpa.testtargets.TestTargetType;
import org.sosy_lab.cpachecker.util.Property;
import org.sosy_lab.cpachecker.util.Property.CommonCoverageType;
import org.sosy_lab.cpachecker.util.Property.CommonPropertyType;
import org.sosy_lab.cpachecker.util.PropertyFileParser;
import org.sosy_lab.cpachecker.util.PropertyFileParser.InvalidPropertyFileException;
import org.sosy_lab.cpachecker.util.SpecificationProperty;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.WitnessType;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;

public class CPAMain {

  static final int ERROR_EXIT_CODE = 1;

  @SuppressWarnings("resource") // We don't close LogManager
  public static void main(String[] args) {
    // CPAchecker uses American English for output,
    // so make sure numbers are formatted appropriately.
    Locale.setDefault(Locale.US);

    if (args.length == 0) {
      // be nice to user
      args = new String[] {"-help"};
    }

    // initialize various components
    Configuration cpaConfig = null;
    LoggingOptions logOptions;
    String outputDirectory = null;
    Set<SpecificationProperty> properties = null;
    try {
      try {
        Config p = createConfiguration(args);
        cpaConfig = p.configuration;
        outputDirectory = p.outputPath;
        properties = p.properties;
      } catch (InvalidCmdlineArgumentException e) {
        throw Output.fatalError("Could not process command line arguments: %s", e.getMessage());
      } catch (IOException e) {
        throw Output.fatalError("Could not read config file %s", e.getMessage());
      } catch (InterruptedException e) {
        throw Output.fatalError("Interrupted: %s", e.getMessage());
      }

      logOptions = new LoggingOptions(cpaConfig);

    } catch (InvalidConfigurationException e) {
      throw Output.fatalError("Invalid configuration: %s", e.getMessage());
    }
    final LogManager logManager = BasicLogManager.create(logOptions);
    cpaConfig.enableLogging(logManager);
    GlobalInfo.getInstance().storeLogManager(logManager);

    // create everything
    final ShutdownManager shutdownManager = ShutdownManager.create();
    final ShutdownNotifier shutdownNotifier = shutdownManager.getNotifier();
    CPAchecker cpachecker = null;
    ProofGenerator proofGenerator = null;
    ResourceLimitChecker limits = null;
    ReportGenerator reportGenerator = null;
    MainOptions options = new MainOptions();
    try {
      cpaConfig.inject(options);
      if (options.programs.isEmpty()) {
        throw new InvalidConfigurationException("Please specify a program to analyze on the command line.");
      }
      dumpConfiguration(options, cpaConfig, logManager);

      // generate correct frontend based on file language
      cpaConfig = detectFrontendLanguageIfNecessary(options, cpaConfig, logManager);

      limits = ResourceLimitChecker.fromConfiguration(cpaConfig, logManager, shutdownManager);
      limits.start();

      cpachecker = new CPAchecker(cpaConfig, logManager, shutdownManager);
      if (options.doPCC) {
        proofGenerator = new ProofGenerator(cpaConfig, logManager, shutdownNotifier);
      }
      reportGenerator =
          new ReportGenerator(cpaConfig, logManager, logOptions.getOutputFile(), options.programs);
    } catch (InvalidConfigurationException e) {
      logManager.logUserException(Level.SEVERE, e, "Invalid configuration");
      System.exit(ERROR_EXIT_CODE);
      return;
    }

    // This is for shutting down when Ctrl+C is caught.
    ShutdownHook shutdownHook = new ShutdownHook(shutdownManager);
    Runtime.getRuntime().addShutdownHook(shutdownHook);

    // This is for actually forcing a termination when CPAchecker
    // fails to shutdown within some time.
    ShutdownRequestListener forcedExitOnShutdown =
        ForceTerminationOnShutdown.createShutdownListener(logManager, shutdownHook);
    shutdownNotifier.register(forcedExitOnShutdown);

    // run analysis
    CPAcheckerResult result = cpachecker.run(options.programs, properties);

    // generated proof (if enabled)
    if (proofGenerator != null) {
      proofGenerator.generateProof(result);
    }

    // We want to print the statistics completely now that we have come so far,
    // so we disable all the limits, shutdown hooks, etc.
    shutdownHook.disable();
    shutdownNotifier.unregister(forcedExitOnShutdown);
    ForceTerminationOnShutdown.cancelPendingTermination();
    limits.cancel();
    Thread.interrupted(); // clear interrupted flag

    try {
      printResultAndStatistics(result, outputDirectory, options, reportGenerator, logManager);
    } catch (IOException e) {
      logManager.logUserException(Level.WARNING, e, "Could not write statistics to file");
    }

    System.out.flush();
    System.err.flush();
    logManager.flush();
  }

  // Default values for options from external libraries
  // that we want to override in CPAchecker.
  private static final ImmutableMap<String, String> EXTERN_OPTION_DEFAULTS = ImmutableMap.of(
      "log.level", Level.INFO.toString());

  private static final String SPECIFICATION_OPTION = "specification";
  private static final String ENTRYFUNCTION_OPTION = "analysis.entryFunction";
  public static final String APPROACH_NAME_OPTION = "analysis.name";

  @Options
  private static class BootstrapOptions {
    @Option(secure=true, name="memorysafety.config",
        description="When checking for memory safety properties, "
            + "use this configuration file instead of the current one.")
    @FileOption(Type.OPTIONAL_INPUT_FILE)
    private @Nullable Path memsafetyConfig = null;

    @Option(
        secure = true,
        name = "memorycleanup.config",
        description =
            "When checking for memory cleanup properties, "
                + "use this configuration file instead of the current one.")
    @FileOption(Type.OPTIONAL_INPUT_FILE)
    private @Nullable Path memcleanupConfig = null;

    @Option(secure=true, name="overflow.config",
        description="When checking for the overflow property, "
            + "use this configuration file instead of the current one.")
    @FileOption(Type.OPTIONAL_INPUT_FILE)
    private @Nullable Path overflowConfig = null;

    @Option(secure=true, name="termination.config",
        description="When checking for the termination property, "
            + "use this configuration file instead of the current one.")
    @FileOption(Type.OPTIONAL_INPUT_FILE)
    private @Nullable Path terminationConfig = null;

    @Option(
      secure = true,
      name = CmdLineArguments.PRINT_USED_OPTIONS_OPTION,
      description = "all used options are printed"
    )
    private boolean printUsedOptions = false;
  }

  @VisibleForTesting
  @Options
  protected static class MainOptions {
    @Option(
      secure = true,
      name = "analysis.programNames",
      //required=true, NOT required because we want to give a nicer user message ourselves
      description = "A String, denoting the programs to be analyzed"
    )
    private ImmutableList<String> programs = ImmutableList.of();

    @Option(secure=true,
        description="Programming language of the input program. If not given explicitly, "
            + "auto-detection will occur")
    // keep option name in sync with {@link CFACreator#language}, value might differ
    private Language language = null;

    @Option(secure=true, name="configuration.dumpFile",
        description="Dump the complete configuration to a file.")
    @FileOption(FileOption.Type.OUTPUT_FILE)
    private Path configurationOutputFile = Paths.get("UsedConfiguration.properties");

    @Option(secure=true, name="statistics.export", description="write some statistics to disk")
    private boolean exportStatistics = true;

    @Option(secure=true, name="statistics.file",
        description="write some statistics to disk")
    @FileOption(FileOption.Type.OUTPUT_FILE)
    private Path exportStatisticsFile = Paths.get("Statistics.txt");

    @Option(secure=true, name="statistics.print", description="print statistics to console")
    private boolean printStatistics = false;

    @Option(secure=true, name = "pcc.proofgen.doPCC", description = "Generate and dump a proof")
    private boolean doPCC = false;
  }

  private static void dumpConfiguration(MainOptions options, Configuration config,
      LogManager logManager) {
    if (options.configurationOutputFile != null) {
      try {
        IO.writeFile(
            options.configurationOutputFile, Charset.defaultCharset(), config.asPropertiesString());
      } catch (IOException e) {
        logManager.logUserException(Level.WARNING, e, "Could not dump configuration to file");
      }
    }
  }

  private static final ImmutableSet<? extends Property> MEMSAFETY_PROPERTY_TYPES =
      Sets.immutableEnumSet(
          CommonPropertyType.VALID_DEREF,
          CommonPropertyType.VALID_FREE,
          CommonPropertyType.VALID_MEMTRACK);

  /**
   * Parse the command line, read the configuration file, and setup the program-wide base paths.
   *
   * @return A Configuration object, the output directory, and the specification properties.
   */
  private static Config createConfiguration(String[] args)
      throws InvalidConfigurationException, InvalidCmdlineArgumentException, IOException,
          InterruptedException {
    // if there are some command line arguments, process them
    Map<String, String> cmdLineOptions = CmdLineArguments.processArguments(args);

    boolean secureMode = cmdLineOptions.remove(CmdLineArguments.SECURE_MODE_OPTION) != null;
    if (secureMode) {
      Configuration.enableSecureModeGlobally();
    }

    // Read property file if present and adjust cmdline options
    Set<SpecificationProperty> properties = handlePropertyFile(cmdLineOptions);

    // get name of config file (may be null)
    // and remove this from the list of options (it's not a real option)
    String configFile = cmdLineOptions.remove(CmdLineArguments.CONFIGURATION_FILE_OPTION);

    // create initial configuration
    // from default values, config file, and command-line arguments
    ConfigurationBuilder configBuilder = Configuration.builder();
    configBuilder.setOptions(EXTERN_OPTION_DEFAULTS);
    if (configFile != null) {
      configBuilder.setOption(APPROACH_NAME_OPTION, extractApproachNameFromConfigName(configFile));
      configBuilder.loadFromFile(configFile);
    }
    configBuilder.setOptions(cmdLineOptions);

    Configuration config = configBuilder.build();

    // We want to be able to use options of type "File" with some additional
    // logic provided by FileTypeConverter, so we create such a converter,
    // add it to our Configuration object and to the the map of default converters.
    // The latter will ensure that it is used whenever a Configuration object
    // is created.
    FileTypeConverter fileTypeConverter =
        secureMode
            ? FileTypeConverter.createWithSafePathsOnly(config)
            : FileTypeConverter.create(config);
    String outputDirectory = fileTypeConverter.getOutputDirectory();
    Configuration.getDefaultConverters().put(FileOption.class, fileTypeConverter);

    config =
        Configuration.builder()
            .copyFrom(config)
            .addConverter(FileOption.class, fileTypeConverter)
            .build();

    // Read witness file if present, switch to appropriate config and adjust cmdline options
    config = handleWitnessOptions(config, cmdLineOptions);

    BootstrapOptions options = new BootstrapOptions();
    config.inject(options);

    // Switch to appropriate config depending on property (if necessary)
    config = handlePropertyOptions(config, options, cmdLineOptions, properties);

    if (options.printUsedOptions) {
      config.dumpUsedOptionsTo(System.out);
    }

    return new Config(config, outputDirectory, properties);
  }

  private static String extractApproachNameFromConfigName(String configFilename) {
    String filename = Paths.get(configFilename).getFileName().toString();
    // remove the extension (most likely ".properties")
    return filename.contains(".") ? filename.substring(0, filename.lastIndexOf(".")) : filename;
  }

  private static final String LANGUAGE_HINT =
      String.format(
          " Please specify a language directly with the option 'language=%s'.",
          Arrays.toString(Language.values()));

  /**
   * Determines the frontend language based on the file endings of the given programs, if no
   * language is given by the user. If a language is detected, it is set in the given {@link
   * MainOptions} object and a new configuration for that language, based on the given
   * configuration, is returned.
   */
  @VisibleForTesting
  static Configuration detectFrontendLanguageIfNecessary(
      MainOptions pOptions, Configuration pConfig, LogManager pLogManager)
      throws InvalidConfigurationException {
    if (pOptions.language == null) {
      // if language was not specified by option, we determine the best matching language
      Language frontendLanguage;
      if (areJavaOptionsSet(pConfig)) {
        frontendLanguage = Language.JAVA;
      } else {
        frontendLanguage = detectFrontendLanguageFromFileEndings(pOptions.programs);
      }
      Preconditions.checkNotNull(frontendLanguage);
      ConfigurationBuilder configBuilder = Configuration.builder();
      configBuilder.copyFrom(pConfig);
      configBuilder.setOption("language", frontendLanguage.name());
      pConfig = configBuilder.build();
      pOptions.language = frontendLanguage;
      pLogManager.logf(Level.INFO, "Language %s detected and set for analysis", frontendLanguage);
    }
    Preconditions.checkNotNull(pOptions.language);
    return pConfig;
  }

  @SuppressWarnings("deprecation") // checking the properties directly is more maintainable
  private static boolean areJavaOptionsSet(Configuration pConfig) {
    // Make sure to keep this synchronized with EclipseJavaParser
    return pConfig.hasProperty("java.sourcepath") || pConfig.hasProperty("java.classpath");
  }

  private static Language detectFrontendLanguageFromFileEndings(ImmutableList<String> pPrograms)
      throws InvalidConfigurationException {
    checkArgument(!pPrograms.isEmpty(), "Empty list of programs");
    Language frontendLanguage = null;
    for (String program : pPrograms) {
      Language language;
      String suffix = program.substring(program.lastIndexOf(".") + 1);
      switch (suffix) {
        case "ll":
        case "bc":
          language = Language.LLVM;
          break;
        case "c":
        case "i":
        case "h":
        default:
          language = Language.C;
          break;
      }
      Preconditions.checkNotNull(language);
      if (frontendLanguage == null) { // first iteration
        frontendLanguage = language;
      }
      if (frontendLanguage != language) { // further iterations: check for conflicting endings
        throw new InvalidConfigurationException(
            String.format(
                    "Differing file formats detected: %s and %s files are declared for analysis.",
                    frontendLanguage, language)
                + LANGUAGE_HINT);
      }
    }
    return frontendLanguage;
  }

  private static final ImmutableMap<Property, TestTargetType> TARGET_TYPES =
      ImmutableMap.<Property, TestTargetType>builder()
          .put(CommonCoverageType.COVERAGE_BRANCH, TestTargetType.ASSUME)
          .put(CommonCoverageType.COVERAGE_CONDITION, TestTargetType.ASSUME)
          .put(CommonCoverageType.COVERAGE_ERROR, TestTargetType.ERROR_CALL)
          .put(CommonCoverageType.COVERAGE_STATEMENT, TestTargetType.STATEMENT)
          .build();

  private static Configuration handlePropertyOptions(
      Configuration config,
      BootstrapOptions options,
      Map<String, String> cmdLineOptions,
      Set<SpecificationProperty> pProperties)
      throws InvalidConfigurationException, IOException {
    Set<Property> properties =
        transformedImmutableSetCopy(pProperties, SpecificationProperty::getProperty);

    final Path alternateConfigFile;

    if (!Collections.disjoint(properties, MEMSAFETY_PROPERTY_TYPES)) {
      if (!MEMSAFETY_PROPERTY_TYPES.containsAll(properties)) {
        // Memsafety property cannot be checked with others in combination
        throw new InvalidConfigurationException(
            "Unsupported combination of properties: " + properties);
      }
      alternateConfigFile = check(options.memsafetyConfig, "memory safety", "memorysafety.config");
    } else if (properties.contains(CommonPropertyType.VALID_MEMCLEANUP)) {
      if (properties.size() != 1) {
        // MemCleanup property cannot be checked with others in combination
        throw new InvalidConfigurationException(
            "Unsupported combination of properties: " + properties);
      }
      alternateConfigFile = check(options.memcleanupConfig, "memory cleanup", "memorycleanup.config");
    } else if (properties.contains(CommonPropertyType.OVERFLOW)) {
      if (properties.size() != 1) {
        // Overflow property cannot be checked with others in combination
        throw new InvalidConfigurationException(
            "Unsupported combination of properties: " + properties);
      }
      alternateConfigFile = check(options.overflowConfig, "overflows", "overflow.config");
    } else if (properties.contains(CommonPropertyType.TERMINATION)) {
      // Termination property cannot be checked with others in combination
      if (properties.size() != 1) {
        throw new InvalidConfigurationException(
            "Unsupported combination of properties: " + properties);
      }
      alternateConfigFile = check(options.terminationConfig, "termination", "termination.config");
    } else if (properties.contains(CommonCoverageType.COVERAGE_ERROR)
        || properties.contains(CommonCoverageType.COVERAGE_BRANCH)
        || properties.contains(CommonCoverageType.COVERAGE_CONDITION)
        || properties.contains(CommonCoverageType.COVERAGE_STATEMENT)) {
      // coverage criterion cannot be checked with other properties in combination
      if (properties.size() != 1) {
        throw new InvalidConfigurationException(
            "Unsupported combination of properties: " + properties);
      }
      return Configuration.builder()
          .copyFrom(config)
          .setOption("testcase.targets.type", TARGET_TYPES.get(properties.iterator().next()).name())
          .build();
    } else {
      alternateConfigFile = null;
    }

    if (alternateConfigFile != null) {
      return Configuration.builder()
          .loadFromFile(alternateConfigFile)
          .setOptions(cmdLineOptions)
          .clearOption("memorysafety.config")
          .clearOption("memorycleanup.config")
          .clearOption("overflow.config")
          .clearOption("termination.config")
          .clearOption("output.disable")
          .clearOption("output.path")
          .clearOption("rootDirectory")
          .clearOption("witness.validation.file")
          .build();
    }
    return config;
  }

  private static Path check(Path config, String verificationTarget, String optionName)
      throws InvalidConfigurationException {
    if (config == null) {
      throw new InvalidConfigurationException(
          String.format(
              "Verifying %s is not supported if option %s is not specified.",
              verificationTarget, optionName));
    }
    return config;
  }

  private static final ImmutableMap<Property, String> SPECIFICATION_FILES =
      ImmutableMap.<Property, String>builder()
          .put(CommonPropertyType.REACHABILITY_LABEL, "sv-comp-errorlabel")
          .put(CommonPropertyType.REACHABILITY, "sv-comp-reachability")
          .put(CommonPropertyType.REACHABILITY_ERROR, "sv-comp-reachability")
          .put(CommonPropertyType.VALID_FREE, "sv-comp-memorysafety")
          .put(CommonPropertyType.VALID_DEREF, "sv-comp-memorysafety")
          .put(CommonPropertyType.VALID_MEMTRACK, "sv-comp-memorysafety")
          .put(CommonPropertyType.VALID_MEMCLEANUP, "sv-comp-memorycleanup")
          .put(CommonPropertyType.OVERFLOW, "sv-comp-overflow")
          .put(CommonPropertyType.DEADLOCK, "deadlock")
          // .put(CommonPropertyType.TERMINATION, "none needed")
          .build();

  private static Set<SpecificationProperty> handlePropertyFile(Map<String, String> cmdLineOptions)
      throws InvalidCmdlineArgumentException {
    List<String> specificationFiles =
        Splitter.on(',')
            .trimResults()
            .omitEmptyStrings()
            .splitToList(cmdLineOptions.getOrDefault(SPECIFICATION_OPTION, ""));

    List<String> propertyFiles =
        specificationFiles.stream().filter(file -> file.endsWith(".prp")).collect(toList());
    if (propertyFiles.isEmpty()) {
      return ImmutableSet.of();
    }
    if (propertyFiles.size() > 1) {
      throw new InvalidCmdlineArgumentException("Multiple property files are not supported.");
    }
    String propertyFile = propertyFiles.get(0);

    // Parse property files
    PropertyFileParser parser = new PropertyFileParser(Paths.get(propertyFile));
    try {
      parser.parse();
    } catch (InvalidPropertyFileException e) {
      throw new InvalidCmdlineArgumentException(
          String.format("Invalid property file '%s': %s", propertyFile, e.getMessage()), e);
    } catch (IOException e) {
      throw new InvalidCmdlineArgumentException(
          "Could not read property file: " + e.getMessage(), e);
    }

    // set the file from where to read the specification automaton
    ImmutableSet<SpecificationProperty> properties =
        transformedImmutableSetCopy(
            parser.getProperties(),
            prop ->
                new SpecificationProperty(
                    parser.getEntryFunction(),
                    prop,
                    Optional.ofNullable(SPECIFICATION_FILES.get(prop))
                        .map(CmdLineArguments::resolveSpecificationFileOrExit)));
    assert !properties.isEmpty();

    String specFiles =
        Optionals.presentInstances(
                properties
                    .stream()
                    .map(SpecificationProperty::getInternalSpecificationPath)
                    .distinct())
            .collect(Collectors.joining(","));
    cmdLineOptions.put(SPECIFICATION_OPTION, specFiles);
    if (cmdLineOptions.containsKey(ENTRYFUNCTION_OPTION)) {
      if (!cmdLineOptions.get(ENTRYFUNCTION_OPTION).equals(parser.getEntryFunction())) {
        throw new InvalidCmdlineArgumentException(
            "Mismatching names for entry function on command line and in property file");
      }
    } else {
      cmdLineOptions.put(ENTRYFUNCTION_OPTION, parser.getEntryFunction());
    }
    return properties;
  }

  @Options
  private static class WitnessOptions {
    @Option(
      secure = true,
      name = "witness.validation.file",
      description = "The witness to validate."
    )
    @FileOption(Type.OPTIONAL_INPUT_FILE)
    private @Nullable Path witness = null;

    @Option(
      secure = true,
      name = "witness.validation.violation.config",
      description =
          "When validating a violation witness, "
              + "use this configuration file instead of the current one."
    )
    @FileOption(Type.OPTIONAL_INPUT_FILE)
    private @Nullable Path violationWitnessValidationConfig = null;

    @Option(
      secure = true,
      name = "witness.validation.correctness.config",
      description =
          "When validating a correctness witness, "
              + "use this configuration file instead of the current one."
    )
    @FileOption(Type.OPTIONAL_INPUT_FILE)
    private @Nullable Path correctnessWitnessValidationConfig = null;
  }

  private static Configuration handleWitnessOptions(
      Configuration config, Map<String, String> overrideOptions)
      throws InvalidConfigurationException, IOException, InterruptedException {
    WitnessOptions options = new WitnessOptions();
    config.inject(options);
    if (options.witness == null) {
      return config;
    }

    WitnessType witnessType = AutomatonGraphmlParser.getWitnessType(options.witness);
    final Path validationConfigFile;
    switch (witnessType) {
      case VIOLATION_WITNESS:
        validationConfigFile = options.violationWitnessValidationConfig;
        String specs = overrideOptions.get(SPECIFICATION_OPTION);
        String witnessSpec = options.witness.toString();
        specs = specs == null ? witnessSpec : Joiner.on(',').join(specs, witnessSpec.toString());
        overrideOptions.put(SPECIFICATION_OPTION, specs);
        break;
      case CORRECTNESS_WITNESS:
        validationConfigFile = options.correctnessWitnessValidationConfig;
        overrideOptions.put(
            "invariantGeneration.kInduction.invariantsAutomatonFile", options.witness.toString());
        break;
      default:
        throw new InvalidConfigurationException(
            "Witness type " + witnessType + " of witness " + options.witness + " is not supported");
    }
    if (validationConfigFile == null) {
      throw new InvalidConfigurationException(
          "Validating (violation|correctness) witnesses is not supported if option witness.validation.(violation|correctness).config is not specified.");
    }
    return Configuration.builder()
        .copyFrom(config)
        .loadFromFile(validationConfigFile)
        .setOptions(overrideOptions)
        .clearOption("witness.validation.file")
        .clearOption("witness.validation.violation.config")
        .clearOption("witness.validation.correctness.config")
        .clearOption("output.path")
        .clearOption("rootDirectory")
        .build();
  }

  @SuppressWarnings("deprecation")
  private static void printResultAndStatistics(
      CPAcheckerResult mResult,
      String outputDirectory,
      MainOptions options,
      ReportGenerator reportGenerator,
      LogManager logManager)
      throws IOException {

    // setup output streams
    PrintStream console = options.printStatistics ? System.out : null;
    OutputStream file = null;
    @SuppressWarnings("resource") // not necessary for Closer, it handles this itself
    Closer closer = Closer.create();

    if (options.exportStatistics && options.exportStatisticsFile != null) {
      try {
        MoreFiles.createParentDirectories(options.exportStatisticsFile);
        file = closer.register(Files.newOutputStream(options.exportStatisticsFile));
      } catch (IOException e) {
        logManager.logUserException(Level.WARNING, e, "Could not write statistics to file");
      }
    }

    PrintStream stream = makePrintStream(mergeStreams(console, file));

    StringWriter statistics = new StringWriter();
    try {
      // print statistics
      PrintStream statisticsStream =
          makePrintStream(mergeStreams(stream, new WriterOutputStream(statistics)));
      mResult.printStatistics(statisticsStream);
      stream.println();

      // print result
      if (!options.printStatistics) {
        stream = makePrintStream(mergeStreams(System.out, file)); // ensure that result is printed to System.out
      }
      mResult.printResult(stream);

      // write output files
      mResult.writeOutputFiles();

      if (outputDirectory != null) {
        stream.println("More details about the verification run can be found in the directory \"" + outputDirectory + "\".");
      }

      stream.flush();
    } catch (Throwable t) {
      throw closer.rethrow(t);

    } finally {
      closer.close();
    }

    // export report
    if (mResult.getResult() != Result.NOT_YET_STARTED) {
      reportGenerator.generate(
          mResult.getResult(), mResult.getCfa(), mResult.getReached(), statistics.toString());
    }
  }

  @SuppressFBWarnings(
      value = "DM_DEFAULT_ENCODING",
      justification = "Default encoding is the correct one for stdout.")
  @SuppressWarnings("checkstyle:IllegalInstantiation") // ok for statistics
  private static PrintStream makePrintStream(OutputStream stream) {
    if (stream instanceof PrintStream) {
      return (PrintStream)stream;
    } else {
      // Default encoding is actually desired here because we output to the terminal,
      // so the default PrintStream constructor is ok.
      return new PrintStream(stream);
    }
  }

  private CPAMain() { } // prevent instantiation

  private static class Config {

    private final Configuration configuration;

    private final String outputPath;

    private final Set<SpecificationProperty> properties;

    public Config(
        Configuration pConfiguration, String pOutputPath, Set<SpecificationProperty> pProperties) {
      configuration = pConfiguration;
      outputPath = pOutputPath;
      properties = ImmutableSet.copyOf(pProperties);
    }
  }
}
