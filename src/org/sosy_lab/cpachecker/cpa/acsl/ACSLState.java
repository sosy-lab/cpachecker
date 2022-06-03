// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.acsl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.acsl.ACSLAnnotation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.ACSLPredicate;
import org.sosy_lab.cpachecker.cfa.ast.acsl.ACSLPredicateToExpressionTreeVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTreeFactory;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;
import org.sosy_lab.cpachecker.util.expressions.Or;
import org.sosy_lab.cpachecker.util.expressions.ToCExpressionVisitor;

public class ACSLState implements AbstractStateWithAssumptions, ExpressionTreeReportingState {

  private final ImmutableSet<ACSLAnnotation> annotations;
  private final ACSLPredicateToExpressionTreeVisitor acslVisitor;
  private final ToCExpressionVisitor expressionTreeVisitor;
  private final LogManager logger;

  public ACSLState(
      Set<ACSLAnnotation> pAnnotations,
      ACSLPredicateToExpressionTreeVisitor pACSLVisitor,
      ToCExpressionVisitor pExpressionTreeVisitor,
      LogManager pLogManager) {
    annotations = ImmutableSet.copyOf(pAnnotations);
    acslVisitor = pACSLVisitor;
    expressionTreeVisitor = pExpressionTreeVisitor;
    logger = pLogManager;
  }

  @Override
  public ExpressionTree<Object> getFormulaApproximation(
      FunctionEntryNode pFunctionScope, CFANode pLocation) {
    return toExpressionTree();
  }

  private ExpressionTree<Object> toExpressionTree() {
    if (annotations.isEmpty()) {
      return ExpressionTrees.getTrue();
    }
    List<ExpressionTree<Object>> representations = new ArrayList<>(annotations.size());
    for (ACSLAnnotation annotation : annotations) {
      ACSLPredicate predicate = annotation.getPredicateRepresentation();
      try {
        representations.add(predicate.accept(acslVisitor));
      } catch (UnrecognizedCodeException e) {
        throw new AssertionError("Could not convert predicate to expression tree: " + predicate);
      }
    }
    ExpressionTreeFactory<Object> factory = ExpressionTrees.newFactory();
    return factory.and(representations);
  }

  public boolean hasAnnotations() {
    return !annotations.isEmpty();
  }

  @Override
  public int hashCode() {
    return Objects.hash(annotations);
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO instanceof ACSLState) {
      ACSLState that = (ACSLState) pO;
      return annotations.equals(that.annotations);
    }
    return false;
  }

  @Override
  public String toString() {
    return "ACSLState " + annotations;
  }

  @Override
  public List<? extends AExpression> getAssumptions() {
    try {
      @SuppressWarnings("unchecked")
      ExpressionTree<AExpression> exp =
          (ExpressionTree<AExpression>) ((ExpressionTree<?>) toExpressionTree());
      if (exp.equals(ExpressionTrees.getTrue())) {
        return ImmutableList.of();
      } else if (exp.equals(ExpressionTrees.getFalse())) {
        return ImmutableList.of(expressionTreeVisitor.visitFalse());
      } else if (exp instanceof LeafExpression) {
        return ImmutableList.of(expressionTreeVisitor.visit((LeafExpression<AExpression>) exp));
      } else if (exp instanceof And) {
        return ImmutableList.of(expressionTreeVisitor.visit((And<AExpression>) exp));
      } else if (exp instanceof Or) {
        return ImmutableList.of(expressionTreeVisitor.visit((Or<AExpression>) exp));
      } else {
        throw new AssertionError("Unknown type of ExpressionTree.");
      }
    } catch (UnrecognizedCodeException e) {
      logger.log(Level.WARNING, "Could not convert some annotations to assumption, ignoring");
      return ImmutableList.of();
    }
  }
}
