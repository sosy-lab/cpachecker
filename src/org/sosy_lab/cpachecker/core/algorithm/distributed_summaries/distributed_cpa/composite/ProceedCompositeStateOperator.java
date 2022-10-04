// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import java.util.Map;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.BlockAnalysisStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.BlockSummaryMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.java_smt.api.SolverException;

public class ProceedCompositeStateOperator implements ProceedOperator {

  private final Map<
          Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
      registered;
  private final AnalysisDirection direction;
  private final BlockAnalysisStatistics stats;

  public ProceedCompositeStateOperator(
      Map<Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
          pRegistered,
      AnalysisDirection pDirection,
      BlockAnalysisStatistics pStats) {
    direction = pDirection;
    registered = pRegistered;
    stats = pStats;
  }

  @Override
  public BlockSummaryMessageProcessing proceedForward(AbstractState pState)
      throws InterruptedException, SolverException {
    BlockSummaryMessageProcessing processing = BlockSummaryMessageProcessing.proceed();
    for (DistributedConfigurableProgramAnalysis value : registered.values()) {
      processing = processing.merge(value.getProceedOperator().proceedForward(pState), true);
    }
    return processing;
  }

  @Override
  public BlockSummaryMessageProcessing proceedBackward(AbstractState pState)
      throws InterruptedException, SolverException {
    BlockSummaryMessageProcessing processing = BlockSummaryMessageProcessing.proceed();
    for (DistributedConfigurableProgramAnalysis value : registered.values()) {
      processing = processing.merge(value.getProceedOperator().proceedBackward(pState), true);
    }
    return processing;
  }

  @Override
  public BlockSummaryMessageProcessing proceed(AbstractState pState)
      throws InterruptedException, SolverException {
    try {
      stats.getProceedCount().inc();
      stats.getProceedTime().start();
      return direction == AnalysisDirection.FORWARD
          ? proceedForward(pState)
          : proceedBackward(pState);
    } finally {
      stats.getProceedTime().stop();
    }
  }

}
