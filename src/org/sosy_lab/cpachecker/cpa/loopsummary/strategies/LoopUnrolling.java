// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary.strategies;

import java.util.Collection;
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.loopsummary.utils.GhostCFA;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class LoopUnrolling extends AbstractStrategy {

  Integer maxUnrollingsStrategy = 0;

  public LoopUnrolling(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      int pStrategyNumber,
      int pMaxUnrollingsStrategy) {
    super(pLogger, pShutdownNotifier, pStrategyNumber);
    maxUnrollingsStrategy = pMaxUnrollingsStrategy;
  }

  private Optional<GhostCFA> summaryCFA(CFANode pLoopStartNode, Integer pLoopBranchIndex) {
    // Initialize Ghost CFA
    CFANode startNodeGhostCFA = CFANode.newDummyCFANode("LSSTARTGHHOST");
    CFANode endNodeGhostCFA = CFANode.newDummyCFANode("LSENDGHHOST");
    CFANode currentNode = startNodeGhostCFA;

    for (int t = 0; t < maxUnrollingsStrategy; t++) {

      Optional<CFANode> loopUnrollingSuccess =
          unrollLoopOnce(pLoopStartNode, pLoopBranchIndex, currentNode, endNodeGhostCFA);
      if (loopUnrollingSuccess.isEmpty()) {
        return Optional.empty();
      } else {
        currentNode = loopUnrollingSuccess.orElseThrow();
      }
    }

    CFAEdge blankOutgoingEdge =
        new BlankEdge("Blank", FileLocation.DUMMY, currentNode, endNodeGhostCFA, "Blank");
    currentNode.addLeavingEdge(blankOutgoingEdge);
    endNodeGhostCFA.addEnteringEdge(blankOutgoingEdge);
    return Optional.of(new GhostCFA(startNodeGhostCFA, endNodeGhostCFA));
  }

  @Override
  public Optional<Collection<? extends AbstractState>> summarizeLoopState(
      AbstractState pState, Precision pPrecision, TransferRelation pTransferRelation)
      throws CPATransferException, InterruptedException {
    // TODO Unroll the Loop some amount of times. Can be improved by checking the maximal amount of
    // Loop iterations and unrolling only that amount of iterations.
    // Can be faster than the normal analysis, since it does not expect the Refinement in order to
    // unroll the loop
    // but may also be slower, since the loop unrolling has been done and must be transversed.
    // TODO, how can we see if we already applied loop unrolling in order to not apply it again once
    // the current unrolling has finished?

    CFANode loopStartNode = AbstractStates.extractLocation(pState);

    if (loopStartNode.getNumLeavingEdges() != 1) {
      return Optional.empty();
    }

    if (!loopStartNode.getLeavingEdge(0).getDescription().equals("while")) {
      return Optional.empty();
    }

    loopStartNode = loopStartNode.getLeavingEdge(0).getSuccessor();

    Optional<Integer> loopBranchIndexOptional = getLoopBranchIndex(loopStartNode);
    Integer loopBranchIndex;

    if (loopBranchIndexOptional.isEmpty()) {
      return Optional.empty();
    } else {
      loopBranchIndex = loopBranchIndexOptional.orElseThrow();
    }

    GhostCFA ghostCFA;
    Optional<GhostCFA> ghostCFASuccess = summaryCFA(loopStartNode, loopBranchIndex);

    if (ghostCFASuccess.isEmpty()) {
      return Optional.empty();
    } else {
      ghostCFA = ghostCFASuccess.orElseThrow();
    }

    @SuppressWarnings("unused")
    Collection<? extends AbstractState> realStatesEndCollection =
        transverseGhostCFA(
            ghostCFA, pState, pPrecision, loopStartNode, loopBranchIndex, pTransferRelation);

    return Optional.empty();
  }

  @Override
  public boolean isPrecise() {
    return true;
  }
}
