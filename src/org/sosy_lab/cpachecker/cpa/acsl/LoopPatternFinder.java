// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * Detect if a loop follows a certain pattern that makes it possible to infer an ACSL invariant. For
 * now the goal is to detect only one pattern: array initialization with a constant value.
 */
@SuppressWarnings("unused")
class LoopPatternFinder {

  private final LogManager logger;
  private final FormulaToAcslConverter formulaConverter;
  private final Level logLevel = Level.INFO; // TODO change to INFO for debugging

  LoopPatternFinder(LogManager pLogger, FormulaToAcslConverter pFormulaConverter) {
    this.logger = pLogger;
    this.formulaConverter = pFormulaConverter;
  }

  public Optional<ArrayInitialization> detect(CFAEdge edge, PredicateAbstractState predState) {
    if (edge.getSuccessor().isLoopStart()) {
      CFANode loopHead = edge.getSuccessor();
      for (CFAEdge e : loopHead.getAllLeavingEdges()) {
        if (e instanceof CAssumeEdge assumeEdge && assumeEdge.getTruthAssumption()) {
          CExpression condition = assumeEdge.getExpression();
          logger.log(logLevel, "Loop head condition: " + condition);
        }
      }
      BooleanFormula formula = predState.getPathFormula().getFormula();
      logger.log(logLevel, "Path formula leading to loop head: " + formula);
      try {
        AcslPredicate acsl = formulaConverter.formulaToAcslExpression(formula);
        // TODO hier mit dem fmg kucken, ob da deklarationen drin sind... wenn nur in deklaration ->
        // Skip
        logger.log(logLevel, "Formula in ACSL: " + acsl);
      } catch (Exception pE) {
        logger.log(Level.WARNING, "Error converting formula to ACSL: " + pE.getMessage());
      }
    }

    // TODO
    // Abstraction locations: Abstraction formula: true, Pathformula: was ich übersetze
    // 1. is this an edge leading to a loop head?
    // 2. is there an A[i] = constant
    // 3. is there a i2 = i + 1
    // 4. no branching and no other changes to the array

    return Optional.empty();
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
}
