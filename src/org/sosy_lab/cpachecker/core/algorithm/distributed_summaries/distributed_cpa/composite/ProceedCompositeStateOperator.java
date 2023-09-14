// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import java.util.Map;
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
  private final BlockAnalysisStatistics stats;

  public ProceedCompositeStateOperator(
      Map<Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
          pRegistered,
      BlockAnalysisStatistics pStats) {
    registered = pRegistered;
    stats = pStats;
  }

  @Override
  public BlockSummaryMessageProcessing proceedForward(AbstractState pState)
      throws InterruptedException, SolverException {
    stats.getProceedTime().start();
    BlockSummaryMessageProcessing processing = BlockSummaryMessageProcessing.proceed();
    for (DistributedConfigurableProgramAnalysis value : registered.values()) {
      processing = processing.merge(value.getProceedOperator().proceedForward(pState), true);
    }
    stats.getProceedTime().stop();
    return processing;
  }

  @Override
  public BlockSummaryMessageProcessing proceedBackward(AbstractState pState)
      throws InterruptedException, SolverException {
    stats.getProceedTime().start();
    BlockSummaryMessageProcessing processing = BlockSummaryMessageProcessing.proceed();
    for (DistributedConfigurableProgramAnalysis value : registered.values()) {
      processing = processing.merge(value.getProceedOperator().proceedBackward(pState), true);
    }
    stats.getProceedTime().stop();
    return processing;
  }
}
