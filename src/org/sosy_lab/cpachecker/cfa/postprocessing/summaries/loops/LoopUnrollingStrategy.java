// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries.loops;

import com.google.common.collect.FluentIterable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.GhostCFA;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategiesEnum;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependency;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.Pair;

public class LoopUnrollingStrategy extends LoopStrategy {

  private Integer maxUnrollingsStrategy;

  public LoopUnrollingStrategy(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      int pMaxUnrollingsStrategy,
      StrategyDependency pStrategyDependencies,
      CFA pCFA) {
    super(pLogger, pShutdownNotifier, pStrategyDependencies, StrategiesEnum.LOOPUNROLLING, pCFA);
    maxUnrollingsStrategy = pMaxUnrollingsStrategy;
  }

  protected Optional<GhostCFA> summarizeLoop(Loop pLoopStructure, CFANode pBeforeWhile) {

    CFANode startNodeGhostCFA = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());
    CFANode endNodeGhostCFA = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

    CFANode currentSummaryNodeCFA = startNodeGhostCFA;
    CFANode nextCFANode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());

    // Unroll the loop
    for (int i = 0; i < maxUnrollingsStrategy; i++) {

      Optional<Pair<CFANode, CFANode>> unrolledLoopNodesMaybe =
          pLoopStructure.unrollOutermostLoop();
      if (unrolledLoopNodesMaybe.isEmpty()) {
        return Optional.empty();
      }

      CFANode startUnrolledLoopNode = unrolledLoopNodesMaybe.orElseThrow().getFirst();
      CFANode endUnrolledLoopNode = unrolledLoopNodesMaybe.orElseThrow().getSecond();

      CFACreationUtils.connectNodes(currentSummaryNodeCFA, startUnrolledLoopNode);
      CFACreationUtils.connectNodes(endUnrolledLoopNode, nextCFANode);

      currentSummaryNodeCFA = nextCFANode;
      nextCFANode = CFANode.newDummyCFANode(pBeforeWhile.getFunctionName());
    }

    // Add the original loop after the unrolled loop
    Optional<Loop> maybeCopyOfLoop = pLoopStructure.deepCopy();
    if (maybeCopyOfLoop.isEmpty()) {
      return Optional.empty();
    }

    Loop copyOfLoop = maybeCopyOfLoop.get();
    if (copyOfLoop.getLoopHeads().size() != 1) {
      return Optional.empty();
    }

    CFANode copiedLoopHead = copyOfLoop.getLoopHeads().asList().get(0);
    CFACreationUtils.connectNodes(currentSummaryNodeCFA, copiedLoopHead);

    for (CFANode n :
        FluentIterable.from(copyOfLoop.getOutgoingEdges()).transform(e -> e.getSuccessor())) {
      CFACreationUtils.connectNodes(n, endNodeGhostCFA);
    }

    CFANode leavingSuccessor;
    Iterator<CFAEdge> iter = pLoopStructure.getOutgoingEdges().iterator();
    if (iter.hasNext()) {
      leavingSuccessor = iter.next().getSuccessor();
    } else {
      return Optional.empty();
    }

    for (CFAEdge e : pLoopStructure.getOutgoingEdges()) {
      if (e.getSuccessor().getNodeNumber() != leavingSuccessor.getNodeNumber()) {
        return Optional.empty();
      }
    }

    return Optional.of(
        new GhostCFA(
            startNodeGhostCFA,
            endNodeGhostCFA,
            pBeforeWhile,
            leavingSuccessor,
            this.strategyEnum,
            new ArrayList<>(),
            StrategyQualifier.Precise));
  }

  @Override
  public Optional<GhostCFA> summarize(final CFANode beforeWhile) {
    Optional<CFANode> maybeLoopHead = this.determineLoopHead(beforeWhile);
    if (maybeLoopHead.isEmpty()) {
      return Optional.empty();
    }
    CFANode loopStartNode = maybeLoopHead.orElseThrow();

    Optional<Loop> loopMaybe = summaryInformation.getLoop(loopStartNode);
    if (loopMaybe.isEmpty()) {
      return Optional.empty();
    }
    Loop loop = loopMaybe.orElseThrow();

    return summarizeLoop(loop, beforeWhile);
  }
}