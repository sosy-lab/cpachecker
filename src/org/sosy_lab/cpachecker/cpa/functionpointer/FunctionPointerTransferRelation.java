/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.functionpointer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.IASTArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTAssignment;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTPointerTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.IASTSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;

class FunctionPointerTransferRelation implements TransferRelation {

  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(
      AbstractElement pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {

    final FunctionPointerElement oldState = (FunctionPointerElement)pElement;
    final FunctionPointerElement newState = oldState.createDuplicate();

    switch(pCfaEdge.getEdgeType()) {

      // if edge is a statement edge, e.g. a = b + c
      case StatementEdge: {
        StatementEdge statementEdge = (StatementEdge) pCfaEdge;
        handleStatement(newState, statementEdge.getStatement(), pCfaEdge);
        break;
      }

      case FunctionCallEdge: {
        FunctionCallEdge functionCallEdge = (FunctionCallEdge) pCfaEdge;
        handleFunctionCall(newState, functionCallEdge);
        break;
      }

      case FunctionReturnEdge: {
        FunctionReturnEdge functionReturnEdge = (FunctionReturnEdge) pCfaEdge;
        handleFunctionReturn(newState, functionReturnEdge);
        break;
      }

      // maybe two function pointers are compared.
      case AssumeEdge: {
        break;
      }

      // nothing to do.
      case BlankEdge: {
        break;
      }

      case CallToReturnEdge: {
        break;
      }

      // declaration of a function pointer.
      case DeclarationEdge: {
        DeclarationEdge decEdge = (DeclarationEdge) pCfaEdge;
        IASTDeclaration declaration = decEdge.getRawAST();

        // store declaration in abstract state
        if (declaration.isGlobal()) {
          newState.declareNewVariable(declaration.getName());
        } else {
          String insideFunctionName = pCfaEdge.getPredecessor().getFunctionName();
          newState.declareNewVariable(scoped(declaration.getName(), insideFunctionName));
        }

        break;
      }

      case ReturnStatementEdge: {
        break;
      }

      default:
        throw new UnrecognizedCFAEdgeException(pCfaEdge);
    }

    if (newState == null) {
      return Collections.emptySet();
    } else {
      return Collections.singleton(newState);
    }
  }

  private void handleFunctionReturn(FunctionPointerElement pNewState,
      FunctionReturnEdge pFunctionReturnEdge) {
    // TODO Auto-generated method stub

  }

  private void handleStatement(
      FunctionPointerElement pNewState, IASTStatement pStatement,
      CFAEdge pCfaEdge) throws UnrecognizedCCodeException {
    // expression is a binary operation, e.g. a = b;
    if (pStatement instanceof IASTAssignment) {
      handleAssignmentStatement(pNewState, (IASTAssignment)pStatement, pCfaEdge);
    }
    // external function call
    else if(pStatement instanceof IASTFunctionCallStatement){
      // TODO
    }
    // there is such a case
    else if(pStatement instanceof IASTExpressionStatement){
      // TODO
    }
    else{
      throw new UnrecognizedCCodeException(pCfaEdge, pStatement);
    }
  }

  private void handleAssignmentStatement(
      FunctionPointerElement pNewState,
      IASTAssignment pStatement, CFAEdge pCfaEdge)
          throws UnrecognizedCCodeException {

    String functionName = pCfaEdge.getPredecessor().getFunctionName();
    IASTExpression op1 = pStatement.getLeftHandSide();
    IASTRightHandSide op2 = pStatement.getRightHandSide();

    if(op1 instanceof IASTIdExpression) {
      // a = ...
      String varName = scopedIfNecessary((IASTIdExpression)op1, functionName);
      handleAssignmentToVariable(pNewState, varName, op2, pCfaEdge);

    } else if (op1 instanceof IASTUnaryExpression
        && ((IASTUnaryExpression)op1).getOperator() == UnaryOperator.STAR) {
      // *a = ...
      // TODO: Support this statement.

    } else if (op1 instanceof IASTFieldReference) {

      //String functionName = pCfaEdge.getPredecessor().getFunctionName();
      //handleAssignmentToVariable(op1.getRawSignature(), op2, v);

      // TODO: Support this statement.
    } else if (op1 instanceof IASTArraySubscriptExpression) {
      // TODO assignment to array cell
    } else {
      throw new UnrecognizedCCodeException("left operand of assignment has to be a variable", pCfaEdge, op1);
    }
  }

  /**
   * Handles an assignment, where the left-hand side is a pointer.
   * If the right-hand side seems to not evaluate to a pointer, the left pointer
   * is just set to unknown (no warning / error etc. is produced).
   */
  private void handleAssignmentToVariable(FunctionPointerElement pNewState,
      String leftVarName, IASTRightHandSide expression, CFAEdge pCfaEdge)
      throws UnrecognizedCCodeException {
    String functionName = pCfaEdge.getPredecessor().getFunctionName();

    if (expression instanceof IASTLiteralExpression) {
      // a = 0
      pNewState.setVariableToBottom(leftVarName);
    } else if (expression instanceof IASTCastExpression) {
      // a = (int*)b
      // ignore cast, we do no type-checking
      handleAssignmentToVariable(pNewState, leftVarName,
                       ((IASTCastExpression)expression).getOperand(), pCfaEdge);

    } else if (expression instanceof IASTFunctionCallExpression) {
      // a = func()

//      IASTFunctionCallExpression funcExpression = (IASTFunctionCallExpression)expression;
//      String calledFunctionName = funcExpression.getFunctionNameExpression().getRawSignature();

      // TODO: Take return value of called function into account.
      pNewState.setVariableToTop(leftVarName);

    } else if (expression instanceof IASTBinaryExpression) {
      // a = b + c
      pNewState.setVariableToUndefined(leftVarName);

    } else if (expression instanceof IASTUnaryExpression) {
      IASTUnaryExpression unaryExpression = (IASTUnaryExpression)expression;
      UnaryOperator op = unaryExpression.getOperator();

      if (op == UnaryOperator.AMPER) {
        // a = &b
        String pointerToFunction = unaryExpression.getOperand().getRawSignature();
        pNewState.setVariablePointsTo(leftVarName, pointerToFunction);
        //TODO: Take type of variables into account.

      } else if (op == UnaryOperator.MINUS) {
        pNewState.setVariableToUndefined(leftVarName);

      } else if (op == UnaryOperator.STAR) {
        // a = *b
        pNewState.setVariableToUndefined(leftVarName);
        //TODO: Implement handling of dereferencing.

      } else {
        throw new UnrecognizedCCodeException("not expected in CIL", pCfaEdge,
            unaryExpression);
      }

    } else if (expression instanceof IASTIdExpression) {
      // a = b
      String rightVarName = scopedIfNecessary((IASTIdExpression)expression, functionName);
      pNewState.assignVariableValueFromVariable(leftVarName, rightVarName);
    } else {
      throw new UnrecognizedCCodeException("not expected in CIL", pCfaEdge,
          expression);
    }
  }

  private void handleFunctionCall(FunctionPointerElement pNewState,
      FunctionCallEdge callEdge)
  throws UnrecognizedCCodeException {

    FunctionDefinitionNode functionEntryNode = callEdge.getSuccessor();
    String calledFunctionName = functionEntryNode.getFunctionName();
    String callerFunctionName = callEdge.getPredecessor().getFunctionName();

    List<IASTParameterDeclaration> paramDecs = functionEntryNode.getFunctionParameters();
    List<String> paramNames = functionEntryNode.getFunctionParameterNames();
    List<IASTExpression> arguments = callEdge.getArguments();

    assert (paramNames.size() == arguments.size());

    for (int i=0; i < arguments.size(); i++) {
      String paramName = scoped(paramNames.get(i), calledFunctionName);
      IASTParameterDeclaration paramDec = paramDecs.get(i);
      IASTExpression actualArgument = arguments.get(i);

      pNewState.declareNewVariable(paramName);
      // get value of actual parameter in caller function context
      if (paramDec.getDeclSpecifier() instanceof IASTPointerTypeSpecifier) {
        if (((IASTPointerTypeSpecifier)paramDec.getDeclSpecifier()).getType() instanceof IASTFunctionTypeSpecifier) {
          if (actualArgument instanceof IASTUnaryExpression) {
            IASTUnaryExpression argUnExpr = (IASTUnaryExpression) actualArgument;
            if (actualArgument.getExpressionType() instanceof IASTPointerTypeSpecifier) {
              if (argUnExpr.getOperator().equals(IASTUnaryExpression.UnaryOperator.AMPER)) {
                pNewState.setVariablePointsTo(paramName, argUnExpr.getOperand().toASTString());
              }
            }
          } else if (actualArgument instanceof IASTIdExpression) {
            String actualArgumentVariable = scopedIfNecessary((IASTIdExpression)actualArgument, callerFunctionName);
            pNewState.assignVariableValueFromVariable(paramName, actualArgumentVariable);
          }
        }
      }
    }
  }

  // looks up the variable in the current namespace
  private String scopedIfNecessary(IASTIdExpression var, String function) {
    IASTSimpleDeclaration decl = var.getDeclaration();
    boolean isGlobal = false;
    if (decl instanceof IASTDeclaration) {
      isGlobal = ((IASTDeclaration)decl).isGlobal();
    }

    if (isGlobal) {
      return var.getName();
    } else {
      return scoped(var.getName(), function);
    }
  }

  // prefixes function to variable name
  // Call only if you are sure you have a local variable!
  private static String scoped(String var, String function) {
    return function + "::" + var;
  }

  @Override
  public Collection<? extends AbstractElement> strengthen(
      AbstractElement pElement, List<AbstractElement> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) {
    // in this method we could access the abstract domains of other CPAs
    // if required.
    return null;
  }
}