// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import java.util.Map;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DSSStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DSSMessagePayload;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;

public class SerializeCompositeStateOperator implements SerializeOperator {

  private final Map<
          Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
      registered;
  private final DSSStatistics stats;

  public SerializeCompositeStateOperator(
      Map<Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
          pRegistered,
      DSSStatistics pStats) {
    registered = pRegistered;
    stats = pStats;
  }

  @Override
  public DSSMessagePayload serialize(AbstractState pState) {
    try {
      stats.getSerializationCount().inc();
      stats.getSerializationTime().start();
      DSSMessagePayload.Builder payload = DSSMessagePayload.builder();
      CompositeState compositeState = ((CompositeState) pState);
      for (AbstractState wrappedState : compositeState.getWrappedStates()) {
        for (DistributedConfigurableProgramAnalysis value : registered.values()) {
          if (value.doesOperateOn(wrappedState.getClass())) {
            payload = payload.addAllEntries(value.getSerializeOperator().serialize(wrappedState));
          }
        }
      }
      return payload.buildPayload();
    } finally {
      stats.getSerializationTime().stop();
    }
  }
}
