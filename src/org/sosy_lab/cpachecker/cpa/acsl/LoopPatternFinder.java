// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslArraySubscriptTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTerm.AcslBinaryTermOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslBinaryTermPredicate.AcslBinaryTermExpressionOperator;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslIntegerLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLiteralTerm;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTerm;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * Detect if a loop follows a certain pattern that makes it possible to infer an ACSL invariant. For
 * now the goal is to detect only one pattern: array initialization with a constant value.
 */
@SuppressWarnings("unused")
class LoopPatternFinder {

  private final LogManager logger;
  private final FormulaToAcslConverter formulaConverter;
  private final Level logLevel = Level.FINER; // TODO change to INFO for debugging

  LoopPatternFinder(LogManager pLogger, FormulaToAcslConverter pFormulaConverter) {
    this.logger = pLogger;
    this.formulaConverter = pFormulaConverter;
  }

  public Optional<ArrayInitialization> detect(CFAEdge edge, PredicateAbstractState predState) {
    // System.out.println("Abstraction State:"+ predState.isAbstractionState() + ", Loop Start?" +
    // edge.getSuccessor().isLoopStart());
    // TODO why do I only get NonAbstraction states?

    if (!edge.getSuccessor().isLoopStart()) {
      return Optional.empty();
    }

    // Now we know our successor is a loop start node
    CFANode loopHead = edge.getSuccessor();
    Optional<CExpression> loopCondition = Optional.empty();
    for (CFAEdge e : loopHead.getAllLeavingEdges()) {
      if (e instanceof CAssumeEdge assumeEdge && assumeEdge.getTruthAssumption()) {
        loopCondition = Optional.of(assumeEdge.getExpression());
        logger.log(logLevel, "Loop head condition: " + loopCondition.get());
      }
    }

    if (loopCondition.isEmpty()) {
      return Optional.empty();
    }

    BooleanFormula formula = predState.getPathFormula().getFormula();
    SSAMap ssa = predState.getPathFormula().getSsa();

    logger.log(logLevel, "Path formula leading to loop head: " + formula);

    AcslPredicate acsl;
    try {
      acsl = formulaConverter.formulaToAcslExpression(formula, ssa);
      logger.log(logLevel, "Formula in ACSL: " + acsl);
    } catch (Exception e) {
      logger.log(Level.WARNING, "Error converting formula to ACSL: " + e.getMessage());
      return Optional.empty();
    }

    ImmutableList<AcslPredicate> conjunction = formulaConverter.flattenAnds(acsl);
    Optional<ArrayStore> store = Optional.empty();
    Optional<ScalarUpdate> update = Optional.empty();
    Optional<AcslPredicate> loopCondPredicate = Optional.empty();

    for (AcslPredicate p : conjunction) {

      if (p instanceof AcslBinaryTermPredicate pred
          && pred.getOperator() == AcslBinaryTermExpressionOperator.EQUALS) {
        AcslTerm op1 = pred.getOperand1();
        AcslTerm op2 = pred.getOperand2();

        // Case 1: try to find an array store
        Optional<ArrayStore> candidate = extractArrayStore(op1, op2);
        if (candidate.isEmpty()) {
          candidate = extractArrayStore(op2, op1); // TODO needed or not?
        }
        if (candidate.isPresent()) {
          if (store.isPresent()) {
            return Optional.empty(); // we cannot deal with multiple stores for now
          } else {
            store = candidate;
            continue;
          }
        }

        // Case 2: we try to find an incrementing scalar
        Optional<ScalarUpdate> increment = extractIncrement(op1, op2);
        if (increment.isEmpty()) {
          increment = extractIncrement(op2, op1); // TODO needed or not?
        }
        if (increment.isPresent()) {
          if (update.isPresent()) {
            return Optional.empty(); // we cannot deal with multiple updates for now, no matter what
            // variable, fix that later
          } else {
            update = increment;
          }
        }
      } else if (hasRelationalOp(p)) {
        if (!predicateMatchesLoopCondition(p, loopCondition.get())) {
          return Optional.empty(); // we found a branching we do not want, abort
        } else {
          loopCondPredicate = Optional.of(p);
        }
      }
    }

    if (store.isEmpty() || update.isEmpty() || loopCondPredicate.isEmpty()) {
      return Optional.empty();
    } else {
      if (!indexMatchesEveryWhere(store.get(), update.get(), loopCondPredicate.get())) {
        return Optional.empty();
      }

      // try to extract the bounds of the initialization we found
      AcslPredicate condition = loopCondPredicate.get();
      AcslIdTerm index = store.get().index();
      AcslTerm upperBound;
      boolean upperBoundInclusive;
      Optional<AcslTerm> exclusiveUpper = extractUpperBoundIfMatchingLTPattern(condition, index);
      if (exclusiveUpper.isPresent()) {
        upperBound = exclusiveUpper.get();
        upperBoundInclusive = false;
      } else {
        Optional<AcslTerm> inclusiveUpper = extractUpperBoundIfMatchingLEPattern(condition, index);
        if (inclusiveUpper.isPresent()) {
          upperBound = inclusiveUpper.get();
          upperBoundInclusive = true;
        } else {
          return Optional.empty();
        }
      }
      AcslTerm lowerBound = AcslIntegerLiteralTerm.ZERO; // TODO do not hard code this!

      return Optional.of(
          new ArrayInitialization(
              loopHead,
              store.get().array(),
              store.get().index(),
              lowerBound,
              upperBound,
              upperBoundInclusive,
              store.get().value()));
    }
  }

  private boolean indexMatchesEveryWhere(
      ArrayStore pArrayStore, ScalarUpdate pScalarUpdate, AcslPredicate pAcslPredicate) {
    if (!pArrayStore.index().equals(pScalarUpdate.var())) {
      return false;
    }
    return true;
  }

  private boolean hasRelationalOp(AcslPredicate p) {
    if (p instanceof AcslBinaryTermPredicate binaryTerm) {
      AcslBinaryTermExpressionOperator op = binaryTerm.getOperator();
      return op == AcslBinaryTermExpressionOperator.GREATER_EQUAL
          || op == AcslBinaryTermExpressionOperator.GREATER_THAN
          || op == AcslBinaryTermExpressionOperator.LESS_EQUAL
          || op == AcslBinaryTermExpressionOperator.LESS_THAN;
    }
    return false;
  }

  private Optional<AcslTerm> extractUpperBoundIfMatchingLTPattern(
      AcslPredicate predicate, AcslIdTerm index) {

    if (predicate instanceof AcslBinaryTermPredicate binary
        && binary.getOperator() == AcslBinaryTermExpressionOperator.LESS_THAN
        && binary.getOperand1().equals(index)) {
      return Optional.of(binary.getOperand2());
    }

    return Optional.empty();
  }

  private Optional<AcslTerm> extractUpperBoundIfMatchingLEPattern(
      AcslPredicate predicate, AcslIdTerm index) {

    if (predicate instanceof AcslBinaryTermPredicate binary
        && binary.getOperator() == AcslBinaryTermExpressionOperator.LESS_EQUAL
        && binary.getOperand1().equals(index)) {
      return Optional.of(binary.getOperand2());
    }

    return Optional.empty();
  }

  private boolean predicateMatchesLoopCondition(AcslPredicate p, CExpression loopCondition) {
    // TODO: I know these checks on a String level are ugly but there is no translation between
    // CExpressions and Acsl that I know of and I cannot be bothered to write that, too until I have
    // a proof of concept that works
    if (p instanceof AcslBinaryTermPredicate acslP
        && loopCondition instanceof CBinaryExpression cBinaryEx) {

      if (!(acslP.getOperand1().toString().equals(cBinaryEx.getOperand1().toString())
          && acslP.getOperand2().toString().equals(cBinaryEx.getOperand2().toString()))) {
        return false; // Operands do not match
      }

      return acslP
          .getOperator()
          .getOperator()
          .equals(cBinaryEx.getOperator().getOperator()); // Operators have to match, too
    }
    return false;
  }

  private Optional<ArrayStore> extractArrayStore(AcslTerm lhs, AcslTerm rhs) {
    if (rhs instanceof AcslLiteralTerm
        && lhs instanceof AcslArraySubscriptTerm arraySubstriptTerm) {
      if (arraySubstriptTerm.getSubscriptExpression() instanceof AcslIdTerm idt) {
        return Optional.of(new ArrayStore(arraySubstriptTerm.getArrayExpression(), idt, rhs));
      }
    }
    return Optional.empty();
  }

  private Optional<ScalarUpdate> extractIncrement(AcslTerm lhs, AcslTerm rhs) {
    if (lhs instanceof AcslIdTerm idt) {
      if (rhs instanceof AcslBinaryTerm bt && bt.getOperator() == AcslBinaryTermOperator.PLUS) {
        if ((bt.getOperand1().equals(AcslIntegerLiteralTerm.ONE) && bt.getOperand2().equals(idt))
            || (bt.getOperand2().equals(AcslIntegerLiteralTerm.ONE)
                && bt.getOperand1().equals(idt))) {
          return Optional.of(new ScalarUpdate(idt, rhs));
        }
      }
    }
    return Optional.empty();
  }
}
