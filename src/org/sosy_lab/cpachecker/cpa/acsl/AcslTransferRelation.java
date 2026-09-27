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
import java.util.Optional;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

@SuppressWarnings("unused")
public class AcslTransferRelation extends SingleEdgeTransferRelation {

  private final CFA cfa;
  private final LogManager logger;
  private final LoopPatternFinder loopPatternFinder;
  private final Level logLevel = Level.FINER; // TODO change this to INFO while debugging

  public AcslTransferRelation(CFA pCFA, LogManager pLogManager) {
    this.cfa = pCFA;
    this.logger = pLogManager;
    this.loopPatternFinder = new LoopPatternFinder(logger);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {

    logger.log(
        logLevel, "[ACSL] Transfer: " + cfaEdge.getPredecessor() + " -> " + cfaEdge.getSuccessor());

    if (cfaEdge.getSuccessor().isLoopStart()) {
      Optional<ArrayInitialization> initialization =
          loopPatternFinder.detect(cfaEdge.getSuccessor());
      initialization.ifPresent(
          pInitialization ->
              logger.log(logLevel, "[ACSL] Loop initialization detected: " + pInitialization));
    }

    // TODO process result from initialization:
    // if nonempty, create ACSL invariant from it or if not possible yet save something else in the
    // state
    // think about saving it with the loop head node and only run the code above if it is not yet
    // stored in the state to be more efficient

    return ImmutableList.of(state);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState state,
      Iterable<AbstractState> otherStates,
      @Nullable CFAEdge cfaEdge,
      Precision precision)
      throws CPATransferException, InterruptedException {

    for (AbstractState otherState : otherStates) {
      if (otherState instanceof ConstraintsState constraintsState) {
        // TODO this is where I think we can communicate with symbolic execution
        // logger.log(logLevel, constraintsState.toString());
      }
      if (otherState instanceof ValueAnalysisState valueState) {
        // logger.log(logLevel, valueState.toString());
      }
    }

    // TODO use the info

    return super.strengthen(state, otherStates, cfaEdge, precision);
  }
}
