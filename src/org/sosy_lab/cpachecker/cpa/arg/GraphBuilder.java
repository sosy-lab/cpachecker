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

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.GraphMlBuilder;

public enum GraphBuilder {

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
          List<CFAEdge> allEdgeToNextState = s.getEdgesToChild(child);
          String prevStateId = sourceStateNodeId;
          CFAEdge edgeToNextState;

          if (allEdgeToNextState.size() == 1) {
            edgeToNextState = Iterables.getOnlyElement(allEdgeToNextState);

            // this is a dynamic multi edge
          } else {
            // The successor state might have several incoming MultiEdges.
            // In this case the state names like ARG<successor>_0 would occur
            // several times.
            // So we add this counter to the state names to make them unique.
            multiEdgeCount++;

            // inner part (without last edge)
            for (int i = 0; i < allEdgeToNextState.size() - 1; i++) {
              CFAEdge innerEdge = allEdgeToNextState.get(i);
              String pseudoStateId = getId(child, i, multiEdgeCount);

              assert (!(innerEdge instanceof AssumeEdge));

              Optional<Collection<ARGState>> absentStates = Optional.empty();
              pEdgeAppender.appendNewEdge(prevStateId, pseudoStateId, innerEdge, absentStates, pValueMap);
              prevStateId = pseudoStateId;
            }

            // last edge connecting it with the real successor
            edgeToNextState = allEdgeToNextState.get(allEdgeToNextState.size() - 1);
          }

          Optional<Collection<ARGState>> state =
              Optional.<Collection<ARGState>>of(Collections.singleton(s));

          // Only proceed with this state if the path states contain the child
          if (pPathStates.apply(child) && pIsRelevantEdge.apply(Pair.of(s, child))) {
            // Child belongs to the path!
            pEdgeAppender.appendNewEdge(
                prevStateId, childStateId, edgeToNextState, state, pValueMap);
          } else {
            // Child does not belong to the path --> add a branch to the SINK node!
            pEdgeAppender.appendNewEdgeToSink(
                prevStateId, edgeToNextState, state, pValueMap);
          }
        }
      }
    }

  },

  CFA_FROM_ARG {

    @Override
    public String getId(ARGState pState) {
      return Joiner.on(",").join(AbstractStates.extractLocations(pState));
    }

    @Override
    public void buildGraph(ARGState pRootState,
        final Predicate<? super ARGState> pPathStates,
        final Predicate<? super Pair<ARGState, ARGState>> pIsRelevantEdge,
        Map<ARGState, CFAEdgeWithAssumptions> pValueMap,
        GraphMlBuilder pDocument,
        Iterable<Pair<ARGState, Iterable<ARGState>>> pARGEdges,
        EdgeAppender pEdgeAppender) {

      // normally there is only one node per state, thus we assume that there is only one root-node
      final CFANode rootNode = Iterables.getOnlyElement(AbstractStates.extractLocations(pRootState));

      // Get all successor nodes of edges
      final Set<CFANode> subProgramNodes = new HashSet<>();
      final Multimap<CFANode, ARGState> states = HashMultimap.create();

      subProgramNodes.add(rootNode);
      for (final Pair<ARGState, Iterable<ARGState>> edge : pARGEdges) {
        for (ARGState target : edge.getSecond()) {
          // where the successor ARG node is in the set of target path states AND the edge is relevant
          if (pPathStates.apply(target) && pIsRelevantEdge.apply(Pair.of(edge.getFirst(), target))) {
            for (CFANode location : AbstractStates.extractLocations(target)) {
              subProgramNodes.add(location);
              states.put(location, target);
            }
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
          final Collection<ARGState> locationStates;
          if (subProgramNodes.contains(successor)) {
            locationStates = states.get(successor);
            if (visited.add(successor)) {
              waitlist.offer(successor);
            }
          } else {
            locationStates = Collections.<ARGState>emptySet();
          }
          appendEdge(pEdgeAppender, leavingEdge, Optional.of(locationStates), pValueMap);
        }
      }
    }

  },

  CFA_FULL {

    @Override
    public String getId(ARGState pState) {
      return Joiner.on(",").join(AbstractStates.extractLocations(pState));
    }

    @Override
    public void buildGraph(ARGState pRootState,
        final Predicate<? super ARGState> pPathStates,
        final Predicate<? super Pair<ARGState, ARGState>> pIsRelevantEdge,
        Map<ARGState, CFAEdgeWithAssumptions> pValueMap,
        GraphMlBuilder pDocument,
        Iterable<Pair<ARGState, Iterable<ARGState>>> pARGEdges,
        EdgeAppender pEdgeAppender) {

      // normally there is only one node per state, thus we assume that there is only one root-node
      final CFANode rootNode = Iterables.getOnlyElement(AbstractStates.extractLocations(pRootState));

      // Get all successor nodes of edges
      final Set<CFANode> subProgramNodes = new HashSet<>();
      final Multimap<CFANode, ARGState> states = HashMultimap.create();

      subProgramNodes.add(rootNode);
      for (final Pair<ARGState, Iterable<ARGState>> edge : pARGEdges) {
        for (ARGState target : edge.getSecond()) {
          // where the successor ARG node is in the set of target path states AND the edge is relevant
          if (pPathStates.apply(target) && pIsRelevantEdge.apply(Pair.of(edge.getFirst(), target))) {
            for (CFANode location : AbstractStates.extractLocations(target)) {
              subProgramNodes.add(location);
              states.put(location, target);
            }
          }
        }
      }

      Queue<CFANode> waitlist = new ArrayDeque<>();
      Set<CFANode> visited = new HashSet<>();
      waitlist.offer(rootNode);
      visited.add(rootNode);
      Set<CFAEdge> appended = new HashSet<>();
      while (!waitlist.isEmpty()) {
        CFANode current = waitlist.poll();
        for (CFAEdge leavingEdge : CFAUtils.leavingEdges(current)) {
          CFANode successor = leavingEdge.getSuccessor();
          final Optional<Collection<ARGState>> locationStates;
          if (subProgramNodes.contains(successor)) {
            locationStates = Optional.of(states.get(successor));
          } else {
            locationStates = Optional.empty();
          }
          if (visited.add(successor)) {
            waitlist.offer(successor);
          }
          if (appended.add(leavingEdge)) {
            appendEdge(pEdgeAppender, leavingEdge, locationStates, pValueMap);
          }
        }
        for (CFAEdge enteringEdge : CFAUtils.enteringEdges(current)) {
          CFANode predecessor = enteringEdge.getPredecessor();
          final Optional<Collection<ARGState>> locationStates;
          if (subProgramNodes.contains(predecessor)) {
            locationStates = Optional.of(states.get(predecessor));
          } else {
            locationStates = Optional.empty();
          }
          if (visited.add(predecessor)) {
            waitlist.offer(predecessor);
          }
          if (appended.add(enteringEdge)) {
            appendEdge(pEdgeAppender, enteringEdge, locationStates, pValueMap);
          }
        }
      }
    }

  };

  private static void appendEdge(
      EdgeAppender pEdgeAppender,
      CFAEdge pEdge,
      Optional<Collection<ARGState>> pStates,
      Map<ARGState, CFAEdgeWithAssumptions> pValueMap) {

    String sourceId = pEdge.getPredecessor().toString();
    String targetId = pEdge.getSuccessor().toString();
    if (!(pEdge instanceof CFunctionSummaryStatementEdge)) {
      pEdgeAppender.appendNewEdge(sourceId, targetId, pEdge, pStates, pValueMap);
    }
  }

  public abstract String getId(ARGState pState);

  public abstract void buildGraph(ARGState pRootState,
      Predicate<? super ARGState> pPathStates,
      Predicate<? super Pair<ARGState, ARGState>> pIsRelevantEdge,
      Map<ARGState, CFAEdgeWithAssumptions> pValueMap,
      GraphMlBuilder pDocument,
      Iterable<Pair<ARGState, Iterable<ARGState>>> pARGEdges,
      EdgeAppender pEdgeAppender);

}