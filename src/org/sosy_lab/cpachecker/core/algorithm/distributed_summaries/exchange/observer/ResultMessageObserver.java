// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.observer;

import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.ActorMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.ActorMessage.MessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Payload;
import org.sosy_lab.cpachecker.core.defaults.DummyTargetState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;

public class ResultMessageObserver implements MessageObserver {

  private final ReachedSet reachedSet;
  private Result result;

  public ResultMessageObserver(ReachedSet pReachedSet) {
    reachedSet = pReachedSet;
    result = Result.UNKNOWN;
  }

  @Override
  public boolean process(ActorMessage pMessage) {
    if (pMessage.getType() == MessageType.FOUND_RESULT) {
      result = Result.valueOf(pMessage.getPayload().get(Payload.RESULT));
      return true;
    }
    return false;
  }

  @Override
  public void finish() {
    if (result == Result.FALSE) {
      ARGState state = (ARGState) reachedSet.getFirstState();
      CompositeState cState = (CompositeState) state.getWrappedState();
      Precision initialPrecision = reachedSet.getPrecision(state);
      List<AbstractState> states = new ArrayList<>(cState.getWrappedStates());
      states.add(DummyTargetState.withoutTargetInformation());
      reachedSet.add(new ARGState(new CompositeState(states), null), initialPrecision);
    } else if (result == Result.TRUE) {
      reachedSet.clear();
    }
  }
}
