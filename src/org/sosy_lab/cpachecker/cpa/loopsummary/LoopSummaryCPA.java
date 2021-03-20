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
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmFactory;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm.CPAAlgorithmFactory;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.ArithmeticStrategy;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.BaseStrategy;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.InterpolationStrategy;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.LoopAcceleration;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.NaiveLoopAcceleration;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.StrategyInterface;

@Options(prefix = "cpa.loopsummary")
public class LoopSummaryCPA extends AbstractLoopSummaryCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(LoopSummaryCPA.class);
  }

  private final LoopSummaryTransferRelation transfer;

  // TODO wie kann man die Klassen angeben
  @Option(
      name = "strategies",
      secure = true,
      description =
          "Strategies to be used in the Summary. The order of the strategies marks in which order they are tried")
  private ArrayList<StrategyInterface> strategies =
      new ArrayList<>(
          Arrays.asList(
              new ArithmeticStrategy(),
              new InterpolationStrategy(),
              new NaiveLoopAcceleration(),
              new LoopAcceleration(),
              new BaseStrategy()));

  @Option(
      name = "lookaheadamntnodes",
      secure = true,
      description =
          "Lookahead a certain amount of nodes in order to see if one can summarize some nodes inside to summarize the current node"
              + "This must be done in order to summarize loops inside loops")
  private int lookaheadamntnodes = 10;

  @Option(
      name = "lookaheaditerations",
      secure = true,
      description =
          "The amount of iterations one wants to summarize the ahead lookep CFA nodes in order to summarize loops inside loops")
  private int lookaheaditerations = 10;

  private LoopSummaryCPA(
      ConfigurableProgramAnalysis pCpa,
      Configuration config,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCfa)
      throws InvalidConfigurationException {
    super(pCpa, config, pLogger, pShutdownNotifier, pSpecification, pCfa);
    config.inject(this);

    AlgorithmFactory factory = new CPAAlgorithmFactory(this, logger, config, pShutdownNotifier);

    transfer =
        new LoopSummaryTransferRelation(
            this,
            pShutdownNotifier,
            factory,
            strategies,
            lookaheadamntnodes,
            lookaheaditerations,
            pCfa);
  }

  @Override
  public LoopSummaryTransferRelation getTransferRelation() {
    return transfer;
  }

}
