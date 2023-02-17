// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages;

import java.time.Instant;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;

public class BlockSummaryAbstractStateMessage extends BlockSummaryMessage {

  private final int targetNodeNumber;
  private final String id;

  /**
   * Messages transports information between different {@link
   * DistributedConfigurableProgramAnalysis}. Messages consist of four parts. The type decide which
   * information is guaranteed to be part of the payload. The unique block id {@code pUniqueBlockId}
   * stores the block id from which this message originates from. The target node number {@code
   * pTargetNodeNumber} provides the unique id of a {@link CFANode}. This id is only relevant for
   * messages that actually trigger an analysis: {@link BlockSummaryPostConditionMessage}, {@link
   * BlockSummaryErrorConditionMessage}. Finally, the payload contains a map of key-value pairs that
   * transport arbitrary information.
   *
   * @param pPayload a map that will be transformed into JSON.
   */
  protected BlockSummaryAbstractStateMessage(
      String pId, int pTargetNodeNumber, BlockSummaryMessagePayload pPayload) {
    super(MessageType.ABSTRACTION_STATE, pId, pTargetNodeNumber, pPayload, Instant.now());
    id = pId;
    targetNodeNumber = pTargetNodeNumber;
  }

  @Override
  protected BlockSummaryMessage replacePayload(BlockSummaryMessagePayload pPayload) {
    return new BlockSummaryAbstractStateMessage(id, targetNodeNumber, pPayload);
  }
}
