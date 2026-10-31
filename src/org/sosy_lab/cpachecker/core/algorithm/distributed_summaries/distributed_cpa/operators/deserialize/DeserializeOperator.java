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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage.DssMessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.payloads.DssStatePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public interface DeserializeOperator {

  static CFANode startLocationFromMessageType(DssMessageType pMessageType, BlockNode pBlockNode) {
    if (Objects.requireNonNull(pMessageType).equals(DssMessageType.VIOLATION_CONDITION)) {
      return pBlockNode.getFinalLocation();
    }
    return pBlockNode.getInitialLocation();
  }

  /**
   * Deserialize a message to an abstract state
   *
   * @param pStatePayload The serialized state payload that is converted to an abstract state {@link
   *     AbstractState}
   * @param pMessageType the message type used to determine the start location
   * @return An abstract state described by {@code pStatePayload}
   * @throws InterruptedException thrown if program is interrupted from the outside.
   */
  AbstractState deserialize(DssStatePayload pStatePayload, DssMessageType pMessageType)
      throws InterruptedException;
}
