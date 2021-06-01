// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.BaseStrategy;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.ConcolicExecution;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.DeterministicExecution;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.LoopAcceleration;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.LoopUnrolling;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.NaiveLoopAcceleration;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.StrategyInterface;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.extrapolation.ConstantExtrapolationStrategy;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.extrapolation.LinearExtrapolationStrategy;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.extrapolation.NondetBoundConstantExtrapolationStrategy;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.extrapolation.PolynomialExtrapolationStrategy;

@Options(prefix = "cpa.loopsummary")
public abstract class AbstractLoopSummaryCPA extends AbstractSingleWrapperCPA {

  private enum StrategiesEnum {
    BASE,
    LOOPACCELERATION,
    NAIVELOOPACCELERATION,
    POLYNOMIALEXTRAPOLATION,
    LINEAREXTRAPOLATION,
    CONSTANTEXTRAPOLATION,
    NONDETBOUNDCONSTANTEXTRAPOLATION,
    LOOPUNROLLING,
    CONCOLICEXECUTION,
    DETERMINISTICEXECUTION,
  }

  @Option(
      name = "strategies",
      secure = true,
      description =
          "Strategies to be used in the Summary. The order of the strategies marks in which order they are tried")
  private List<StrategiesEnum> strategies =
      new ArrayList<>(
          Arrays.asList(
              StrategiesEnum.CONSTANTEXTRAPOLATION,
              StrategiesEnum.LINEAREXTRAPOLATION,
              StrategiesEnum.POLYNOMIALEXTRAPOLATION,
              StrategiesEnum.NONDETBOUNDCONSTANTEXTRAPOLATION, // See TODO in NondetBound File
              StrategiesEnum.NAIVELOOPACCELERATION,
              StrategiesEnum.LOOPACCELERATION, // TODO Not yet implemented
              StrategiesEnum.LOOPUNROLLING,
              StrategiesEnum.BASE));

  @Option(
      name = "maxAmntFirstRefinements",
      secure = true,
      description =
          "Maximal amount of refinements the first refiner can make before going to the second refiner")
  public int maxAmntFirstRefinements = 100;

  @Option(
      name = "maxUnrollings",
      secure = true,
      description = "Maximal amount of unrollings the loop Unrolling strategy can make.")
  public int maxUnrollingsStrategy = 10;

  private List<StrategyInterface> strategiesClass = new ArrayList<>();

  protected final LogManager logger;
  protected final ShutdownNotifier shutdownNotifier;
  private final LoopSummaryCPAStatistics stats;

  @SuppressWarnings("unused")
  private CFA cfa;

  @SuppressWarnings("unused")
  private Specification specification;

  protected AbstractLoopSummaryCPA(
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa)
      throws InvalidConfigurationException {
    super(pCpa);
    pConfig.inject(this, AbstractLoopSummaryCPA.class);

    for (int i = 0; i < strategies.size(); i++) {
      switch (strategies.get(i)) {
        case BASE:
          strategiesClass.add(new BaseStrategy(pLogger, pShutdownNotifier, i));
          break;
        case LOOPACCELERATION:
          strategiesClass.add(new LoopAcceleration(pLogger, pShutdownNotifier, i));
          break;
        case NAIVELOOPACCELERATION:
          strategiesClass.add(new NaiveLoopAcceleration(pLogger, pShutdownNotifier, i));
          break;
        case POLYNOMIALEXTRAPOLATION:
          strategiesClass.add(new PolynomialExtrapolationStrategy(pLogger, pShutdownNotifier, i));
          break;
        case LINEAREXTRAPOLATION:
          strategiesClass.add(new LinearExtrapolationStrategy(pLogger, pShutdownNotifier, i));
          break;
        case CONSTANTEXTRAPOLATION:
          strategiesClass.add(new ConstantExtrapolationStrategy(pLogger, pShutdownNotifier, i));
          break;
        case NONDETBOUNDCONSTANTEXTRAPOLATION:
          strategiesClass.add(
              new NondetBoundConstantExtrapolationStrategy(pLogger, pShutdownNotifier, i));
          break;
        case LOOPUNROLLING:
          strategiesClass.add(
              new LoopUnrolling(pLogger, pShutdownNotifier, i, maxUnrollingsStrategy));
          break;
        case CONCOLICEXECUTION:
          strategiesClass.add(new ConcolicExecution(pLogger, pShutdownNotifier, i));
          break;
        case DETERMINISTICEXECUTION:
          strategiesClass.add(new DeterministicExecution(pLogger, pShutdownNotifier, i));
          break;
      }
    }

    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    stats = new LoopSummaryCPAStatistics(pConfig, pLogger, this);
    cfa = pCfa;
    specification = pSpecification;
  }

  @Override
  protected ConfigurableProgramAnalysis getWrappedCpa() {
    // override for visibility
    // TODO There is an error when casting to ConfigurableProgramAnalysisWithLoopSummary like is
    // done in AbstractBAMCPA
    return super.getWrappedCpa();
  }

  public LogManager getLogger() {
    return logger;
  }

  LoopSummaryCPAStatistics getStatistics() {
    return stats;
  }

  public List<StrategyInterface> getStrategies() {
    return strategiesClass;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
    super.collectStatistics(pStatsCollection);
  }
}
