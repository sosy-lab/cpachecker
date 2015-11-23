/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.arg;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.GraphMlBuilder;

import com.google.common.base.Predicate;

enum GraphBuilder {

  ARG_PATH {

    @Override
    public String getId(ARGState pState) {
      return getId(pState, "");
    }

    private String getId(ARGState pState, String pIdentPostfix) {
      return String.format("A%d%s", pState.getStateId(), pIdentPostfix);
    }

    private String getId(ARGState pState, int pSubStateNo, int pSubStateCount) {
      return getId(pState, String.format("_%d_%d", pSubStateNo, pSubStateCount));
    }

    @Override
    public void buildGraph(ARGState pRootState,
        Predicate<? super ARGState> pPathStates,
        Predicate<? super Pair<ARGState, ARGState>> pIsRelevantEdge,
        Map<ARGState, CFAEdgeWithAssumptions> pValueMap,
        GraphMlBuilder pDocument,
        Iterable<Pair<ARGState, Iterable<ARGState>>> pARGEdges,
        EdgeAppender pEdgeAppender) {
      int multiEdgeCount = 0;
      for (Pair<ARGState, Iterable<ARGState>> argEdges : pARGEdges) {
        ARGState s = argEdges.getFirst();
        String sourceStateNodeId = getId(s);

        // Process child states
        for (ARGState child : argEdges.getSecond()) {

          String childStateId = getId(child);
          CFAEdge edgeToNextState = s.getEdgeToChild(child);
          String prevStateId = sourceStateNodeId;

          if (edgeToNextState instanceof MultiEdge) {
            // The successor state might have several incoming MultiEdges.
            // In this case the state names like ARG<successor>_0 would occur
            // several times.
            // So we add this counter to the state names to make them unique.
            multiEdgeCount++;

            // Write out a long linear chain of pseudo-states (one state encodes multiple edges)
            // because the AutomatonCPA also iterates through the MultiEdge.
            List<CFAEdge> edges = ((MultiEdge)edgeToNextState).getEdges();

            // inner part (without last edge)
            for (int i = 0; i < edges.size()-1; i++) {
              CFAEdge innerEdge = edges.get(i);
              String pseudoStateId = getId(child, i, multiEdgeCount);

              assert (!(innerEdge instanceof AssumeEdge));

              pEdgeAppender.appendNewEdge(pDocument, prevStateId, pseudoStateId, innerEdge, null, pValueMap);
              prevStateId = pseudoStateId;
            }

            // last edge connecting it with the real successor
            edgeToNextState = edges.get(edges.size()-1);
          }

          // Only proceed with this state if the path states contain the child
          if (pPathStates.apply(child) && pIsRelevantEdge.apply(Pair.of(s, child))) {
            // Child belongs to the path!
            pEdgeAppender.appendNewEdge(pDocument, prevStateId, childStateId, edgeToNextState, s, pValueMap);
          } else {
            // Child does not belong to the path --> add a branch to the SINK node!
            pEdgeAppender.appendNewEdgeToSink(pDocument, prevStateId, edgeToNextState, s, pValueMap);
          }
        }
      }
    }
  },

  SUB_PROGRAM {

    @Override
    public String getId(ARGState pState) {
      return AbstractStates.extractLocation(pState).toString();
    }

    @Override
    public void buildGraph(ARGState pRootState,
        final Predicate<? super ARGState> pPathStates,
        final Predicate<? super Pair<ARGState, ARGState>> pIsRelevantEdge,
        Map<ARGState, CFAEdgeWithAssumptions> pValueMap,
        GraphMlBuilder pDocument,
        Iterable<Pair<ARGState, Iterable<ARGState>>> pARGEdges,
        EdgeAppender pEdgeAppender) {

      final CFANode rootNode = AbstractStates.extractLocation(pRootState);

      // Get all successor nodes of edges
      final Set<CFANode> subProgramNodes = new HashSet<>();
      subProgramNodes.add(rootNode);
      for (final Pair<ARGState, Iterable<ARGState>> edge : pARGEdges) {
        for (ARGState target : edge.getSecond()) {
          // where the successor ARG node is in the set of target path states AND the edge is relevant
          if (pPathStates.apply(target) && pIsRelevantEdge.apply(Pair.of(edge.getFirst(), target))) {
            subProgramNodes.add(AbstractStates.extractLocation(target));
          }
        }
      }

      Queue<CFANode> waitlist = new ArrayDeque<>();
      Set<CFANode> visited = new HashSet<>();
      waitlist.offer(rootNode);
      visited.add(rootNode);
      while (!waitlist.isEmpty()) {
        CFANode current = waitlist.poll();
        for (CFAEdge leavingEdge : CFAUtils.leavingEdges(current)) {
          CFANode successor = leavingEdge.getSuccessor();
          if (subProgramNodes.contains(successor)) {
            appendEdge(pDocument, pEdgeAppender, leavingEdge);
            if (visited.add(successor)) {
              waitlist.offer(successor);
            }
          } else {
            String sourceId = current.toString();
            pEdgeAppender.appendNewEdgeToSink(pDocument, sourceId, leavingEdge);
          }
        }
      }
    }

    private void appendEdge(GraphMlBuilder pDocument, EdgeAppender pEdgeAppender, CFAEdge pEdge) {
      if (pEdge instanceof MultiEdge) {
        for (CFAEdge edge : (MultiEdge) pEdge) {
          appendEdge(pDocument, pEdgeAppender, edge);
        }
      } else {
        String sourceId = pEdge.getPredecessor().toString();
        String targetId = pEdge.getSuccessor().toString();
        pEdgeAppender.appendNewEdge(pDocument, sourceId, targetId, pEdge);
      }
    }

  };

  public abstract String getId(ARGState pState);

  public abstract void buildGraph(ARGState pRootState,
      Predicate<? super ARGState> pPathStates,
      Predicate<? super Pair<ARGState, ARGState>> pIsRelevantEdge,
      Map<ARGState, CFAEdgeWithAssumptions> pValueMap,
      GraphMlBuilder pDocument,
      Iterable<Pair<ARGState, Iterable<ARGState>>> pARGEdges,
      EdgeAppender pEdgeAppender);

}