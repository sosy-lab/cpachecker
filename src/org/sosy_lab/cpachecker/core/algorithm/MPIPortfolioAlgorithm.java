// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verify;
import static java.util.function.Predicate.not;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.MoreCollectors;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.sosy_lab.common.Classes;
import org.sosy_lab.common.JSON;
import org.sosy_lab.common.MoreStrings;
import org.sosy_lab.common.ProcessExecutor;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
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

  @Option(secure = true, required = true, description = "Number of processes to be used by MPI.")
  private int numberProcesses;

  @Option(description = "File containing the ip adresses to be used by MPI.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path hostfile;

  @Option(
    description = "Ip adress of the main node. Used by the CPAchecker child instances for "
        + "writing their results back to the output directory of the main node.")
  private String mainNodeIPAdress;

  private final Configuration globalConfig;
  private final LogManager logger;
  private final ShutdownManager shutdownManager;
  private final Specification specification;
  // private final MPIPortfolioAlgorithmStatistics stats; // TODO

  private final Map<String, Path> binaries;
  private final Map<Path, Path> subanalysesOutputPaths;
  private final Map<Path, Path> subanalysesLogfilePaths;

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

    binaries = new HashMap<>();
    binaries.put(PYTHON3_BIN, getPathOrThrowError(PYTHON3_BIN));
    binaries.put(MPI_BIN, getPathOrThrowError(MPI_BIN));

    subanalysesOutputPaths = new HashMap<>();
    subanalysesLogfilePaths = new HashMap<>();

    if (hostfile == null) {
      String envVariable = System.getenv("HOST_FILE_PATH");
      if (envVariable != null) {
        hostfile = Path.of(envVariable);
        logger.logf(
            Level.INFO,
            "Found env variable 'HOST_FILE_PATH' ('%s'). Continuing using this value.",
            hostfile.toString());
      } else {
        logger.log(
            Level.INFO,
            "Neither was a hostfile specified nor is one found in path. "
                + "Running analysis on the local machine only.");

        if (numberProcesses > 1) {
          logger.log(
              Level.WARNING,
              "No hostfile was given, but a number of available processes was specified. "
                  + "The sequential execution using MPI is not (yet) supported. Setting "
                  + "the number of processes to 1.");
          numberProcesses = 1;
        }
      }
    }

    if (hostfile != null) {
      verify(
          hostfile.normalize().toFile().exists(),
          "Hostfile specified, but cannot find it at the given location '%s'",
          hostfile.toString());
    }

    try (StringWriter stringWriter = new StringWriter();) {
      Map<String, Object> analysisMap = new LinkedHashMap<>();
      for (int i = 0; i < configFiles.size(); i++) {
        analysisMap.put("Analysis_" + i, createCommand(i));
      }

      // The following settings are required for the child CPAchecker instances. They might be
      // executed on different machines and thus need these information for copying the results
      // back to the main node after completing their analysis.
      if (hostfile != null) {
        Map<String, String> networkSettings = new HashMap<>();
        if (mainNodeIPAdress == null) {
          mainNodeIPAdress = InetAddress.getLocalHost().getHostAddress();
        }
        networkSettings.put("main_node_ipv4_address", mainNodeIPAdress);
        networkSettings.put("user_name_main_node", System.getProperty("user.name"));
        networkSettings.put("project_location_main_node", System.getProperty("user.dir"));

        analysisMap.put("network_settings", networkSettings);
      }

      JSON.writeJSONString(analysisMap, stringWriter);
      mpiArgs = stringWriter.toString();

    } catch (IOException e) {
      throw new InvalidConfigurationException(
          "Failed to create a valid CPAchecker-cmdline from the config",
          e);
    }
  }

  private ImmutableMap<String, Object> createCommand(int pIndex)
      throws InvalidConfigurationException {

    String subprocess_timelimit = "90s"; // arbitrary value for now
    Path subprocess_config = configFiles.get(pIndex);
    Path subprocess_output_basedir = Path.of("output", "output_portfolio-analysis_" + pIndex);
    Path subprocess_logfile = Path.of("logfile_portfolio-analysis_" + pIndex + ".log");
    Path spec_path = Iterables.getOnlyElement(specification.getSpecFiles());

    subanalysesOutputPaths.put(subprocess_config, subprocess_output_basedir);
    subanalysesLogfilePaths.put(subprocess_config, subprocess_logfile);

    /*
     * Ugly hack to setup the desired config options for the child CPAchecker processes. The idea is
     * to keep all (user-)configurations except the ones necessary for running this portfolio
     * analysis.
     *
     * In other words, if the sub-analysis is e.g. a predicateAnalysis, then keep all configurations
     * (especially those manually set by the user) and remove any configuration options that are
     * necessary only for the MPIPortfolioAlgorithm itself.
     */
    Configuration childargs =
        Configuration.builder()
            .copyFrom(globalConfig)
            .clearOption("mpiAlgorithm.hostfile")
            .clearOption("analysis.algorithm.MPI")
            .clearOption("mpiAlgorithm.configFiles")
            .clearOption("analysis.name")
            .clearOption("mpiAlgorithm.numberProcesses")
            .setOption("limits.time.cpu", subprocess_timelimit)
            .setOption("output.path", subprocess_output_basedir.toString())
            .setOption("specification", spec_path.toString())
            // .setOption(
            // "log.file",
            // subprocess_logfile
            // .toString())
            //
            // .setOption(
            // "output.disable",
            // "false") // the main instance requires this for parsing and taking over the
            // // correct result from the children instances
            .build();

    // Bring the command-line into a format which is directly executable by a
    // subprocess.run() command in python
    ImmutableList<String> formattedOptions =
        Pattern.compile("\n")
            .splitAsStream(childargs.asPropertiesString())
            .map(x -> x.replace(" = ", "="))
            .map(x -> "-setprop " + x)
            .map(x -> x.split(" "))
            .flatMap(Stream::of)
            .collect(ImmutableList.toImmutableList());
    ImmutableList<Object> cmdline =
        ImmutableList.builder()
            .add("scripts/cpa.sh")
            .add("-config")
            .add(subprocess_config.toString())
            .addAll(formattedOptions)
            .build();

    ImmutableMap.Builder<String, Object> builder = new ImmutableMap.Builder<>();
    builder.put("analysis", subprocess_config.getFileName().toString());
    builder.put("cmd", cmdline);
    builder.put("output", subprocess_output_basedir.toString());
    builder.put("logfile", subprocess_output_basedir.resolve(subprocess_logfile).toString());

    return builder.build();
  }

  private static Path getPathOrThrowError(String pRequiredBin)
      throws InvalidConfigurationException {
    Optional<Path> pathOpt =
        Stream.of(System.getenv("PATH").split(Pattern.quote(File.pathSeparator)))
            .map(Paths::get)
            .filter(path -> Files.exists(path.resolve(pRequiredBin)))
            .findFirst();
    if (pathOpt.isEmpty()) {
      throw new InvalidConfigurationException(
          pRequiredBin
              + " is required for performing the portfolio-analysis, but could not find it in PATH");
    }

    return pathOpt.orElseThrow().resolve(pRequiredBin);
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    // TODO Auto-generated method stub

  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {

    List<String> cmdList = new ArrayList<>();
    cmdList.add(binaries.get(MPI_BIN).toString());

    // if no hostfile is specified, all CPAchecker instances
    // for the subanalyses will be executed on the local machine
    if (hostfile != null) {
      cmdList.add("-hostfile");
      cmdList.add(hostfile.normalize().toString());

      if (numberProcesses > 1) {
        cmdList.add("-np");
        cmdList.add(String.valueOf(numberProcesses));
      }
    }

    cmdList.add(binaries.get(PYTHON3_BIN).toString());
    cmdList.add(MPI_PYTHON_MAIN_PATH.normalize().toString());
    logger.log(Level.FINE, "Executing command (arguments trimmed): " + cmdList);

    cmdList.add("--input");
    cmdList.add(mpiArgs);
    logger.log(Level.FINEST, "MPI arguments: " + MoreStrings.lazyString(() -> mpiArgs));

    ProcessExecutor<IOException> executor = null;
    try {
      shutdownManager.getNotifier().shutdownIfNecessary();
      logger.log(Level.INFO, "Running subprocesses orchestrated by MPI");
      executor =
          new ProcessExecutor<>(
              logger,
              IOException.class,
              Iterables.toArray(cmdList, String.class));

      int exitCode = executor.join();
      logger.log(Level.INFO, "MPI has finished its job. Continuing in main node.");

      if (exitCode != 0) {

        logger.log(Level.INFO, "Printing output from tool");
        List<String> output = executor.getOutput();
        output.stream().forEach(x -> logger.log(Level.INFO, x));

        logger.log(Level.WARNING, "Printing warnings from tool");
        List<String> err = executor.getErrorOutput();
        err.stream().forEach(x -> logger.log(Level.WARNING, x));
        throw new CPAException("MPI failed with exit code " + exitCode);
      }

      List<String> errorOutput = executor.getErrorOutput();
      if (!errorOutput.isEmpty()) {
        logger.log(Level.WARNING, "MPI script returned successfully, but printed warnings");
      }

      List<String> output = executor.getOutput();
      if (!output.isEmpty()) {
        logger.logf(Level.INFO, "MPI produced %d output lines", output.size());
      }

      String RESULT_TRUE = "TRUE";
      String RESULT_FALSE = "FALSE";

      boolean resultsFound = false;
      ImmutableList<String> subanalysisLog = null;

      Map<Path, String> results = new HashMap<>();
      for (Entry<Path, Path> entry : subanalysesOutputPaths.entrySet()) {

        Path logfilePath = entry.getValue().resolve(subanalysesLogfilePaths.get(entry.getKey()));
        if (logfilePath.toFile().exists()) {

          try (Stream<String> lines = Files.lines(logfilePath)) {
            subanalysisLog =
                lines.filter(not(String::isBlank)).collect(ImmutableList.toImmutableList());
          }

          Optional<String> subanalysisResultOpt =
              subanalysisLog.stream()
                  .filter(x -> x.startsWith("Verification result:"))
                  .collect(MoreCollectors.toOptional());

          if (subanalysisResultOpt.isEmpty()) {
            continue;
          }

          String subanalysisResult = subanalysisResultOpt.orElseThrow().substring(21);
          if (subanalysisResult.startsWith(RESULT_TRUE)) {
            resultsFound = true;
            logger.logf(
                Level.WARNING,
                "Received result for analysis '%s': %s",
                entry.getKey().toString(),
                RESULT_TRUE);
            results.put(entry.getKey(), RESULT_TRUE);
            break;
          } else if (subanalysisResult.startsWith(RESULT_FALSE)) {
            resultsFound = true;
            logger.logf(
                Level.WARNING,
                "Received result for analysis '%s': %s",
                entry.getKey().toString(),
                RESULT_FALSE);
            results.put(entry.getKey(), RESULT_FALSE);
            break;
          }
        }
      }

      if (!resultsFound) {
        logger.logf(Level.WARNING, "None of the subanalyses produced a result.");
      } else {
        if (results.values().stream().anyMatch(x -> x.equals(RESULT_TRUE))) {
          logger.logf(Level.FINE, "Returning result: TRUE");
          // One of the subanalyses returned "TRUE" as result, so an empty reachedset is
          // purposefully returned to reflect that in the main analysis
          pReachedSet.clear();

        } else if (results.values().stream().anyMatch(x -> x.equals(RESULT_FALSE))) {
          logger.logf(Level.FINE, "Returning result: FALSE");
          // One of the subanalyses returned "FALSE" as result, so a reachedset with one dummy
          // targetstate is returned to reflect that in the main analysis
          pReachedSet.clear();
          pReachedSet.add(new DummyTargetState(), SingletonPrecision.getInstance());
        }

        logger.log(Level.WARNING, "In the following the log of the successful subanalysis");
        logger.log(Level.WARNING, "---------------- START SUBANALYSIS LOG ----------------");

        if (subanalysisLog == null) {
          throw new RuntimeException("Log of subanalysis may not be null at this point");
        }
        subanalysisLog.stream().forEach(x -> logger.log(Level.INFO, x));

        logger.log(Level.WARNING, "----------------- END SUBANALYSIS LOG -----------------");
        return AlgorithmStatus.SOUND_AND_IMPRECISE;
      }

    } catch (IOException e) {
      throw new CPAException("Execution of MPI failed", e);
    }

    // we didn't receive any results from the subanalyses, thus we can't tell anything about
    // them
    return AlgorithmStatus.UNSOUND_AND_IMPRECISE;

  }

  private static class DummyTargetState implements AbstractState {
    // implements Targetable
  }
}
