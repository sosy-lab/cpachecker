// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.unsequenced;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.unsequenced.SideEffectInfo.AccessType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class UnseqBehaviorAnalysisTransferRelation
    extends ForwardingTransferRelation<
        UnseqBehaviorAnalysisState, UnseqBehaviorAnalysisState, Precision> {

  private final LogManager logger;

  public UnseqBehaviorAnalysisTransferRelation(LogManager pLogger) {
    logger = pLogger;
  }

  @Override
  protected UnseqBehaviorAnalysisState handleStatementEdge(
      CStatementEdge statementEdge, CStatement stat) throws UnrecognizedCodeException {
    UnseqBehaviorAnalysisState newState = state;

    logger.log(Level.INFO, String.format("[HandleStatement] Processing: %s", stat.toASTString()));

    if (stat
        instanceof
        CExpressionAssignmentStatement
            exprAssign) {
      CExpression lhsExpr = exprAssign.getLeftHandSide();
      CExpression rhsExpr = exprAssign.getRightHandSide();

      // if functioncall true, then record side effects inside it
      if(lhsExpr instanceof CIdExpression){
        recordSideEffectsIfInFunctionCall(lhsExpr, statementEdge, AccessType.WRITE, newState);
        recordSideEffectsIfInFunctionCall(rhsExpr, statementEdge, AccessType.READ, newState);
      }else if(lhsExpr instanceof CPointerExpression pointerExpr){
        recordSideEffectsIfInFunctionCall(lhsExpr, statementEdge, AccessType.READ, newState);
        recordSideEffectsIfInFunctionCall(rhsExpr, statementEdge, AccessType.READ, newState);

        if (pointerExpr.getOperand() instanceof CBinaryExpression binaryExpr){ // to detect unseq behavior like *(f() + x) = 3;
          detectConflictsInUnsequencedBinaryExprs(binaryExpr, statementEdge, newState);
        }

        // to detect unseq behavior like *f() = g() + g(); and return *f() = g() + g();
        detectAssignmentStatementConflicts(lhsExpr,rhsExpr,statementEdge,newState);
      }

      // check if there exists unsequenced behavior and cause conflict
      // to detect unseq behavior like y = (f() + g()) + x;
      if (rhsExpr instanceof CBinaryExpression binaryExpr) {
        detectConflictsInUnsequencedBinaryExprs(binaryExpr, statementEdge, newState);
      }

    } else if (stat
        instanceof CExpressionStatement exStat) { // to detect unseq behavior like (f() + g()) + x;
      CExpression expr = exStat.getExpression();

      recordSideEffectsIfInFunctionCall(expr, statementEdge, AccessType.READ, newState);

      if (expr instanceof CBinaryExpression binaryExpr) {
        detectConflictsInUnsequencedBinaryExprs(binaryExpr, statementEdge, newState);
      }
    }

    return newState;
  }

  @Override
  protected UnseqBehaviorAnalysisState handleDeclarationEdge(
      CDeclarationEdge declarationEdge, CDeclaration declaration) throws UnrecognizedCodeException {
    logger.log(Level.INFO, String.format("[HandleDeclaration] Processing: %s", declaration.toASTString()));
    UnseqBehaviorAnalysisState newState = state;

    if (declaration instanceof CVariableDeclaration varDecl) {
      if (varDecl.getInitializer() instanceof CInitializerExpression init) {
        CExpression initExpr = init.getExpression();
        // if functioncall true, then record side effects rhs
        recordSideEffectsIfInFunctionCall(initExpr, declarationEdge, AccessType.READ, newState);

        if (initExpr
            instanceof
            CBinaryExpression
                binaryExpr) { // to detect unseq behavior like int y = (f() + g()) + x;
          detectConflictsInUnsequencedBinaryExprs(binaryExpr, declarationEdge, newState);
        }
      }
    }

    return newState;
  }

  @Override
  protected UnseqBehaviorAnalysisState handleFunctionCallEdge(
      CFunctionCallEdge callEdge,
      List<CExpression> arguments,
      List<CParameterDeclaration> parameters,
      String calledFunctionName)
      throws UnrecognizedCodeException {
    UnseqBehaviorAnalysisState newState = state;
    newState.setFunctionCalled(true);
    newState.setCalledFunctionName(calledFunctionName);

    for (CExpression argument : arguments) {
      recordSideEffectsIfInFunctionCall(argument, callEdge, AccessType.READ, newState);
      // to detect unseq behavior inside single argument
      if (argument instanceof CBinaryExpression binaryExpr) {
        detectConflictsInUnsequencedBinaryExprs(binaryExpr, callEdge, newState);
      }
    }

    return newState;
  }

  @Override
  protected UnseqBehaviorAnalysisState handleFunctionReturnEdge(
      CFunctionReturnEdge funReturnEdge, CFunctionCall summaryExpr, String callerFunctionName)
      throws UnrecognizedCodeException {
    logger.log(Level.INFO, String.format("[HandleSFunctionReturn] Processing: %s", funReturnEdge.getSummaryEdge().getExpression()));
    UnseqBehaviorAnalysisState newState = state;
    newState.setFunctionCalled(false);
    newState.setCalledFunctionName(null);

    ExpressionBehaviorGatherVisitor visitor =
        new ExpressionBehaviorGatherVisitor(newState, funReturnEdge, AccessType.READ, logger);

    if (summaryExpr instanceof CFunctionCallAssignmentStatement assignStmt) {
      CExpression lhs = assignStmt.getLeftHandSide();
      CFunctionCallExpression rhs = assignStmt.getRightHandSide();

      // to detect unseq behavior between arguments,like int c = foo(f1(), f2()); and return foo(f1(), f2());
      ExpressionAnalysisSummary summary = rhs.accept(visitor);
      detectCrossArgumentConflicts(summary.getSideEffectsPerSubExpr(), funReturnEdge, newState);

      if (lhs instanceof CIdExpression tmpVar) { // map tmp name and function name
        String tmpName = tmpVar.getDeclaration().getQualifiedName();
        newState.mapTmpToFunction(tmpName, rhs);

        logger.log(
            Level.INFO,
            String.format(
                "[TmpMapping] Map tmp variable '%s' to function call '%s' (Caller='%s')",
                tmpName, rhs.toQualifiedASTString(), callerFunctionName));
      }else if(lhs instanceof CPointerExpression pointerExpr){ // to detect unseq behavior *f()=g(); and return *f()=g();
        detectAssignmentStatementConflicts(pointerExpr, summaryExpr.getFunctionCallExpression(),funReturnEdge,newState);
      }
    }
    return newState;
  }

  @Override
  protected UnseqBehaviorAnalysisState handleReturnStatementEdge(CReturnStatementEdge returnEdge)
      throws UnrecognizedCodeException {
    UnseqBehaviorAnalysisState newState = state;

    Optional<CExpression> expressionOptional = returnEdge.getExpression();

    if (expressionOptional.isPresent()) {
      CExpression returnExpr = expressionOptional.orElseThrow();
      recordSideEffectsIfInFunctionCall(returnExpr, returnEdge, AccessType.READ, newState);

      if (returnExpr instanceof CBinaryExpression returnBinExpr) { // to detect unseq behavior like return (f() + g()) + x;
        detectConflictsInUnsequencedBinaryExprs(returnBinExpr, returnEdge, newState);
      }

    }


    return newState;
  }

  @Override
  protected UnseqBehaviorAnalysisState handleAssumption(
      CAssumeEdge cfaEdge, CExpression expression, boolean truthValue)
      throws UnrecognizedCodeException {
    UnseqBehaviorAnalysisState newState = state;
    // TODO: detect unseq behavior in condition
    return newState;
  }

  @Override
  protected UnseqBehaviorAnalysisState handleBlankEdge(BlankEdge cfaEdge) {
    return state;
  }

  /** Record side effects inside a function call. */
  private void recordSideEffectsIfInFunctionCall(
      CExpression expr, CFAEdge edge, AccessType accessType, UnseqBehaviorAnalysisState pState)
      throws UnrecognizedCodeException {

    String funName = pState.getCalledFunctionName();
    if (pState.hasFunctionCallOccurred()) {
      ExpressionBehaviorGatherVisitor visitor =
          new ExpressionBehaviorGatherVisitor(pState, edge, accessType, logger);
      ExpressionAnalysisSummary summary = expr.accept(visitor);
      Set<SideEffectInfo> effects = summary.getSideEffects();
      if (!effects.isEmpty()) {
        pState.addSideEffectsToFunction(funName, effects);

        logger.log(
            Level.INFO,
            String.format(
                "[CollectSideEffect] Function='%s', Expr='%s', Effects=%s",
                funName, expr.toQualifiedASTString(), effects));
      }
    }
  }

  /** Detect conflicts inside unsequenced binary expressions. */
  private void detectConflictsInUnsequencedBinaryExprs(
      CExpression expr, CFAEdge edge, UnseqBehaviorAnalysisState pState)
      throws UnrecognizedCodeException {

    ExpressionBehaviorGatherVisitor visitor =
        new ExpressionBehaviorGatherVisitor(pState, edge, AccessType.READ, logger);
    ExpressionAnalysisSummary summary = expr.accept(visitor);

    for (CBinaryExpression unseqExpr : summary.getUnsequencedBinaryExprs()) {
      CExpression left = unseqExpr.getOperand1();
      CExpression right = unseqExpr.getOperand2();

      ExpressionAnalysisSummary leftSummary = left.accept(visitor);
      ExpressionAnalysisSummary rightSummary = right.accept(visitor);

      Set<SideEffectInfo> leftEffects = leftSummary.getSideEffects();
      Set<SideEffectInfo> rightEffects = rightSummary.getSideEffects();

      String leftExprStr = leftSummary.getOriginalExpressionStr();
      String rightExprStr = rightSummary.getOriginalExpressionStr();

      logger.log(
          Level.INFO,
          String.format(
              "[UnseqExpr] Detected: (%s) ⊕ (%s)\n  → Left Side Effects: %s\n  → Right Side Effects: %s",
              leftExprStr, rightExprStr, leftEffects, rightEffects));

      Set<ConflictPair> conflicts =
          getUnsequencedConflicts(leftEffects, rightEffects, edge, leftExprStr, rightExprStr);
      if (!conflicts.isEmpty()) {
        pState.addConflicts(conflicts);
      }
    }
  }

  /** Find conflict pairs from two sets of side effects. */
  private Set<ConflictPair> getUnsequencedConflicts(
      Set<SideEffectInfo> op1Effects,
      Set<SideEffectInfo> op2Effects,
      CFAEdge location,
      String op1ExprStr,
      String op2ExprStr) {

    Set<ConflictPair> result = new HashSet<>();
    for (SideEffectInfo s1 : op1Effects) {
      for (SideEffectInfo s2 : op2Effects) {
        if (conflictOnSameLocation(s1, s2)) {
          result.add(new ConflictPair(s1, s2, location, op1ExprStr, op2ExprStr));

          logger.log(
              Level.INFO,
              String.format(
                  "[Conflict] Unsequenced conflict detected at %s: '%s' vs '%s' on location '%s'"
                      + " (access: %s / %s)",
                  location.getFileLocation(),
                  op1ExprStr,
                  op2ExprStr,
                  s1.getMemoryLocation(),
                  s1.getAccessType(),
                  s2.getAccessType()));
        }
      }
    }
    return result;
  }

  private boolean conflictOnSameLocation(
      SideEffectInfo sideEffectInfo1, SideEffectInfo sideEffectInfo2) {
    return sideEffectInfo1.getMemoryLocation().equals(sideEffectInfo2.getMemoryLocation())
        && (sideEffectInfo1.isWrite() || sideEffectInfo2.isWrite());
  }

  private void detectCrossArgumentConflicts(
      Map<String, Set<SideEffectInfo>> sideEffectsPerSubExprStr,
      CFAEdge edge,
      UnseqBehaviorAnalysisState pState) {

    List<String> exprStrs = new ArrayList<>(sideEffectsPerSubExprStr.keySet());

    for (int i = 0; i < exprStrs.size(); i++) {
      for (int j = i + 1; j < exprStrs.size(); j++) {
        String expr1 = exprStrs.get(i);
        String expr2 = exprStrs.get(j);

        Set<SideEffectInfo> effects1 =
            sideEffectsPerSubExprStr.getOrDefault(expr1, ImmutableSet.of());
        Set<SideEffectInfo> effects2 =
            sideEffectsPerSubExprStr.getOrDefault(expr2, ImmutableSet.of());

        logger.log(
            Level.INFO,
            String.format(
                "[CrossArgumentConflicts] Detected: Argument 1: (%s) ⊕ Argument 2: (%s)\n  → Argument 1 Side Effects: %s\n  → Argument 2 Side Effects: %s",
                expr1, expr2, effects1, effects2));

        Set<ConflictPair> conflicts =
            getUnsequencedConflicts(effects1, effects2, edge, expr1, expr2);
        if (!conflicts.isEmpty()) {
          pState.addConflicts(conflicts);
        }
      }
    }
  }

  private void detectAssignmentStatementConflicts(
      CExpression lhsExpr,
      CRightHandSide rhsExpr,
      CFAEdge edge,
      UnseqBehaviorAnalysisState pState
  ) throws UnrecognizedCodeException {

    ExpressionBehaviorGatherVisitor visitor =
        new ExpressionBehaviorGatherVisitor(pState, edge, AccessType.READ, logger);
    ExpressionAnalysisSummary lhsSummary = lhsExpr.accept(visitor);
    ExpressionAnalysisSummary rhsSummary = rhsExpr.accept(visitor);

    logger.log(
        Level.INFO,
        String.format(
            "[AssignmentConflict] Detected: LHS: (%s) ⊕ RHS: (%s)\n  → LHS Side Effects: %s\n  → RHS Side Effects: %s",
            lhsSummary.getOriginalExpressionStr(),
            rhsSummary.getOriginalExpressionStr(),
            lhsSummary.getSideEffects(),
            rhsSummary.getSideEffects()));

    Set<ConflictPair> conflicts =
        getUnsequencedConflicts(
            lhsSummary.getSideEffects(),
            rhsSummary.getSideEffects(),
            edge,
            lhsSummary.getOriginalExpressionStr(),
            rhsSummary.getOriginalExpressionStr());

    if (!conflicts.isEmpty()) {
      pState.addConflicts(conflicts);
    }
  }

}
