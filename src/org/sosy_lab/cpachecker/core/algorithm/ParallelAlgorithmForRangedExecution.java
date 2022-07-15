// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.base.Predicates.or;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import org.sosy_lab.common.Classes.UnexpectedCheckedException;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.conditions.ReachedSetAdjustingCPA;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CompoundException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;

public class ParallelAlgorithmForRangedExecution extends ParallelAlgorithm {

  protected static final String FAILURE_MESSAGE =
      "The analysis '%s'  of the parallel analyses did not compute a result, cancelling all other"
          + " runs.";

  public ParallelAlgorithmForRangedExecution(
      Configuration pGlobalConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa,
      AggregatedReachedSets pAggregatedReachedSets)
      throws InvalidConfigurationException {
    super(
        pGlobalConfig,
        pLogger,
        pShutdownNotifier,
        pSpecification,
        pCfa,
        pAggregatedReachedSets,
        ImmutableList.of());
  }

  public void computeAnalyses(List<Path> pPath2LoopBound, List<Path> pConfigFiles)
      throws InvalidConfigurationException, IOException, CPAException, InterruptedException {
    List<Callable<ParallelAnalysisResult>> newAnanlyses = new ArrayList<>();
    for (int i = 0; i < pConfigFiles.size(); i++) {
      ConfigurationBuilder builder = Configuration.builder();
      builder.loadFromFile(pConfigFiles.get(i));
      if (i > 0) {
        // Lower bound not for first analysis
        builder.setOption(
            "cpa.rangedAnalysis.path2LowerInputFile",
            pPath2LoopBound.get(i - 1).toAbsolutePath().toString());
      }
      if (i < pConfigFiles.size() - 1) {
        // UpperBpund not for last analysis

        builder.setOption(
            "cpa.rangedAnalysis.path2UpperInputFile",
            pPath2LoopBound.get(i).toAbsolutePath().toString());
      }
      newAnanlyses.add(
          super.createParallelAnalysis(
              builder.build(), i, false, false, pConfigFiles.get(i).toString()));
    }
    super.analyses = ImmutableList.copyOf(newAnanlyses);
  }

  @Override
  protected void handleFutureResults(List<ListenableFuture<ParallelAnalysisResult>> futures)
      throws InterruptedException, Error, CPAException {

    List<CPAException> exceptions = new ArrayList<>();
    for (ListenableFuture<ParallelAnalysisResult> f : Futures.inCompletionOrder(futures)) {
      try {
        ParallelAnalysisResult result = f.get();
        if (result.hasValidReachedSet() && finalResult == null) {

          finalResult = result;

          // IF the result is false, then we need to stop the other analysis
          if (finalResult.getReached().wasTargetReached()) {
            stats.successfulAnalysisName = result.getAnalysisName();
            futures.forEach(future -> future.cancel(true));
            logger.log(
                Level.INFO,
                result.getAnalysisName()
                    + " finished successfully and found a property violation.");
            shutdownManager.requestShutdown(SUCCESS_MESSAGE);

          } else {
            // Let the other analyses run
            logger.log(Level.INFO, result.getAnalysisName() + " finished successfully.");
          }

        } else if (!result.hasValidReachedSet()) {
          logger.log(
              Level.INFO,
              result.getAnalysisName() + " finished without usable result, hence aborting!");

          stats.successfulAnalysisName =
              String.format("NONE ('%s' failed)", result.getAnalysisName());

          // cancel other computations
          futures.forEach(future -> future.cancel(true));
          logger.log(Level.INFO, result.getAnalysisName() + " finished successfully.");
          shutdownManager.requestShutdown(String.format(FAILURE_MESSAGE, result.getAnalysisName()));
        }
      } catch (ExecutionException e) {
        Throwable cause = e.getCause();
        if (cause instanceof CPAException) {
          if (cause.getMessage().contains("recursion")) {
            logger.logUserException(
                Level.WARNING, cause, "Analysis not completed due to recursion");
          }
          if (cause.getMessage().contains("pthread_create")) {
            logger.logUserException(
                Level.WARNING, cause, "Analysis not completed due to concurrency");
          }
          exceptions.add((CPAException) cause);

        } else {
          // cancel other computations
          futures.forEach(future -> future.cancel(true));
          shutdownManager.requestShutdown("cancelling all remaining analyses");
          Throwables.throwIfUnchecked(cause);
          // probably we need to handle IOException, ParserException,
          // InvalidConfigurationException, and InterruptedException here (#326)
          throw new UnexpectedCheckedException("analysis", cause);
        }
      } catch (CancellationException e) {
        // do nothing, this is normal if we cancel other analyses
      }
    }

    // we do not have any result, so we propagate the found CPAExceptions upwards
    if (finalResult == null && !exceptions.isEmpty()) {
      if (exceptions.size() == 1) {
        throw Iterables.getOnlyElement(exceptions);
      } else {
        throw new CompoundException(exceptions);
      }
    }
  }

  @Override
  protected ParallelAnalysisResult runParallelAnalysis(
      final String analysisName,
      final Algorithm algorithm,
      final ReachedSet reached,
      final LogManager singleLogger,
      final ConfigurableProgramAnalysis cpa,
      final boolean supplyReached,
      final boolean supplyRefinableReached,
      final CoreComponentsFactory coreComponents,
      final StatisticsEntry pStatisticsEntry)
      throws CPAException {
    try {
      AlgorithmStatus status = null;
      ReachedSet currentReached = reached;
      AtomicReference<ReachedSet> oldReached = new AtomicReference<>();

      if (!supplyRefinableReached) {
        status = algorithm.run(currentReached);
      } else {
        boolean stopAnalysis = true;
        do {

          // explore statespace fully only if the analysis is sound and no reachable error is found
          while (currentReached.hasWaitingState()) {
            status = algorithm.run(currentReached);
            if (!status.isSound()) {
              break;
            }
          }

          Preconditions.checkState(status != null, "algorithm should run at least once.");

          // check if we could prove the program to be safe
          if (status.isSound()
              && !from(currentReached)
                  .anyMatch(or(AbstractStates::isTargetState, AbstractStates::hasAssumptions))) {
            ReachedSet oldReachedSet = oldReached.get();
            if (oldReachedSet != null) {
              aggregatedReachedSetManager.updateReachedSet(oldReachedSet, currentReached);
            } else {
              aggregatedReachedSetManager.addReachedSet(currentReached);
            }
            return ParallelAnalysisResult.of(currentReached, status, analysisName);
          }

          // reset the flag
          stopAnalysis = true;
          for (ReachedSetAdjustingCPA innerCpa :
              CPAs.asIterable(cpa).filter(ReachedSetAdjustingCPA.class)) {
            if (innerCpa.adjustPrecision()) {
              singleLogger.log(Level.INFO, "Adjusting precision for CPA", innerCpa);

              stopAnalysis = false;
            }
          }
          for (ConditionAdjustmentEventSubscriber conditionAdjustmentEventSubscriber :
              conditionAdjustmentEventSubscribers) {
            if (stopAnalysis) {
              conditionAdjustmentEventSubscriber.adjustmentRefused(cpa);
            } else {
              conditionAdjustmentEventSubscriber.adjustmentSuccessful(cpa);
            }
          }

          if (status.isSound()) {
            singleLogger.log(Level.INFO, "Updating reached set provided to other analyses");
            ReachedSet oldReachedSet = oldReached.get();
            if (oldReachedSet != null) {
              aggregatedReachedSetManager.updateReachedSet(oldReachedSet, currentReached);
            } else {
              aggregatedReachedSetManager.addReachedSet(currentReached);
            }
            oldReached.set(currentReached);
          }

          if (!stopAnalysis) {
            currentReached = coreComponents.createReachedSet(cpa);
            pStatisticsEntry.reachedSet.set(currentReached);
            initializeReachedSet(cpa, mainEntryNode, currentReached);
          }
        } while (!stopAnalysis);
      }

      // only add to aggregated reached set if we haven't done so, and all necessary requirements
      // are fulfilled
      if (!currentReached.hasWaitingState()
          && supplyReached
          && !supplyRefinableReached
          && status.isPrecise()
          && status.isSound()) {
        aggregatedReachedSetManager.addReachedSet(currentReached);
      }

      return ParallelAnalysisResult.of(currentReached, status, analysisName);

    } catch (InterruptedException e) {
      singleLogger.log(Level.INFO, "Analysis was terminated");
      return ParallelAnalysisResult.absent(analysisName);
    }
  }
}
