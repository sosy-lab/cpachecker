// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.unsequenced;

import java.util.HashSet;
import java.util.List;
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
    extends ForwardingTransferRelation<UnseqBehaviorAnalysisState, UnseqBehaviorAnalysisState, Precision> {

  private final LogManager logger;

  public UnseqBehaviorAnalysisTransferRelation(LogManager pLogger) {
    logger = pLogger;
  }

  @Override
  protected UnseqBehaviorAnalysisState handleStatementEdge(CStatementEdge statementEdge, CStatement stat)
      throws UnrecognizedCodeException {
    UnseqBehaviorAnalysisState newState = state;
    if (stat instanceof CExpressionAssignmentStatement exprAssign) {// to detect unseq behavior like y = (f() + g()) + x
      CExpression lhsExpr = exprAssign.getLeftHandSide();
      CExpression rhsExpr = exprAssign.getRightHandSide();

      //if functioncall true, then record side effects inside it
      recordSideEffectsIfInFunctionCall(lhsExpr, statementEdge, AccessType.WRITE, newState);
      recordSideEffectsIfInFunctionCall(rhsExpr, statementEdge, AccessType.READ, newState);

      //check if there exists unsequenced behavior and cause conflict
      if (rhsExpr instanceof CBinaryExpression binaryExpr){
        detectConflictsInUnsequencedBinaryExprs(binaryExpr, statementEdge, newState);
      }
    }else if (stat instanceof CExpressionStatement exStat) {// to detect unseq behavior like (f() + g()) + x
      CExpression expr = exStat.getExpression();

      recordSideEffectsIfInFunctionCall(expr, statementEdge, AccessType.READ, newState);

      if (expr instanceof CBinaryExpression binaryExpr) {
        detectConflictsInUnsequencedBinaryExprs(binaryExpr, statementEdge, newState);
      }

      //TODO: *f() = g()
    }

    return newState;
  }

  @Override
  protected UnseqBehaviorAnalysisState handleDeclarationEdge(
      CDeclarationEdge declarationEdge, CDeclaration declaration) throws UnrecognizedCodeException {
    UnseqBehaviorAnalysisState newState = state;
    if (declaration instanceof CVariableDeclaration varDecl) {
      if (varDecl.getInitializer() instanceof CInitializerExpression init) {
        CExpression initExpr = init.getExpression();
        //if functioncall true, then record side effects rhs
        recordSideEffectsIfInFunctionCall(initExpr, declarationEdge, AccessType.READ, newState);

        if (initExpr instanceof CBinaryExpression binaryExpr){ //to detect unseq behavior like int y = (f() + g()) + x
          detectConflictsInUnsequencedBinaryExprs(binaryExpr, declarationEdge, newState);
        }
      }
    }

    return newState;
  }

  @Override
  protected UnseqBehaviorAnalysisState handleFunctionCallEdge(
      CFunctionCallEdge callEdge, List<CExpression> arguments, List<CParameterDeclaration> parameters, String calledFunctionName)
      throws UnrecognizedCodeException {
    UnseqBehaviorAnalysisState newState = state;
    newState.setFunctionCalled(true);
    newState.setCalledFunctionName(calledFunctionName);

    //TODO: record side effects for parameters, if a function is called

    //TODO: detect unseq behavior in function arguments like f(g(), a() + b())

    return newState;
  }

  @Override
  protected UnseqBehaviorAnalysisState handleFunctionReturnEdge(
      CFunctionReturnEdge cfaEdge, CFunctionCall summaryExpr, String callerFunctionName)
      throws UnrecognizedCodeException {

    //map tmp name and function name
    if (summaryExpr instanceof CFunctionCallAssignmentStatement assignStmt) {
      CExpression lhs = assignStmt.getLeftHandSide();
      CFunctionCallExpression rhs = assignStmt.getRightHandSide();

      if (lhs instanceof CIdExpression tmpVar) {
        String tmpName = tmpVar.getName();
        String funName = rhs.getDeclaration().getName();

        state.mapTmpToFunction(tmpName, funName);
      }
    }

    return state;
  }

  @Override
  protected UnseqBehaviorAnalysisState handleReturnStatementEdge(CReturnStatementEdge returnEdge)
      throws UnrecognizedCodeException {
    UnseqBehaviorAnalysisState newState = state;
    newState.setFunctionCalled(false);
    newState.setCalledFunctionName(null);
    // TODO: detect unseq behavior in return statement
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

  private void recordSideEffectsIfInFunctionCall(
      CExpression expr,
      CFAEdge edge,
      AccessType accessType,
      UnseqBehaviorAnalysisState pstate
  ) throws UnrecognizedCodeException {
    String funName = pstate.getCalledFunctionName();
    if (funName != null) {
      SideEffectGatherVisitor visitor = new SideEffectGatherVisitor(pstate, edge, accessType, logger);
      Set<SideEffectInfo> effects = expr.accept(visitor);
      pstate.addSideEffectsToFunction(funName, effects);
    }
  }

  private void detectConflictsInUnsequencedBinaryExprs(
      CExpression binaryExprs,
      CFAEdge pCFAEdge,
      UnseqBehaviorAnalysisState pState) throws UnrecognizedCodeException {

    BinaryExpressionGatherVisitor binaryVisitor = new BinaryExpressionGatherVisitor(logger);
    Set<CBinaryExpression> unseqBinExprs = binaryExprs.accept(binaryVisitor);

    for (CBinaryExpression unseqExpr : unseqBinExprs) {
      CExpression left = unseqExpr.getOperand1();
      CExpression right = unseqExpr.getOperand2();

      Set<SideEffectInfo> leftEffects = resolveSideEffectsFromExpr(left, pState, pCFAEdge);
      Set<SideEffectInfo> rightEffects = resolveSideEffectsFromExpr(right, pState, pCFAEdge);

      Set<ConflictPair> conflicts = getUnsequencedConflicts(leftEffects, rightEffects, pCFAEdge, left, right);
      if (!conflicts.isEmpty()) {
        pState.addConflicts(conflicts);
      }
    }
    pState.clearTmpMappings();
  }

  private Set<ConflictPair> getUnsequencedConflicts(
      Set<SideEffectInfo> op1Effects,
      Set<SideEffectInfo> op2Effects,
      CFAEdge location,
      CExpression op1Expr,
      CExpression op2Expr) {

    TmpReplacingToStringVisitor visitor = new TmpReplacingToStringVisitor(state.getTmpNameFunNameMap());

    String exprStrA, exprStrB;

    try {
      exprStrA = op1Expr.accept(visitor);
      exprStrB = op2Expr.accept(visitor);
    } catch (Exception e) {
      exprStrA = op1Expr.toASTString();
      exprStrB = op2Expr.toASTString();
    }

    Set<ConflictPair> result = new HashSet<>();
    for (SideEffectInfo s1 : op1Effects) {
      for (SideEffectInfo s2 : op2Effects) {
        if (conflictOnSameLocation(s1, s2)) {
          result.add(new ConflictPair(s1, s2, location, exprStrA, exprStrB));
        }
      }
    }
    return result;
  }

  private boolean conflictOnSameLocation(SideEffectInfo sideEffectInfo1, SideEffectInfo sideEffectInfo2) {
    return sideEffectInfo1.getMemoryLocation().equals(sideEffectInfo2.getMemoryLocation()) &&
        (sideEffectInfo1.isWrite() || sideEffectInfo2.isWrite());
  }

  private Set<SideEffectInfo> resolveSideEffectsFromExpr(CExpression expr, UnseqBehaviorAnalysisState pState, CFAEdge pCFAEdge) {
    Set<SideEffectInfo> result = new HashSet<>();

    if (expr instanceof CIdExpression idExpr) {
      String tmp = idExpr.getName();
      String fun = pState.getFunctionForTmp(tmp);

      if (fun != null) {
        Set<SideEffectInfo> effects = pState.getSideEffectsInFun().getOrDefault(fun, Set.of());
        logger.log(Level.INFO, String.format(
            "[resolveSideEffects] TMP: %s → Function: %s → SideEffects: %d → %s",
            tmp, fun, effects.size(), effects
        ));
        result.addAll(effects);
        return result;
      }
    }

    try {
      SideEffectGatherVisitor visitor = new SideEffectGatherVisitor(pState, pCFAEdge, AccessType.READ, logger);
      result.addAll(expr.accept(visitor));
    } catch (Exception e) {
      logger.log(Level.WARNING, "[resolveSideEffects] Exception while visiting expr: " + expr.toASTString(), e);
    }

    logger.log(Level.INFO, String.format(
        "[resolveSideEffects] Final side effects for expr: %s → %s",
        expr.toASTString(), result));
    return result;
  }


}
