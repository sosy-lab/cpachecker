// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public interface DeserializeOperator {

  String STATE_KEY = SerializeOperator.STATE_KEY;

  static CFANode startLocationFromMessageType(DssMessage pMessage, BlockNode blockNode) {
    return switch (pMessage.getType()) {
      case VIOLATION_CONDITION -> blockNode.getFinalLocation();
      case POST_CONDITION -> blockNode.getInitialLocation();
      case EXCEPTION, RESULT, STATISTIC ->
          throw new IllegalArgumentException(
              "Cannot deserialize BlockState from message of type: "
                  + pMessage.getClass().getName());
    };
  }

  /**
   * Deserialize a message to an abstract state
   *
   * @param pMessage The message that is converted to an abstract state {@link AbstractState}
   * @return An abstract state described by {@code pMessage}
   * @throws InterruptedException thrown if program is interrupted from the outside.
   */
  AbstractState deserialize(DssMessage pMessage) throws InterruptedException;
}
