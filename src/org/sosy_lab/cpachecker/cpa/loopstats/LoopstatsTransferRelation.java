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
package org.sosy_lab.cpachecker.cpa.loopstats;

import static com.google.common.base.Predicates.*;
import static com.google.common.collect.Iterables.filter;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;

/**
 * Transfer relation for {@link LoopstatsState}s:
 *    add to stack if we are entering the loop,
 *    pop from the stack if we are leaving the loop,
 *    identity otherwise.
 */
public class LoopstatsTransferRelation extends SingleEdgeTransferRelation {

  private final Map<CFAEdge, Loop> loopEntryEdges;
  private final Map<CFAEdge, Loop> loopExitEdges;

  public LoopstatsTransferRelation(LoopStructure pLoops) {

    ImmutableMap.Builder<CFAEdge, Loop> entryEdges = ImmutableMap.builder();
    ImmutableMap.Builder<CFAEdge, Loop> exitEdges  = ImmutableMap.builder();

    for (final Loop l : pLoops.getAllLoops()) {
      Iterable<CFAEdge> edgesToLoopBody = Collections2.transform(l.getLoopHeads(), new Function<CFANode, CFAEdge>() {

        @Override
        public CFAEdge apply(CFANode pNode) {
          FluentIterable<CFAEdge> leaving = CFAUtils.leavingEdges(pNode).filter(in(l.getInnerLoopEdges()));

          // Two leaving edges might enter a loop (or at least stay in the same loop)
          //
          //    Example:
          //
          //          goto label2;
          //          label1:
          //          foo(m + (unsigned long ) i);
          //          i = i + 1;
          //          label2: ;
          //          if (k > i) {
          //            goto ldv_22165;
          //          } else {}

          // TODO: The following condition will fail for several program.
          //    It is here as a TODO-Marker.
          //    Program for which it fails: kernel-locking-locktorture.c
          Preconditions.checkState(leaving.size() == 1);

          return leaving.first().get();
        }

      });

      Iterable<CFAEdge> outgoingEdges = filter(l.getOutgoingEdges(),
                                               not(instanceOf(CFunctionCallEdge.class)));

      for (CFAEdge e : edgesToLoopBody) {
        entryEdges.put(e, l);
      }

      for (CFAEdge e : outgoingEdges) {
        exitEdges.put(e, l);
      }
    }

    loopEntryEdges = entryEdges.build();
    loopExitEdges = exitEdges.build();
  }


  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException {

    final LoopstatsState sucessor = getAbstractSuccessorsForEdge0((LoopstatsState) pState,
        pPrecision, pCfaEdge);

    return Collections.singleton(sucessor);
  }

  private LoopstatsState getAbstractSuccessorsForEdge0(
      LoopstatsState pPredState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException {

    if (pCfaEdge instanceof CFunctionCallEdge) {
      // Such edges do never change loop stack status.
      // Return here because they might be mis-classified as exit edges
      // because our idea of a loop contains only those nodes within the same function.
      return pPredState;
    }

    // We are leaving a loop
    //  (we might never have entered the loop body itself)
    {
      final Loop leavingLoop = loopExitEdges.get(pCfaEdge);
      if (leavingLoop != null) {
        return LoopstatsState.createSuccessorForLeavingLoop(pPredState, leavingLoop);
      }
    }

    if (pCfaEdge instanceof CFunctionReturnEdge) {
      // Such edges may be real loop-exit edges "while () { return; }",
      // but never loop-entry edges.
      // Return here because they might be mis-classified as entry edges.
      return pPredState;
    }

    // We are entering a loop body (edge that leaves a loop head into the loop body)
    {
      final Loop enteringLoop = loopEntryEdges.get(pCfaEdge);
      if (enteringLoop != null) {
        return LoopstatsState.createSuccessorForEnteringLoopBody(pPredState, enteringLoop);
      }
    }


    return pPredState;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pElement, List<AbstractState> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) {

    return null;
  }
}