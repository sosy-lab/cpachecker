package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.infer;

import java.time.Instant;
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
    return new InferRootProofMessage(pUniqueBlockId, pTargetNodeNumber, pPayload, Instant.now());
  }
}
