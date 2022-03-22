// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Verify.verify;
import static com.google.common.base.Verify.verifyNotNull;
import static java.util.function.Predicate.not;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.MoreCollectors;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.Classes;
import org.sosy_lab.common.JSON;
import org.sosy_lab.common.ProcessExecutor;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.DummyTargetState;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options(prefix = "mpiAlgorithm")
public class MPIPortfolioAlgorithm implements Algorithm, StatisticsProvider {

  private static final String MPI_BIN = "mpiexec";
  private static final String PYTHON3_BIN = "python3";

  private static final Path MPI_PYTHON_MAIN_PATH =
      Classes.getCodeLocation(MPIPortfolioAlgorithm.class)
          .resolveSibling(Path.of("scripts", "mpi_portfolio.py"));

  @Option(
      secure = true,
      required = true,
      description = "List of property-files to be run by the subprocesses.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private List<Path> configFiles;

  @Option(secure = true, description = "Max. amount of processes to be used by MPI.")
  private int numberProcesses;

  @Option(secure = true, description = "File containing the ip addresses to be used by MPI.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path hostfile;

  @Option(
      secure = true,
      description =
          "The MCA parameter ('Modular Component Architecture') "
              + "is available only on Open MPI frameworks. It might thus need to be "
              + "disabled if unavailable on the working machine.")
  private boolean disableMCAOptions;

  private final Configuration globalConfig;
  private final LogManager logger;
  private final ShutdownManager shutdownManager;
  private final Specification specification;
  private final MPIPortfolioAlgorithmStatistics stats;

  private final Map<String, Path> binaries;
  private final ImmutableList<SubanalysisConfig> subanalyses;
  private final int availableProcessors;

  private final String mpiArgs;

  public MPIPortfolioAlgorithm(
      Configuration config,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification)
      throws InvalidConfigurationException {
    config.inject(this);

    globalConfig = config;
    logger = checkNotNull(pLogger);
    shutdownManager = ShutdownManager.createWithParent(checkNotNull(pShutdownNotifier));
    specification = checkNotNull(pSpecification);
    stats = new MPIPortfolioAlgorithmStatistics();

    binaries = new HashMap<>();
    binaries.put(PYTHON3_BIN, getPathOrThrowError(PYTHON3_BIN));
    binaries.put(MPI_BIN, getPathOrThrowError(MPI_BIN));

    String subanalysesTimelimit = computeTimelimitForSubanalyses(config);

    ImmutableList.Builder<SubanalysisConfig> subanalysesBuilder = new ImmutableList.Builder<>();
    for (int i = 0; i < configFiles.size(); i++) {
      subanalysesBuilder.add(new SubanalysisConfig(i, subanalysesTimelimit));
    }
    subanalyses = subanalysesBuilder.build();

    stats.subanalysesTimelimit = subanalysesTimelimit;

    availableProcessors = Runtime.getRuntime().availableProcessors();
    verify(availableProcessors > 0);
    logger.logf(Level.INFO, "Found %d logical processors on main node", availableProcessors);

    if (numberProcesses < 1) {
      // The user has not specified an amount of copies of the program that are to be executed on
      // the given nodes. A heuristic is therefore computed below.

      numberProcesses = 1;
      numberProcesses *= availableProcessors;

      String numNodesEnv = System.getenv("AWS_BATCH_JOB_NUM_NODES");
      if (!isNullOrEmpty(numNodesEnv)) {
        logger.logf(
            Level.INFO,
            "Env variable 'AWS_BATCH_JOB_NUM_NODES' found with value '%s'. Continuing using this"
                + " value.",
            numNodesEnv);
        try {
          numberProcesses *= Integer.parseInt(numNodesEnv);
        } catch (NumberFormatException e) {
          throw new InvalidConfigurationException(
              "Env variable 'AWS_BATCH_JOB_NUM_NODES' does not contain a valid integer value", e);
        }
      }
    } else {
      logger.logf(
          Level.INFO,
          "Number of available processes specified (%d). Not using a heuristic.",
          numberProcesses);
    }

    if (configFiles.size() < numberProcesses) {
      numberProcesses = configFiles.size();
    }

    stats.noOfAlgorithmsExecuted = numberProcesses;
    stats.usedSubanalyses =
        FluentIterable.from(subanalyses)
            .filter(x -> x.getIndex() < numberProcesses)
            .transform(x -> x.getConfigName().toString())
            .join(Joiner.on(", "));

    if (hostfile == null) {
      String envVariable = System.getenv("HOST_FILE_PATH");
      if (!isNullOrEmpty(envVariable)) {
        hostfile = Path.of(envVariable);
        logger.logf(
            Level.INFO,
            "Found env variable 'HOST_FILE_PATH' ('%s'). Continuing using this value.",
            hostfile);
      } else {
        logger.log(
            Level.INFO,
            "Neither was a hostfile specified nor is one found in path. "
                + "Running analysis on the local machine only.");
      }
    }

    if (hostfile != null) {
      verify(
          hostfile.normalize().toFile().exists(),
          "Hostfile specified, but cannot find it at the given location '%s'",
          hostfile);
      stats.hostfilePath = hostfile.toString();
    }

    try (StringWriter stringWriter = new StringWriter()) {
      Map<String, Object> analysisMap = new LinkedHashMap<>();

      for (int i = 0; i < configFiles.size(); i++) {
        SubanalysisConfig subanalysis = subanalyses.get(i);
        analysisMap.putAll(subanalysis.buildCommandLine());
      }

      // The following settings are required for the child CPAchecker instances. They might be
      // executed on different machines and thus need these information for copying the results
      // back to the main node after completing their analysis.
      if (hostfile != null) {
        Map<String, String> networkSettings = new HashMap<>();

        String mainNodeIPAddress = null;
        try {
          mainNodeIPAddress =
              InetAddress.getByName(System.getProperty("user.name")).getHostAddress();
        } catch (IOException e) {
          logger.log(
              Level.WARNING,
              "Could not retrieve the ip address from the main node. Proceeding without it.");
          logger.logDebugException(
              e, "Failed to retrieve the ip address of the main node from PATH.");
        }

        if (mainNodeIPAddress != null) {
          networkSettings.put("main_node_ipv4_address", mainNodeIPAddress);
        }
        networkSettings.put("user_name_main_node", System.getProperty("user.name"));
        networkSettings.put("project_location_main_node", System.getProperty("user.dir"));

        analysisMap.put("network_settings", networkSettings);
      }

      JSON.writeJSONString(analysisMap, stringWriter);
      mpiArgs = stringWriter.toString();

    } catch (IOException e) {
      throw new InvalidConfigurationException(
          "Failed to create a valid CPAchecker-cmdline from the config", e);
    }
  }

  @SuppressWarnings("deprecation")
  private static String computeTimelimitForSubanalyses(Configuration pConfig)
      throws InvalidConfigurationException {
    if (!pConfig.hasProperty("limits.time.cpu")) {
      return SubanalysisConfig.SUBPROCESS_DEFAULT_TIMELIMIT;
    }

    @Nullable String property = pConfig.getProperty("limits.time.cpu");
    verify(!isNullOrEmpty(property));

    String rawValue = Iterables.get(Splitter.on('s').split(property), 0).strip();
    int limitMain;
    try {
      limitMain = Integer.parseInt(rawValue);
    } catch (NumberFormatException e) {
      throw new InvalidConfigurationException(
          String.format("Unable to turn the value '%s' into an int value", rawValue), e);
    }

    int limitSubanalyses;

    // the values below were arbitrarily chosen, but I found them to be reasonable.
    if (limitMain < 60) {
      limitSubanalyses = (int) (limitMain * 0.8);
    } else if (limitMain < 150) {
      limitSubanalyses = limitMain - 15;
    } else {
      limitSubanalyses = Math.max((int) (limitMain * 0.8), limitMain - 90);
    }

    verify(limitSubanalyses > 0);

    return limitSubanalyses + "s";
  }

  private static Path getPathOrThrowError(String pRequiredBin)
      throws InvalidConfigurationException {
    Optional<Path> pathOpt =
        Stream.of(System.getenv("PATH").split(Pattern.quote(File.pathSeparator)))
            .map(Path::of)
            .filter(path -> Files.exists(path.resolve(pRequiredBin)))
            .findFirst();
    if (pathOpt.isEmpty()) {
      throw new InvalidConfigurationException(
          pRequiredBin
              + " is required for performing the portfolio-analysis, but could not find it in"
              + " PATH");
    }

    return pathOpt.orElseThrow().resolve(pRequiredBin);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {

    List<String> cmdList = new ArrayList<>();
    cmdList.add(binaries.get(MPI_BIN).toString());

    // if no hostfile is specified, all CPAchecker instances
    // for the subanalyses will be executed on the local machine only
    if (hostfile != null) {

      // The MCA parameter ('Modular Component Architecture') is only available to Open MPI
      // frameworks
      if (!disableMCAOptions) {
        // Force Open MPI to only send messages via eth0.
        // https://stackoverflow.com/a/15256822
        cmdList.add("--mca");
        cmdList.add("btl_tcp_if_include");
        cmdList.add("eth0");
      }

      cmdList.add("-hostfile");
      cmdList.add(hostfile.normalize().toString());
    } else {
      // Setting the following option makes by default use of the 'use-hwthreads-as-cpus' option,
      // which leads to hardware threads being used as independent cpus.
      cmdList.add("--host");
      cmdList.add("localhost:" + numberProcesses);
    }

    cmdList.add("-np");
    cmdList.add(String.valueOf(numberProcesses));

    /*
     * The map-by argument uses the following syntax: ppr:N:resource:<options>
     *
     * ppr is short for 'processes per resource'. The above line means "assign N processes to each
     * resource of type 'resource' available on the host".
     *
     * An exhaustive description that explains all options in detail can be found at
     * https://www.open-mpi.org/doc/v3.0/man1/mpirun.1.php
     */
    cmdList.add("--map-by");
    cmdList.add("ppr:" + availableProcessors + ":core");

    cmdList.add(binaries.get(PYTHON3_BIN).toString());
    cmdList.add(MPI_PYTHON_MAIN_PATH.normalize().toString());
    logger.log(Level.INFO, "Executing command (arguments trimmed): " + cmdList);

    cmdList.add("--input");
    cmdList.add(mpiArgs);
    logger.log(Level.FINEST, "MPI arguments: " + mpiArgs);

    ProcessExecutor<IOException> executor = null;
    try {
      shutdownManager.getNotifier().shutdownIfNecessary();
      logger.log(Level.INFO, "Running subprocesses orchestrated by MPI");
      stats.mpiBinaryTotalTimer.start();
      try {
        executor =
            new ProcessExecutor<>(
                logger, IOException.class, Iterables.toArray(cmdList, String.class)) {

              @Override
              protected void handleOutput(String line) {
                checkNotNull(line);
                logger.logf(
                    Level.INFO, "%s - %s", "scripts/" + MPI_PYTHON_MAIN_PATH.getFileName(), line);
              }
            };

        int exitCode = executor.join();
        logger.log(Level.INFO, "MPI has finished its job. Continuing in main node.");

        if (exitCode != 0) {
          if (executor.getErrorOutput().stream()
              .anyMatch(x -> x.contains("unrecognized argument mca"))) {
            logger.log(
                Level.SEVERE,
                "The error log of the MPI binary indicates that the 'mca' option"
                    + "is not available. You can try executing this analysis again "
                    + "with this option disabled (mpiAlgorithm.disableMCAOptions=true)");
          }
          throw new CPAException("MPI script has failed with exit code " + exitCode);
        }
      } finally {
        stats.mpiBinaryTotalTimer.stop();
      }

      Optional<SubanalysisConfig> successfulAnalysisOpt = Optional.empty();
      for (SubanalysisConfig subconf : subanalyses) {
        Path logfilePath = subconf.getOutputPath().resolve(subconf.getLogfileName());
        if (logfilePath.toFile().exists()) {

          ImmutableList<String> subanalysisLog = null;
          try (Stream<String> lines = Files.lines(logfilePath)) {
            subanalysisLog =
                lines.filter(not(String::isBlank)).collect(ImmutableList.toImmutableList());
          }

          subconf.addResultLog(subanalysisLog);

          Optional<String> subanalysisResultOpt =
              subanalysisLog.stream()
                  .filter(x -> x.startsWith("Verification result:"))
                  .collect(MoreCollectors.toOptional());

          if (subanalysisResultOpt.isEmpty()) {
            continue;
          }

          Optional<CPAcheckerResult> resultOpt =
              CPAcheckerResult.parseResultString(subanalysisResultOpt.orElseThrow());
          CPAcheckerResult subanalyisResult = resultOpt.orElseThrow();
          subconf.addResult(subanalyisResult);

          if (subanalyisResult.getResult() == Result.UNKNOWN) {
            continue;
          }

          Result result = subanalyisResult.getResult();
          verify(result == Result.TRUE || result == Result.FALSE);
          logger.logf(
              Level.INFO,
              "Received the results for analysis '%s': %s",
              subconf.getConfigName(),
              result);
          successfulAnalysisOpt = Optional.of(subconf);
          break;
        }
      }

      if (successfulAnalysisOpt.isEmpty()) {
        logger.logf(Level.WARNING, "None of the subanalyses produced a result.");
      } else {
        SubanalysisConfig successfulAnalysis = successfulAnalysisOpt.orElseThrow();
        CPAcheckerResult result = successfulAnalysis.getResult();
        if (result.getResult() == Result.TRUE) {
          logger.logf(Level.FINE, "Returning result: TRUE");
          // One of the subanalyses returned "TRUE" as result, so an empty reachedset is
          // purposefully returned to reflect that in the main analysis
          pReachedSet.clear();

        } else if (result.getResult() == Result.FALSE) {
          logger.logf(Level.FINE, "Returning result: FALSE");
          // One of the subanalyses returned "FALSE" as result, so a reachedset with one dummy
          // targetstate is returned to reflect that in the main analysis
          pReachedSet.clear();
          pReachedSet.add(
              DummyTargetState.withSimpleTargetInformation(result.getTargetDescription()),
              SingletonPrecision.getInstance());
        }

        logger.log(Level.INFO, "Executed the following command for the successful subanalysis:");
        String formattedCmdline =
            FluentIterable.from(successfulAnalysis.getCmdLine())
                .transform(CharMatcher.whitespace()::removeFrom)
                .join(Joiner.on(" "));
        logger.log(Level.INFO, formattedCmdline);

        logger.log(Level.WARNING, "Subsequently the log of the successful subanalysis is printed");
        logger.log(Level.WARNING, "------------------- START SUBANALYSIS LOG -------------------");

        ImmutableList<String> resultLog = successfulAnalysis.getResultLog();
        verifyNotNull(resultLog);
        verify(!resultLog.isEmpty(), "Result log may not be empty");
        logger.log(Level.INFO, Joiner.on("\n\n").join(resultLog));

        logger.log(Level.WARNING, "-------------------- END SUBANALYSIS LOG --------------------");
        return AlgorithmStatus.SOUND_AND_PRECISE;
      }

    } catch (IOException e) {
      throw new CPAException("Execution of MPI failed", e);
    }

    // we didn't receive any results from the subanalyses, thus we can't tell anything about
    // them
    return AlgorithmStatus.UNSOUND_AND_IMPRECISE;
  }

  private class SubanalysisConfig {

    private static final String SUBPROCESS_DEFAULT_TIMELIMIT = "750s"; // arbitrarily chosen

    private static final String OUTPUT_DIR = "output";
    private static final String SUBANALYSIS_DIR = "output_portfolio-analysis_";

    private static final String ANALYSIS_KEY = "analysis";
    private static final String CMD_KEY = "cmd";
    private static final String OUPUT_KEY = "output";
    private static final String LOGFILE_KEY = "logfile";

    private final int subanalysis_index;

    // The following variables are specific to this subanalysis
    private final Path configPath;
    private final Path outputPath;
    private final Path logfileName;

    private final Configuration config;
    private final ImmutableList<String> cmdLine;

    private ImmutableList<String> resultLog = null;
    private CPAcheckerResult result = null;

    SubanalysisConfig(int index, String pSubanalysesTimelimit)
        throws InvalidConfigurationException {
      subanalysis_index = index;
      checkArgument(!isNullOrEmpty(pSubanalysesTimelimit));

      configPath = configFiles.get(subanalysis_index);
      outputPath = Path.of(OUTPUT_DIR, SUBANALYSIS_DIR + subanalysis_index);
      logfileName = Path.of(SUBANALYSIS_DIR + subanalysis_index + ".log");
      String specPath = Joiner.on(", ").join(specification.getFiles());

      /*
       * Hack to setup the desired config options for the child CPAchecker processes. The idea is to
       * keep all (user-)configurations except the ones necessary for running this portfolio
       * analysis.
       *
       * In other words, if the sub-analysis is e.g. a predicateAnalysis, then keep all
       * configurations (especially those manually set by the user) and remove any configuration
       * options that are necessary only for the MPIPortfolioAlgorithm itself.
       */
      config =
          Configuration.builder()
              .copyFrom(globalConfig)
              .clearOption("mpiAlgorithm.hostfile")
              .clearOption("analysis.algorithm.MPI")
              .clearOption("mpiAlgorithm.configFiles")
              .clearOption("analysis.name")
              .clearOption("mpiAlgorithm.numberProcesses")
              .clearOption("mpiAlgorithm.disableMCAOptions")
              .setOption("limits.time.cpu", pSubanalysesTimelimit)
              .setOption("output.path", checkNotNull(outputPath.toString()))
              .setOption("specification", specPath)
              .build();

      // Bring the command-line into a format which is executable by a python-script
      ImmutableList.Builder<String> cmdLineBuilder = ImmutableList.builder();
      cmdLineBuilder.add("scripts/cpa.sh").add("-config").add(configPath.toString());
      for (String opt : Splitter.on('\n').omitEmptyStrings().split(config.asPropertiesString())) {
        cmdLineBuilder.add("-setprop").add(opt);
      }
      cmdLine = cmdLineBuilder.build();
    }

    ImmutableMap<String, ImmutableMap<String, Object>> buildCommandLine() {
      ImmutableMap.Builder<String, Object> builder = new ImmutableMap.Builder<>();
      builder.put(ANALYSIS_KEY, configPath.getFileName().toString());
      builder.put(CMD_KEY, cmdLine);
      builder.put(OUPUT_KEY, outputPath.toString());
      builder.put(LOGFILE_KEY, outputPath.resolve(logfileName).toString());

      return ImmutableMap.of("Analysis_" + subanalysis_index, builder.buildOrThrow());
    }

    int getIndex() {
      return subanalysis_index;
    }

    Path getConfigName() {
      return configPath.getFileName();
    }

    Path getOutputPath() {
      return outputPath;
    }

    Path getLogfileName() {
      return logfileName;
    }

    ImmutableList<String> getCmdLine() {
      return cmdLine;
    }

    @Nullable ImmutableList<String> getResultLog() {
      return resultLog;
    }

    void addResultLog(ImmutableList<String> pResultLog) {
      if (resultLog != null) {
        throw new RuntimeException("ResultLog is expected to be null");
      }
      resultLog = pResultLog;
    }

    @Nullable CPAcheckerResult getResult() {
      return result;
    }

    void addResult(CPAcheckerResult pResult) {
      if (result != null) {
        throw new RuntimeException("Result is expected to be null");
      }
      result = pResult;
    }

    @Override
    public String toString() {
      return String.format("Subanalysis_%d-%s", subanalysis_index, getConfigName());
    }
  }

  private static class MPIPortfolioAlgorithmStatistics implements Statistics {

    private int noOfAlgorithmsExecuted;
    private String usedSubanalyses;
    private String hostfilePath;
    private final Timer mpiBinaryTotalTimer = new Timer();
    private String subanalysesTimelimit;

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      pOut.println("Number of algorithms used:                  " + noOfAlgorithmsExecuted);
      pOut.println("Subanalyses executed by MPI:                " + usedSubanalyses);
      if (hostfilePath != null) {
        pOut.println("Hostfile path:                              " + hostfilePath);
      }
      pOut.println("MPI binary total execution time:         " + mpiBinaryTotalTimer);
      pOut.println("Timelimit for the CPAchecker sub-instances: " + subanalysesTimelimit);
    }

    @Override
    public @Nullable String getName() {
      return "MPI Portfolio Algorithm";
    }
  }
}
