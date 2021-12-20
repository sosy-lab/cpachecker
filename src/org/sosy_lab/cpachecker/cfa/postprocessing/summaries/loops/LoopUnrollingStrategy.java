// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops;

import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.GhostCFA;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependencyInterface;

public class LoopUnrollingStrategy extends AbstractLoopStrategy {

  @SuppressWarnings("unused")
  Integer maxUnrollingsStrategy = 0;

  public LoopUnrollingStrategy(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      int pMaxUnrollingsStrategy,
      StrategyDependencyInterface pStrategyDependencies,
      CFA pCFA) {
    super(pLogger, pShutdownNotifier, pStrategyDependencies, pCFA);
    maxUnrollingsStrategy = pMaxUnrollingsStrategy;
  }

  @Override
  public Optional<GhostCFA> summarize(CFANode pLoopStartNode) {
    return Optional.empty();
  }

  /*
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
    return Optional.of(
        new GhostCFA(
            startNodeGhostCFA,
            endNodeGhostCFA,
            pLoopStartNode,
            pLoopStartNode,
            StrategiesEnum.LoopUnrolling));
  }

  @Override
  public Optional<GhostCFA> summarize(final CFANode loopStartNode) {
    // TODO Unroll the Loop some amount of times. Can be improved by checking the maximal amount of
    // Loop iterations and unrolling only that amount of iterations.
    // Can be faster than the normal analysis, since it does not expect the Refinement in order to
    // unroll the loop
    // but may also be slower, since the loop unrolling has been done and must be transversed.
    // TODO, how can we see if we already applied loop unrolling in order to not apply it again once
    // the current unrolling has finished?

    if (loopStartNode.getNumLeavingEdges() != 1) {
      return Optional.empty();
    }

    if (!loopStartNode.getLeavingEdge(0).getDescription().equals("while")) {
      return Optional.empty();
    }

    CFANode loopStartNodeLocal = loopStartNode.getLeavingEdge(0).getSuccessor();

    Optional<Integer> loopBranchIndexOptional = getLoopBranchIndex(loopStartNodeLocal);
    Integer loopBranchIndex;

    if (loopBranchIndexOptional.isEmpty()) {
      return Optional.empty();
    } else {
      loopBranchIndex = loopBranchIndexOptional.orElseThrow();
    }

    GhostCFA ghostCFA;
    Optional<GhostCFA> ghostCFASuccess = summaryCFA(loopStartNodeLocal, loopBranchIndex);

    if (ghostCFASuccess.isEmpty()) {
      return Optional.empty();
    } else {
      ghostCFA = ghostCFASuccess.orElseThrow();
    }

    return Optional.of(ghostCFA);
  }

  */
}
