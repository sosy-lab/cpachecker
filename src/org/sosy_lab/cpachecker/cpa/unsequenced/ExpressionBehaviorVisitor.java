// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.unsequenced;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.unsequenced.SideEffectInfo.AccessType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class ExpressionBehaviorVisitor
    extends DefaultCExpressionVisitor<ExpressionAnalysisSummary, UnrecognizedCodeException>
    implements CRightHandSideVisitor<ExpressionAnalysisSummary, UnrecognizedCodeException> {

  private final UnseqBehaviorAnalysisState state;
  private final CFAEdge cfaEdge;
  private final AccessType accessType;
  private final LogManager logger;

  public ExpressionBehaviorVisitor(
      UnseqBehaviorAnalysisState pState,
      CFAEdge pEdge,
      AccessType pAccessType,
      LogManager pLogger) {
    state = pState;
    cfaEdge = pEdge;
    accessType = pAccessType;
    logger = pLogger;
  }

  @Override
  protected ExpressionAnalysisSummary visitDefault(CExpression exp)
      throws UnrecognizedCodeException {
    return ExpressionAnalysisSummary.empty();
  }

  @Override
  public ExpressionAnalysisSummary visit(CIdExpression idExpr) throws UnrecognizedCodeException {
    String varName = idExpr.getDeclaration().getQualifiedName();
    ExpressionAnalysisSummary result = ExpressionAnalysisSummary.empty();

    // 1. Check if this is a TMP variable mapped to the original expression
    CRightHandSide originalExpr = state.getFunctionForTmp(varName);
    if (originalExpr != null) {
      return originalExpr.accept(this);
    }

    // 2. Handel side effect
    if (idExpr.getDeclaration() instanceof CVariableDeclaration decl) {
      MemoryLocation loc = MemoryLocation.fromQualifiedName(decl.getQualifiedName());
      if (!loc.isOnFunctionStack()) {
        result.addSideEffect(new SideEffectInfo(loc, accessType, cfaEdge));
        logger.logf(
            Level.INFO,
            "[GlobalAccess] %s on %s at %s",
            accessType,
            loc,
            cfaEdge.getFileLocation());
      }
    }

    return result;
  }

  @Override
  public ExpressionAnalysisSummary visit(CFunctionCallExpression funCallExpr)
      throws UnrecognizedCodeException {
    ExpressionAnalysisSummary result = ExpressionAnalysisSummary.empty();
    Set<SideEffectInfo> sideEffects = new HashSet<>();
    Map<CRightHandSide, Set<SideEffectInfo>> sideEffectsPerSubExpr = new HashMap<>();

    CExpression funcExpr = funCallExpr.getFunctionNameExpression();

    // Gather side effects for each function parameter
    for (CRightHandSide param : funCallExpr.getParameterExpressions()) {
      ExpressionAnalysisSummary paramSummary = param.accept(this);
      Set<SideEffectInfo> paramEffects = paramSummary.getSideEffects();

      sideEffectsPerSubExpr.put(param, paramEffects);
    }

    if (funcExpr instanceof CIdExpression idExpr) { // side effects inside function body
      String functionName = idExpr.getName();

      // Add callee's side effects
      if (state.getSideEffectsInFun().containsKey(functionName)) {
        sideEffects.addAll(state.getSideEffectsInFun().get(functionName));
      }
    }

    result.addSideEffects(sideEffects);
    result.addSideEffectsForSubExprs(sideEffectsPerSubExpr);
    return result;
  }

  @Override
  public ExpressionAnalysisSummary visit(CBinaryExpression binaryExpr)
      throws UnrecognizedCodeException {

    ExpressionAnalysisSummary result = ExpressionAnalysisSummary.empty();
    ExpressionAnalysisSummary leftSummary = binaryExpr.getOperand1().accept(this);
    ExpressionAnalysisSummary rightSummary = binaryExpr.getOperand2().accept(this);

    result.addSideEffects(leftSummary.getSideEffects());
    result.addSideEffects(rightSummary.getSideEffects());
    result.addUnsequencedBinaryExprs(leftSummary.getUnsequencedBinaryExprs());
    result.addUnsequencedBinaryExprs(rightSummary.getUnsequencedBinaryExprs());

    // Check if current binary operator itself is unsequenced
    if (isUnsequencedBinaryOperator(binaryExpr.getOperator())) {
      result.addUnsequencedBinaryExpr(binaryExpr);

      logger.logf(
          Level.INFO,
          "Detected unsequenced binary expression '%s' at %s",
          UnseqUtils.replaceTmpInExpression(binaryExpr, state),
          binaryExpr.getFileLocation());
    }

    return result;
  }

  @Override
  public ExpressionAnalysisSummary visit(CUnaryExpression unaryExpr)
      throws UnrecognizedCodeException {
    return switch (unaryExpr.getOperator()) {
      // According to the C11 standard, there are two relevant definitions regarding the
      // evaluation of operands within the `sizeof` operator:
      // C11: 6.5.3.4
      // If the type of the operand is a variable length array type, the operand is evaluated;
      // otherwise, the operand is not evaluated and the result is an integer constant.
      // C11: 6.7.6.2.
      // Whether or not a size expression is evaluated when it is part of the operand of sizeof
      // operator
      // and changing the value of the size expression would not affect the result of the operator
      // See: CPAchecker issue #1234
      // In this analysis, we conservatively ignore `sizeof` expressions and log their presence
      // only.
      case SIZEOF -> {
        logger.logf(
            Level.WARNING,
            "Encountered `sizeof` expression at %s, ignoring operand for side-effect analysis.",
            unaryExpr.getFileLocation());
        yield ExpressionAnalysisSummary.empty();
      }
      // C11: 6.5.3.4
      // The alignof operator yields the alignment requirement of its operand type.
      case ALIGNOF -> ExpressionAnalysisSummary.empty();
      case MINUS, TILDE, AMPER -> unaryExpr.getOperand().accept(this);
    };
  }

  @Override
  public ExpressionAnalysisSummary visit(CArraySubscriptExpression arrayExpr)
      throws UnrecognizedCodeException {
    ExpressionAnalysisSummary result = ExpressionAnalysisSummary.empty();
    Set<SideEffectInfo> sideEffects = new HashSet<>();

    ExpressionAnalysisSummary arrayBaseSummary = arrayExpr.getArrayExpression().accept(this);
    ExpressionAnalysisSummary indexSummary = arrayExpr.getSubscriptExpression().accept(this);
    sideEffects.addAll(arrayBaseSummary.getSideEffects());
    sideEffects.addAll(indexSummary.getSideEffects());

    result.addSideEffects(sideEffects);

    return result;
  }

  @Override
  public ExpressionAnalysisSummary visit(CFieldReference fieldRef)
      throws UnrecognizedCodeException {
    return fieldRef.getFieldOwner().accept(this);
  }

  @Override
  public ExpressionAnalysisSummary visit(CCastExpression castExpr)
      throws UnrecognizedCodeException {
    return castExpr.getOperand().accept(this);
  }

  @Override
  public ExpressionAnalysisSummary visit(CComplexCastExpression complexCastExpr)
      throws UnrecognizedCodeException {
    return complexCastExpr.getOperand().accept(this);
  }

  @Override
  public ExpressionAnalysisSummary visit(CPointerExpression pointerExpr)
      throws UnrecognizedCodeException {
    ExpressionAnalysisSummary result = ExpressionAnalysisSummary.empty();

    CExpression operand = pointerExpr.getOperand();
    ExpressionAnalysisSummary operandSummary = operand.accept(this);

    result.addSideEffects(operandSummary.getSideEffects());

    return result;
  }

  private boolean isUnsequencedBinaryOperator(CBinaryExpression.BinaryOperator op) {
    // C11: J.1
    // The order in which subexpressions are evaluated and the order in which side effects take
    // place,
    // except as specified for the function-call (), &&, ||, ?:, and comma operators (6.5).
    return switch (op) {
      case BINARY_AND, BINARY_OR -> false;
      case MULTIPLY,
          DIVIDE,
          MODULO,
          PLUS,
          MINUS,
          SHIFT_LEFT,
          SHIFT_RIGHT,
          BINARY_XOR,
          LESS_EQUAL,
          LESS_THAN,
          GREATER_EQUAL,
          GREATER_THAN,
          EQUALS,
          NOT_EQUALS ->
          true;
    };
  }
}
