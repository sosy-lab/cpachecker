// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.witnessexport;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiPredicate;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAdditionalInfo;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;

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
    public void buildGraph(
        ARGState pRootState,
        Predicate<? super ARGState> pPathStates,
        final BiPredicate<ARGState, ARGState> pIsRelevantEdge,
        Multimap<ARGState, CFAEdgeWithAssumptions> pValueMap,
        Map<ARGState, CFAEdgeWithAdditionalInfo> pAdditionalInfo,
        Iterable<Pair<ARGState, Iterable<ARGState>>> pARGEdges,
        EdgeAppender pEdgeAppender)
        throws InterruptedException {
      int multiEdgeCount = 0;
      for (Pair<ARGState, Iterable<ARGState>> argEdges : pARGEdges) {
        ARGState s = argEdges.getFirst();
        if (!s.equals(pRootState)
            && s.getParents().stream().noneMatch(p -> pIsRelevantEdge.test(p, s))) {
          continue;
        }
        String sourceStateNodeId = getId(s);

        // Process child states
        for (ARGState child : argEdges.getSecond()) {

          String childStateId = getId(child);
          List<CFAEdge> allEdgeToNextState = s.getEdgesToChild(child);
          String prevStateId = sourceStateNodeId;
          CFAEdge edgeToNextState;

          if (allEdgeToNextState.isEmpty()) {
            edgeToNextState = null; // TODO no next state, what to do?

          } else if (allEdgeToNextState.size() == 1) {
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

              assert !(innerEdge instanceof AssumeEdge);

              boolean isAssumptionAvailableForEdge =
                  Iterables.any(pValueMap.get(s), a -> a.getCFAEdge().equals(innerEdge));
              Optional<Collection<ARGState>> absentStates =
                  isAssumptionAvailableForEdge
                      ? Optional.of(Collections.singleton(s))
                      : Optional.empty();
              pEdgeAppender.appendNewEdge(
                  prevStateId,
                  pseudoStateId,
                  innerEdge,
                  absentStates,
                  pValueMap,
                  CFAEdgeWithAdditionalInfo.of(innerEdge));
              prevStateId = pseudoStateId;
            }

            // last edge connecting it with the real successor
            edgeToNextState = allEdgeToNextState.get(allEdgeToNextState.size() - 1);
          }

          Optional<Collection<ARGState>> state = Optional.of(Collections.singleton(s));

          // Only proceed with this state if the path states contain the child
          if (pPathStates.apply(child) && pIsRelevantEdge.test(s, child)) {
            // Child belongs to the path!
            pEdgeAppender.appendNewEdge(
                prevStateId,
                childStateId,
                edgeToNextState,
                state,
                pValueMap,
                pAdditionalInfo.get(s));
            // For branchings, it is important to have both branches explicitly in the witness
            if (edgeToNextState instanceof AssumeEdge) {
              AssumeEdge assumeEdge = (AssumeEdge) edgeToNextState;
              AssumeEdge siblingEdge = CFAUtils.getComplimentaryAssumeEdge(assumeEdge);
              boolean addArtificialSinkEdge = true;
              for (ARGState sibling : s.getChildren()) {
                if (!Objects.equals(sibling, child)
                    && siblingEdge.equals(s.getEdgeToChild(sibling))
                    && pIsRelevantEdge.test(s, sibling)) {
                  addArtificialSinkEdge = false;
                  break;
                }
              }
              if (addArtificialSinkEdge) {
                // Child does not belong to the path --> add a branch to the SINK node!
                pEdgeAppender.appendNewEdgeToSink(
                    prevStateId, siblingEdge, state, pValueMap, pAdditionalInfo.get(s));
              }
            }
          } else {
            // Child does not belong to the path --> add a branch to the SINK node!
            pEdgeAppender.appendNewEdgeToSink(
                prevStateId, edgeToNextState, state, pValueMap, pAdditionalInfo.get(s));
          }
        }
      }
    }
  },

  @Deprecated
  CFA_FROM_ARG {

    @Override
    public String getId(ARGState pState) {
      return Joiner.on(",").join(AbstractStates.extractLocations(pState));
    }

    @Override
    public void buildGraph(
        ARGState pRootState,
        final Predicate<? super ARGState> pIsRelevantState,
        final BiPredicate<ARGState, ARGState> pIsRelevantEdge,
        Multimap<ARGState, CFAEdgeWithAssumptions> pValueMap,
        Map<ARGState, CFAEdgeWithAdditionalInfo> pAdditionalInfo,
        Iterable<Pair<ARGState, Iterable<ARGState>>> pARGEdges,
        EdgeAppender pEdgeAppender)
        throws InterruptedException {

      // normally there is only one node per state, thus we assume that there is only one root-node
      final CFANode rootNode =
          Iterables.getOnlyElement(AbstractStates.extractLocations(pRootState));

      // Get all successor nodes of edges
      final Set<CFANode> subProgramNodes = new HashSet<>();
      final Multimap<CFANode, ARGState> states = HashMultimap.create();

      subProgramNodes.add(rootNode);
      for (final Pair<ARGState, Iterable<ARGState>> edge : pARGEdges) {
        for (ARGState target : edge.getSecond()) {
          // where the successor ARG node is in the set of target path states AND the edge is
          // relevant
          if (pIsRelevantState.apply(target) && pIsRelevantEdge.test(edge.getFirst(), target)) {
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
          boolean tryAddToWaitlist = false;
          if (subProgramNodes.contains(successor)) {
            locationStates = states.get(successor);
            tryAddToWaitlist = true;
          } else {
            locationStates = ImmutableSet.of();
          }
          boolean appended =
              appendEdge(pEdgeAppender, leavingEdge, Optional.of(locationStates), pValueMap);
          if (tryAddToWaitlist && appended && visited.add(successor)) {
            waitlist.offer(successor);
          }
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
    public void buildGraph(
        ARGState pRootState,
        final Predicate<? super ARGState> pIsRelevantState,
        final BiPredicate<ARGState, ARGState> pIsRelevantEdge,
        Multimap<ARGState, CFAEdgeWithAssumptions> pValueMap,
        Map<ARGState, CFAEdgeWithAdditionalInfo> pAdditionalInfo,
        Iterable<Pair<ARGState, Iterable<ARGState>>> pARGEdges,
        EdgeAppender pEdgeAppender)
        throws InterruptedException {

      // normally there is only one node per state, thus we assume that there is only one root-node
      final CFANode rootNode =
          Iterables.getOnlyElement(AbstractStates.extractLocations(pRootState));

      // Get all successor nodes of edges
      final Set<CFANode> subProgramNodes = new HashSet<>();
      final Multimap<CFANode, ARGState> states = HashMultimap.create();

      subProgramNodes.add(rootNode);
      for (final Pair<ARGState, Iterable<ARGState>> edge : pARGEdges) {
        for (ARGState target : edge.getSecond()) {
          // where the successor ARG node is in the set of target path states AND the edge is
          // relevant
          if (pIsRelevantState.apply(target) && pIsRelevantEdge.test(edge.getFirst(), target)) {
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
          CFANode successor = enteringEdge.getSuccessor();
          final Optional<Collection<ARGState>> locationStates;
          if (subProgramNodes.contains(successor)) {
            locationStates = Optional.of(states.get(successor));
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

  private static boolean appendEdge(
      EdgeAppender pEdgeAppender,
      CFAEdge pEdge,
      Optional<Collection<ARGState>> pStates,
      Multimap<ARGState, CFAEdgeWithAssumptions> pValueMap)
      throws InterruptedException {

    String sourceId = pEdge.getPredecessor().toString();
    String targetId = pEdge.getSuccessor().toString();
    if (!(pEdge instanceof CFunctionSummaryStatementEdge)) {
      pEdgeAppender.appendNewEdge(
          sourceId, targetId, pEdge, pStates, pValueMap, CFAEdgeWithAdditionalInfo.of(pEdge));
      return true;
    }
    return false;
  }

  public abstract String getId(ARGState pState);

  public abstract void buildGraph(
      ARGState pRootState,
      Predicate<? super ARGState> pIsRelevantState,
      BiPredicate<ARGState, ARGState> pIsRelevantEdge,
      Multimap<ARGState, CFAEdgeWithAssumptions> pValueMap,
      Map<ARGState, CFAEdgeWithAdditionalInfo> pAdditionalInfo,
      Iterable<Pair<ARGState, Iterable<ARGState>>> pARGEdges,
      EdgeAppender pEdgeAppender)
      throws InterruptedException;
}
