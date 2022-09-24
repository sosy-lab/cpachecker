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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;

public class SerializeCompositeStateOperator implements SerializeOperator {

  private final Map<
          Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
      registered;

  public SerializeCompositeStateOperator(
      Map<Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
          pRegistered) {
    registered = pRegistered;
  }

  @Override
  public BlockSummaryMessagePayload serialize(AbstractState pState) {
    BlockSummaryMessagePayload.Builder payload = new BlockSummaryMessagePayload.Builder();
    for (AbstractState wrappedState : ((CompositeState) pState).getWrappedStates()) {
      for (DistributedConfigurableProgramAnalysis value : registered.values()) {
        if (value.doesOperateOn(wrappedState.getClass())) {
          payload = payload.addAllEntries(value.getSerializeOperator().serialize(wrappedState));
          break;
        }
      }
    }
    return payload.buildPayload();
  }
}
