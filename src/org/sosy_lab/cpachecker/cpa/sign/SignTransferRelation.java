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
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.common.LogManager;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
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

  private Deque<Set<String>> stackVariables = new ArrayDeque<>();

  public final static String FUNC_RET_VAR = "__func_ret__";

  public SignTransferRelation(LogManager pLogger) {
    logger = pLogger;
    stackVariables.push(new HashSet<String>());
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
    SignState result = handleAssignmentToVariable(state, assignedVar, pExpression);

    // Clear stack TODO move to handleFunctionReturnEdge otherwise these variables are not removed if no return statement exists
    Set<String> localFunctionVars = stackVariables.pop();
    for(String scopedVarIdent : localFunctionVars) {
      result = result.removeSignAssumptionOfVariable(scopedVarIdent); // TODO performance
    }

    return result;
  }

  @Override
  protected SignState handleFunctionCallEdge(FunctionCallEdge pCfaEdge, List<? extends IAExpression> pArguments,
      List<? extends AParameterDeclaration> pParameters, String pCalledFunctionName) throws CPATransferException {
    if (!pCfaEdge.getSuccessor().getFunctionDefinition().getType().takesVarArgs()) {
      assert (pParameters.size() == pArguments.size());
    }
    SignState successor = state;
    stackVariables.push(new HashSet<String>()); // side-effect: allocate space for local function variables
    for(int i = 0; i < pParameters.size(); i++) {
      IAExpression exp = pArguments.get(i);
      if(!(exp instanceof CExpression)) {
        throw new UnrecognizedCodeException("Unsupported code found", pCfaEdge);
      }
      String scopedVarIdent = getScopedVariableName(pParameters.get(i).getName(), pCalledFunctionName);
      stackVariables.getFirst().add(scopedVarIdent);
      successor = handleAssignmentToVariable(successor, scopedVarIdent, exp); // TODO performance
    }
    return successor;
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

      SignState result = state
          .assignSignToVariable(assignedVarName, state.getSignMap().getSignForVariable(returnVarName))
          .removeSignAssumptionOfVariable(returnVarName);
      return result;
    }

    // fun()
    if (pSummaryExpr instanceof AFunctionCallStatement) {
      return state.removeSignAssumptionOfVariable(getScopedVariableName(FUNC_RET_VAR, functionName));
    }

    throw new UnrecognizedCodeException("Unsupported code found", pCfaEdge);
  }

  // helper class
  private static class Operand {

    private boolean left = false;

    private final CExpression exp;

    private final SIGN sign;

    public Operand(CExpression pExp, boolean pLeft, SIGN pSign) {
      left = pLeft;
      exp = pExp;
      sign = pSign;
    }

    public boolean isLeft() {
      return left;
    }

    public SIGN getSign() {
      return sign;
    }

    public boolean isId() {
      return (exp instanceof CIdExpression);
    }

    public CExpression getExp() {
      return exp;
    }

  }

  private Operand getWeakestOperand(Operand left, Operand right) {
    if(left.getSign().covers(right.getSign()) && left.isId()) {
      return right;
    }
    if(right.getSign().covers(left.getSign()) && right.isId()) {
      return left;
    }
    if(right.isId()) {
      return right;
    }
    return left;
  }

  @Override
  protected SignState handleAssumption(CAssumeEdge cfaEdge, CExpression expression, boolean truthAssumption)
      throws CPATransferException {
    // Analyse only expressions of the form x op y
    if(!(expression instanceof CBinaryExpression)) {
      return state;
    }
    CBinaryExpression binExp = (CBinaryExpression)expression;
    // At least x or y has to be an identifier
    if(!(binExp.getOperand1() instanceof CIdExpression || binExp.getOperand2() instanceof CIdExpression)) {
      return state;
    }
    // TODO else-branch analysis
    if(truthAssumption) {
      SIGN leftResultSign = binExp.getOperand1().accept(new SignCExpressionVisitor(cfaEdge, state, this));
      SIGN rightResultSign = binExp.getOperand2().accept(new SignCExpressionVisitor(cfaEdge, state, this));

      Operand left = new Operand(binExp.getOperand1(), true, leftResultSign);
      Operand right = new Operand(binExp.getOperand1(), false, rightResultSign);
      Operand weakest = getWeakestOperand(left, right);
      Operand assign = weakest == left ? right : left;
      BinaryOperator op = binExp.getOperator();

      SIGN result = SIGN.EMPTY;
      // Case 1: 0- > x or x < 0- => Sign(x) = -
      if(weakest.isLeft() && (op == BinaryOperator.GREATER_THAN || op == BinaryOperator.GREATER_EQUAL) && SIGN.MINUS0.covers(weakest.getSign())
          || !weakest.isLeft() && (op == BinaryOperator.LESS_THAN || op == BinaryOperator.LESS_EQUAL) && SIGN.MINUS0.covers(weakest.getSign())) {
        result = SIGN.MINUS;
      }

      // Case 2: 0+ < x or x > 0+ => Sign(x) = +
      if(weakest.isLeft() && (op == BinaryOperator.LESS_THAN || op == BinaryOperator.LESS_EQUAL) && SIGN.PLUS0.covers(weakest.getSign())
          || !weakest.isLeft() && op == BinaryOperator.GREATER_THAN && SIGN.PLUS0.covers(weakest.getSign())) {
        result = SIGN.PLUS;
      }

      // Subcases (a) and (b)
      if(!result.isEmpty() && (op == BinaryOperator.GREATER_EQUAL || op == BinaryOperator.LESS_EQUAL) && weakest.getSign().covers(SIGN.ZERO)) {
        result = result.combineWith(SIGN.ZERO);
      }

      // Finally, refine the variable if possible
      if(!result.isEmpty()) {
        return state.assignSignToVariable(getScopedVariableName(((CIdExpression)assign.getExp()).getName(), functionName), result);
      }


//      SIGN leftResultSign = binExp.getOperand1().accept(new SignCExpressionVisitor(cfaEdge, state, this));
//      SIGN rightResultSign = binExp.getOperand2().accept(new SignCExpressionVisitor(cfaEdge, state, this));
//
//      Optional<String> leftIdent = Optional.absent();
//      if(binExp.getOperand1() instanceof CIdExpression) { // left side is atomic
//        leftIdent = Optional.of(((CIdExpression)binExp.getOperand1()).getName());
//      }
//      Optional<String> rightIdent = Optional.absent();
//      if(binExp.getOperand2() instanceof CIdExpression) { // right side is atomic
//        rightIdent = Optional.of(((CIdExpression)binExp.getOperand2()).getName());
//      }


//      if(leftIdent.isPresent()) {
//        switch(binExp.getOperator()) {
//        case EQUALS:
//          // make sure both sides have the same value
//          SignState result = state;
//          for(Optional<String> ident : ImmutableList.of(leftIdent, rightIdent)) {
//            if(ident.isPresent()) {
//              // TODO scope
//              result = result.assignSignToVariable(getScopedVariableName(ident.get(), functionName), SIGN.min(leftResultSign, rightResultSign));
//            }
//          }
//          return result;
//        default:
//          throw new UnrecognizedCodeException("Unrecognized assume transition", cfaEdge);
//        }
//      }
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
      stackVariables.getFirst().add(scopedId);
    }
    IAInitializer init = decl.getInitializer();
    if(init instanceof AInitializerExpression) {
      return handleAssignmentToVariable(state, scopedId, ((AInitializerExpression)init).getExpression());
    }
    // since it is C, we assume it may have any value here
    return state.assignSignToVariable(scopedId, SIGN.ALL);
  }

  @Override
  protected SignState handleStatementEdge(AStatementEdge pCfaEdge, IAStatement pStatement) throws CPATransferException {
    // expression is a binary expressionm e.g. a = b.
    if(pStatement instanceof IAssignment) {
      return handleAssignment((IAssignment)pStatement);
    }

    // only expression expr; does not change state
    if(pStatement instanceof AExpressionStatement){
      return state;
    }

    throw new UnrecognizedCodeException("only assignments are supported at this time", edge);
  }

  private SignState handleAssignment(IAssignment pAssignExpr)
      throws CPATransferException {
    IAExpression left = pAssignExpr.getLeftHandSide();
    // a = ...
    if(left instanceof AIdExpression) {
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
