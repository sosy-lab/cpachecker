// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssBlockAnalysisStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;

public class DeserializeCompositeStateOperator implements DeserializeOperator {

  private final List<ConfigurableProgramAnalysis> analyses;
  private final DssBlockAnalysisStatistics stats;
  private final BlockNode blockNode;

  public DeserializeCompositeStateOperator(
      List<ConfigurableProgramAnalysis> pWrappedCPAs,
      BlockNode pBlockNode,
      DssBlockAnalysisStatistics pStats) {
    analyses = pWrappedCPAs;
    stats = pStats;
    blockNode = pBlockNode;
  }

  @Override
  public CompositeState deserialize(DssMessage pMessage) throws InterruptedException {
    try {
      stats.getDeserializationCount().inc();
      stats.getDeserializationTime().start();
      ImmutableList.Builder<AbstractState> states = ImmutableList.builder();
      for (ConfigurableProgramAnalysis analysis : analyses) {
        if (analysis instanceof DistributedConfigurableProgramAnalysis dcpa) {
          states.add(dcpa.getDeserializeOperator().deserialize(pMessage));
        } else {
          states.add(
              analysis.getInitialState(
                  DeserializeOperator.startLocationFromMessageType(pMessage, blockNode),
                  StateSpacePartition.getDefaultPartition()));
        }
      }
      return new CompositeState(states.build());
    } finally {
      stats.getDeserializationTime().stop();
    }
  }
}
