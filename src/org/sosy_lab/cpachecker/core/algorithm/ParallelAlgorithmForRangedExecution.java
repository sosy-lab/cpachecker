// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

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
import java.util.logging.Level;
import org.sosy_lab.common.Classes.UnexpectedCheckedException;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CompoundException;

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
    ParallelAnalysisResult myFinalResult = null;
    for (ListenableFuture<ParallelAnalysisResult> f : Futures.inCompletionOrder(futures)) {
      try {
        ParallelAnalysisResult result = f.get();
        if (result.hasValidReachedSet() && myFinalResult == null) {

          myFinalResult = result;

          // IF the result is false, then we need to stop the other analysis
          assert myFinalResult.getReached() != null;
          if (myFinalResult.getReached().wasTargetReached()) {
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
    if (myFinalResult == null && !exceptions.isEmpty()) {
      if (exceptions.size() == 1) {
        throw Iterables.getOnlyElement(exceptions);
      } else {
        throw new CompoundException(exceptions);
      }
    }
  }


}
