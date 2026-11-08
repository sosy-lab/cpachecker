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
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTreeFactory;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.expressions.LeafExpression;
import org.sosy_lab.cpachecker.util.expressions.Or;
import org.sosy_lab.cpachecker.util.expressions.ToCExpressionVisitor;

public class ACSLState implements AbstractStateWithAssumptions, ExpressionTreeReportingState {

  private final ImmutableSet<AcslPredicate> annotations;
  private final AcslToExpressionTreeConverter converter;
  private final ToCExpressionVisitor expressionTreeVisitor;
  private final LogManager logger;

  ACSLState(
      Set<AcslPredicate> pAnnotations,
      AcslToExpressionTreeConverter pConverter,
      ToCExpressionVisitor pExpressionTreeVisitor,
      LogManager pLogManager) {
    annotations = ImmutableSet.copyOf(pAnnotations);
    converter = pConverter;
    expressionTreeVisitor = pExpressionTreeVisitor;
    logger = pLogManager;
  }

  @Override
  public ExpressionTree<Object> getFormulaApproximationAllVariablesInFunctionScope(
      FunctionEntryNode pFunctionScope, CFANode pLocation, MachineModel pMachineModel)
      throws TranslationToExpressionTreeFailedException {
    try {
      return toExpressionTree();
    } catch (UnrecognizedCodeException e) {
      throw new TranslationToExpressionTreeFailedException(e.getMessage());
    }
  }

  private ExpressionTree<Object> toExpressionTree() throws UnrecognizedCodeException {
    if (annotations.isEmpty()) {
      return ExpressionTrees.getTrue();
    }
    List<ExpressionTree<Object>> representations = new ArrayList<>(annotations.size());
    for (AcslPredicate predicate : annotations) {
      representations.add(converter.convert(predicate));
    }
    ExpressionTreeFactory<Object> factory = ExpressionTrees.newFactory();
    return factory.and(representations);
  }

  @Override
  public ExpressionTree<Object> getFormulaApproximationInputProgramInScopeVariables(
      FunctionEntryNode pFunctionScope,
      CFANode pLocation,
      AstCfaRelation pAstCfaRelation,
      boolean useOldKeywordForVariables,
      MachineModel pMachineModel)
      throws InterruptedException,
          ReportingMethodNotImplementedException,
          TranslationToExpressionTreeFailedException {
    throw new ReportingMethodNotImplementedException(
        "The method 'getFormulaApproximationInputProgramInScopeVariable' is not implemented for"
            + " 'ACSLState'");
  }

  @Override
  public ExpressionTree<Object> getFormulaApproximationFunctionReturnVariableOnly(
      FunctionEntryNode pFunctionScope,
      AIdExpression pFunctionReturnVariable,
      MachineModel pMachineModel)
      throws InterruptedException,
          ReportingMethodNotImplementedException,
          TranslationToExpressionTreeFailedException {
    throw new ReportingMethodNotImplementedException(
        "The method 'getFormulaApproximationFunctionReturnVariableOnly' is not implemented for"
            + " 'ACSLState'");
  }

  public boolean hasAnnotations() {
    return !annotations.isEmpty();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(annotations);
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    return pO instanceof ACSLState that && annotations.equals(that.annotations);
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
