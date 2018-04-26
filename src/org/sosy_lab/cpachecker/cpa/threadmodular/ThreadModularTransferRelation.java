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

import com.google.common.collect.FluentIterable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.EmptyInferenceObject;
import org.sosy_lab.cpachecker.core.defaults.EpsilonState;
import org.sosy_lab.cpachecker.core.defaults.TauInferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CompatibilityCheck;
import org.sosy_lab.cpachecker.core.interfaces.InferenceObject;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelationTM;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;

public class ThreadModularTransferRelation implements TransferRelation {

  private final TransferRelationTM transfer;
  private final CompatibilityCheck compatible;

  public ThreadModularTransferRelation(TransferRelationTM pWrapperTransfer, CompatibilityCheck pCompatible) {
    transfer = pWrapperTransfer;
    compatible = pCompatible;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pState, ReachedSet pReached, Precision pPrecision)
      throws CPATransferException, InterruptedException {

    ThreadModularState tmState = (ThreadModularState) pState;

    if (tmState.getWrappedState() == EpsilonState.getInstance()) {
      return Collections.emptySet();
    }

    Collection<Pair<AbstractState, InferenceObject>> pairs =
        transfer.getAbstractSuccessors(
            tmState.getWrappedState(), tmState.getInferenceObject(), pPrecision);

    FluentIterable<ThreadModularState> reached =
        from(pReached.asCollection()).transform(s -> (ThreadModularState) s);

    FluentIterable<AbstractState> states =
        reached
            .filter(s -> s.getInferenceObject() == TauInferenceObject.getInstance())
            .transform(s -> s.getWrappedState());

    FluentIterable<InferenceObject> objects =
        reached
            .filter(s -> s.getWrappedState() == EpsilonState.getInstance())
            .transform(s -> s.getInferenceObject());

    Collection<ThreadModularState> result = new ArrayList<>();

    for (Pair<AbstractState, InferenceObject> pair : pairs) {
      AbstractState state = pair.getFirst();
      InferenceObject iObject = pair.getSecond();
      result.add(new ThreadModularState(state, TauInferenceObject.getInstance()));
      if (iObject != EmptyInferenceObject.getInstance()) {
        // We need to add the object into reached set
        result.add(new ThreadModularState(EpsilonState.getInstance(), iObject));
      }

      for (AbstractState s : states) {
        checkStatesAndAdd(s, iObject, result);
      }
      for (InferenceObject o : objects) {
        checkStatesAndAdd(state, o, result);
      }

    }

    return result;
  }

  private void checkStatesAndAdd(
      AbstractState pState, InferenceObject pObject, Collection<ThreadModularState> pSet) {
    if (pObject != TauInferenceObject.getInstance()
        && pObject != EmptyInferenceObject.getInstance()
        && pState != EpsilonState.getInstance()) {
      if (compatible.compatible(pState, pObject)) {
        pSet.add(new ThreadModularState(pState, pObject));
      }
    }
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    throw new UnsupportedOperationException("Unsfupported transition via edge");
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pState, Precision pPrecision)
      throws CPATransferException, InterruptedException {
    throw new UnsupportedOperationException("Unsfupported transition without reached set");
  }
}
