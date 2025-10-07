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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
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
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.unsequenced.SideEffectInfo.AccessType;
import org.sosy_lab.cpachecker.cpa.unsequenced.SideEffectInfo.SideEffectKind;
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
    Set<SideEffectInfo> sideEffects = new HashSet<>();

    // 1. Check if this is a TMP variable mapped to the original expression
    Optional<CRightHandSide> originalExpr = state.getFunctionForTmp(varName);
    if (originalExpr.isPresent()) {
      return originalExpr.orElseThrow().accept(this);
    }

    // 2. Handel side effect
    if (idExpr.getDeclaration() instanceof CVariableDeclaration decl) {
      MemoryLocation loc = MemoryLocation.fromQualifiedName(decl.getQualifiedName());
      // global variable
      if (decl.isGlobal()) {
        SideEffectInfo sideEffectInfo =
            new SideEffectInfo(loc, accessType, cfaEdge, SideEffectKind.GLOBAL_VARIABLE);
        sideEffects.add(sideEffectInfo);
        logger.logf(Level.INFO, "%s", sideEffectInfo);
      }
    }

    return ExpressionAnalysisSummary.of(sideEffects, new HashSet<>(), new HashMap<>());
  }

  @Override
  public ExpressionAnalysisSummary visit(CFunctionCallExpression funCallExpr)
      throws UnrecognizedCodeException {
    Set<SideEffectInfo> sideEffects = new HashSet<>();
    Map<CRightHandSide, Set<SideEffectInfo>> sideEffectsPerSubExpr = new HashMap<>();

    CExpression funcExpr = funCallExpr.getFunctionNameExpression();

    // Gather side effects for each function parameter
    for (CRightHandSide param : funCallExpr.getParameterExpressions()) {
      ExpressionAnalysisSummary paramSummary = param.accept(this);
      Set<SideEffectInfo> paramEffects = paramSummary.sideEffects();

      sideEffectsPerSubExpr.put(param, paramEffects);
      sideEffects.addAll(paramEffects);
    }

    if (funcExpr instanceof CIdExpression idExpr) { // side effects inside function body
      String functionName = idExpr.getName();

      // Add callee's side effects
      if (state.getSideEffectsInFun().containsKey(functionName)) {
        sideEffects.addAll(Objects.requireNonNull(state.getSideEffectsInFun().get(functionName)));
      }
    }

    return ExpressionAnalysisSummary.of(sideEffects, new HashSet<>(), sideEffectsPerSubExpr);
  }

  @Override
  public ExpressionAnalysisSummary visit(CBinaryExpression binaryExpr)
      throws UnrecognizedCodeException {

    Set<SideEffectInfo> sideEffects = new HashSet<>();
    Set<CBinaryExpression> unsequencedBinaryExprs = new HashSet<>();
    ExpressionAnalysisSummary leftSummary = binaryExpr.getOperand1().accept(this);
    ExpressionAnalysisSummary rightSummary = binaryExpr.getOperand2().accept(this);

    sideEffects.addAll(leftSummary.sideEffects());
    sideEffects.addAll(rightSummary.sideEffects());
    unsequencedBinaryExprs.addAll(leftSummary.unsequencedBinaryExprs());
    unsequencedBinaryExprs.addAll(rightSummary.unsequencedBinaryExprs());

    // Check if current binary operator itself is unsequenced
    if (isUnsequencedBinaryOperator(binaryExpr.getOperator())) {
      unsequencedBinaryExprs.add(binaryExpr);
      logger.logf(
          Level.INFO,
          "Detected unsequenced binary expression '%s' at %s",
          UnseqUtils.replaceTmpInExpression(binaryExpr, state),
          binaryExpr.getFileLocation());

      if (isPointerArithmetic(binaryExpr)) {
        logger.logf(
            Level.WARNING,
            "[Pointer Arithmetic] '%s' at %s. CPAchecker currently cannot fully analyze this"
                + " expression. Analysis may miss alias resolution or report imprecise conflicts.",
            UnseqUtils.replaceTmpInExpression(binaryExpr, state),
            binaryExpr.getFileLocation());
      }
    }

    return ExpressionAnalysisSummary.of(sideEffects, unsequencedBinaryExprs, new HashMap<>());
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
      // C11: 6.3.2.1
      // Except when it is the operand of the unary & operator,
      // an lvalue that does not have array type is converted to the value stored in the designated
      // object
      // (this is called lvalue conversion).
      // Then no lvalue conversion means no read here
      case ALIGNOF, AMPER -> ExpressionAnalysisSummary.empty();
      case MINUS, TILDE -> unaryExpr.getOperand().accept(this);
    };
  }

  @Override
  public ExpressionAnalysisSummary visit(CArraySubscriptExpression arrayExpr)
      throws UnrecognizedCodeException {
    Set<SideEffectInfo> sideEffects = new HashSet<>();

    ExpressionAnalysisSummary arrayBaseSummary = arrayExpr.getArrayExpression().accept(this);
    ExpressionAnalysisSummary indexSummary = arrayExpr.getSubscriptExpression().accept(this);
    sideEffects.addAll(arrayBaseSummary.sideEffects());
    sideEffects.addAll(indexSummary.sideEffects());

    CExpression base = arrayExpr.getArrayExpression();
    if (base instanceof CIdExpression idExpr) {
      String qName = idExpr.getDeclaration().getQualifiedName();
      MemoryLocation baseLoc = MemoryLocation.fromQualifiedName(qName);

      if (idExpr.getDeclaration() instanceof CVariableDeclaration decl) {
        if (decl.isGlobal()) {
          SideEffectInfo effect =
              new SideEffectInfo(baseLoc, accessType, cfaEdge, SideEffectKind.GLOBAL_VARIABLE);
          sideEffects.add(effect);
          logger.logf(Level.INFO, "[ARRAY] Record global array access: %s", effect);
        }
      } else {
        SideEffectInfo effect =
            new SideEffectInfo(
                baseLoc, accessType, cfaEdge, SideEffectKind.POINTER_DEREFERENCE_UNRESOLVED);
        sideEffects.add(effect);
        logger.logf(Level.INFO, "[ARRAY] Record array access as pointer dereference: %s", effect);
      }
    }

    return ExpressionAnalysisSummary.of(sideEffects, new HashSet<>(), new HashMap<>());
  }

  @Override
  public ExpressionAnalysisSummary visit(CFieldReference fieldRef)
      throws UnrecognizedCodeException {
    CExpression fieldOwner = fieldRef.getFieldOwner();
    ExpressionAnalysisSummary ownerSummary = fieldOwner.accept(this);
    Set<SideEffectInfo> sideEffects = new HashSet<>(ownerSummary.sideEffects());

    if (fieldRef.isPointerDereference()) {
      // g->cache, (*p).field
      if (fieldOwner instanceof CIdExpression idExpr) {
        MemoryLocation pointerLoc =
            MemoryLocation.fromQualifiedName(idExpr.getDeclaration().getQualifiedName());
        SideEffectInfo sideEffect =
            new SideEffectInfo(
                pointerLoc, accessType, cfaEdge, SideEffectKind.POINTER_DEREFERENCE_UNRESOLVED);
        sideEffects.add(sideEffect);
      }
    } else { // treat all accesses to fields of global structs as accesses to the entire variable,
      // to preserve soundness under aliasing.
      // g.cache
      if (fieldOwner instanceof CIdExpression idExpr
          && idExpr.getDeclaration() instanceof CVariableDeclaration decl
          && decl.isGlobal()) {

        MemoryLocation fieldLoc = MemoryLocation.fromQualifiedName(decl.getQualifiedName());
        SideEffectInfo sideEffect =
            new SideEffectInfo(fieldLoc, accessType, cfaEdge, SideEffectKind.GLOBAL_VARIABLE);
        sideEffects.add(sideEffect);
      }
    }

    return ExpressionAnalysisSummary.of(sideEffects, new HashSet<>(), new HashMap<>());
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

    CExpression operand = pointerExpr.getOperand();
    ExpressionAnalysisSummary operandSummary = operand.accept(this);
    Set<SideEffectInfo> sideEffects = new HashSet<>(operandSummary.sideEffects());

    if (operand instanceof CIdExpression idExpr) {
      MemoryLocation pointerLoc =
          MemoryLocation.fromQualifiedName(idExpr.getDeclaration().getQualifiedName());
      SideEffectInfo sideEffectInfo =
          new SideEffectInfo(
              pointerLoc, // pointer itself address here
              accessType,
              cfaEdge,
              SideEffectKind.POINTER_DEREFERENCE_UNRESOLVED);
      sideEffects.add(sideEffectInfo);
      logger.logf(Level.INFO, "%s", sideEffectInfo);
    }

    return ExpressionAnalysisSummary.of(sideEffects, new HashSet<>(), new HashMap<>());
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

  private boolean isPointerArithmetic(CBinaryExpression expr) {
    BinaryOperator op = expr.getOperator();
    if (op != BinaryOperator.PLUS && op != BinaryOperator.MINUS) {
      return false;
    }

    CType leftType = expr.getOperand1().getExpressionType();
    CType rightType = expr.getOperand2().getExpressionType();

    return (isPointerType(leftType) && isIntegerType(rightType))
        || (isIntegerType(leftType) && isPointerType(rightType))
        || (isPointerType(leftType) && isPointerType(rightType) && op == BinaryOperator.MINUS);
  }

  private boolean isPointerType(CType type) {
    return type instanceof CPointerType;
  }

  private boolean isIntegerType(CType type) {
    return type instanceof CSimpleType simpleType && simpleType.getType().isIntegerType();
  }
}
