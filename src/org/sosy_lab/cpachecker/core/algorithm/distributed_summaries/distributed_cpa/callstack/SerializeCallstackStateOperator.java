// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack;

import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;

public class SerializeCallstackStateOperator implements SerializeOperator {

  @Override
  public BlockSummaryMessagePayload serialize(AbstractState pCallstackState) {
    List<String> states = new ArrayList<>();
    CallstackState callstackState = (CallstackState) pCallstackState;
    while (callstackState != null) {
      states.add(
          callstackState.getCallNode().getNodeNumber() + "." + callstackState.getCurrentFunction());
      callstackState = callstackState.getPreviousState();
    }
    Collections.reverse(states);
    String result = Joiner.on(DistributedCallstackCPA.DELIMITER).join(states);
    return new BlockSummaryMessagePayload.Builder()
        .addEntry(CallstackCPA.class.getName(), result)
        .buildPayload();
  }
}
