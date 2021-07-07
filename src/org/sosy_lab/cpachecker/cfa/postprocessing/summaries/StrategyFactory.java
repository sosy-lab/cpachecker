// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependencyInterface;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.execution.ConcolicExecutionStrategy;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.execution.DeterministicExecutionStrategy;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops.ConstantExtrapolationStrategy;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops.LinearExtrapolationStrategy;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops.LoopAccelerationStrategy;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops.LoopUnrollingStrategy;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops.NaiveLoopAccelerationStrategy;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops.NondetBoundConstantExtrapolationStrategy;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.recursion.RecursionConstantExtrapolationStrategy;

public class StrategyFactory {

  protected final LogManager logger;
  protected final ShutdownNotifier shutdownNotifier;
  protected final int maxUnrollingsStrategy;
  private boolean useCompilerForSummary;
  private StrategyDependencyInterface strategyDependencies;

  public StrategyFactory(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      int pMaxUnrollingsStrategy,
      boolean pUseCompilerForSummary,
      StrategyDependencyInterface pStrategyDependencies) {
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    maxUnrollingsStrategy = pMaxUnrollingsStrategy;
    useCompilerForSummary = pUseCompilerForSummary;
    strategyDependencies = pStrategyDependencies;
  }

  public StrategyInterface buildStrategy(StrategiesEnum strategy) {
    switch (strategy) {
      case LoopAcceleration:
        return new LoopAccelerationStrategy(logger, shutdownNotifier, strategyDependencies);
      case ConcolicExecution:
        return new ConcolicExecutionStrategy(logger, shutdownNotifier, strategyDependencies);
      case LoopConstantExtrapolation:
        return new ConstantExtrapolationStrategy(logger, shutdownNotifier, strategyDependencies);
      case DeterministicExecution:
        return new DeterministicExecutionStrategy(
            logger, shutdownNotifier, useCompilerForSummary, strategyDependencies);
      case LoopLinearExtrapolation:
        return new LinearExtrapolationStrategy(logger, shutdownNotifier, strategyDependencies);
      case LoopUnrolling:
        return new LoopUnrollingStrategy(
            logger, shutdownNotifier, maxUnrollingsStrategy, strategyDependencies);
      case NaiveLoopAcceleration:
        return new NaiveLoopAccelerationStrategy(logger, shutdownNotifier, strategyDependencies);
      case NonDetBoundConstantExtrapolation:
        return new NondetBoundConstantExtrapolationStrategy(
            logger, shutdownNotifier, strategyDependencies);
      case RecursionConstantExtrapolation:
        return new RecursionConstantExtrapolationStrategy(
            logger, shutdownNotifier, strategyDependencies);
      default:
        return null;
    }
  }
}
