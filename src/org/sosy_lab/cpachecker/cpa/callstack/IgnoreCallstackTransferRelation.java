// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.callstack;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class IgnoreCallstackTransferRelation extends CallstackTransferRelation {

  public IgnoreCallstackTransferRelation(CallstackOptions pOptions, LogManager pLogger) {
    super(pOptions, pLogger);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge) throws CPATransferException {
    CallstackState callstackState = (CallstackState) state;
    return ImmutableList.of(
        new CallstackState(
            callstackState.getPreviousState(),
            callstackState.getCurrentFunction(),
            callstackState.getCallNode()));
  }
}
