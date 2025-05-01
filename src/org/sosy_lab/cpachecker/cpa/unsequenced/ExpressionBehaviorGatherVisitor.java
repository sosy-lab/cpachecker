// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.unsequenced;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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

public class ExpressionBehaviorGatherVisitor
    extends DefaultCExpressionVisitor<ExpressionAnalysisSummary, UnrecognizedCodeException>
    implements CRightHandSideVisitor<ExpressionAnalysisSummary, UnrecognizedCodeException> {

  private final UnseqBehaviorAnalysisState state;
  private final CFAEdge cfaEdge;
  private final AccessType accessType;
  private final LogManager logger;

  public ExpressionBehaviorGatherVisitor(
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
    ExpressionAnalysisSummary result = ExpressionAnalysisSummary.empty();
    result.setOriginalExpressionStr(exp.toQualifiedASTString());
    return result;
  }

  @Override
  public ExpressionAnalysisSummary visit(CIdExpression idExpr) throws UnrecognizedCodeException {
    String varName = idExpr.getDeclaration().getQualifiedName();

    // 1. Check if this is a TMP variable mapped to the original expression
    CRightHandSide originalExpr = state.getFunctionForTmp(varName);
    if (originalExpr != null) {
      ExpressionAnalysisSummary resolvedSummary = originalExpr.accept(this);
      return resolvedSummary;
    }

    // 2. Handel side effect
    ExpressionAnalysisSummary result = ExpressionAnalysisSummary.empty();

    if (idExpr.getDeclaration() instanceof CVariableDeclaration decl) {
      MemoryLocation loc = MemoryLocation.fromQualifiedName(decl.getQualifiedName());
      if (!loc.isOnFunctionStack()) {
        result.addSideEffect(new SideEffectInfo(loc, accessType, cfaEdge));
        logger.log(
            Level.INFO,
            String.format(
                "[GlobalAccess] %s on %s at %s", accessType, loc, cfaEdge.getFileLocation()));
      }
    }

    result.setOriginalExpressionStr(idExpr.toQualifiedASTString());
    return result;
  }

  @Override
  public ExpressionAnalysisSummary visit(CFunctionCallExpression funCallExpr)
      throws UnrecognizedCodeException {
    ExpressionAnalysisSummary result = ExpressionAnalysisSummary.empty();
    Set<SideEffectInfo> sideEffects = new HashSet<>();
    Map<String, Set<SideEffectInfo>> sideEffectsPerSubExpr = new HashMap<>();
    StringBuilder reconstructedCall = new StringBuilder();

    CExpression funcExpr = funCallExpr.getFunctionNameExpression();

    // Gather side effects for each function parameter
    List<String> reconstructedArguments = new ArrayList<>();
    for (CExpression param : funCallExpr.getParameterExpressions()) {
      ExpressionAnalysisSummary paramSummary = param.accept(this);
      Set<SideEffectInfo> paramEffects = paramSummary.getSideEffects();

      String exprStr = getExpressionStrOrFallback(paramSummary, param);
      sideEffectsPerSubExpr.put(exprStr, paramEffects);
      reconstructedArguments.add(exprStr);
    }

    if (funcExpr instanceof CIdExpression idExpr) { // side effects inside function body
      String functionName = idExpr.getName();
      if (state.getSideEffectsInFun().containsKey(functionName)) {
        sideEffects.addAll(state.getSideEffectsInFun().get(functionName));
      }

      reconstructedCall.append(functionName);
      reconstructedCall.append("(");
      reconstructedCall.append(String.join(", ", reconstructedArguments));
      reconstructedCall.append(")");
    }

    result.addSideEffects(sideEffects);
    result.setOriginalExpressionStr(reconstructedCall.toString());
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

    String newExpr = reconstructBinaryExpr(binaryExpr, leftSummary, rightSummary);
    result.setOriginalExpressionStr(newExpr);

    // Check if current binary operator itself is unsequenced
    if (isUnsequencedBinaryOperator(binaryExpr.getOperator())) {
      result.addUnsequencedBinaryExpr(binaryExpr);

      logger.log(
          Level.INFO,
          String.format(
              "Detected unsequenced binary expression '%s' at %s",
              result.getOriginalExpressionStr(), binaryExpr.getFileLocation()));
    }

    return result;
  }

  @Override
  public ExpressionAnalysisSummary visit(CUnaryExpression unaryExpr)
      throws UnrecognizedCodeException {
    return switch (unaryExpr.getOperator()) {
      case SIZEOF, ALIGNOF -> ExpressionAnalysisSummary.empty();
      case MINUS, TILDE, AMPER -> unaryExpr.getOperand().accept(this);
      default -> throw new UnrecognizedCodeException("Unknown unary operator", cfaEdge, unaryExpr);
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
    result.setOriginalExpressionStr(arrayExpr.toQualifiedASTString());

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
    result.setOriginalExpressionStr("*" + getExpressionStrOrFallback(operandSummary, operand));

    return result;
  }

  private boolean isUnsequencedBinaryOperator(CBinaryExpression.BinaryOperator op) {
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
      default ->
          throw new AssertionError("Unhandled operator in isUnsequencedBinaryOperator: " + op);
    };
  }

  private String reconstructBinaryExpr(
      CBinaryExpression binExpr,
      ExpressionAnalysisSummary leftSummary,
      ExpressionAnalysisSummary rightSummary) {

    String leftStr = getExpressionStrOrFallback(leftSummary, binExpr.getOperand1());
    String rightStr = getExpressionStrOrFallback(rightSummary, binExpr.getOperand2());
    return "(" + leftStr + binExpr.getOperator().getOperator() + rightStr + ")";
  }

  private String getExpressionStrOrFallback(
      ExpressionAnalysisSummary summary, CExpression fallbackExpr) {
    if (summary.getOriginalExpressionStr() != null) {
      return summary.getOriginalExpressionStr();
    }
    return fallbackExpr.toQualifiedASTString();
  }
}
