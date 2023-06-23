package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.infer;

import java.time.Instant;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;

public class InferRootViolationsMessage extends BlockSummaryMessage {

  protected InferRootViolationsMessage(
      String pUniqueBlockId,
      int pTargetNodeNumber,
      BlockSummaryMessagePayload pPayload,
      Instant pTimeStamp) {
    super(
        MessageType.INFER_ROOT_VIOLATIONS, pUniqueBlockId, pTargetNodeNumber, pPayload, pTimeStamp);
  }

  public static BlockSummaryMessage newInferRootViolations(
      String pUniqueBlockId, int pTargetNodeNumber, BlockSummaryMessagePayload pPayload) {
    return new InferRootViolationsMessage(
        pUniqueBlockId, pTargetNodeNumber, pPayload, Instant.now());
  }
}
