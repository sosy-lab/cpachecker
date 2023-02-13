// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;

public class IdentityDeserializeOperator implements DeserializeOperator {

  private final ConfigurableProgramAnalysis parentCPA;
  private final CFANode blockStart;

  public IdentityDeserializeOperator(ConfigurableProgramAnalysis pParentCPA, CFANode pBlockStart) {
    parentCPA = pParentCPA;
    blockStart = pBlockStart;
  }

  @Override
  public AbstractState deserialize(BlockSummaryMessage pMessage) throws InterruptedException {
    return (AbstractState)
        pMessage
            .getAbstractState(parentCPA.getClass())
            .orElse(
                parentCPA.getInitialState(blockStart, StateSpacePartition.getDefaultPartition()));
  }
}
