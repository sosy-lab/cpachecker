// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy.partitioning;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;

public class CMCPartitionChecker {

  private Precision initPrec;
  private final StopOperator stopOp;
  private final TransferRelation transfer;
  private final ConfigurableProgramAnalysis cpa;

  private final AtomicBoolean checkResult;

  private final LogManager logger;
  private final ShutdownNotifier shutdown;

  private final ARGState root;
  private Map<AbstractState, ARGState> toARGState;
  private List<ARGState> incompleteStates;
  private Collection<AbstractState> inspectedStates;
  private Collection<AbstractState> externalNodes;
  private Multimap<CFANode, AbstractState> partitionNodes;

  public CMCPartitionChecker(
      final ConfigurableProgramAnalysis pCpa,
      final AtomicBoolean pCheckResult,
      final ShutdownNotifier pShutdown,
      final LogManager pLogger,
      final AbstractState pRoot)
      throws InterruptedException {
    cpa = pCpa;
    stopOp = cpa.getStopOperator();
    transfer = cpa.getTransferRelation();

    checkResult = pCheckResult;
    logger = pLogger;
    shutdown = pShutdown;

    inspectedStates = new ArrayList<>();
    incompleteStates = new ArrayList<>();
    toARGState = new HashMap<>();
    externalNodes = new ArrayList<>();
    partitionNodes = HashMultimap.create();

    root = new ARGState(pRoot, null);
    toARGState.put(pRoot, root);
    externalNodes.add(pRoot);

    CFANode main = AbstractStates.extractLocation(pRoot);
    initPrec = cpa.getInitialPrecision(main, StateSpacePartition.getDefaultPartition());
  }

  public void checkPartition(
      final AbstractState[] pPartitionNodes,
      final AbstractState[] pExternalNodes,
      final int[][] partitionEdgesAdjacencyList,
      final int maxProofSize)
      throws InterruptedException {
    prepareCoverageInspectionOfExternalNodes(pPartitionNodes, pExternalNodes);

    AbstractState current;
    Multimap<CFANode, AbstractState> statesPerLocation =
        computeMappingStatesToLocation(pPartitionNodes, pExternalNodes);

    try {
      int[] successorEdges;

      for (int i = 0; i < pPartitionNodes.length; i++) {
        shutdown.shutdownIfNecessary();
        if (!checkResult.get()) {
          return;
        }
        if (moreThanMaxSizeInspected(maxProofSize)) {

          logger.log(
              Level.SEVERE,
              "Checking failed, recomputed certificate bigger than original reached set.");
          abortChecking();
          return;
        }

        current = pPartitionNodes[i];
        inspectedStates.add(current);

        // check successors
        successorEdges = partitionEdgesAdjacencyList[i];

        if (successorEdges == null) {
          // standard case
          checkSuccessorsStandard(
              transfer.getAbstractSuccessors(current, initPrec), statesPerLocation, maxProofSize);

        } else {
          if (toARGState.get(current) == null) {
            toARGState.put(current, new ARGState(current, null));
          }
          if (successorEdges.length == 0) {

            incompleteStates.add(toARGState.get(current));

          } else {
            checkSuccessorsInRecomputedARG(
                transfer.getAbstractSuccessors(current, initPrec),
                getARGSuccessors(successorEdges, pPartitionNodes, pExternalNodes),
                maxProofSize,
                toARGState.get(current));
          }
        }
      }

    } catch (CPATransferException e) {
      logger.log(Level.SEVERE, "Successor computation failed.");
      abortChecking();
    } catch (CPAException e) {
      logger.log(Level.SEVERE, "Coverage check of initial state or successor failed.");
      abortChecking();
    }
  }

  private Collection<AbstractState> getARGSuccessors(
      final int[] pSuccessorEdges,
      final AbstractState[] pPartitionNodes,
      final AbstractState[] pExternalNodes) {
    List<AbstractState> result = new ArrayList<>(pSuccessorEdges.length);

    for (int successor : pSuccessorEdges) {
      if (successor >= pPartitionNodes.length) {
        result.add(pExternalNodes[successor - pPartitionNodes.length]);
      } else {
        result.add(pPartitionNodes[successor]);
      }
    }

    return result;
  }

  protected void abortChecking() {
    checkResult.set(false);
  }

  private Multimap<CFANode, AbstractState> computeMappingStatesToLocation(
      final AbstractState[] pPartitionNodes, final AbstractState[] pExternalNodes) {
    Multimap<CFANode, AbstractState> result = HashMultimap.create();

    addStatesToMapping(pPartitionNodes, result);
    addStatesToMapping(pExternalNodes, result);

    return result;
  }

  private void addStatesToMapping(
      final AbstractState[] stateSet, final Multimap<CFANode, AbstractState> mapping) {
    for (AbstractState state : stateSet) {
      mapping.put(AbstractStates.extractLocation(state), state);
    }
  }

  private boolean moreThanMaxSizeInspected(final int maxSize) {
    return inspectedStates.size() > maxSize;
  }

  private void checkSuccessorsStandard(
      final Collection<? extends AbstractState> pCollection,
      final Multimap<CFANode, AbstractState> pStatesPerLocation,
      final int maxSize)
      throws CPAException, InterruptedException {
    Deque<AbstractState> successorsToInspect = new ArrayDeque<>(pCollection);

    AbstractState successor;

    while (!successorsToInspect.isEmpty()) {
      shutdown.shutdownIfNecessary();

      successor = successorsToInspect.pop();

      if (!stopOp.stop(
          successor, pStatesPerLocation.get(AbstractStates.extractLocation(successor)), initPrec)) {
        // recomputed state
        inspectedStates.add(successor);
        if (moreThanMaxSizeInspected(maxSize)) {
          abortChecking();
          return;
        }

        // compute successors
        successorsToInspect.addAll(transfer.getAbstractSuccessors(successor, initPrec));
      }
    }
  }

  private void checkSuccessorsInRecomputedARG(
      final Collection<? extends AbstractState> pAbstractSuccessors,
      final Collection<AbstractState> pArgSuccessors,
      final int pMaxProofSize,
      final ARGState predecessor)
      throws CPAException, InterruptedException {
    if (pAbstractSuccessors.size() != 1 || pArgSuccessors.size() > 1) {
      // successors must be covered directly by saved successors
      for (AbstractState successor : pAbstractSuccessors) {
        shutdown.shutdownIfNecessary();
        if (!stopOp.stop(successor, pArgSuccessors, initPrec)) {
          logger.log(Level.SEVERE, "Successor not covered.");
          abortChecking();
          return;
        }
      }

      // reestablish edges
      for (AbstractState argSuccessor : pArgSuccessors) {
        shutdown.shutdownIfNecessary();
        addChild(predecessor, argSuccessor);
      }

    } else {
      ARGState currentPredecessor = predecessor;
      AbstractState singleState = pAbstractSuccessors.iterator().next();
      Collection<? extends AbstractState> successors;

      while (!stopOp.stop(singleState, pArgSuccessors, initPrec)) {
        shutdown.shutdownIfNecessary();

        // add edge
        addChild(currentPredecessor, singleState);
        currentPredecessor = toARGState.get(singleState);

        // inspect child (singleState)
        inspectedStates.add(singleState);
        if (moreThanMaxSizeInspected(pMaxProofSize)) {
          abortChecking();
          return;
        }

        successors = transfer.getAbstractSuccessors(singleState, initPrec);

        if (successors.size() > 1) {
          logger.log(
              Level.SEVERE,
              "More than one successor cannot be recomputed if ARG is reconstructed.");
          abortChecking();
          return;
        }

        if (successors.isEmpty()) {
          break;
        }

        singleState = successors.iterator().next();
      }

      addChild(currentPredecessor, pArgSuccessors.iterator().next());
    }
  }

  private void addChild(final ARGState parent, final AbstractState child) {
    if (!toARGState.containsKey(child)) {
      toARGState.put(child, new ARGState(child, parent));
    } else {
      toARGState.get(child).addParent(parent);
    }
  }

  public Pair<ARGState, List<ARGState>> getAutomatonReconstructionInfo() {
    return Pair.of(root, incompleteStates);
  }

  public Collection<AbstractState> getInspectedStates() {
    return inspectedStates;
  }

  private void prepareCoverageInspectionOfExternalNodes(
      AbstractState[] pPartitionNodes, AbstractState[] pExternalNodes) {
    externalNodes.addAll(Arrays.asList(pExternalNodes));

    addStatesToMapping(pPartitionNodes, partitionNodes);
  }

  public boolean checkCoverageOfExternalsAndInitialState()
      throws CPAException, InterruptedException {
    return stopOp.stop(
            cpa.getInitialState(
                AbstractStates.extractLocation(root), StateSpacePartition.getDefaultPartition()),
            Collections.singleton(root.getWrappedState()),
            initPrec)
        && PartitioningUtils.areElementsCoveredByPartitionElement(
            externalNodes, partitionNodes, stopOp, initPrec);
  }
}
