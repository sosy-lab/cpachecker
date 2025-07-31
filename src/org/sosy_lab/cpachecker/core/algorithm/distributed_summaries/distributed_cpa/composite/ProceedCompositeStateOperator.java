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

  @Override
  public DssMessageProcessing processForward(AbstractState pState)
      throws InterruptedException, SolverException {
    stats.getProceedTime().start();
    DssMessageProcessing processing = DssMessageProcessing.proceed();
    for (DistributedConfigurableProgramAnalysis value : registered.values()) {
      processing = processing.merge(value.getProceedOperator().processForward(pState), true);
    }
    stats.getProceedTime().stop();
    return processing;
  }

  @Override
  public DssMessageProcessing processBackward(AbstractState pState)
      throws InterruptedException, SolverException {
    stats.getProceedTime().start();
    CompositeState compositeState = (CompositeState) pState;
    DssMessageProcessing processing = DssMessageProcessing.proceed();
    for (AbstractState wrappedState : compositeState.getWrappedStates()) {
      boolean existsDcpaForState = false;
      for (DistributedConfigurableProgramAnalysis value : registered.values()) {
        if (value.doesOperateOn(wrappedState.getClass())) {
          existsDcpaForState = true;
          processing =
              processing.merge(value.getProceedOperator().processBackward(wrappedState), true);
        }
      }
      if (!existsDcpaForState) {
        throw new UnregisteredDistributedCpaError("Found no Dcpa for " + wrappedState.getClass());
      }
    }
    stats.getProceedTime().stop();
    return processing;
  }
}
