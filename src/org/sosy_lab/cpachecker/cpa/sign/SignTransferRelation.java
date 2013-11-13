/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;


public class SignTransferRelation extends ForwardingTransferRelation<SignState, SingletonPrecision> {

  @SuppressWarnings("unused")
  private LogManager logger;

  private Set<String> globalVariables = new HashSet<>();

  private Deque<List<String>> stackVariables = new ArrayDeque<>();

  private final static String FUNC_RET_VAR = "__func_ret__";

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
  protected SignState handleReturnStatementEdge(AReturnStatementEdge pCfaEdge, IAExpression pExpression)
      throws CPATransferException {
    if(pExpression == null) {
      pExpression = CNumericTypes.ZERO; // default in c
    }
    String assignedVar = getScopedVariableName(FUNC_RET_VAR, functionName);
    stackVariables.peek().add(assignedVar);
    return handleAssignmentToVariable(state, assignedVar, pExpression);
  }

  @Override
  protected SignState handleFunctionCallEdge(FunctionCallEdge pCfaEdge, List<? extends IAExpression> pArguments,
      List<? extends AParameterDeclaration> pParameters, String pCalledFunctionName) throws CPATransferException {
    if (!pCfaEdge.getSuccessor().getFunctionDefinition().getType().takesVarArgs()) {
      assert (pParameters.size() == pArguments.size());
    }
    SignState successor = state;
    stackVariables.push(new ArrayList<String>()); // side-effect: allocate space for local function vars
    for(int i = 0; i < pParameters.size(); i++) {
      IAExpression exp = pArguments.get(i);
      if(!(exp instanceof CExpression)) {
        throw new UnrecognizedCodeException("Unsupported code found", pCfaEdge);
      }
      String scopedVarIdent = getScopedVariableName(pParameters.get(i).getName(), pCalledFunctionName);
      stackVariables.peek().add(scopedVarIdent);
      successor = handleAssignmentToVariable(successor, scopedVarIdent, exp);
    }
    return successor;
  }

  @Override
  protected SignState handleFunctionReturnEdge(FunctionReturnEdge pCfaEdge, FunctionSummaryEdge pFnkCall,
      AFunctionCall pSummaryExpr, String pCallerFunctionName) throws CPATransferException {
    if(!(pSummaryExpr instanceof AFunctionCallAssignmentStatement)) {
      throw new UnrecognizedCodeException("Unsupported code found", pCfaEdge);
    }
    AFunctionCallAssignmentStatement assignStmt = (AFunctionCallAssignmentStatement)pSummaryExpr;
    IAExpression leftSide = assignStmt.getLeftHandSide();
    if(!(leftSide instanceof AIdExpression)) {
      throw new UnrecognizedCodeException("Unsupported code found", pCfaEdge);
    }
    String returnVarName = getScopedVariableName(FUNC_RET_VAR, functionName);
    String assignedVarName = getScopedVariableName(leftSide, pCallerFunctionName);
    SignState result = state.assignSignToVariable(assignedVarName, state.getSignMap().getSignForVariable(returnVarName));
    // Clear stack
    List<String> localFunctionVars = stackVariables.pop();
    for(String scopedVarIdent : localFunctionVars) {
      result = result.removeSignAssumptionOfVariable(scopedVarIdent); // TODO performance
    }
    return result;
  }


  @Override
  protected SignState handleAssumption(CAssumeEdge cfaEdge, CExpression expression, boolean truthAssumption)
      throws CPATransferException {
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
    if(init instanceof AInitializerExpression) {
      return handleAssignmentToVariable(state, scopedId, ((AInitializerExpression)init).getExpression());
    }
    // default sign is zero
	// TODO since it is C, we better assume it may have any value here
    return state.assignSignToVariable(scopedId, SIGN.ALL);
  }

  @Override
  protected SignState handleStatementEdge(AStatementEdge pCfaEdge, IAStatement pStatement) throws CPATransferException {
    // expression is a binary expressionm e.g. a = b.
    if(pStatement instanceof IAssignment) {
      return handleAssignment((IAssignment)pStatement);
    }
    throw new UnrecognizedCodeException("only assignments are supported at this time", edge);
  }

  private SignState handleAssignment(IAssignment pAssignExpr)
      throws CPATransferException {
    IAExpression left = pAssignExpr.getLeftHandSide();
    // a = ...
    if(left instanceof AIdExpression) {
      String pId = getScopedVariableName(left, functionName);
      return handleAssignmentToVariable(state, pId, pAssignExpr.getRightHandSide());
    }
    throw new UnrecognizedCodeException("left operand has to be an id expression", edge);
  }

  private SignState handleAssignmentToVariable(SignState pState, String pVarIdent, IARightHandSide pRightExpr)
      throws CPATransferException {
    if(pRightExpr instanceof CRightHandSide) {
      CRightHandSide right = (CRightHandSide)pRightExpr;
      SIGN result = right.accept(new SignCExpressionVisitor(edge, pState, this));
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
