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
  private final BlockAnalysisStatistics stats;

  public SerializeCompositeStateOperator(
      Map<Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
          pRegistered,
      BlockAnalysisStatistics pStats) {
    registered = pRegistered;
    stats = pStats;
  }

  @Override
  public BlockSummaryMessagePayload serialize(AbstractState pState) {
    try {
      stats.getSerializationCount().inc();
      stats.getSerializationTime().start();
      BlockSummaryMessagePayload.Builder payload = BlockSummaryMessagePayload.builder();
      for (AbstractState wrappedState : ((CompositeState) pState).getWrappedStates()) {
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