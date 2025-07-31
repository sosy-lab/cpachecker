// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import java.util.Map;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssBlockAnalysisStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.java_smt.api.SolverException;

public class ProceedCompositeStateOperator implements ProceedOperator {

  private final Map<
          Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
      registered;
  private final DssBlockAnalysisStatistics stats;

  public ProceedCompositeStateOperator(
      Map<Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
          pRegistered,
      DssBlockAnalysisStatistics pStats) {
    registered = pRegistered;
    stats = pStats;
  }

  private DssMessageProcessing process(AbstractState pState, ProcessingFunction operator)
      throws InterruptedException, SolverException {
    stats.getProceedTime().start();
    CompositeState compositeState = (CompositeState) pState;
    DssMessageProcessing processing = DssMessageProcessing.proceed();
    for (AbstractState wrappedState : compositeState.getWrappedStates()) {
      for (DistributedConfigurableProgramAnalysis dcpa : registered.values()) {
        if (dcpa.doesOperateOn(wrappedState.getClass())) {
          processing = processing.merge(operator.apply(dcpa, wrappedState), true);
          break;
        }
      }
    }
    stats.getProceedTime().stop();
    return processing;
  }

  @Override
  public DssMessageProcessing processForward(AbstractState pState)
      throws InterruptedException, SolverException {
    return process(
        pState, (analysis, state) -> analysis.getProceedOperator().processForward(state));
  }

  @Override
  public DssMessageProcessing processBackward(AbstractState pState)
      throws InterruptedException, SolverException {
    return process(
        pState, (analysis, state) -> analysis.getProceedOperator().processBackward(state));
  }

  @FunctionalInterface
  private interface ProcessingFunction {
    DssMessageProcessing apply(DistributedConfigurableProgramAnalysis analysis, AbstractState state)
        throws InterruptedException, SolverException;
  }
}
