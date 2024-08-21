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
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;

public class DeserializeCompositeStateOperator implements DeserializeOperator {

  private final Map<
          Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
      registered;
  private final CompositeCPA compositeCPA;
  private final BlockNode block;

  public DeserializeCompositeStateOperator(
      CompositeCPA pCompositeCPA,
      BlockNode pBlockNode,
      Map<Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
          pRegistered) {
    compositeCPA = pCompositeCPA;
    block = pBlockNode;
    registered = pRegistered;
  }

  @Override
  public CompositeState deserialize(BlockSummaryMessage pMessage) throws InterruptedException {
    CFANode location = block.getNodeWithNumber(pMessage.getTargetNodeNumber());
    List<AbstractState> states = new ArrayList<>();
    for (ConfigurableProgramAnalysis wrappedCPA : compositeCPA.getWrappedCPAs()) {
      if (registered.containsKey(wrappedCPA.getClass())) {
        DistributedConfigurableProgramAnalysis entry = registered.get(wrappedCPA.getClass());
        states.add(entry.getDeserializeOperator().deserialize(pMessage));
      } else {
        states.add(wrappedCPA.getInitialState(location, StateSpacePartition.getDefaultPartition()));
      }
    }
    return new CompositeState(states);
  }
}
