package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.dataflow;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public class SerializeDataflowAnalysisStateOperator implements SerializeOperator {

  @Override
  public BlockSummaryMessagePayload serialize(AbstractState pState) {

    throw new UnsupportedOperationException("Unimplemented method 'serialize'");
  }
}
