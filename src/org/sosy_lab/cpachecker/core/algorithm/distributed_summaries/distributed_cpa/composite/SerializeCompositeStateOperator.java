// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentBuilder;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssBlockAnalysisStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;

public class SerializeCompositeStateOperator implements SerializeOperator {

  private final Map<
          Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
      registered;
  private final DssBlockAnalysisStatistics stats;

  public SerializeCompositeStateOperator(
      Map<Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
          pRegistered,
      DssBlockAnalysisStatistics pStats) {
    registered = pRegistered;
    stats = pStats;
  }

  @Override
  public ImmutableMap<String, String> serialize(AbstractState pState) {
    try {
      stats.getSerializationCount().inc();
      stats.getSerializationTime().start();
      ContentBuilder contentBuilder = ContentBuilder.builderWithExpectedSize(registered.size());
      CompositeState compositeState = ((CompositeState) pState);
      for (AbstractState wrappedState : compositeState.getWrappedStates()) {
        for (DistributedConfigurableProgramAnalysis value : registered.values()) {
          if (value.doesOperateOn(wrappedState.getClass())) {
            contentBuilder.putAll(value.getSerializeOperator().serialize(wrappedState));
          }
        }
      }
      return contentBuilder.build();
    } finally {
      stats.getSerializationTime().stop();
    }
  }
}
