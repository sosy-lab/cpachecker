/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.bounds;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

public class BoundsTransferRelation extends SingleEdgeTransferRelation {

  private Multimap<CFANode, Loop> loopHeads = null;

  private final int maxLoopIterations;
  private final int loopIterationsBeforeAbstraction;

  public BoundsTransferRelation(
      int pLoopIterationsBeforeAbstraction,
      int pMaxLoopIterations,
      LoopStructure pLoops) {

    loopIterationsBeforeAbstraction = pLoopIterationsBeforeAbstraction;
    this.maxLoopIterations = pMaxLoopIterations;

    ImmutableMultimap.Builder<CFANode, Loop> heads = ImmutableMultimap.builder();

    for (Loop l : pLoops.getAllLoops()) {
      for (CFANode h : l.getLoopHeads()) {
        heads.put(h, l);
      }
    }
    loopHeads = heads.build();
  }


  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException {

    BoundsState e = (BoundsState) pElement;

    if (e.isStopState()) {
      return Collections.emptySet();
    }

    if (pCfaEdge instanceof FunctionCallEdge) {
      // such edges do never change loop stack status
      return Collections.singleton(pElement);
    }

    if (pCfaEdge instanceof FunctionReturnEdge) {
      e = e.returnFromFunction();
    }

    CFANode loc = pCfaEdge.getSuccessor();

    Collection<Loop> loops = loopHeads.get(loc);
    assert loops.size() <= 1;
    if (!loops.isEmpty()) {
      for (Loop loop : loops) {
        e = e.enter(loop, loopIterationsBeforeAbstraction);
      }
      if ((maxLoopIterations > 0)
          && e.getDeepestIteration() > maxLoopIterations) {
        e = e.stopIt();
      }
    }

    return Collections.singleton(e);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) {

    BoundsState state = (BoundsState) pState;

    for (CallstackState callstackState : FluentIterable.from(pOtherStates).filter(CallstackState.class)) {
      state = state.setCurrentFunction(callstackState.getCurrentFunction());
    }
    return state.equals(pState) ? Collections.singleton(pState) : Collections.singleton(state);
  }
}