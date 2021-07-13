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
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ForcedCoveringStopOperator;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;

public class LoopSummaryCPA extends AbstractLoopSummaryCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(LoopSummaryCPA.class);
  }

  private final LoopSummaryTransferRelation transfer;

  private LoopSummaryCPA(
      ConfigurableProgramAnalysis pCpa,
      Configuration config,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException {
    super(pCpa, config, pLogger, pShutdownNotifier);
    config.inject(this);

    transfer = new LoopSummaryTransferRelation(this, pShutdownNotifier);
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
