// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageWithStates;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssViolationConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public interface DeserializeOperator {

  String STATE_KEY = SerializeOperator.STATE_KEY;

  static CFANode startLocationFromMessageType(DssMessageWithStates pMessage, BlockNode blockNode) {
    if (Objects.requireNonNull(pMessage) instanceof DssViolationConditionMessage) {
      return blockNode.getFinalLocation();
    }
    return blockNode.getInitialLocation();
  }

  /**
   * Deserialize a message to an abstract state
   *
   * @param pMessage The message that contains the serialized state that is converted to an abstract
   *     state {@link AbstractState}
   * @param pStateIndex The index of the state to deserialize
   * @return An abstract state described by {@code pMessage, pStateContent}
   * @throws InterruptedException thrown if program is interrupted from the outside.
   */
  AbstractState deserialize(DssMessageWithStates pMessage, int pStateIndex)
      throws InterruptedException;
}
