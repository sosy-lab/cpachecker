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
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
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
  private final Level logLevel = Level.FINER; // TODO change this later on

  public AcslTransferRelation(CFA pCFA, LogManager pLogManager) {
    this.cfa = pCFA;
    this.logger = pLogManager;
    this.loopPatternFinder = new LoopPatternFinder(logger);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    // TODO
    if (cfaEdge.getEdgeType() == CFAEdgeType.StatementEdge) {
      Optional<ArrayStore> store = extractArrayStore(cfaEdge);
      store.ifPresent(
          pArrayStore -> logger.log(logLevel, "[ACSL] Array store detected: " + pArrayStore));
      Optional<ScalarUpdate> update = extractScalarUpdate(cfaEdge);
      update.ifPresent(
          pScalarUpdate -> logger.log(logLevel, "[ACSL] Scalar update detected: " + pScalarUpdate));
    }
    if (cfaEdge.getSuccessor().isLoopStart()) {
      Optional<ArrayInitialization> initialization = loopPatternFinder.detect(cfaEdge.getSuccessor());
      initialization.ifPresent(
          pInitialization ->
              logger.log(logLevel, "[ACSL] Loop initialization detected: " + pInitialization));
    }
    logger.log(
        logLevel, "[ACSL] Transfer: " + cfaEdge.getPredecessor() + " -> " + cfaEdge.getSuccessor());
    return ImmutableList.of(state);
  }

  private Optional<ArrayStore> extractArrayStore(CFAEdge pCfaEdge) {
    if (pCfaEdge instanceof CStatementEdge statementEdge) {
      CStatement statement = statementEdge.getStatement();
      if (statement instanceof CAssignment assign) {
        if (assign.getLeftHandSide() instanceof CArraySubscriptExpression left) {
          return Optional.of(
              new ArrayStore(
                  left.getArrayExpression(),
                  left.getSubscriptExpression(),
                  assign.getRightHandSide()));
        }
      }
    }
    return Optional.empty();
  }

  private Optional<ScalarUpdate> extractScalarUpdate(CFAEdge pCfaEdge) {
    if (pCfaEdge instanceof CStatementEdge statementEdge) {
      CStatement statement = statementEdge.getStatement();
      if (statement instanceof CAssignment assign) {
        if (assign.getLeftHandSide() instanceof CIdExpression left) {
          return Optional.of(new ScalarUpdate(left, assign.getRightHandSide()));
        }
      }
    }
    return Optional.empty();
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
        // System.out.println(constraintsState.toString());
      }
      if (otherState instanceof ValueAnalysisState valueState) {
        // System.out.println(valueState.toString());
      }
    }

    // TODO
    return super.strengthen(state, otherStates, cfaEdge, precision);
  }
}
