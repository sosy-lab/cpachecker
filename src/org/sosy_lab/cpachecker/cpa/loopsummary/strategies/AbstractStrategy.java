// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary.strategies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;

public abstract class AbstractStrategy implements StrategyInterface {

  protected Optional<Integer> getLoopBranchIndex(CFANode loopStartNode) {
    if (loopStartNode.getNumLeavingEdges() != 2) {
      return Optional.empty();
    }
    ArrayList<CFANode> reachableNodesIndex0 = new ArrayList<>();
    reachableNodesIndex0.add(loopStartNode.getLeavingEdge(0).getSuccessor());
    ArrayList<CFANode> reachableNodesIndex1 = new ArrayList<>();
    reachableNodesIndex1.add(loopStartNode.getLeavingEdge(1).getSuccessor());
    Integer loopBranchIndex = -1;
    while (loopBranchIndex == -1) {
      if (reachableNodesIndex1.isEmpty() && reachableNodesIndex0.isEmpty()) {
        return Optional.empty();
      }
      ArrayList<CFANode> newReachableNodesIndex0 = new ArrayList<>();
      for (CFANode s : reachableNodesIndex0) {
        if (s == loopStartNode) {
          loopBranchIndex = 0;
          break;
        } else {
          for (int i = 0; i < s.getNumLeavingEdges(); i++) {
            newReachableNodesIndex0.add(s.getLeavingEdge(i).getSuccessor());
          }
        }
      }
      reachableNodesIndex0 = newReachableNodesIndex0;
      ArrayList<CFANode> newReachableNodesIndex1 = new ArrayList<>();
      for (CFANode s : reachableNodesIndex1) {
        if (s == loopStartNode) {
          loopBranchIndex = 1;
          break;
        } else {
          for (int i = 0; i < s.getNumLeavingEdges(); i++) {
            newReachableNodesIndex1.add(s.getLeavingEdge(i).getSuccessor());
          }
        }
      }
      reachableNodesIndex1 = newReachableNodesIndex1;
    }

    return Optional.of(loopBranchIndex);
  }

  protected AbstractState overwriteLocationState(AbstractState pState, LocationState locState) {
    List<AbstractState> allWrappedStatesByCompositeState = new ArrayList<>();
    for (AbstractState a :
        ((CompositeState) ((ARGState) pState).getWrappedState()).getWrappedStates()) {
      if (a instanceof LocationState) {
        allWrappedStatesByCompositeState.add(locState);
      } else {
        allWrappedStatesByCompositeState.add(a);
      }
    }
    AbstractState wrappedCompositeState = new CompositeState(allWrappedStatesByCompositeState);
    return new ARGState(wrappedCompositeState, null);
  }

  protected CAssumeEdge overwriteStartEndStateEdge(
      CAssumeEdge edge, boolean truthAssignment, CFANode startNode, CFANode endNode) {
    return new CAssumeEdge(
        edge.getDescription(),
        FileLocation.DUMMY,
        startNode,
        endNode,
        edge.getExpression(),
        truthAssignment);
  }

  protected CStatementEdge overwriteStartEndStateEdge(
      CStatementEdge edge, CFANode startNode, CFANode endNode) {
    return new CStatementEdge(
        edge.getRawStatement(), edge.getStatement(), FileLocation.DUMMY, startNode, endNode);
  }

  protected Collection<AbstractState> transverseGhostCFA(
      GhostCFA ghostCFA,
      final AbstractState pState,
      final Precision pPrecision,
      TransferRelation pTransferRelation,
      int loopBranchIndex)
      throws CPATransferException, InterruptedException {
    LocationState oldLocationState = AbstractStates.extractStateByType(pState, LocationState.class);
    LocationState newLocationState = AbstractStates.extractStateByType(pState, LocationState.class);
    LocationState ghostStartLocationState =
        new LocationState(ghostCFA.getStartNode(), oldLocationState.getFollowFunctionCalls());
    AbstractState dummyStateStart = overwriteLocationState(pState, ghostStartLocationState);
    @SuppressWarnings("unchecked")
    ArrayList<AbstractState> dummyStatesEndCollection =
        new ArrayList<>(
            pTransferRelation.getAbstractSuccessors(
                dummyStateStart,
                pPrecision)); // TODO Can you write Collection<AbstractState> insetad of
    // Collection<?
    // extends AbstractState>
    Collection<AbstractState> realStatesEndCollection = new ArrayList<>();
    LocationState afterLoopLocationState =
        new LocationState(
            AbstractStates.extractLocation(pState)
                .getLeavingEdge(1 - loopBranchIndex)
                .getSuccessor(),
            newLocationState.getFollowFunctionCalls());
    // Iterate till the end of the ghost CFA
    while (!dummyStatesEndCollection.isEmpty()) {
      ArrayList<AbstractState> newStatesNotFinished = new ArrayList<>();
      Iterator<? extends AbstractState> iterator = dummyStatesEndCollection.iterator();
      while (iterator.hasNext()) {
        AbstractState stateGhostCFA = iterator.next();
        if (AbstractStates.extractLocation(stateGhostCFA) == ghostCFA.getStopNode()) {
          realStatesEndCollection.add(
              overwriteLocationState(stateGhostCFA, afterLoopLocationState));
        } else {
          newStatesNotFinished.addAll(
              pTransferRelation.getAbstractSuccessors(stateGhostCFA, pPrecision));
        }
      }
      dummyStatesEndCollection = newStatesNotFinished;
    }

    return realStatesEndCollection;
  }

  protected CFANode unrollLoopOnce(
      CFANode loopStartNode,
      Integer loopBranchIndex,
      CFANode endNodeGhostCFA,
      CFANode startNodeGhostCFA) {
    // TODO Loops inside the loop to be unrolled, are unrolled completely, meaning it is possible
    // that this function does not terminate. How do we handle this?
    boolean initial = true;
    CFANode endLoopUnrollingNode = CFANode.newDummyCFANode("LSU");
    // First entry is the ghostCFA Node, the second entry is the real CFA Node
    ArrayList<Pair<CFANode, CFANode>> currentVisitedNodes = new ArrayList<>();
    while (!currentVisitedNodes.isEmpty() || initial) {
      if (initial) {
        CFANode currentUnrollingNode = CFANode.newDummyCFANode("LSU");
        CFAEdge currentLoopEdge = loopStartNode.getLeavingEdge(loopBranchIndex);
        assert currentLoopEdge instanceof CAssumeEdge;
        CFAEdge tmpLoopEdgeFalse =
            overwriteStartEndStateEdge(
                (CAssumeEdge) currentLoopEdge, false, startNodeGhostCFA, endNodeGhostCFA);
        startNodeGhostCFA.addLeavingEdge(tmpLoopEdgeFalse);
        endNodeGhostCFA.addEnteringEdge(tmpLoopEdgeFalse);
        CFAEdge tmpLoopEdgeTrue =
            overwriteStartEndStateEdge(
                (CAssumeEdge) currentLoopEdge, true, startNodeGhostCFA, currentUnrollingNode);
        startNodeGhostCFA.addLeavingEdge(tmpLoopEdgeTrue);
        currentUnrollingNode.addEnteringEdge(tmpLoopEdgeTrue);
        currentVisitedNodes.add(Pair.of(currentUnrollingNode, currentLoopEdge.getSuccessor()));
        initial = false;
      } else {
        ArrayList<Pair<CFANode, CFANode>> newVisitedNodes = new ArrayList<>();
        for (Pair<CFANode, CFANode> p : currentVisitedNodes) {
          for (int i = 0; i < p.getSecond().getNumLeavingEdges(); i++) {
            CFANode nextGhostCFANode = CFANode.newDummyCFANode("LSU");
            CFAEdge currentLoopEdge = p.getSecond().getLeavingEdge(i);
            CFAEdge tmpLoopEdge;
            if (currentLoopEdge.getSuccessor() == loopStartNode) {
              nextGhostCFANode = endLoopUnrollingNode;
            }
            if (currentLoopEdge instanceof CStatementEdge) {
              tmpLoopEdge =
                  overwriteStartEndStateEdge(
                      (CStatementEdge) currentLoopEdge, p.getFirst(), nextGhostCFANode);
            } else {
              assert currentLoopEdge instanceof BlankEdge;
              tmpLoopEdge =
                  new BlankEdge(
                      currentLoopEdge.getRawStatement(),
                      FileLocation.DUMMY,
                      p.getFirst(),
                      nextGhostCFANode,
                      currentLoopEdge.getDescription());
            }
            p.getFirst().addLeavingEdge(tmpLoopEdge);
            nextGhostCFANode.addEnteringEdge(tmpLoopEdge);
            if (currentLoopEdge.getSuccessor() != loopStartNode) {
              newVisitedNodes.add(Pair.of(nextGhostCFANode, currentLoopEdge.getSuccessor()));
            }
          }
        }
        currentVisitedNodes = newVisitedNodes;
      }
    }

    return endLoopUnrollingNode;
  }
}
