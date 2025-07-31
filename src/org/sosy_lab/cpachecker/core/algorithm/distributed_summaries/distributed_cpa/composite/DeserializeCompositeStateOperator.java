// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssBlockAnalysisStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;

public class DeserializeCompositeStateOperator implements DeserializeOperator {

  private final Map<
          Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
      registered;
  private final CompositeCPA compositeCPA;
  private final DssBlockAnalysisStatistics stats;
  private final BlockNode blockNode;

  public DeserializeCompositeStateOperator(
      CompositeCPA pCompositeCPA,
      Map<Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
          pRegistered,
      BlockNode pBlockNode,
      DssBlockAnalysisStatistics pStats) {
    compositeCPA = pCompositeCPA;
    registered = pRegistered;
    stats = pStats;
    blockNode = pBlockNode;
  }

  @Override
  public CompositeState deserialize(DssMessage pMessage) throws InterruptedException {
    try {
      stats.getDeserializationCount().inc();
      stats.getDeserializationTime().start();
      List<AbstractState> states = new ArrayList<>();
      for (ConfigurableProgramAnalysis wrappedCPA : compositeCPA.getWrappedCPAs()) {
        if (!registered.containsKey(wrappedCPA.getClass())) {
          throw new UnregisteredDistributedCpaError("Unregistered cpa " + wrappedCPA.getClass());
        }
        DistributedConfigurableProgramAnalysis entry = registered.get(wrappedCPA.getClass());
        states.add(entry.getDeserializeOperator().deserialize(pMessage));
      }
      return new CompositeState(states);
    } finally {
      stats.getDeserializationTime().stop();
    }
  }
}
