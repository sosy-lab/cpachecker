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
package org.sosy_lab.cpachecker.cpa.loopbound;

import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Iterables.filter;

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

public class LoopBoundTransferRelation extends SingleEdgeTransferRelation {

  private Map<CFAEdge, Loop> loopEntryEdges = null;
  private Map<CFAEdge, Loop> loopExitEdges = null;

  private Map<CFANode, Loop> loopHeads = null;

  private final int maxLoopIterations;
  private final int loopIterationsBeforeAbstraction;

  public LoopBoundTransferRelation(
      int pLoopIterationsBeforeAbstraction,
      int pMaxLoopIterations,
      LoopStructure pLoops) {

    loopIterationsBeforeAbstraction = pLoopIterationsBeforeAbstraction;
    this.maxLoopIterations = pMaxLoopIterations;

    ImmutableMap.Builder<CFAEdge, Loop> entryEdges = ImmutableMap.builder();
    ImmutableMap.Builder<CFAEdge, Loop> exitEdges  = ImmutableMap.builder();
    ImmutableMap.Builder<CFANode, Loop> heads = ImmutableMap.builder();

    for (Loop l : pLoops.getAllLoops()) {
      // function edges do not count as incoming/outgoing edges
      Iterable<CFAEdge> incomingEdges = filter(l.getIncomingEdges(),
                                               not(instanceOf(CFunctionReturnEdge.class)));
      Iterable<CFAEdge> outgoingEdges = filter(l.getOutgoingEdges(),
                                               not(instanceOf(CFunctionCallEdge.class)));

      for (CFAEdge e : incomingEdges) {
        entryEdges.put(e, l);
      }
      for (CFAEdge e : outgoingEdges) {
        exitEdges.put(e, l);
      }
      for (CFANode h : l.getLoopHeads()) {
        heads.put(h, l);
      }
    }
    loopEntryEdges = entryEdges.build();
    loopExitEdges = exitEdges.build();
    loopHeads = heads.build();
  }


  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException {

    LoopBoundState e = (LoopBoundState) pElement;
    LoopBoundPrecision precision = (LoopBoundPrecision) pPrecision;

    if (pCfaEdge instanceof FunctionCallEdge) {
      // such edges do never change loop status
      return Collections.singleton(pElement);
    }

    CFANode loc = pCfaEdge.getSuccessor();

    Loop oldLoop = loopExitEdges.get(pCfaEdge);
    if (oldLoop != null) {
      e = e.exit(oldLoop);
    }

    if (pCfaEdge instanceof CFunctionReturnEdge) {
      // Such edges may be real loop-exit edges "while () { return; }",
      // but never loop-entry edges.
      // Return here because they might be mis-classified as entry edges.
      return Collections.singleton(e);
    }

    Loop newLoop = null;
    if (precision.shouldTrackStack()) {
      // Push a new loop onto the stack if we enter it
      newLoop = loopEntryEdges.get(pCfaEdge);
      if (newLoop != null) {
        e = e.enter(new LoopEntry(loc, newLoop));
      }
    }

    // Check if we need to increment the loop counter
    Loop loop = loopHeads.get(loc);
    assert newLoop == null || newLoop.equals(loop);
    if (loop != null) {
      e = e.visitLoopHead(new LoopEntry(loc, loop), loopIterationsBeforeAbstraction);
      if ((maxLoopIterations > 0)
          && e.getDeepestIteration() > maxLoopIterations) {
        e = e.stopIt();
      }
    }

    return Collections.singleton(e);
  }

}