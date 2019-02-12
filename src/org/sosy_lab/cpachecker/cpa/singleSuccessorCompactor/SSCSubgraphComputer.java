/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.singleSuccessorCompactor;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class SSCSubgraphComputer {

  private final SingleSuccessorCompactorCPA sscCpa;

  SSCSubgraphComputer(SingleSuccessorCompactorCPA pSscCpa) {
    sscCpa = pSscCpa;
  }

  public SSCARGState computeCounterexampleSubgraph(
      final ARGState pLastElement, final ARGReachedSet pMainReachedSet)
      throws CPATransferException, InterruptedException {
    final Map<ARGState, List<SSCARGState>> statesOnLongPaths = new LinkedHashMap<>();
    final SingleSuccessorCompactorTransferRelation transfer = sscCpa.getTransferRelation();
    final ImmutableSet<ARGState> statesOnShortPaths = ARGUtils.getAllStatesOnPathsTo(pLastElement);
    for (final ARGState sscState : statesOnShortPaths) {

      // recompute all inner states of a chain.
      // we actually ignore the new successor states and simply use the previously computed ones.
      final List<AbstractState> innerStates = new ArrayList<>();
      final Collection<? extends AbstractState> successors =
          transfer.getAbstractSuccessorsWithList(
              sscState.getWrappedState(),
              pMainReachedSet.asReachedSet().getPrecision(sscState),
              innerStates);

      assert !innerStates.isEmpty();
      checkSuccessors(sscState.getChildren(), successors);

      // wrap inner states and connect them into an ARG (linear chains)
      final List<SSCARGState> argStates = new ArrayList<>();
      ARGState parent = null;
      for (AbstractState innerState : innerStates) {
        SSCARGState argState = new SSCARGState(sscState, innerState, parent);
        argStates.add(argState);
        parent = argState;
      }

      statesOnLongPaths.put(sscState, argStates);
    }

    // insert connections between the new ARGstates from front and end of chains (branching points)
    for (ARGState state : statesOnShortPaths) {
      SSCARGState lastState = Iterables.getLast(statesOnLongPaths.get(state));
      for (ARGState successor : state.getChildren()) {
        if (statesOnShortPaths.contains(successor)) {
          ARGState successorState = statesOnLongPaths.get(successor).get(0);
          successorState.addParent(lastState);
        }
      }
    }

    return Iterables.getLast(statesOnLongPaths.get(pLastElement));
  }

  private void checkSuccessors(
      Collection<ARGState> pChildren, Collection<? extends AbstractState> pSuccessors) {
    // previously computed children might be covered or merged,
    // thus there should be equal or more successors.
    assert pChildren.size() <= pSuccessors.size();

    // TODO check equality of children and successors, or at least both-sided coverage.
  }

  /** This SSC-ARGState is used to build the Pseudo-ARG for CEX-retrieval. */
  static class SSCARGState extends ARGState {

    private static final long serialVersionUID = 1L;

    private final ARGState sscState;

    public SSCARGState(ARGState pSscState, AbstractState innerState) {
      this(pSscState, innerState, null);
    }

    public SSCARGState(ARGState pSscState, AbstractState innerState, ARGState parent) {
      super(innerState, parent);
      sscState = pSscState;
    }

    public ARGState getSSCState() {
      return sscState;
    }

    @Override
    public String toString() {
      return "SSCARGState {{" + super.toString() + "}}";
    }
  }
}
