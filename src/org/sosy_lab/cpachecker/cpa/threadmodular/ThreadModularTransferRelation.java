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
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;

public class ThreadModularTransferRelation implements TransferRelation {

  private final TransferRelationTM transfer;
  private final CompatibilityCheck compatible;
  private final ThreadModularStatistics tStats;

  public ThreadModularTransferRelation(
      TransferRelationTM pWrapperTransfer,
      CompatibilityCheck pCompatible,
      ThreadModularStatistics pStatistics) {
    transfer = pWrapperTransfer;
    compatible = pCompatible;
    tStats = pStatistics;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pState, ReachedSet pReached, Precision pPrecision)
      throws CPATransferException, InterruptedException {

    tStats.transferTimer.start();
    ThreadModularState tmState = (ThreadModularState) pState;

    try {
      if (tmState.getWrappedState() == EpsilonState.getInstance()) {
        return Collections.emptySet();
      }

      ARGState aState = (ARGState) tmState.getWrappedState();

      if (aState.isCovered()) {
        return Collections.emptySet();
      }
      if (aState.isDestroyed()) {
        if (aState.getReplacedWith() != null) {
          aState = aState.getReplacedWith();
        } else {
          return Collections.emptySet();
        }
      }
      aState.markExpanded();

      Collection<Pair<AbstractState, InferenceObject>> pairs;

      InferenceObject parentIState = tmState.getInferenceObject();
      /*
       * if (parentIState == TauInferenceObject.getInstance()) { pairs =
       * transfer.getAbstractSuccessors(aState.getWrappedState(), parentIState, pPrecision); } else
       * { ARGState iState = (ARGState) parentIState;
       */
      pairs = transfer.getAbstractSuccessors(aState.getWrappedState(), parentIState, pPrecision);
      // }

      tStats.successorCalculationTimer.start();
      FluentIterable<ThreadModularState> reached =
          from(pReached.asCollection()).transform(s -> (ThreadModularState) s);

      // TODO set correct parent for a transfer in env
      // Remove epsilon
      FluentIterable<AbstractState> states =
          reached.filter(s -> s.getInferenceObject() == TauInferenceObject.getInstance())
              .transform(s -> s.getWrappedState());

      FluentIterable<InferenceObject> objects =
          reached.filter(s -> s.getWrappedState() == EpsilonState.getInstance())
              .transform(s -> s.getInferenceObject());

      Collection<ThreadModularState> result = new ArrayList<>();

      for (Pair<AbstractState, InferenceObject> pair : pairs) {
        /*
         * ARGState parent; if (parentIState == TauInferenceObject.getInstance()) { parent = aState;
         * } else { parent = }
         */
        ARGState state = new ARGState(pair.getFirst(), aState);
        if (parentIState != TauInferenceObject.getInstance()) {
          state.setAppliedEffect(parentIState);
        }
        InferenceObject iObject = pair.getSecond();
        result.add(new ThreadModularState(state, TauInferenceObject.getInstance()));
        if (iObject != EmptyInferenceObject.getInstance()) {
          // We need to add the object into reached set
          // iObject = new ARGState(iObject, aState);
          result.add(new ThreadModularState(EpsilonState.getInstance(), iObject));
        }

        for (AbstractState s : states) {
          checkStatesAndAdd(s, iObject, result);
        }
        for (InferenceObject o : objects) {
          checkStatesAndAdd(state, o, result);
        }

      }
      tStats.successorCalculationTimer.stop();

      return result;
    } finally {
      tStats.transferTimer.stop();
    }
  }

  private void checkStatesAndAdd(
      AbstractState pState, InferenceObject pObject, Collection<ThreadModularState> pSet) {
    if (pObject != TauInferenceObject.getInstance()
        && pObject != EmptyInferenceObject.getInstance()
        && pState != EpsilonState.getInstance()) {
      ARGState argState = (ARGState) pState;
      if (!argState.isDestroyed()) {
        tStats.compatibleCheckTimer.start();
        boolean b = compatible.compatible(argState.getWrappedState(), pObject);
        tStats.compatibleCheckTimer.stop();
        if (b) {
          pSet.add(new ThreadModularState(pState, pObject));
        }
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
