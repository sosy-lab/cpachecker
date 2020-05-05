/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.stream.Stream;
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
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options(prefix = "mpiAlgorithm")
public class MPIPortfolioAlgorithm implements Algorithm, StatisticsProvider {

  private static final String MPI_BIN = "mpiexec";
  private static final String PYTHON3_BIN = "python3";
  private static final Path HELPER_SCRIPT_PATH =
      Path.of("scripts", "mpi_portfolio.py").toAbsolutePath();

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
  @SuppressWarnings("unused")
  private final Specification specification;
  // private final ParallelAlgorithmStatistics stats; // TODO

  private final Map<String, Path> binaries;

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

    try (StringWriter stringWriter = new StringWriter();) {
      Map<String, Object> analysisMap = new LinkedHashMap<>();
      for (int i = 0; i < configFiles.size(); i++) {
        analysisMap.put("Analysis_" + i, createCommand(i));
      }

      // The following settings are required for the child CPAchecker instances. They might be
      // executed on different machines and thus need these informations for copying the results
      // back to the main node (using scp for now) after completing their analysis.
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

  private ImmutableMap<String, Object> createCommand(
      int pIndex)
      throws InvalidConfigurationException {

    String subprocess_timelimit = "90s"; // arbitrary value for now
    Path subprocess_config = configFiles.get(pIndex);
    Path subprocess_output_basedir = Path.of("output", "output_portfolio-analysis_" + pIndex);
    Path subprocess_logfile = Path.of("logfile_portfolio-analysis_" + pIndex + ".log");

    /*
     * Ugly hack to setup the desired config options for the child CPAchecker processes. The idea is
     * to keep all (user-)configurations except the ones necessary for running this portfolio
     * analysis.
     *
     * (In other words, if the sub-analysis is e.g. a predicateAnalysis, then keep all
     * configurations (especially those manually set by the user) and remove any configuration
     * options that are necessary only for the MPIPortfolioAlgorithm (=this class) itself.)
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
            .setOption(
                "log.file",
                subprocess_logfile
                    .toString())
            .build();

    // Bring the command-line in a format such that it can be directly executed by a
    // subprocess.run() command in a python script
    ImmutableList<String> formattedOptions =
        Pattern.compile(
            "\n")
            .splitAsStream(childargs.asPropertiesString())
            .map(x -> x.replace(" = ", "="))
            .map(x -> "-setprop " + x)
            .map(
                x -> x.split(
                    " "))
            .flatMap(x -> Stream.of(x))
            .collect(ImmutableList.toImmutableList());
    ImmutableList<Object> cmdline =
        ImmutableList.builder()
            .add(
                "scripts/cpa.sh")
            .add("-config")
            .add(subprocess_config.toString())
            .addAll(formattedOptions)
            .build();

    ImmutableMap.Builder<String, Object> builder = new ImmutableMap.Builder<>();
    builder.put("analysis", subprocess_config.getFileName().toString());
    builder.put("cmd", cmdline);
    builder.put("logfile", subprocess_output_basedir.toString());
    builder.put("results", subprocess_output_basedir.resolve(subprocess_logfile).toString());

    return builder.build();
  }

  private Path getPathOrThrowError(String pRequiredBin) throws InvalidConfigurationException {
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

    ImmutableList.Builder<String> cmdBuilder = ImmutableList.builder();
    cmdBuilder.add(binaries.get(MPI_BIN).toString());

    if (numberProcesses > 1) {
      cmdBuilder.add("-np");
      cmdBuilder.add(String.valueOf(numberProcesses));
    }

    // if no hostfile is specified, all CPAchecker instances
    // for the sub-analyses will be executed on the local machine
    if (hostfile != null) {
      cmdBuilder.add("-hostfile");
      cmdBuilder.add(hostfile.toAbsolutePath().toString());
    }

    cmdBuilder.add(binaries.get(PYTHON3_BIN).toString());
    cmdBuilder.add(HELPER_SCRIPT_PATH.toString());
    cmdBuilder.add("--input");
    cmdBuilder.add(mpiArgs);

    String[] cmds = Iterables.toArray(cmdBuilder.build(), String.class);

    ProcessExecutor<IOException> executor = null;
    try {
      shutdownManager.getNotifier().shutdownIfNecessary();
      logger.log(Level.INFO, "Running subprocesses orchestrated by MPI");
      executor = new ProcessExecutor<>(logger, IOException.class, ImmutableMap.of(), cmds);

      int exitCode = executor.join();
      logger.log(Level.INFO, "MPI has finished its job. Continuing in main node.");

      if (exitCode != 0) {
        throw new CPAException("MPI failed with exit code " + exitCode);
      }

      List<String> errorOutput = executor.getErrorOutput();
      if (!errorOutput.isEmpty()) {
        logger.log(Level.WARNING, "MPI script returned successfully, but printed warnings");
      }

      List<String> output = executor.getOutput();
      logger.logf(Level.INFO, "MPI produced %d output lines", output.size());
      // String s = Joiner.on("\n").join(output) + "\n";
      // logger.log(Level.INFO, s);

    } catch (IOException e) {
      throw new CPAException("Execution of MPI failed", e);
    }

    // TODO: In python-script.py:
    // connect to master via ssh and send the result files

    // TODO: shutdown algo when one subprocess is successful
    // TODO: overtake data from the successful subprocess

    // we don't know anything about the sub-analyses at this point
    return AlgorithmStatus.UNSOUND_AND_IMPRECISE;
  }
}
