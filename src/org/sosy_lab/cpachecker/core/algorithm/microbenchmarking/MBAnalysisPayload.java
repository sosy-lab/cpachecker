// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.microbenchmarking;

import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.microbenchmarking.MicroBenchmarking.BenchmarkExecutionRun;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ParserException;

public class MBAnalysisPayload {

  private final LogManager logger;
  private final int numWarmupExecutions;
  private final int numExecutions;
  private final List<Path> propertyFiles;
  private final List<Path> programFiles;
  private final ShutdownNotifier shutdownNotifier;
  private final Specification specification;

  public MBAnalysisPayload(
      LogManager logger,
      int numWarmupExecutions,
      int numExecutions,
      List<Path> propertyFiles,
      List<Path> programFiles,
      ShutdownNotifier shutdownNotifier,
      Specification specification) {
    this.logger = logger;
    this.numWarmupExecutions = numWarmupExecutions;
    this.numExecutions = numExecutions;
    this.propertyFiles = propertyFiles;
    this.programFiles = programFiles;
    this.shutdownNotifier = shutdownNotifier;
    this.specification = specification;
  }

  List<BenchmarkExecutionRun> runMicrobenchmark()
      throws IOException, InvalidConfigurationException, ParserException, InterruptedException,
      CPAException {

    List<BenchmarkExecutionRun> runTimes = new ArrayList<>();

    for (int i = 0; i < propertyFiles.size(); i++) { // Iterate over all user-defined analysis'
      for (int z = 0; z < programFiles.size(); z++) {

        logger.log(
            Level.INFO,
            String.format(
                "Running analysis defined in '%s' on program '%s'",
                propertyFiles.get(i),
                programFiles.get(i)));

        ConfigurationBuilder configurationBuilder = Configuration.builder();
        configurationBuilder.loadFromFile(propertyFiles.get(i));
        Configuration configuration = configurationBuilder.build();
        CFACreator cfaCreator = new CFACreator(configuration, logger, shutdownNotifier);

        CFA cfa =
            cfaCreator.parseFileAndCreateCFA(ImmutableList.of(programFiles.get(z).toString()));

        ReachedSetFactory reachedSetFactory = new ReachedSetFactory(configuration, logger);
        CPABuilder cpaBuilder =
            new CPABuilder(configuration, logger, shutdownNotifier, reachedSetFactory);
        final ConfigurableProgramAnalysis cpa =
            cpaBuilder.buildCPAs(cfa, specification, AggregatedReachedSets.empty());

        CoreComponentsFactory factory =
            new CoreComponentsFactory(
                configuration,
                logger,
                shutdownNotifier,
                AggregatedReachedSets.empty());
        Algorithm algorithm = factory.createAlgorithm(cpa, cfa, specification);
        ReachedSet reached =
            reachedSetFactory.createAndInitialize(
                cpa,
                cfa.getMainFunction(),
                StateSpacePartition.getDefaultPartition());

        List<BenchmarkExecutionRun> runTimesSingleExecution =
            runProgramAnalysis(algorithm, reached, reachedSetFactory, cpa, cfa);
        runTimes.addAll(runTimesSingleExecution);
      }
    }

    return runTimes;
  }

  private List<BenchmarkExecutionRun> runProgramAnalysis(
      Algorithm algorithm,
      ReachedSet reached,
      ReachedSetFactory reachedSetFactory,
      ConfigurableProgramAnalysis cpa,
      CFA cfa)
      throws InterruptedException {

    Ticker ticker = Ticker.systemTicker();

    List<BenchmarkExecutionRun> list = new ArrayList<>();
    for (int i = 0; i < numWarmupExecutions + numExecutions; i++) {

      if (i < numWarmupExecutions) {
        logger
            .log(Level.INFO, "Running microbenchmarking analysis as warmup iteration #" + (i + 1));
      } else {
        logger.log(Level.INFO, "Running microbenchmarking analysis - iteration #" + (i + 1));
      }

      long startTime = ticker.read();

      try {
        algorithm.run(reached);
      } catch (CPAException | InterruptedException e) {
        logger.log(
            Level.FINE,
            "Error during microbenchmarking run. Ignoring result and continuing...");
        continue;
      }

      long endTime = ticker.read();
      long timeDiff = endTime - startTime;

      if (i < ((numExecutions + numWarmupExecutions) - 1)) { // Create new empty reached set for
                                                             // every iteration
        reached =
            reachedSetFactory.createAndInitialize(
                cpa,
                cfa.getMainFunction(),
                StateSpacePartition.getDefaultPartition());
      }

      if (i >= numWarmupExecutions) {
        BenchmarkExecutionRun run = new BenchmarkExecutionRun();
        run.duration = timeDiff;
        list.add(run);
      }
      logger.logf(Level.INFO, "Finished run #%s in %s ms", (i + 1), timeDiff);

    }
    return list;
  }

}
