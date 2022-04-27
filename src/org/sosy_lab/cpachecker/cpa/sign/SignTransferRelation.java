// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.sign;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AInitializer;
import org.sosy_lab.cpachecker.cfa.ast.AInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.ARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class SignTransferRelation
    extends ForwardingTransferRelation<SignState, SignState, SingletonPrecision> {

  LogManager logger;

  public static final String FUNC_RET_VAR = "__func_ret__";

  public SignTransferRelation(LogManager pLogger) {
    logger = pLogger;
  }

  public String getScopedVariableName(AExpression pVariableName) {
    return getScopedVariableName(pVariableName, functionName);
  }

  @Override
  protected SignState handleReturnStatementEdge(CReturnStatementEdge pCfaEdge)
      throws CPATransferException {

    CExpression expression =
        pCfaEdge.getExpression().orElse(CIntegerLiteralExpression.ZERO); // 0 is the default in C
    String assignedVar = getScopedVariableNameForNonGlobalVariable(FUNC_RET_VAR, functionName);
    return handleAssignmentToVariable(state, assignedVar, expression, pCfaEdge);
  }

  @Override
  protected SignState handleFunctionCallEdge(
      FunctionCallEdge pCfaEdge,
      List<? extends AExpression> pArguments,
      List<? extends AParameterDeclaration> pParameters,
      String pCalledFunctionName)
      throws CPATransferException {
    if (!pCfaEdge.getSuccessor().getFunctionDefinition().getType().takesVarArgs()) {
      assert (pParameters.size() == pArguments.size());
    }
    // Collect arguments
    ImmutableMap.Builder<String, SIGN> mapBuilder = ImmutableMap.builder();
    for (int i = 0; i < pParameters.size(); i++) {
      AExpression exp = pArguments.get(i);
      if (!(exp instanceof CRightHandSide)) {
        throw new UnrecognizedCodeException("Unsupported code found", pCfaEdge);
      }
      String scopedVarId =
          getScopedVariableNameForNonGlobalVariable(
              pParameters.get(i).getName(), pCalledFunctionName);
      mapBuilder.put(
          scopedVarId,
          ((CRightHandSide) exp).accept(new SignCExpressionVisitor(pCfaEdge, state, this)));
    }
    ImmutableMap<String, SIGN> argumentMap = mapBuilder.buildOrThrow();
    logger.log(
        Level.FINE, "Entering function " + pCalledFunctionName + " with arguments " + argumentMap);
    return state.enterFunction(argumentMap);
  }

  @Override
  protected SignState handleFunctionReturnEdge(
      FunctionReturnEdge pCfaEdge,
      FunctionSummaryEdge pFnkCall,
      AFunctionCall pSummaryExpr,
      String pCallerFunctionName)
      throws CPATransferException {

    // x = fun();
    if (pSummaryExpr instanceof AFunctionCallAssignmentStatement) {
      AFunctionCallAssignmentStatement assignStmt = (AFunctionCallAssignmentStatement) pSummaryExpr;
      AExpression leftSide = assignStmt.getLeftHandSide();
      if (!(leftSide instanceof AIdExpression)) {
        throw new UnrecognizedCodeException("Unsupported code found", pCfaEdge);
      }
      String returnVarName = getScopedVariableNameForNonGlobalVariable(FUNC_RET_VAR, functionName);
      String assignedVarName = getScopedVariableName(leftSide, pCallerFunctionName);
      logger.log(
          Level.FINE,
          "Leave function "
              + functionName
              + " with return assignment: "
              + assignedVarName
              + " = "
              + state.getSignForVariable(returnVarName));
      SignState result =
          state
              .leaveFunction(functionName)
              .assignSignToVariable(assignedVarName, state.getSignForVariable(returnVarName));
      return result;
    }

    // fun()
    if (pSummaryExpr instanceof AFunctionCallStatement) {
      logger.log(Level.FINE, "Leave function " + functionName);
      return state
          .removeSignAssumptionOfVariable(
              getScopedVariableNameForNonGlobalVariable(FUNC_RET_VAR, functionName))
          .leaveFunction(functionName);
    }

    throw new UnrecognizedCodeException("Unsupported code found", pCfaEdge);
  }

  private static class IdentifierValuePair {
    CExpression identifier;
    SIGN value;

    public IdentifierValuePair(CExpression pIdentifier, SIGN pValue) {
      identifier = pIdentifier;
      value = pValue;
    }
  }

  private Optional<IdentifierValuePair> evaluateAssumption(
      CBinaryExpression pAssumeExp, boolean truthAssumption, CFAEdge pCFAEdge) {
    Optional<CExpression> optStrongestId = getStrongestIdentifier(pAssumeExp, pCFAEdge);
    if (!optStrongestId.isPresent()) {
      return Optional.empty(); // No refinement possible, since no strongest identifier was found
    }
    CExpression strongestId = optStrongestId.orElseThrow();
    logger.log(
        Level.FINER,
        "Filtered strongest identifier " + strongestId + " from assume expression" + pAssumeExp);
    CExpression refinementExpression = getRefinementExpression(strongestId, pAssumeExp);
    BinaryOperator resultOp =
        truthAssumption
            ? pAssumeExp.getOperator()
            : pAssumeExp.getOperator().getOppositLogicalOperator();
    SIGN resultSign;
    try {
      resultSign = refinementExpression.accept(new SignCExpressionVisitor(pCFAEdge, state, this));
    } catch (UnrecognizedCodeException e) {
      return Optional.empty();
    }
    return evaluateAssumption(
        strongestId, resultOp, resultSign, isLeftOperand(strongestId, pAssumeExp));
  }

  private boolean isLeftOperand(CExpression pExp, CBinaryExpression pBinExp) {
    if (pExp == pBinExp.getOperand1()) {
      return true;
    } else if (pExp == pBinExp.getOperand2()) {
      return false;
    }
    throw new IllegalArgumentException("Argument pExp is not part of pBinExp");
  }

  private Optional<IdentifierValuePair> evaluateAssumption(
      CExpression pIdExp, BinaryOperator pOp, SIGN pResultSign, boolean pIdentIsLeft) {
    boolean equalZero = false;
    switch (pOp) {
      case GREATER_EQUAL:
        equalZero = pResultSign.covers(SIGN.ZERO);
        // $FALL-THROUGH$
      case GREATER_THAN:
        if (pIdentIsLeft) {
          if (SIGN.PLUS0.covers(pResultSign)) { // x > (0)+
            return Optional.of(new IdentifierValuePair(pIdExp, equalZero ? SIGN.PLUS0 : SIGN.PLUS));
          }
        } else {
          if (SIGN.MINUS0.covers(pResultSign)) { // (0)- > x
            return Optional.of(
                new IdentifierValuePair(pIdExp, equalZero ? SIGN.MINUS0 : SIGN.MINUS));
          }
        }
        break;
      case LESS_EQUAL:
        equalZero = pResultSign.covers(SIGN.ZERO);
        // $FALL-THROUGH$
      case LESS_THAN:
        if (pIdentIsLeft) { // x < (0)-
          if (SIGN.MINUS0.covers(pResultSign)) {
            return Optional.of(
                new IdentifierValuePair(pIdExp, equalZero ? SIGN.MINUS0 : SIGN.MINUS));
          }
        } else {
          if (SIGN.PLUS0.covers(pResultSign)) { // (0)+ < x
            return Optional.of(new IdentifierValuePair(pIdExp, equalZero ? SIGN.PLUS0 : SIGN.PLUS));
          }
        }
        break;
      case EQUALS:
        return Optional.of(new IdentifierValuePair(pIdExp, pResultSign));
      case NOT_EQUALS:
        if (pResultSign == SIGN.ZERO) {
          return Optional.of(new IdentifierValuePair(pIdExp, SIGN.PLUSMINUS));
        }
        break;
      default:
        // nothing to do here
    }
    return Optional.empty();
  }

  private CExpression getRefinementExpression(
      CExpression pStrongestIdent, CBinaryExpression pBinExp) {
    if (pStrongestIdent == pBinExp.getOperand1()) {
      return pBinExp.getOperand2();
    } else if (pStrongestIdent == pBinExp.getOperand2()) {
      return pBinExp.getOperand1();
    }
    throw new IllegalArgumentException("Strongest identifier is not part of binary expression");
  }

  private List<CExpression> filterIdentifier(CBinaryExpression pAssumeExp) {
    List<CExpression> result = new ArrayList<>();
    if ((pAssumeExp.getOperand1() instanceof CIdExpression)
        || (pAssumeExp.getOperand1() instanceof CFieldReference)) {
      result.add(pAssumeExp.getOperand1());
    }
    if ((pAssumeExp.getOperand2() instanceof CIdExpression)
        || (pAssumeExp.getOperand2() instanceof CFieldReference)) {
      result.add(pAssumeExp.getOperand2());
    }
    return result;
  }

  private Optional<CExpression> getStrongestIdentifier(
      CBinaryExpression pAssumeExp, CFAEdge pCFAEdge) {
    List<CExpression> result = filterIdentifier(pAssumeExp); // TODO
    if (result.isEmpty()) {
      return Optional.empty();
    }
    if (result.size() == 1) {
      return Optional.of(result.get(0));
    }
    try {
      SIGN leftResultSign = result.get(0).accept(new SignCExpressionVisitor(pCFAEdge, state, this));
      SIGN rightResultSign =
          result.get(1).accept(new SignCExpressionVisitor(pCFAEdge, state, this));
      if (leftResultSign.covers(rightResultSign)) {
        return Optional.of(result.get(0));
      } else {
        return Optional.of(result.get(1));
      }
    } catch (UnrecognizedCodeException ex) {
      return Optional.empty();
    }
  }

  @Override
  protected SignState handleAssumption(
      CAssumeEdge pCfaEdge, CExpression pExpression, boolean pTruthAssumption)
      throws CPATransferException { // TODO more complex things
    // Analyse only expressions of the form x op y
    if (!(pExpression instanceof CBinaryExpression)) {
      return state;
    }
    Optional<IdentifierValuePair> result =
        evaluateAssumption((CBinaryExpression) pExpression, pTruthAssumption, pCfaEdge);
    if (result.isPresent()) {
      logger.log(
          Level.FINE,
          "Assumption: "
              + (pTruthAssumption ? pExpression : "!(" + pExpression + ")")
              + " --> "
              + result.orElseThrow().identifier
              + " = "
              + result.orElseThrow().value);
      // assure that does not become more abstract after assumption
      if (state
          .getSignForVariable(getScopedVariableName(result.orElseThrow().identifier))
          .covers(result.orElseThrow().value)) {
        return state.assignSignToVariable(
            getScopedVariableName(result.orElseThrow().identifier), result.orElseThrow().value);
      }
      // check if results distinct, then no successor exists
      if (!result
          .orElseThrow()
          .value
          .intersects(
              state.getSignForVariable(getScopedVariableName(result.orElseThrow().identifier)))) {
        return null;
      }
    }
    return state;
  }

  @Override
  protected SignState handleDeclarationEdge(ADeclarationEdge pCfaEdge, ADeclaration pDecl)
      throws CPATransferException {
    if (!(pDecl instanceof AVariableDeclaration)) {
      return state;
    }
    AVariableDeclaration decl = (AVariableDeclaration) pDecl;
    String scopedId;
    if (decl.isGlobal()) {
      scopedId = decl.getName();
    } else {
      scopedId = getScopedVariableNameForNonGlobalVariable(decl.getName(), functionName);
    }
    AInitializer init = decl.getInitializer();
    logger.log(Level.FINE, "Declaration: " + scopedId);
    // type x = expression;
    if (init instanceof AInitializerExpression) {
      return handleAssignmentToVariable(
          state, scopedId, ((AInitializerExpression) init).getExpression(), pCfaEdge);
    }
    // type x;
    // since it is C, we assume it may have any value here
    return state.assignSignToVariable(scopedId, SIGN.ALL);
  }

  @Override
  protected SignState handleStatementEdge(AStatementEdge pCfaEdge, AStatement pStatement)
      throws CPATransferException {
    // expression is a binary expression e.g. a = b.
    if (pStatement instanceof AAssignment) {
      return handleAssignment((AAssignment) pStatement, pCfaEdge);
    }

    // only expression expr; does not change state
    if (pStatement instanceof AExpressionStatement) {
      return state;
    }
    // only function call f(); to external method: assume that it does not change global state
    // TODO check really only external methods?
    if (pStatement instanceof AFunctionCallStatement) {
      return state;
    }
    throw new UnrecognizedCodeException("only assignments are supported at this time", pCfaEdge);
  }

  private SignState handleAssignment(AAssignment pAssignExpr, CFAEdge edge)
      throws CPATransferException {
    AExpression left = pAssignExpr.getLeftHandSide();
    // a = ...
    if (left instanceof AIdExpression) { // TODO also consider arrays, pointer, etc.?
      if (!((left.getExpressionType() instanceof CSimpleType)
          || (left.getExpressionType() instanceof CTypedefType))) {
        return state;
      }
      String scopedId = getScopedVariableName(left, functionName);
      return handleAssignmentToVariable(state, scopedId, pAssignExpr.getRightHandSide(), edge);
    }
    // TODO become more precise, handle &x, (int *) x on right hand side, deal with int* x = s;
    // p->x = .., c.x =
    if (left instanceof CFieldReference) {
      String scopedId = getScopedVariableName(left, functionName);
      return handleAssignmentToVariable(state, scopedId, pAssignExpr.getRightHandSide(), edge);
    }

    // x[index] = ..,
    if (left instanceof CArraySubscriptExpression) {
      // currently only overapproximate soundly and assume any value
      return state.assignSignToVariable(
          getScopedVariableName(
              ((CArraySubscriptExpression) left).getArrayExpression(), functionName),
          SIGN.ALL);
    }
    throw new UnrecognizedCodeException("left operand has to be an id expression", edge);
  }

  private SignState handleAssignmentToVariable(
      SignState pState, String pVarIdent, ARightHandSide pRightExpr, CFAEdge edge)
      throws CPATransferException {
    if (pRightExpr instanceof CRightHandSide) {
      CRightHandSide right = (CRightHandSide) pRightExpr;
      SIGN result = right.accept(new SignCExpressionVisitor(edge, pState, this));
      logger.log(Level.FINE, "Assignment: " + pVarIdent + " = " + result);
      return pState.assignSignToVariable(pVarIdent, result);
    }
    throw new UnrecognizedCodeException("unhandled righthandside expression", edge);
  }

  private String getScopedVariableName(AExpression pVariableName, String pCalledFunctionName) {
    if (isGlobal(pVariableName)) {
      return pVariableName.toASTString();
    }
    return pCalledFunctionName + "::" + pVariableName.toASTString();
  }

  private String getScopedVariableNameForNonGlobalVariable(
      String pVariableName, String pCallFunctionName) {
    return pCallFunctionName + "::" + pVariableName;
  }
}
