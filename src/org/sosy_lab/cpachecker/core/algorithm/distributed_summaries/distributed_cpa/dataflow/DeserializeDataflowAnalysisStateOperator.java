package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.dataflow;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public class DeserializeDataflowAnalysisStateOperator implements DeserializeOperator {

  @Override
  public AbstractState deserialize(BlockSummaryMessage pMessage) throws InterruptedException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'deserialize'");
  }
}
