/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.threadmodular;

import static com.google.common.collect.FluentIterable.from;

import java.util.Collection;
import org.sosy_lab.cpachecker.core.defaults.EmptyInferenceObject;
import org.sosy_lab.cpachecker.core.defaults.TauInferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.IOStopOperator;
import org.sosy_lab.cpachecker.core.interfaces.InferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ThreadModularStopOperator implements StopOperator {

  private final StopOperator stateStop;
  private final IOStopOperator ioStop;

  public ThreadModularStopOperator(StopOperator pStateStop, IOStopOperator pIOStop) {
    stateStop = pStateStop;
    ioStop = pIOStop;
  }

  @Override
  public boolean stop(
      AbstractState pState, Collection<AbstractState> pReached, Precision pPrecision)
      throws CPAException, InterruptedException {

    ThreadModularState targetTMState = (ThreadModularState) pState;
    AbstractState targetState = targetTMState.getWrappedState();
    InferenceObject targetIO = targetTMState.getInferenceObject();

    if (targetIO == EmptyInferenceObject.getInstance()) {
      return true;
    }

    Collection<AbstractState> innerStates =
        from(pReached).transform(s -> ((ThreadModularState) s).getWrappedState()).toSet();

    boolean result = stateStop.stop(targetState, innerStates, pPrecision);

    if (targetIO == TauInferenceObject.getInstance()) {
      return result;
    }

    Collection<InferenceObject> innerObjects =
        from(pReached).transform(s -> ((ThreadModularState) s).getInferenceObject()).toSet();

    return result && ioStop.stop(targetIO, innerObjects, pPrecision);
  }
}
