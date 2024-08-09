// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import java.util.Map;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DSSMessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DSSStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.java_smt.api.SolverException;

public class ProceedCompositeStateOperator implements ProceedOperator {

  private final Map<
          Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
      registered;
  private final DSSStatistics stats;

  public ProceedCompositeStateOperator(
      Map<Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
          pRegistered,
      DSSStatistics pStats) {
    registered = pRegistered;
    stats = pStats;
  }

  @Override
  public DSSMessageProcessing processForward(AbstractState pState)
      throws InterruptedException, SolverException {
    stats.getProceedTime().start();
    DSSMessageProcessing processing = DSSMessageProcessing.proceed();
    for (DistributedConfigurableProgramAnalysis value : registered.values()) {
      processing =
          processing.merge(
              value
                  .getProceedOperator()
                  .processForward(
                      AbstractStates.extractStateByType(pState, value.getAbstractStateClass())),
              true);
    }
    stats.getProceedTime().stop();
    return processing;
  }

  @Override
  public DSSMessageProcessing processBackward(AbstractState pState)
      throws InterruptedException, SolverException {
    stats.getProceedTime().start();
    DSSMessageProcessing processing = DSSMessageProcessing.proceed();
    for (DistributedConfigurableProgramAnalysis value : registered.values()) {
      processing =
          processing.merge(
              value
                  .getProceedOperator()
                  .processBackward(
                      AbstractStates.extractStateByType(pState, value.getAbstractStateClass())),
              true);
    }
    stats.getProceedTime().stop();
    return processing;
  }
}
