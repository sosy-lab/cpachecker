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
package org.sosy_lab.cpachecker.cpa.loopstack;

import static com.google.common.base.Predicates.*;
import static com.google.common.collect.Iterables.filter;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.InvalidCFAException;
import org.sosy_lab.cpachecker.util.CFAUtils.Loop;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

public class LoopstackTransferRelation implements TransferRelation {

  private Map<CFAEdge, Loop> loopEntryEdges = null;
  private Map<CFAEdge, Loop> loopExitEdges = null;

  private Multimap<CFANode, Loop> loopHeads = null;

  private final int maxLoopIterations;

  public LoopstackTransferRelation(int maxLoopIterations, CFA pCfa) throws InvalidCFAException {
    this.maxLoopIterations = maxLoopIterations;
    if (!pCfa.getLoopStructure().isPresent()) {
      throw new InvalidCFAException("LoopstackCPA does not work without loop information!");
    }
    Multimap<String, Loop> loops = pCfa.getLoopStructure().get();

    ImmutableMap.Builder<CFAEdge, Loop> entryEdges = ImmutableMap.builder();
    ImmutableMap.Builder<CFAEdge, Loop> exitEdges  = ImmutableMap.builder();
    ImmutableMultimap.Builder<CFANode, Loop> heads = ImmutableMultimap.builder();

    for (Loop l : loops.values()) {
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
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException {

    if (pCfaEdge instanceof CFunctionCallEdge) {
      // such edges do never change loop stack status
      // Return here because they might be mis-classified as exit edges
      // because our idea of a loop contains only those nodes within the same function
      return Collections.singleton(pElement);
    }

    CFANode loc = pCfaEdge.getSuccessor();
    LoopstackState e = (LoopstackState)pElement;

    Loop oldLoop = loopExitEdges.get(pCfaEdge);
    if (oldLoop != null) {
      assert oldLoop.equals(e.getLoop()) : e + " " + oldLoop + " " + pCfaEdge;
      e = e.getPreviousState();
    }

    if (pCfaEdge instanceof CFunctionReturnEdge) {
      // such edges may be real loop-exit edges "while () { return; }",
      // but never loop-entry edges
      // Return here because they might be mis-classified as entry edges
      return Collections.singleton(pElement);
    }

    Loop newLoop = loopEntryEdges.get(pCfaEdge);
    if (newLoop != null) {
      e = new LoopstackState(e, newLoop, 0, false);
    }

    Collection<Loop> loops = loopHeads.get(loc);
    assert loops.size() <= 1;
    if (loops.contains(e.getLoop())) {
      boolean stop = (maxLoopIterations > 0) && (e.getIteration() >= maxLoopIterations);
      e = new LoopstackState(e.getPreviousState(), e.getLoop(), e.getIteration()+1, stop);
    }

    return Collections.singleton(e);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pElement, List<AbstractState> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) {

    return null;
  }
}