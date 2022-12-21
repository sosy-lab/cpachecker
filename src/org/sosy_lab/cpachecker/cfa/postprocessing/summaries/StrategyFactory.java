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
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependency;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops.AbstractAccelerationStrategy;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops.ConstantExtrapolationStrategy;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops.HavocStrategy;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops.LinearExtrapolationStrategy;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops.LoopUnrollingStrategy;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops.NaiveLoopAccelerationStrategy;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops.NondetBoundConstantExtrapolationStrategy;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops.OutputLoopAccelerationStrategy;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.underapproximating.NondetVariableAssignmentStrategy;

public class StrategyFactory {

  protected final LogManager logger;
  protected final ShutdownNotifier shutdownNotifier;
  protected final int maxUnrollingsStrategy;
  private StrategyDependency strategyDependencies;
  private CFA cfa;

  public StrategyFactory(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      int pMaxUnrollingsStrategy,
      StrategyDependency pStrategyDependencies,
      CFA pCFA) {
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    maxUnrollingsStrategy = pMaxUnrollingsStrategy;
    strategyDependencies = pStrategyDependencies;
    cfa = pCFA;
  }

  public Strategy buildStrategy(StrategiesEnum strategy) {
    switch (strategy) {
      case LOOPCONSTANTEXTRAPOLATION:
        return new ConstantExtrapolationStrategy(
            logger, shutdownNotifier, strategyDependencies, cfa);
      case LOOPLINEAREXTRAPOLATION:
        return new LinearExtrapolationStrategy(logger, shutdownNotifier, strategyDependencies, cfa);
      case LOOPUNROLLING:
        return new LoopUnrollingStrategy(
            logger, shutdownNotifier, maxUnrollingsStrategy, strategyDependencies, cfa);
      case NAIVELOOPACCELERATION:
        return new NaiveLoopAccelerationStrategy(
            logger, shutdownNotifier, strategyDependencies, cfa);
      case OUTPUTLOOPACCELERATION:
        return new OutputLoopAccelerationStrategy(
            logger, shutdownNotifier, strategyDependencies, cfa);
      case NONDETBOUNDCONSTANTEXTRAPOLATION:
        return new NondetBoundConstantExtrapolationStrategy(
            logger, shutdownNotifier, strategyDependencies, cfa);
      case BASE:
        return new BaseStrategy();
      case HAVOCSTRATEGY:
        return new HavocStrategy(logger, shutdownNotifier, strategyDependencies, cfa);
      case ABSTRACTACCELERATIONSTRATEGY:
        return new AbstractAccelerationStrategy(
            logger, shutdownNotifier, strategyDependencies, cfa);
      case NONDETVARIABLEASSIGNMENTSTRATEGY:
        return new NondetVariableAssignmentStrategy(
            logger, shutdownNotifier, strategyDependencies, cfa);
      default:
        return null;
    }
  }
}
