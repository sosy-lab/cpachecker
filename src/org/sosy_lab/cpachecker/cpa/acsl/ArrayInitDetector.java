// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import com.google.common.base.Splitter;
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
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * Detect the pattern of an array initialization that makes it possible to infer an ACSL invariant.
 */
class ArrayInitDetector implements AcslLoopInvariantDetector {

  private final LogManager logger;
  private final FormulaToAcslConverter formulaConverter;
  private final Level logLevel = Level.FINER; // TODO change to INFO for debugging
  private PredicateAbstractState predStateAtLoopEntry;

  ArrayInitDetector(LogManager pLogger, FormulaToAcslConverter pFormulaConverter) {
    this.logger = pLogger;
    this.formulaConverter = pFormulaConverter;
  }

  @Override
  public Optional<ArrayInitialization> detect(
      CFAEdge edge, Optional<LoopStructure> loopStruct, PredicateAbstractState predState) {

    LoopStructure.Loop loop = loopStruct.orElseThrow().getAllLoops().asList().getFirst();

    if (loop != null && loop.getIncomingEdges().contains(edge)) {
      predStateAtLoopEntry = predState;
    }

    if (!edge.getSuccessor().isLoopStart()) {
      return Optional.empty();
    }

    // Now we know our successor is a loop start node
    // TODO use loop structure to determine loop condition if present
    CFANode loopHead = edge.getSuccessor();
    Optional<CExpression> loopCondition = Optional.empty();
    for (CFAEdge e : loopHead.getAllLeavingEdges()) {
      if (e instanceof CAssumeEdge assumeEdge && assumeEdge.getTruthAssumption()) {
        loopCondition = Optional.of(assumeEdge.getExpression());
        logger.log(logLevel, "Loop head condition: " + loopCondition.orElseThrow());
      }
    }

    if (loopCondition.isEmpty()) {
      return Optional.empty();
    }

    BooleanFormula formula = predState.getPathFormula().getFormula();

    logger.log(logLevel, "Path formula leading to loop head: " + formula);

    AcslPredicate acsl;
    try {
      acsl = formulaConverter.formulaToAcslExpression(formula);
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
        if (!predicateMatchesLoopCondition(p, loopCondition.orElseThrow())) {
          return Optional.empty(); // we found a branching we do not want, abort
        } else {
          loopCondPredicate = Optional.of(p);
        }
      }
    }

    if (store.isEmpty() || update.isEmpty() || loopCondPredicate.isEmpty()) {
      return Optional.empty();
    } else {
      if (!indexMatchesEveryWhere(store.orElseThrow(), update.orElseThrow())) {
        return Optional.empty();
      }

      if (store.orElseThrow().value().equals(update.orElseThrow().var())) {
        // the loop variable changes its value in the loop body, we cannot handle it as
        // initialization value safely for now
        return Optional.empty();
      }

      // try to extract the bounds of the initialization we found
      AcslPredicate condition = loopCondPredicate.orElseThrow();
      AcslIdTerm index = store.orElseThrow().index();
      AcslTerm upperBound;
      boolean upperBoundInclusive;
      Optional<AcslTerm> exclusiveUpper = extractUpperBoundIfMatchingLTPattern(condition, index);
      if (exclusiveUpper.isPresent()) {
        upperBound = exclusiveUpper.orElseThrow();
        upperBoundInclusive = false;
      } else {
        Optional<AcslTerm> inclusiveUpper = extractUpperBoundIfMatchingLEPattern(condition, index);
        if (inclusiveUpper.isPresent()) {
          upperBound = inclusiveUpper.orElseThrow();
          upperBoundInclusive = true;
        } else {
          return Optional.empty();
        }
      }

      Optional<AcslTerm> extractedLower = extractLowerBoundFromLoopEntryPred(index);
      if (extractedLower.isEmpty()) {
        logger.log(
            Level.WARNING,
            "Found Array Initialization, but could not extract lower bound from loop entry"
                + " predicate.");
        return Optional.empty();
      } else {

        return Optional.of(
            new ArrayInitialization(
                loopHead,
                store.orElseThrow().array(),
                index,
                extractedLower.orElseThrow(),
                upperBound,
                upperBoundInclusive,
                store.orElseThrow().value()));
      }
    }
  }

  @Override
  public Class<? extends AcslInvariantProvider> getInvariantClass() {
    return ArrayInitialization.class;
  }

  private boolean indexMatchesEveryWhere(ArrayStore pArrayStore, ScalarUpdate pScalarUpdate) {
    if (!isSameVariable(pArrayStore.index(), pScalarUpdate.var())) {
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
        && isSameVariable(binary.getOperand1(), index)) {
      return Optional.of(binary.getOperand2());
    }

    return Optional.empty();
  }

  private Optional<AcslTerm> extractUpperBoundIfMatchingLEPattern(
      AcslPredicate predicate, AcslIdTerm index) {

    if (predicate instanceof AcslBinaryTermPredicate binary
        && binary.getOperator() == AcslBinaryTermExpressionOperator.LESS_EQUAL
        && isSameVariable(binary.getOperand1(), index)) {
      return Optional.of(binary.getOperand2());
    }

    return Optional.empty();
  }

  private boolean predicateMatchesLoopCondition(AcslPredicate p, CExpression loopCondition) {
    // TODO: I know these checks on a String level are ugly but there is no translation between
    // CExpressions and Acsl that I know of
    if (p instanceof AcslBinaryTermPredicate acslP
        && loopCondition instanceof CBinaryExpression cBinaryEx) {

      AcslRemoveSsaIndexVisitor remover = new AcslRemoveSsaIndexVisitor();
      String o1 = acslP.getOperand1().accept(remover).toASTString();
      String o2 = acslP.getOperand2().accept(remover).toASTString();

      if (!(o1.equals(cBinaryEx.getOperand1().toASTString())
          && o2.equals(cBinaryEx.getOperand2().toASTString()))) {
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
    if ((rhs instanceof AcslLiteralTerm || rhs instanceof AcslIdTerm)
        && lhs instanceof AcslArraySubscriptTerm arraySubscriptTerm) {
      if (arraySubscriptTerm.getSubscriptExpression() instanceof AcslIdTerm idt) {
        return Optional.of(new ArrayStore(arraySubscriptTerm.getArrayExpression(), idt, rhs));
      }
    }
    return Optional.empty();
  }

  private Optional<ScalarUpdate> extractIncrement(AcslTerm lhs, AcslTerm rhs) {
    if (lhs instanceof AcslIdTerm idt) {
      if (rhs instanceof AcslBinaryTerm bt && bt.getOperator() == AcslBinaryTermOperator.PLUS) {
        if ((bt.getOperand1().equals(AcslIntegerLiteralTerm.ONE)
                && isSameVariable(bt.getOperand2(), idt))
            || (bt.getOperand2().equals(AcslIntegerLiteralTerm.ONE)
                && isSameVariable(bt.getOperand1(), idt))) {
          return Optional.of(new ScalarUpdate(idt, rhs));
        }
      }
    }
    return Optional.empty();
  }

  private Boolean isSameVariable(AcslTerm t1, AcslTerm t2) {
    if (t1 instanceof AcslIdTerm idt1 && t2 instanceof AcslIdTerm idt2) {
      String nameBeforeSSA1 = Splitter.on("@").splitToList(idt1.getName()).getFirst();
      String nameBeforeSSA2 = Splitter.on("@").splitToList(idt2.getName()).getFirst();
      return idt1.getExpressionType().equals(idt2.getExpressionType())
          && nameBeforeSSA1.equals(nameBeforeSSA2);
    }
    return false;
  }

  private Optional<AcslTerm> extractLowerBoundFromLoopEntryPred(AcslIdTerm index) {
    if (predStateAtLoopEntry == null) {
      return Optional.empty();
    }

    BooleanFormula formula = predStateAtLoopEntry.getPathFormula().getFormula();
    AcslPredicate acsl;
    try {
      acsl = formulaConverter.formulaToAcslExpression(formula);
      logger.log(logLevel, "Formula in ACSL: " + acsl);
    } catch (Exception e) {
      logger.log(Level.WARNING, "Error converting formula to ACSL: " + e.getMessage());
      return Optional.empty();
    }

    ImmutableList<AcslPredicate> conjunction = formulaConverter.flattenAnds(acsl);

    for (AcslPredicate p : conjunction) {
      if (p instanceof AcslBinaryTermPredicate pred
          && pred.getOperator() == AcslBinaryTermExpressionOperator.EQUALS
          && isSameVariable(pred.getOperand1(), index)) {
        return Optional.of(pred.getOperand2());
      }
    }

    return Optional.empty();
  }
}
