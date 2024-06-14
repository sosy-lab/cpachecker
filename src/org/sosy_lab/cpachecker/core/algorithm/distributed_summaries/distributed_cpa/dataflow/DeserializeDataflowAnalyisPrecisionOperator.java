package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.dataflow;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

public class DeserializeDataflowAnalyisPrecisionOperator implements DeserializePrecisionOperator {

  @Override
  public Precision deserializePrecision(BlockSummaryMessage pMessage) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'deserializePrecision'");
  }
}
