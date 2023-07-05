// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.infer;

import java.time.Instant;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DCPAAlgorithms;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;

public class InferRootProofMessage extends BlockSummaryMessage {

  protected InferRootProofMessage(
      String pUniqueBlockId,
      int pTargetNodeNumber,
      BlockSummaryMessagePayload pPayload,
      Instant pTimeStamp) {
    super(MessageType.INFER_ROOT_PROOF, pUniqueBlockId, pTargetNodeNumber, pPayload, pTimeStamp);
  }

  public static BlockSummaryMessage newInferRootProof(
      String pUniqueBlockId, int pTargetNodeNumber, BlockSummaryMessagePayload pPayload) {
    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;
    BlockSummaryMessagePayload payload = DCPAAlgorithms.appendStatus(status, pPayload);
    return new InferRootProofMessage(pUniqueBlockId, pTargetNodeNumber, payload, Instant.now());
  }
}
