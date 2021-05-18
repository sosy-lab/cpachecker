// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ForcedCoveringStopOperator;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.specification.Specification;

@Options(prefix = "cpa.loopsummary")
public class LoopSummaryCPA extends AbstractLoopSummaryCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(LoopSummaryCPA.class);
  }

  private final LoopSummaryTransferRelation transfer;

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

    transfer =
        new LoopSummaryTransferRelation(
            this,
            pShutdownNotifier,
            super.getStrategies(),
            lookaheadamntnodes,
            lookaheaditerations,
            pCfa);
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    return new LoopSummaryPrecision(getWrappedCpa().getInitialPrecision(pNode, pPartition));
  }

  @Override
  public LoopSummaryPrecisionAdjustment getPrecisionAdjustment() {
      return new LoopSummaryPrecisionAdjustment(getWrappedCpa().getPrecisionAdjustment());
  }

  @Override
  public MergeOperator getMergeOperator() {
    return new LoopSummaryMergeJoin(getWrappedCpa().getMergeOperator());
  }

  @Override
  public ForcedCoveringStopOperator getStopOperator() {
    return new LoopSummaryStopSep(getWrappedCpa().getStopOperator());
  }

  @Override
  public LoopSummaryTransferRelation getTransferRelation() {
    return transfer;
  }

}
