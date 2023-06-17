// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.parallelRangedConditions;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.AbstractParallelAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.pcc.PartialARGsCombiner;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.HistoryForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.InvalidAutomatonException;
import org.sosy_lab.cpachecker.cpa.automaton.RangedConditionFactory;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.rangedconditions.CFAPath;

@Options(prefix = "parallelRangedConditionsAlgorithm")
public class ParallelRangedConditionsAlgorithm extends AbstractParallelAlgorithm {

  @Option(description = "Assumption guiding automaton specification file")
  @FileOption(Type.REQUIRED_INPUT_FILE)
  Path assumtionGuidingAutomatonFile =
      Path.of("config/specification/AssumptionGuidingAutomaton.spc");

  @Option(description = "Path generation heuristic to use for Parallel Ranged Conditions.")
  private Heuristic.Type pathHeuristic = Heuristic.Type.PATHS_FILE;

  @Option(description = "Assumption automaton out-file pattern")
  @FileOption(Type.OUTPUT_FILE)
  private @Nullable PathTemplate automatonOutput;

  private ParallelRangedConditionsAlgorithm(
      Configuration config,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa,
      AggregatedReachedSets pAggregatedReachedSets)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    super(
        pLogger,
        pShutdownNotifier,
        pCfa,
        pAggregatedReachedSets,
        new ParallelRangedConditionsAlgorithmStatistics(pLogger));

    config.inject(this);

    checkNotNull(pCfa);

    Heuristic heuristic = Heuristic.getHeuristic(pathHeuristic, pCfa, config, logger);

    List<CFAPath> cfaPaths = heuristic.generatePaths();
    logger.log(Level.INFO, "RangedConditions Paths:");
    for (CFAPath path : cfaPaths) {
      logger.log(Level.INFO, path.toString());
    }

    RangedConditionFactory conditionFactory = new RangedConditionFactory(pCfa);
    ImmutableList.Builder<Callable<ParallelAnalysisResult>> analysesBuilder =
        ImmutableList.builder();

    Configuration singleConfig = cleanupConfig(config);

    Specification specification =
        pSpecification.withAdditionalSpecificationFile(
            ImmutableSet.of(assumtionGuidingAutomatonFile), pCfa, config, pLogger, pShutdownNotifier);

    for (int i = 0; i <= cfaPaths.size(); i++) {
      Automaton condition;
      try {
        if (i == 0) {
          condition =
              conditionFactory.createForSmallestRange(cfaPaths.get(0), ImmutableSet.copyOf(cfaPaths));
        } else if (i == cfaPaths.size()) {
          condition =
              conditionFactory.createForLargestRange(cfaPaths.get(i - 1), ImmutableSet.copyOf(cfaPaths));
        } else {
          condition =
              conditionFactory.createForRange(
                  cfaPaths.get(i - 1), cfaPaths.get(i), ImmutableSet.copyOf(cfaPaths));
        }
      } catch (InvalidAutomatonException exception) {
        throw new InvalidConfigurationException(
            String.format("Can not create Ranged Condition for Range %s", i), exception);
      }

      Specification singleSpecification =
          specification.withAdditionalSpecification(
              Specification.fromAutomata(
                  Collections.unmodifiableList(ImmutableList.of(condition))));
      analysesBuilder.add(
          createParallelAnalysis(
              String.format("Ranged Analysis #%s", i), i, singleConfig, singleSpecification));

      if (automatonOutput != null) {
        Path path = automatonOutput.getPath(i);
        try (FileWriter writer = new FileWriter(path.toFile(), StandardCharsets.UTF_8)) {
          condition.writeDotFile(writer);
        } catch (IOException pE) {
          throw new RuntimeException(pE);
        }
      }
    }

    setAnalyses(analysesBuilder.build());
  }

  public static Algorithm create(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa,
      AggregatedReachedSets pReachedSet)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    ParallelRangedConditionsAlgorithm algorithm =
        new ParallelRangedConditionsAlgorithm(
            pConfig, pLogger, pShutdownNotifier, pSpecification, pCfa, pReachedSet);
    return new PartialARGsCombiner(algorithm, pConfig, pLogger, pShutdownNotifier);
  }

  private Configuration cleanupConfig(Configuration pConfig) throws InvalidConfigurationException {
    ConfigurationBuilder configurationBuilder = Configuration.builder();
    configurationBuilder.copyFrom(pConfig);
    configurationBuilder.clearOption("analysis.useParallelRangedConditions");
    return configurationBuilder.build();
  }

  @Override
  protected AlgorithmStatus determineAlgorithmStatus(
      ReachedSet pReachedSet, List<ListenableFuture<ParallelAnalysisResult>> pFutures) {
    HistoryForwardingReachedSet reachedSet = (HistoryForwardingReachedSet) pReachedSet;

    AlgorithmStatus combinedStatus = AlgorithmStatus.SOUND_AND_PRECISE;

    for (ListenableFuture<ParallelAnalysisResult> future : pFutures) {
      try {
        ParallelAnalysisResult result = future.get();

        reachedSet.setDelegate(result.getReached());
        combinedStatus = combinedStatus.update(result.getStatus());

        if (result.getReached().wasTargetReached()) {
          logger.log(
              Level.INFO,
              "Analysis "
                  + result.getAnalysisName()
                  + " reached target. Skipping aggregation of following analyses.");
          return result.getStatus();
        }
      } catch (InterruptedException | ExecutionException pE) {
        logger.log(
            Level.SEVERE,
            "At least one parallel execution did not finish. Result may be inaccurate.");
      }
    }
    return combinedStatus;
  }

  @Override
  protected void handleSingleFutureResult(
      ListenableFuture<ParallelAnalysisResult> singleFuture,
      List<ListenableFuture<ParallelAnalysisResult>> futures)
      throws InterruptedException, ExecutionException, Error {
    ParallelAnalysisResult result = singleFuture.get();

    if (result.hasValidReachedSet()) {
      logger.log(
          Level.INFO,
          result.getAnalysisName() + " finished successfully. Result: " + result.getStatus());
      if (result.getReached().wasTargetReached()) {
        futures.forEach(future -> future.cancel(true));
        shutdownManager.requestShutdown(
            "One analysis reached a target state, canceling remaining analyses.");
      }
    } else if (!result.hasValidReachedSet()) {
      logger.log(Level.INFO, result.getAnalysisName() + " finished without usable result.");
    }
  }

  protected static class ParallelRangedConditionsAlgorithmStatistics
      extends AbstractParallelAlgorithmStatistics {

    ParallelRangedConditionsAlgorithmStatistics(LogManager pLogger) {
      super(pLogger);
    }

    @Override
    public String getName() {
      return "Parallel Ranged Conditions Algorithm";
    }

    @Override
    protected Result determineAnalysisResult(Result pResult, String pActualAnalysisName) {
      return null;
    }
  }
}
