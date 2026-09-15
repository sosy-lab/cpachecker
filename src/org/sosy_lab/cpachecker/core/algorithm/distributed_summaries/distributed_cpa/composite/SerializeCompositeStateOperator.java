// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.DistributedCompositeCPA.zip;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentBuilder;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssBlockAnalysisStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.DistributedCompositeCPA.CpaAndState;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;

public class SerializeCompositeStateOperator implements SerializeOperator {

  private final List<ConfigurableProgramAnalysis> wrapped;
  private final DssBlockAnalysisStatistics stats;

  public SerializeCompositeStateOperator(
      List<ConfigurableProgramAnalysis> pWrapped, DssBlockAnalysisStatistics pStats) {
    wrapped = pWrapped;
    stats = pStats;
  }

  @Override
  public ImmutableMap<String, String> serialize(AbstractState pState) {
    try {
      stats.getSerializationCount().inc();
      stats.getSerializationTime().start();
      ContentBuilder contentBuilder = ContentBuilder.builder();
      CompositeState compositeState = ((CompositeState) pState);
      for (CpaAndState cpaAndState : zip(wrapped, compositeState)) {
        if (cpaAndState.cpa() instanceof DistributedConfigurableProgramAnalysis dcpa) {
          contentBuilder.putAll(dcpa.getSerializeOperator().serialize(cpaAndState.state()));
        }
      }
      return contentBuilder.build();
    } finally {
      stats.getSerializationTime().stop();
    }
  }
}
