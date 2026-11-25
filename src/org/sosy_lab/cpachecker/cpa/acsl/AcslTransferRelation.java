// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

public class AcslTransferRelation extends SingleEdgeTransferRelation {

  private final CFA cfa;
  private final LogManager logger;
  private final AcslLoopInvariantDetectorManager detectorManager;
  private final boolean hasNestedLoop;
  private final Level logLevel = Level.FINER; // TODO change this to INFO while debugging

  public AcslTransferRelation(
      CFA pCFA,
      LogManager pLogManager,
      FormulaToAcslConverter pFormulaConverter,
      Configuration config)
      throws InvalidConfigurationException {
    this.cfa = pCFA;
    this.logger = pLogManager;
    this.detectorManager = new AcslLoopInvariantDetectorManager(logger, pFormulaConverter, config);
    if (cfa.getLoopStructure().isEmpty()) {
      this.logger.log(
          Level.WARNING,
          "Could not retrieve loop structure to determine if ACSL invariants can be generated.");
      this.hasNestedLoop = false;
    } else {
      this.hasNestedLoop = hasNestedLoops(cfa.getLoopStructure().orElseThrow());
    }
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {

    logger.log(
        logLevel, "[ACSL] Transfer: " + cfaEdge.getPredecessor() + " -> " + cfaEdge.getSuccessor());

    if (cfa.getAllLoopHeads().orElseThrow().contains(cfaEdge.getSuccessor())) {
      // TODO
    }
    return ImmutableList.of(state);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState state,
      Iterable<AbstractState> otherStates,
      @Nullable CFAEdge cfaEdge,
      Precision precision)
      throws CPATransferException, InterruptedException {

    if (hasNestedLoop) {
      logger.log(
          Level.INFO, "Generating ACSL Loop invariants with nested loops is not implemented.");
      return super.strengthen(state, otherStates, cfaEdge, precision);
    }

    AbstractState updatedState = state;
    for (AbstractState otherState : otherStates) {

      if (otherState instanceof PredicateAbstractState predicateState) {
        ImmutableList<AcslInvariantProvider> detected =
            detectorManager.detect(state, cfaEdge, cfa.getLoopStructure(), predicateState);

        for (AcslInvariantProvider invariant : detected) {
          logger.log(Level.INFO, "SUCCESS: Detected ACSL invariant: " + invariant);
          if (updatedState instanceof AcslState acslState) {
            updatedState = acslState.withAddedInvariant(invariant.loopHead(), invariant);
          }
        }
      }
      if (otherState instanceof ValueAnalysisState valueState) {
        logger.log(logLevel, valueState.toString());
      }
    }

    return super.strengthen(updatedState, otherStates, cfaEdge, precision);
  }

  private static boolean hasNestedLoops(LoopStructure loopStructure) {
    ImmutableList<Loop> loops = ImmutableList.copyOf(loopStructure.getAllLoops());

    for (int i = 0; i < loops.size(); i++) {
      for (int j = i + 1; j < loops.size(); j++) {
        Loop first = loops.get(i);
        Loop second = loops.get(j);

        if (first.isOuterLoopOf(second) || second.isOuterLoopOf(first)) {
          return true;
        }
      }
    }
    return false;
  }

  @SuppressWarnings("unused") // TODO delete when you are sure it is no longer needed
  private static ImmutableList<CFANode> getExitNodes(
      LoopStructure loopStructure, CFANode loopHead) {
    ImmutableList<Loop> loops = ImmutableList.copyOf(loopStructure.getLoopsForLoopHead(loopHead));
    ImmutableList.Builder<CFANode> exitNodes = ImmutableList.builder();

    for (Loop l : loops) {
      for (CFAEdge e : l.getOutgoingEdges()) {
        exitNodes.add(e.getSuccessor());
      }
    }

    return exitNodes.build();
  }
}
