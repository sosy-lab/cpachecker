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
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependencyInterface;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.execution.ConcolicExecutionStrategy;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.execution.DeterministicExecutionStrategy;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops.ConstantExtrapolationStrategy;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops.HavocStrategy;
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
  private CFA cfa;

  public StrategyFactory(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      int pMaxUnrollingsStrategy,
      boolean pUseCompilerForSummary,
      StrategyDependencyInterface pStrategyDependencies,
      CFA pCFA) {
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    maxUnrollingsStrategy = pMaxUnrollingsStrategy;
    useCompilerForSummary = pUseCompilerForSummary;
    strategyDependencies = pStrategyDependencies;
    cfa = pCFA;
  }

  public StrategyInterface buildStrategy(StrategiesEnum strategy) {
    switch (strategy) {
      case LoopAcceleration:
        return new LoopAccelerationStrategy(logger, shutdownNotifier, strategyDependencies, cfa);
      case ConcolicExecution:
        return new ConcolicExecutionStrategy(logger, shutdownNotifier, strategyDependencies, cfa);
      case LoopConstantExtrapolation:
        return new ConstantExtrapolationStrategy(
            logger, shutdownNotifier, strategyDependencies, cfa);
      case DeterministicExecution:
        return new DeterministicExecutionStrategy(
            logger, shutdownNotifier, useCompilerForSummary, strategyDependencies, cfa);
      case LoopLinearExtrapolation:
        return new LinearExtrapolationStrategy(logger, shutdownNotifier, strategyDependencies, cfa);
      case LoopUnrolling:
        return new LoopUnrollingStrategy(
            logger, shutdownNotifier, maxUnrollingsStrategy, strategyDependencies, cfa);
      case NaiveLoopAcceleration:
        return new NaiveLoopAccelerationStrategy(
            logger, shutdownNotifier, strategyDependencies, cfa);
      case NonDetBoundConstantExtrapolation:
        return new NondetBoundConstantExtrapolationStrategy(
            logger, shutdownNotifier, strategyDependencies, cfa);
      case RecursionConstantExtrapolation:
        return new RecursionConstantExtrapolationStrategy(
            logger, shutdownNotifier, strategyDependencies, cfa);
      case Base:
        return new BaseStrategy();
      case HavocStrategy:
        return new HavocStrategy(logger, shutdownNotifier, strategyDependencies, cfa);
      default:
        return null;
    }
  }
}
