/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.sign;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IAExpression;
import org.sosy_lab.cpachecker.cfa.ast.IAInitializer;
import org.sosy_lab.cpachecker.cfa.ast.IARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.IAStatement;
import org.sosy_lab.cpachecker.cfa.ast.IAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;


public class SignTransferRelation extends ForwardingTransferRelation<SignState, SignState, SingletonPrecision> {

  LogManager logger;

  private Set<String> globalVariables = new HashSet<>();

  public final static String FUNC_RET_VAR = "__func_ret__";

  public SignTransferRelation(LogManager pLogger) {
    logger = pLogger;
  }

  public String getScopedVariableName(IAExpression pVariableName) {
    return getScopedVariableName(pVariableName, functionName);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {
    return null;
  }

  @Override
  protected SignState handleReturnStatementEdge(CReturnStatementEdge pCfaEdge)
      throws CPATransferException {

    CExpression expression = pCfaEdge.getExpression().or(CNumericTypes.ZERO); // 0 is the default in C
    String assignedVar = getScopedVariableName(FUNC_RET_VAR, functionName);
    return handleAssignmentToVariable(state, assignedVar, expression);
  }

  @Override
  protected SignState handleFunctionCallEdge(FunctionCallEdge pCfaEdge, List<? extends IAExpression> pArguments,
      List<? extends AParameterDeclaration> pParameters, String pCalledFunctionName) throws CPATransferException {
    if (!pCfaEdge.getSuccessor().getFunctionDefinition().getType().takesVarArgs()) {
      assert (pParameters.size() == pArguments.size());
    }
    // Collect arguments
    ImmutableMap.Builder<String, SIGN> mapBuilder = ImmutableMap.builder();
    for(int i = 0; i < pParameters.size(); i++) {
      IAExpression exp = pArguments.get(i);
      if(!(exp instanceof CRightHandSide)) {
        throw new UnrecognizedCodeException("Unsupported code found", pCfaEdge);
      }
      String scopedVarId = getScopedVariableName(pParameters.get(i).getName(), pCalledFunctionName);
      mapBuilder.put(scopedVarId, ((CRightHandSide)exp).accept(new SignCExpressionVisitor(edge, state, this)));
    }
    ImmutableMap<String, SIGN> argumentMap = mapBuilder.build();
    logger.log(Level.FINE, "Entering function " + pCalledFunctionName + " with arguments " + argumentMap);
    return state.enterFunction(argumentMap);
  }

  @Override
  protected SignState handleFunctionReturnEdge(FunctionReturnEdge pCfaEdge, FunctionSummaryEdge pFnkCall,
      AFunctionCall pSummaryExpr, String pCallerFunctionName) throws CPATransferException {

    // x = fun();
    if (pSummaryExpr instanceof AFunctionCallAssignmentStatement) {
      AFunctionCallAssignmentStatement assignStmt = (AFunctionCallAssignmentStatement) pSummaryExpr;
      IAExpression leftSide = assignStmt.getLeftHandSide();
      if (!(leftSide instanceof AIdExpression)) { throw new UnrecognizedCodeException("Unsupported code found",
          pCfaEdge); }
      String returnVarName = getScopedVariableName(FUNC_RET_VAR, functionName);
      String assignedVarName = getScopedVariableName(leftSide, pCallerFunctionName);
      logger.log(Level.FINE, "Leave function " + functionName + " with return assignment: " + assignedVarName + " = " + state.getSignMap().getSignForVariable(returnVarName));
      SignState result = state
              .leaveFunction()
              .assignSignToVariable(assignedVarName, state.getSignMap().getSignForVariable(returnVarName));
      return result;
    }

    // fun()
    if (pSummaryExpr instanceof AFunctionCallStatement) {
      logger.log(Level.FINE, "Leave function " + functionName);
      return state.removeSignAssumptionOfVariable(getScopedVariableName(FUNC_RET_VAR, functionName)).leaveFunction();
    }

    throw new UnrecognizedCodeException("Unsupported code found", pCfaEdge);
  }

  private static class IdentifierValuePair {
    CExpression identifier;
    SIGN value;
    public IdentifierValuePair(CExpression pIdentifier, SIGN pValue) {
      super();
      identifier = pIdentifier;
      value = pValue;
    }
  }

  private BinaryOperator negateComparisonOperator(BinaryOperator pOp) {
    switch(pOp) {
    case LESS_THAN:
      return BinaryOperator.GREATER_EQUAL;
    case LESS_EQUAL:
      return BinaryOperator.GREATER_THAN;
    case GREATER_THAN:
      return BinaryOperator.LESS_EQUAL;
    case GREATER_EQUAL:
      return BinaryOperator.LESS_THAN;
    case EQUALS:
      return BinaryOperator.NOT_EQUALS;
    case NOT_EQUALS:
      return BinaryOperator.EQUALS;
     default:
       throw new IllegalArgumentException("Cannot negate given operator");
    }
  }

  private Optional<IdentifierValuePair> evaluateAssumption(CBinaryExpression pAssumeExp, boolean truthAssumption, CFAEdge pCFAEdge)  {
    Optional<CExpression> optStrongestId = getStrongestIdentifier(pAssumeExp, pCFAEdge);
    if(!optStrongestId.isPresent()) {
      return Optional.absent(); // No refinement possible, since no strongest identifier was found
    }
    CExpression strongestId = optStrongestId.get();
    logger.log(Level.FINER, "Filtered strongest identifier " + strongestId + " from assume expression" + pAssumeExp);
    CExpression refinementExpression = getRefinementExpression(strongestId, pAssumeExp);
    BinaryOperator resultOp = !truthAssumption ? negateComparisonOperator(pAssumeExp.getOperator()) : pAssumeExp.getOperator();
    SIGN resultSign;
    try {
      resultSign = refinementExpression.accept(new SignCExpressionVisitor(pCFAEdge, state, this));
    } catch (UnrecognizedCodeException e) {
      return Optional.absent();
    }
    return evaluateAssumption(strongestId, resultOp, resultSign, isLeftOperand(strongestId, pAssumeExp));
  }

  private boolean isLeftOperand(CExpression pExp, CBinaryExpression  pBinExp) {
    if(pExp == pBinExp.getOperand1()) {
      return true;
    } else if(pExp == pBinExp.getOperand2()) {
      return false;
    }
    throw new IllegalArgumentException("Argument pExp is not part of pBinExp");
  }

  private Optional<IdentifierValuePair> evaluateAssumption(CExpression pIdExp, BinaryOperator pOp, SIGN pResultSign, boolean pIdentIsLeft) {
    boolean equalZero = false;
    switch(pOp) {
    case GREATER_EQUAL:
      equalZero = pResultSign.covers(SIGN.ZERO);
      //$FALL-THROUGH$
    case GREATER_THAN:
      if(pIdentIsLeft) {
        if(SIGN.PLUS0.covers(pResultSign)) { // x > (0)+
          return Optional.of(new IdentifierValuePair(pIdExp, equalZero ? SIGN.PLUS0 : SIGN.PLUS));
        }
      } else {
        if(SIGN.MINUS0.covers(pResultSign)) { // (0)- > x
          return Optional.of(new IdentifierValuePair(pIdExp, equalZero ? SIGN.MINUS0 : SIGN.MINUS));
        }
      }
      break;
    case LESS_EQUAL:
      equalZero = pResultSign.covers(SIGN.ZERO);
      //$FALL-THROUGH$
    case LESS_THAN:
      if(pIdentIsLeft) { // x < (0)-
        if(SIGN.MINUS0.covers(pResultSign)) {
          return Optional.of(new IdentifierValuePair(pIdExp, equalZero ? SIGN.MINUS0 : SIGN.MINUS));
        }
      } else {
        if(SIGN.PLUS0.covers(pResultSign)) { // (0)+ < x
          return Optional.of(new IdentifierValuePair(pIdExp, equalZero ? SIGN.PLUS0 : SIGN.PLUS));
        }
      }
      break;
    case EQUALS:
      return Optional.of(new IdentifierValuePair(pIdExp, pResultSign));
    case NOT_EQUALS:
      if(pResultSign == SIGN.ZERO){
        return Optional.of(new IdentifierValuePair(pIdExp, SIGN.PLUSMINUS));
      }
    }
    return Optional.absent();
  }

  private CExpression getRefinementExpression(CExpression pStrongestIdent, CBinaryExpression pBinExp) {
    if(pStrongestIdent == pBinExp.getOperand1()) {
      return pBinExp.getOperand2();
    } else if(pStrongestIdent == pBinExp.getOperand2()) {
      return pBinExp.getOperand1();
    }
    throw new IllegalArgumentException("Strongest identifier is not part of binary expression");
  }

  private List<CExpression> filterIdentifier(CBinaryExpression pAssumeExp) {
    List<CExpression> result = new ArrayList<>();
    if((pAssumeExp.getOperand1() instanceof CIdExpression) || (pAssumeExp.getOperand1() instanceof CFieldReference)) {
      result.add(pAssumeExp.getOperand1());
    }
    if((pAssumeExp.getOperand2() instanceof CIdExpression)|| (pAssumeExp.getOperand2() instanceof CFieldReference)) {
      result.add(pAssumeExp.getOperand2());
    }
    return result;
  }

  private Optional<CExpression> getStrongestIdentifier(CBinaryExpression pAssumeExp, CFAEdge pCFAEdge) {
    List<CExpression> result = filterIdentifier(pAssumeExp); // TODO
    if(result.isEmpty()) {
      return Optional.absent();
    }
    if(result.size() == 1) {
      return Optional.of(result.get(0));
    }
    try {
      SIGN leftResultSign = result.get(0).accept(new SignCExpressionVisitor(pCFAEdge, state, this));
      SIGN rightResultSign = result.get(1).accept(new SignCExpressionVisitor(pCFAEdge, state, this));
      if(leftResultSign.covers(rightResultSign)) {
        return Optional.of(result.get(0));
      } else {
        return Optional.of(result.get(1));
      }
    } catch(UnrecognizedCodeException ex) {
      return Optional.absent();
    }
  }

  @Override
  protected SignState handleAssumption(CAssumeEdge pCfaEdge, CExpression pExpression, boolean pTruthAssumption)
      throws CPATransferException {// TODO more complex things
    // Analyse only expressions of the form x op y
    if(!(pExpression instanceof CBinaryExpression)) {
      return state;
    }
    Optional<IdentifierValuePair> result = evaluateAssumption((CBinaryExpression)pExpression, pTruthAssumption, pCfaEdge);
    if(result.isPresent()) {
      logger.log(Level.FINE, "Assumption: " + (pTruthAssumption ? pExpression : "!(" + pExpression + ")") + " --> " + result.get().identifier + " = " + result.get().value);
      // assure that does not become more abstract after assumption
      if (state.getSignMap().getSignForVariable(getScopedVariableName(result.get().identifier))
          .covers(result.get().value)) { return state.assignSignToVariable(
          getScopedVariableName(result.get().identifier), result.get().value); }
      // check if results distinct, then no successor exists
      if (!result.get().value.intersects(state.getSignMap().getSignForVariable(
          getScopedVariableName(result.get().identifier)))) { return null; }
    }
    return state;
  }

  @Override
  protected SignState handleDeclarationEdge(ADeclarationEdge pCfaEdge, IADeclaration pDecl) throws CPATransferException {
    if(!(pDecl instanceof AVariableDeclaration)) {
      return state;
    }
    AVariableDeclaration decl = (AVariableDeclaration)pDecl;
    String scopedId;
    if(decl.isGlobal()) {
      scopedId = decl.getName();
      globalVariables.add(decl.getName());
    } else {
      scopedId = getScopedVariableName(decl.getName(), functionName);
    }
    IAInitializer init = decl.getInitializer();
    logger.log(Level.FINE, "Declaration: " + scopedId);
    // type x = expression;
    if(init instanceof AInitializerExpression) {
      return handleAssignmentToVariable(state, scopedId, ((AInitializerExpression)init).getExpression());
    }
    // type x;
    // since it is C, we assume it may have any value here
    return state.assignSignToVariable(scopedId, SIGN.ALL);
  }

  @Override
  protected SignState handleStatementEdge(AStatementEdge pCfaEdge, IAStatement pStatement) throws CPATransferException {
    // expression is a binary expression e.g. a = b.
    if(pStatement instanceof IAssignment) {
      return handleAssignment((IAssignment)pStatement);
    }

    // only expression expr; does not change state
    if(pStatement instanceof AExpressionStatement){
      return state;
    }
    // only function call f(); to external method: assume that it does not change global state
    //TODO check really only external methods?
    if(pStatement instanceof AFunctionCallStatement) {
      return state;
    }
      throw new UnrecognizedCodeException("only assignments are supported at this time", edge);
  }

  private SignState handleAssignment(IAssignment pAssignExpr)
      throws CPATransferException {
    IAExpression left = pAssignExpr.getLeftHandSide();
    // a = ...
    if(left instanceof AIdExpression) {// TODO also consider arrays, pointer, etc.?
      if(!((left.getExpressionType() instanceof CSimpleType)|| (left.getExpressionType() instanceof CTypedefType))){
        return state;
      }
      String scopedId = getScopedVariableName(left, functionName);
      return handleAssignmentToVariable(state, scopedId, pAssignExpr.getRightHandSide());
    }
    // TODO become more precise, handle &x, (int *) x on right hand side, deal with int* x = s;
    // p->x = .., c.x =
    if(left instanceof CFieldReference){
      String scopedId = getScopedVariableName(left, functionName);
      return handleAssignmentToVariable(state, scopedId, pAssignExpr.getRightHandSide());
    }
    throw new UnrecognizedCodeException("left operand has to be an id expression", edge);
  }

  private SignState handleAssignmentToVariable(SignState pState, String pVarIdent, IARightHandSide pRightExpr)
      throws CPATransferException {
    if(pRightExpr instanceof CRightHandSide) {
      CRightHandSide right = (CRightHandSide)pRightExpr;
      SIGN result = right.accept(new SignCExpressionVisitor(edge, pState, this));
      logger.log(Level.FINE,  "Assignment: " + pVarIdent + " = " + result);
      return pState.assignSignToVariable(pVarIdent, result);
    }
    throw new UnrecognizedCodeException("unhandled righthandside expression", edge);
  }

  private String getScopedVariableName(IAExpression pVariableName, String pCalledFunctionName) {
    if (isGlobal(pVariableName)) {
      return pVariableName.toASTString();
    }
    return pCalledFunctionName + "::" + pVariableName.toASTString();
  }

  private String getScopedVariableName(String pVariableName, String pCallFunctionName) {
     if(globalVariables.contains(pVariableName)) {
       return pVariableName;
     }
     return pCallFunctionName + "::" + pVariableName;
  }

}
