/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.octagon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageState;
import org.sosy_lab.cpachecker.cpa.pointer.PointerState;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
/**
 * Handles transfer relation for Octagon abstract domain library.
 * See <a href="http://www.di.ens.fr/~mine/oct/">Octagon abstract domain library</a>
 */
class OctTransferRelation implements TransferRelation{

  // set to set global variables
  private List<String> globalVars;

//  private String missingInformationLeftVariable = null;
//  private String missingInformationRightPointer = null;
//  private String missingInformationLeftPointer  = null;
//  private CExpression missingInformationRightExpression = null;

  /**
   * Class constructor.
   */
  public OctTransferRelation ()
  {
    globalVars = new ArrayList<String>();
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors (AbstractState element, Precision prec, CFAEdge cfaEdge) throws UnrecognizedCCodeException
  {

    // octElement is the region of the current state
    // this state will be updated using the edge

    OctState octState = null;
    OctState prevElement = (OctState)element;
    octState = ((OctState)element).clone();

    assert(octState != null);

    // check the type of the edge
    switch (cfaEdge.getEdgeType ())
    {

    // if edge is a statement edge, e.g. a = b + c
    case StatementEdge:
    {
      CStatementEdge statementEdge = (CStatementEdge) cfaEdge;
      CStatement expression = statementEdge.getStatement();
      octState = handleStatement (octState, expression, cfaEdge);
      break;
    }

    case ReturnStatementEdge:
    {
      CReturnStatementEdge statementEdge = (CReturnStatementEdge) cfaEdge;
      // this statement is a function return, e.g. return (a);
      // note that this is different from return edge
      // this is a statement edge which leads the function to the
      // last node of its CFA, where return edge is from that last node
      // to the return site of the caller function
      octState = handleExitFromFunction(octState, statementEdge.getExpression(), statementEdge);
      break;
    }

    // edge is a decleration edge, e.g. int a;
    case DeclarationEdge:
    {
      CDeclarationEdge declarationEdge = (CDeclarationEdge) cfaEdge;
      octState = handleDeclaration (octState, declarationEdge);
      break;
    }

    // this is an assumption, e.g. if(a == b)
    case AssumeEdge:
    {
      CAssumeEdge assumeEdge = (CAssumeEdge) cfaEdge;
      CExpression expression = assumeEdge.getExpression();
      octState = (OctState)handleAssumption (octState, expression, cfaEdge, assumeEdge.getTruthAssumption());
      break;

    }

    case BlankEdge:
    {
      break;
    }

    case FunctionCallEdge:
    {
      CFunctionCallEdge functionCallEdge = (CFunctionCallEdge) cfaEdge;
      octState = handleFunctionCall(octState, prevElement, functionCallEdge, cfaEdge);
      break;
    }

    // this is a return edge from function, this is different from return statement
    // of the function. See case for statement edge for details
    case FunctionReturnEdge:
    {
      CFunctionReturnEdge functionReturnEdge = (CFunctionReturnEdge) cfaEdge;
      octState = handleFunctionReturn(octState, functionReturnEdge);
      break;
    }

    // Summary edge, we handle this on function return, do nothing
    case CallToReturnEdge:
    {
      assert(false);
      break;
    }
    }

    if (octState == null || octState.isEmpty()) {
      return Collections.emptySet();
    }

    return Collections.singleton(octState);
  }

  /**
   * Handles return from one function to another function.
   * @param element previous abstract state.
   * @param functionReturnEdge return edge from a function to its call site.
   * @return new abstract state.
   */
  private OctState handleFunctionReturn(OctState element,
      CFunctionReturnEdge functionReturnEdge)
  throws UnrecognizedCCodeException {

    CFunctionSummaryEdge summaryEdge = functionReturnEdge.getSummaryEdge();
    CFunctionCall exprOnSummary = summaryEdge.getExpression();

    OctState previousElem = element.getPreviousState();

    String callerFunctionName = functionReturnEdge.getSuccessor().getFunctionName();
    String calledFunctionName = functionReturnEdge.getPredecessor().getFunctionName();

    //expression is an assignment operation, e.g. a = g(b);
    if (exprOnSummary instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement binExp = ((CFunctionCallAssignmentStatement)exprOnSummary);
      CExpression op1 = binExp.getLeftHandSide();

      //we expect left hand side of the expression to be a variable
      if(op1 instanceof CIdExpression ||
          op1 instanceof CFieldReference)
      {
        String varName = op1.toASTString();
        String returnVarName = calledFunctionName + "::" + "___cpa_temp_result_var_";

        String assignedVarName = getvarName(varName, callerFunctionName);

        assignVariable(element, assignedVarName, returnVarName, 1);
      }
      else{
        throw new UnrecognizedCCodeException("on function return", summaryEdge, op1);
      }
    }
    // g(b)
    else if (exprOnSummary instanceof CFunctionCallStatement)
    {
      // do nothing
    }
    else{
      throw new UnrecognizedCCodeException("on function return", summaryEdge, exprOnSummary.asStatement());
    }

    // delete local variables
    element.removeLocalVariables(previousElem, globalVars.size());

    return element;
  }

  private OctState handleExitFromFunction(OctState element,
      CExpression expression,
      CReturnStatementEdge returnEdge)
  throws UnrecognizedCCodeException {
    String tempVarName = getvarName("___cpa_temp_result_var_", returnEdge.getSuccessor().getFunctionName());
    element.declareVariable(tempVarName);
    return handleAssignmentToVariable(element, "___cpa_temp_result_var_", expression, returnEdge);
  }

  private OctState handleFunctionCall(OctState octagonElement,
      OctState pPrevElement, CFunctionCallEdge callEdge, CFAEdge edge)
  throws UnrecognizedCCodeException {

    octagonElement.setPreviousState(pPrevElement);

    CFunctionEntryNode functionEntryNode = callEdge.getSuccessor();
    String calledFunctionName = functionEntryNode.getFunctionName();
    String callerFunctionName = callEdge.getPredecessor().getFunctionName();

    List<String> paramNames = functionEntryNode.getFunctionParameterNames();
    List<CExpression> arguments = callEdge.getArguments();

    assert (paramNames.size() == arguments.size());

    for (int i=0; i<arguments.size(); i++){
      CExpression arg = arguments.get(i);
      if (arg instanceof CCastExpression) {
        // ignore casts
        arg = ((CCastExpression)arg).getOperand();
      }

      String nameOfParam = paramNames.get(i);
      String formalParamName = getvarName(nameOfParam, calledFunctionName);

      declareVariable(octagonElement, formalParamName);

      if(arg instanceof CIdExpression){
        CIdExpression idExp = (CIdExpression) arg;
        String nameOfArg = idExp.getName();
        String actualParamName = getvarName(nameOfArg, callerFunctionName);

        assignVariable(octagonElement, formalParamName, actualParamName, 1);
      }

      else if(arg instanceof CLiteralExpression){
        Long val = parseLiteral((CLiteralExpression)arg, edge);

        if (val != null) {
          octagonElement.assignConstant(formalParamName, val);
        }
      }

      else if(arg instanceof CTypeIdExpression){
        // do nothing
      }

      else if(arg instanceof CUnaryExpression){
        CUnaryExpression unaryExp = (CUnaryExpression) arg;
        assert(unaryExp.getOperator() == UnaryOperator.STAR || unaryExp.getOperator() == UnaryOperator.AMPER);
      }

      else if(arg instanceof CFieldReference){
     // do nothing
      }

      else{
        // TODO forgetting
     // do nothing
        //      throw new ExplicitTransferException("Unhandled case");
      }
    }

    return octagonElement;
  }

  private AbstractState handleAssumption (OctState pElement,
      CExpression expression, CFAEdge cfaEdge, boolean truthValue)
  throws UnrecognizedCCodeException {

    String functionName = cfaEdge.getPredecessor().getFunctionName();
    // Binary operation
    if (expression instanceof CBinaryExpression) {
      CBinaryExpression binExp = ((CBinaryExpression)expression);
      BinaryOperator opType = binExp.getOperator ();

      CExpression op1 = binExp.getOperand1();
      CExpression op2 = binExp.getOperand2();
      return propagateBooleanExpression(pElement, opType, op1, op2, functionName, truthValue, cfaEdge);
    }
    // Unary operation
    else if (expression instanceof CUnaryExpression)
    {
      CUnaryExpression unaryExp = ((CUnaryExpression)expression);
      // ! exp
      if(unaryExp.getOperator() == UnaryOperator.NOT)
      {
        CExpression exp1 = unaryExp.getOperand();
        return handleAssumption(pElement, exp1, cfaEdge, !truthValue);
      }
      else {
        throw new UnrecognizedCCodeException("Unknown unary operator in assumption", cfaEdge, expression);
      }
    }

    else if(expression instanceof CIdExpression
        || expression instanceof CFieldReference){
      return propagateBooleanExpression(pElement, null, expression, null, functionName, truthValue, cfaEdge);
    }

    else if(expression instanceof CCastExpression){
      return handleAssumption(pElement, ((CCastExpression)expression).getOperand(), cfaEdge, truthValue);
    }

    else{
      throw new UnrecognizedCCodeException("Unknown expression type in assumption", cfaEdge, expression);
    }

  }

  private AbstractState propagateBooleanExpression(OctState pElement,
      BinaryOperator opType,CExpression op1,
      CExpression op2, String functionName, boolean truthValue, CFAEdge edge)
  throws UnrecognizedCCodeException {

    // a (bop) ?
    if(op1 instanceof CIdExpression ||
        op1 instanceof CFieldReference ||
        op1 instanceof CArraySubscriptExpression)
    {
      // [literal]
      if(op2 == null && opType == null){
        String varName = op1.toASTString();
        if(truthValue){
          String variableName = getvarName(varName, functionName);
          return addIneqConstraint(pElement, variableName, 0);
        }
        // ! [literal]
        else {
          String variableName = getvarName(varName, functionName);
          return addEqConstraint(pElement, variableName, 0);
        }
      }
      // a (bop) 9
      else if(op2 instanceof CLiteralExpression)
      {
        CLiteralExpression literalExp = (CLiteralExpression)op2;
        String varName = op1.toASTString();
        String variableName = getvarName(varName, functionName);

        if (literalExp instanceof CIntegerLiteralExpression
            || literalExp instanceof CCharLiteralExpression) {
          long valueOfLiteral = parseLiteral(literalExp, edge);
          // a == 9
          if(opType == BinaryOperator.EQUALS) {
            if(truthValue){
              return addEqConstraint(pElement, variableName, valueOfLiteral);
            }
            // ! a == 9
            else {
              return propagateBooleanExpression(pElement, BinaryOperator.NOT_EQUALS, op1, op2, functionName, !truthValue, edge);
            }
          }
          // a != 9
          else if(opType == BinaryOperator.NOT_EQUALS)
          {
            if(truthValue){
              return addIneqConstraint(pElement, variableName, valueOfLiteral);
            }
            // ! a != 9
            else {
              return propagateBooleanExpression(pElement, BinaryOperator.EQUALS, op1, op2, functionName, !truthValue, edge);
            }
          }

          // a > 9
          else if(opType == BinaryOperator.GREATER_THAN)
          {
            if(truthValue){
              return addGreaterConstraint(pElement, variableName, valueOfLiteral);
            }
            else {
              return propagateBooleanExpression(pElement, BinaryOperator.LESS_EQUAL, op1, op2, functionName, !truthValue, edge);
            }
          }
          // a >= 9
          else if(opType == BinaryOperator.GREATER_EQUAL)
          {
            if(truthValue){
              return addGreaterEqConstraint(pElement, variableName, valueOfLiteral);
            }
            else {
              return propagateBooleanExpression(pElement, BinaryOperator.LESS_THAN, op1, op2, functionName, !truthValue, edge);
            }
          }
          // a < 9
          else if(opType == BinaryOperator.LESS_THAN)
          {
            if(truthValue){
              return addSmallerConstraint(pElement, variableName, valueOfLiteral);
            }
            else {
              return propagateBooleanExpression(pElement, BinaryOperator.GREATER_EQUAL, op1, op2, functionName, !truthValue, edge);
            }
          }
          // a <= 9
          else if(opType == BinaryOperator.LESS_EQUAL)
          {
            if(truthValue){
              return addSmallerEqConstraint(pElement, variableName, valueOfLiteral);
            }
            else {
              return propagateBooleanExpression(pElement, BinaryOperator.GREATER_THAN, op1, op2, functionName, !truthValue, edge);
            }
          }
          // [a - 9]
          else if(opType == BinaryOperator.MINUS)
          {
            if(truthValue){
              return addIneqConstraint(pElement, variableName, valueOfLiteral);
            }
            // ! a != 9
            else {
              return propagateBooleanExpression(pElement, BinaryOperator.EQUALS, op1, op2, functionName, !truthValue, edge);
            }
          }

          // [a + 9]
          else if(opType == BinaryOperator.PLUS)
          {
            valueOfLiteral = parseLiteralWithOppositeSign(literalExp, edge);
            if(truthValue){
              return addIneqConstraint(pElement, variableName, valueOfLiteral);
            }
            else {
              valueOfLiteral = parseLiteralWithOppositeSign(literalExp, edge);

              return addEqConstraint(pElement, variableName, valueOfLiteral);
            }
          }

          // TODO nothing
          else if(opType == BinaryOperator.BINARY_AND ||
              opType == BinaryOperator.BINARY_OR ||
              opType == BinaryOperator.BINARY_XOR){
            return pElement;
          }

          else{
            throw new UnrecognizedCCodeException("Unhandled case ", edge);
          }
        }
        else{
          throw new UnrecognizedCCodeException("Unhandled case ", edge);
        }
      }
      // a (bop) b
      else if(op2 instanceof CIdExpression ||
          (op2 instanceof CUnaryExpression && (
              (((CUnaryExpression)op2).getOperator() == UnaryOperator.AMPER) ||
              (((CUnaryExpression)op2).getOperator() == UnaryOperator.STAR))))
      {
        String leftVarName = op1.toASTString();
        String rightVarName = op2.toASTString();

        String leftVariableName = getvarName(leftVarName, functionName);
        String rightVariableName = getvarName(rightVarName, functionName);

        // a == b
        if(opType == BinaryOperator.EQUALS)
        {
          if(truthValue){
            return addEqConstraint(pElement, rightVariableName, leftVariableName);
          }
          else{
            return propagateBooleanExpression(pElement, BinaryOperator.NOT_EQUALS, op1, op2, functionName, !truthValue, edge);
          }
        }
        // a != b
        else if(opType == BinaryOperator.NOT_EQUALS)
        {
          if(truthValue){
            return addIneqConstraint(pElement, rightVariableName, leftVariableName);
          }
          else{
            return propagateBooleanExpression(pElement, BinaryOperator.EQUALS, op1, op2, functionName, !truthValue, edge);
          }
        }
        // a > b
        else if(opType == BinaryOperator.GREATER_THAN)
        {
          if(truthValue){
            return addGreaterConstraint(pElement, rightVariableName, leftVariableName);
          }
          else{
            return  propagateBooleanExpression(pElement, BinaryOperator.LESS_EQUAL, op1, op2, functionName, !truthValue, edge);
          }
        }
        // a >= b
        else if(opType == BinaryOperator.GREATER_EQUAL)
        {
          if(truthValue){
            return addGreaterEqConstraint(pElement, rightVariableName, leftVariableName);
          }
          else{
            return propagateBooleanExpression(pElement, BinaryOperator.LESS_THAN, op1, op2, functionName, !truthValue, edge);
          }
        }
        // a < b
        else if(opType == BinaryOperator.LESS_THAN)
        {
          if(truthValue){
            return addSmallerConstraint(pElement, rightVariableName, leftVariableName);
          }
          else{
            return propagateBooleanExpression(pElement, BinaryOperator.GREATER_EQUAL, op1, op2, functionName, !truthValue, edge);
          }
        }
        // a <= b
        else if(opType == BinaryOperator.LESS_EQUAL)
        {
          if(truthValue){
            return addSmallerEqConstraint(pElement, rightVariableName, leftVariableName);
          }
          else{
            return propagateBooleanExpression(pElement, BinaryOperator.GREATER_THAN, op1, op2, functionName, !truthValue, edge);
          }
        }
        else{
          throw new UnrecognizedCCodeException("Unhandled case ", edge);
        }
      }
      else if(op2 instanceof CUnaryExpression)
      {
        String varName = op1.toASTString();

        CUnaryExpression unaryExp = (CUnaryExpression)op2;
        CExpression unaryExpOp = unaryExp.getOperand();

        UnaryOperator operatorType = unaryExp.getOperator();
        // a == -8
        if(operatorType == UnaryOperator.MINUS){

          if(unaryExpOp instanceof CLiteralExpression){
            CLiteralExpression literalExp = (CLiteralExpression)unaryExpOp;

            if (literalExp instanceof CIntegerLiteralExpression
                || literalExp instanceof CCharLiteralExpression) {
              long valueOfLiteral = parseLiteralWithOppositeSign(literalExp, edge);
              String variableName = getvarName(varName, functionName);

              // a == 9
              if(opType == BinaryOperator.EQUALS) {
                if(truthValue){
                  return addEqConstraint(pElement, variableName, valueOfLiteral);
                }
                // ! a == 9
                else {
                  return propagateBooleanExpression(pElement, BinaryOperator.NOT_EQUALS, op1, op2, functionName, !truthValue, edge);
                }
              }
              // a != 9
              else if(opType == BinaryOperator.NOT_EQUALS)
              {
                if(truthValue){
                  return addIneqConstraint(pElement, variableName, valueOfLiteral);
                }
                // ! a != 9
                else {
                  return propagateBooleanExpression(pElement, BinaryOperator.EQUALS, op1, op2, functionName, !truthValue, edge);
                }
              }

              // a > 9
              else if(opType == BinaryOperator.GREATER_THAN)
              {
                if(truthValue){
                  return addGreaterConstraint(pElement, variableName, valueOfLiteral);
                }
                else {
                  return propagateBooleanExpression(pElement, BinaryOperator.LESS_EQUAL, op1, op2, functionName, !truthValue, edge);
                }
              }
              // a >= 9
              else if(opType == BinaryOperator.GREATER_EQUAL)
              {
                if(truthValue){
                  return addGreaterEqConstraint(pElement, variableName, valueOfLiteral);
                }
                else {
                  return propagateBooleanExpression(pElement, BinaryOperator.LESS_THAN, op1, op2, functionName, !truthValue, edge);
                }
              }
              // a < 9
              else if(opType == BinaryOperator.LESS_THAN)
              {
                if(truthValue){
                  return addSmallerConstraint(pElement, variableName, valueOfLiteral);
                }
                else {
                  return propagateBooleanExpression(pElement, BinaryOperator.GREATER_EQUAL, op1, op2, functionName, !truthValue, edge);
                }
              }
              // a <= 9
              else if(opType == BinaryOperator.LESS_EQUAL)
              {
                if(truthValue){
                  return addSmallerEqConstraint(pElement, variableName, valueOfLiteral);
                }
                else {
                  return propagateBooleanExpression(pElement, BinaryOperator.GREATER_THAN, op1, op2, functionName, !truthValue, edge);
                }
              }
              else{
                throw new UnrecognizedCCodeException("Unhandled case ", edge);
              }
            }
            else{
              throw new UnrecognizedCCodeException("Unhandled case ", edge);
            }
          }
          else{
            throw new UnrecognizedCCodeException("Unhandled case ", edge);
          }
        }
        else{
          throw new UnrecognizedCCodeException("Unhandled case ", edge);
        }
      }
      else if(op2 instanceof CBinaryExpression){
        String varName = op1.toASTString();
        String variableName = getvarName(varName, functionName);
        return forgetState(pElement, variableName);
      }
      // right hand side is a cast exp
      else if(op2 instanceof CCastExpression){
        CCastExpression castExp = (CCastExpression)op2;
        CExpression exprInCastOp = castExp.getOperand();
        return propagateBooleanExpression(pElement, opType, op1, exprInCastOp, functionName, truthValue, edge);
      }
      else{
        String varName = op1.toASTString();
        String variableName = getvarName(varName, functionName);
        return forgetState(pElement, variableName);
      }
    }
    else if(op1 instanceof CCastExpression){
      CCastExpression castExp = (CCastExpression) op1;
      CExpression castOperand = castExp.getOperand();
      return propagateBooleanExpression(pElement, opType, castOperand, op2, functionName, truthValue, edge);
    }
    else{
      String varName = op1.toASTString();
      String variableName = getvarName(varName, functionName);
      return forgetState(pElement, variableName);
    }
  }


  private AbstractState forgetState(OctState pElement,
      String pVariableName) {
    pElement.forget(pVariableName);
    return pElement;
  }

  private AbstractState addSmallerEqConstraint(OctState pElement,
      String pRightVariableName, String pLeftVariableName) {
    int rVarIdx = pElement.getVariableIndexFor(pRightVariableName);
    int lVarIdx = pElement.getVariableIndexFor(pLeftVariableName);
    pElement.addConstraint(3, lVarIdx, rVarIdx, 0);
    return pElement;
  }

  // Note that this only works if both variables are integers
  private AbstractState addSmallerConstraint(OctState pElement,
      String pRightVariableName, String pLeftVariableName) {
    int rVarIdx = pElement.getVariableIndexFor(pRightVariableName);
    int lVarIdx = pElement.getVariableIndexFor(pLeftVariableName);
    pElement.addConstraint(3, lVarIdx, rVarIdx, -1);
    return pElement;
  }


  private AbstractState addGreaterEqConstraint(OctState pElement,
      String pRightVariableName, String pLeftVariableName) {
    int rVarIdx = pElement.getVariableIndexFor(pRightVariableName);
    int lVarIdx = pElement.getVariableIndexFor(pLeftVariableName);
    pElement.addConstraint(4, lVarIdx, rVarIdx, 0);
    return pElement;
  }

  // Note that this only works if both variables are integers
  private AbstractState addGreaterConstraint(OctState pElement,
      String pRightVariableName, String pLeftVariableName) {
    int rVarIdx = pElement.getVariableIndexFor(pRightVariableName);
    int lVarIdx = pElement.getVariableIndexFor(pLeftVariableName);
    pElement.addConstraint(4, lVarIdx, rVarIdx, -1);
    return pElement;
  }

  // Note that this only works if both variables are integers
  private AbstractState addIneqConstraint(OctState pElement,
      String pRightVariableName, String pLeftVariableName) {
    OctState newElem1 = null;
    newElem1 = pElement.clone();
    addEqConstraint(newElem1, pLeftVariableName, pRightVariableName);
    if(! newElem1.isEmpty()){
      return null;
    }
    else{
      return pElement;
    }
  }

  private AbstractState addEqConstraint(OctState pElement,
      String pRightVariableName, String pLeftVariableName) {
//    addSmallerEqConstraint(pElement, pRightVariableName, pLeftVariableName);
//    addGreaterEqConstraint(pElement, pRightVariableName, pLeftVariableName);
//    return pElement;

    OctState newElem1 = null;
    newElem1 = pElement.clone();
    addSmallerEqConstraint(pElement, pRightVariableName, pLeftVariableName);
    addGreaterEqConstraint(pElement, pRightVariableName, pLeftVariableName);
    if(newElem1.isEmpty()){
      return null;
    }
    else{
      return assignVariable(pElement, pLeftVariableName, pRightVariableName, 1);
    }
  }

  private AbstractState addSmallerEqConstraint(OctState pElement,
      String pVariableName, long pValueOfLiteral) {
    int varIdx = pElement.getVariableIndexFor(pVariableName);
    pElement.addConstraint(0, varIdx, 0, (int)pValueOfLiteral);
    return pElement;
  }

  // Note that this only works if both variables are integers
  private AbstractState addSmallerConstraint(OctState pElement,
      String pVariableName, long pValueOfLiteral) {
    int varIdx = pElement.getVariableIndexFor(pVariableName);
    pElement.addConstraint(0, varIdx, -1, (int)pValueOfLiteral-1);
    return pElement;
  }

  private AbstractState addGreaterEqConstraint(OctState pElement,
      String pVariableName, long pValueOfLiteral) {
    int varIdx = pElement.getVariableIndexFor(pVariableName);
    pElement.addConstraint(1, varIdx, 0, (0 - (int)pValueOfLiteral));
    return pElement;
  }

  // Note that this only works if both variables are integers
  private AbstractState addGreaterConstraint(OctState pElement,
      String pVariableName, long pValueOfLiteral) {
    int varIdx = pElement.getVariableIndexFor(pVariableName);
    pElement.addConstraint(1, varIdx, 0, (-1 - (int)pValueOfLiteral));
    return pElement;
  }

  private AbstractState addEqConstraint(OctState pElement,
      String pVariableName, long pI) {
//    addGreaterEqConstraint(pElement, pVariableName, pI);
//    addSmallerEqConstraint(pElement, pVariableName, pI);
//    return pElement;

    OctState newElem1 = null;
    newElem1 = pElement.clone();
    addSmallerEqConstraint(pElement, pVariableName, pI);
    addGreaterEqConstraint(pElement, pVariableName, pI);
    if(newElem1.isEmpty()){
      return null;
    }
    else{
      return assignConstant(pElement, pVariableName, pI);
    }

  }

  // Note that this only works if both variables are integers
  private AbstractState addIneqConstraint(OctState pElement,
      String pVariableName, long pI) {
    OctState newElem1 = null;
    newElem1 = pElement.clone();
    addEqConstraint(newElem1, pVariableName, pI);
    if(! newElem1.isEmpty()){
      return pElement;
    }
    else{
      return pElement;
    }
  }

  private OctState handleDeclaration(OctState pElement,
      CDeclarationEdge declarationEdge) throws UnrecognizedCCodeException {

    if (declarationEdge.getDeclaration() instanceof CVariableDeclaration) {
      CVariableDeclaration decl = (CVariableDeclaration)declarationEdge.getDeclaration();

      // get the variable name in the declarator
      String varName = decl.getName();

      // TODO check other types of variables later - just handle primitive
      // types for the moment
      // don't add pointer variables to the list since we don't track them
      if (decl.getDeclSpecifier() instanceof CPointerType) {
        return pElement;
      }
      // if this is a global variable, add to the list of global variables
      if(decl.isGlobal())
      {
        globalVars.add(varName);

        Long v;

        CInitializer init = decl.getInitializer();
        if (init != null) {
          if (init instanceof CInitializerExpression) {
            CExpression exp = ((CInitializerExpression)init).getExpression();

            v = getExpressionValue(pElement, exp, varName, declarationEdge);
          } else {
            // TODO show warning
            v = null;
          }
        } else {
          // global variables without initializer are set to 0 in C
          v = 0L;
        }

        String variableName = getvarName(varName, declarationEdge.getPredecessor().getFunctionName());
        declareVariable(pElement, variableName);

        if (v != null) {
          return assignConstant(pElement, variableName, v.longValue());
        }
      }
      else{
        String variableName = getvarName(varName, declarationEdge.getPredecessor().getFunctionName());
        return declareVariable(pElement, variableName);
      }
    }
    assert(false);
    return null;
  }

  private OctState declareVariable(OctState pElement, String pVariableName) {
    pElement.declareVariable(pVariableName);
    return pElement;
  }

  private OctState assignConstant(OctState pElement, String pVarName,
      long pLongValue) {
    pElement.assignConstant(pVarName, pLongValue);
    return pElement;
  }

  private OctState handleStatement(OctState pElement,
      CStatement expression, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException {
    // expression is a binary operation, e.g. a = b;
    if (expression instanceof CAssignment) {
      return handleAssignment(pElement, (CAssignment)expression, cfaEdge);
    }
    // external function call
    else if(expression instanceof CFunctionCallStatement){
      // do nothing
    }
    // there is such a case
    else if(expression instanceof CExpressionStatement){
      // do nothing
    }
    else{
      throw new UnrecognizedCCodeException(cfaEdge, expression);
    }
    assert(false);
    return null;
  }

  private OctState handleAssignment(OctState pElement,
      CAssignment assignExpression, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException {

    CExpression op1 = assignExpression.getLeftHandSide();
    CRightHandSide op2 = assignExpression.getRightHandSide();

    if(op1 instanceof CIdExpression) {
      // a = ...
      return handleAssignmentToVariable(pElement, ((CIdExpression)op1).getName(), op2, cfaEdge);

    } else if (op1 instanceof CUnaryExpression
        && ((CUnaryExpression)op1).getOperator() == UnaryOperator.STAR) {
      // *a = ...

      op1 = ((CUnaryExpression)op1).getOperand();

      // Cil produces code like
      // *((int*)__cil_tmp5) = 1;
      // so remove cast
      if (op1 instanceof CCastExpression) {
        op1 = ((CCastExpression)op1).getOperand();
      }

      if (op1 instanceof CIdExpression) {
//        missingInformationLeftPointer = op1.getRawSignature();
//        missingInformationRightExpression = op2;

      } else {
        throw new UnrecognizedCCodeException("left operand of assignment has to be a variable", cfaEdge, op1);
      }
            return pElement;

    } else if (op1 instanceof CFieldReference) {
      // TODO assignment to field
            return pElement;

    } else if (op1 instanceof CArraySubscriptExpression) {
      // TODO assignment to array cell
            return pElement;

    } else {
      throw new UnrecognizedCCodeException("left operand of assignment has to be a variable", cfaEdge, op1);
    }
  }

  private OctState handleAssignmentToVariable(OctState pElement,
      String lParam, CRightHandSide rightExp, CFAEdge cfaEdge) throws UnrecognizedCCodeException {
    String functionName = cfaEdge.getPredecessor().getFunctionName();

    // a = 8.2 or "return;" (when rightExp == null)
    if(rightExp == null || rightExp instanceof CLiteralExpression){
      return handleAssignmentOfLiteral(pElement, lParam, (CLiteralExpression)rightExp, functionName, cfaEdge);
    }
    // a = b
    else if (rightExp instanceof CIdExpression){
      return handleAssignmentOfVariable(pElement, lParam, (CIdExpression)rightExp, functionName, 1);
    }
    // a = (cast) ?
    else if(rightExp instanceof CCastExpression) {
      return handleAssignmentOfCast(pElement, lParam, (CCastExpression)rightExp, cfaEdge);
    }
    // a = -b
    else if(rightExp instanceof CUnaryExpression){
      return handleAssignmentOfUnaryExp(pElement, lParam, (CUnaryExpression)rightExp, cfaEdge);
    }
    // a = b op c
    else if(rightExp instanceof CBinaryExpression){
      CBinaryExpression binExp = (CBinaryExpression)rightExp;

      return handleAssignmentOfBinaryExp(pElement, lParam, binExp.getOperand1(),
          binExp.getOperand2(), binExp.getOperator(), cfaEdge);
    }
    // a = extCall();  or  a = b->c;
    else if(rightExp instanceof CFunctionCallExpression
        || rightExp instanceof CFieldReference){
      //      OctState newElement = element.clone();
      String lvarName = getvarName(lParam, functionName);
      return forget(pElement, lvarName);
    }
    else{
      throw new UnrecognizedCCodeException(cfaEdge, rightExp);
    }
  }

  private OctState forget(OctState pElement, String pLvarName) {
    pElement.forget(pLvarName);
    return pElement;
  }

  private OctState handleAssignmentOfCast(OctState pElement,
      String lParam, CCastExpression castExp, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException
  {
    CExpression castOperand = castExp.getOperand();
    return handleAssignmentToVariable(pElement, lParam, castOperand, cfaEdge);
  }

  private OctState handleAssignmentOfUnaryExp(OctState pElement,
      String lParam, CUnaryExpression unaryExp, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException {

    String functionName = cfaEdge.getPredecessor().getFunctionName();
    // name of the updated variable, so if a = -b is handled, lParam is a
    String assignedVar = getvarName(lParam, functionName);
    //    OctState newElement = element.clone();

    CExpression unaryOperand = unaryExp.getOperand();
    UnaryOperator unaryOperator = unaryExp.getOperator();

    if (unaryOperator == UnaryOperator.STAR) {
      // a = * b
      // TODO what is this?
//      OctState newElement = forget(pElement, assignedVar);

      // Cil produces code like
      // __cil_tmp8 = *((int *)__cil_tmp7);
      // so remove cast
      if (unaryOperand instanceof CCastExpression) {
        unaryOperand = ((CCastExpression)unaryOperand).getOperand();
      }

      if (unaryOperand instanceof CIdExpression) {
//        missingInformationLeftVariable = assignedVar;
//        missingInformationRightPointer = unaryOperand.getRawSignature();
      } else{
        throw new UnrecognizedCCodeException(cfaEdge, unaryOperand);
      }

    }
    else {
      // a = -b or similar
      Long value = getExpressionValue(pElement, unaryExp, functionName, cfaEdge);
      if (value != null) {
        return assignConstant(pElement, assignedVar, value);
      } else {
        String rVarName = unaryOperand.toASTString();
        return assignVariable(pElement, assignedVar, rVarName, -1);
      }
    }

    //TODO ?
    return null;
  }

  private OctState handleAssignmentOfBinaryExp(OctState pElement,
      String lParam, CExpression lVarInBinaryExp, CExpression rVarInBinaryExp,
      BinaryOperator binaryOperator, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException {

    String functionName = cfaEdge.getPredecessor().getFunctionName();
    // name of the updated variable, so if a = b + c is handled, lParam is a
    String assignedVar = getvarName(lParam, functionName);
    //    OctState newElement = element.clone();

    switch (binaryOperator) {
    case DIVIDE:
    case MODULO:
    case LESS_EQUAL:
    case GREATER_EQUAL:
    case BINARY_AND:
    case BINARY_OR:
      // TODO check which cases can be handled (I think all)
      return forget(pElement, assignedVar);

    case PLUS:
    case MINUS:
    case MULTIPLY:

      Long val1;
      Long val2;

      val1 = getExpressionValue(pElement, lVarInBinaryExp, functionName, cfaEdge);
      val2 = getExpressionValue(pElement, rVarInBinaryExp, functionName, cfaEdge);

      if(val1 != null && val2 != null){
        long value;
        switch (binaryOperator) {

        case PLUS:
          value = val1 + val2;
          break;

        case MINUS:
          value = val1 - val2;
          break;

        case MULTIPLY:
          value = val1 * val2;
          break;

        default:
          throw new UnrecognizedCCodeException("unkown binary operator", cfaEdge);
        }
        return assignConstant(pElement, assignedVar, value);
      }

      int lVarCoef = 0;
      int rVarCoef = 0;
      int constVal = 0;

      String lVarName = null;
      String rVarName = null;

      if(val1 == null && val2 != null){
        if(lVarInBinaryExp instanceof CIdExpression){
          lVarName = ((CIdExpression)lVarInBinaryExp).getName();

          switch (binaryOperator) {

          case PLUS:
            constVal = val2.intValue();
            lVarCoef = 1;
            rVarCoef = 0;
            break;

          case MINUS:
            constVal = 0 - val2.intValue();
            lVarCoef = 1;
            rVarCoef = 0;
            break;

          case MULTIPLY:
            lVarCoef = val2.intValue();
            rVarCoef = 0;
            constVal = 0;
            break;

          default:
            throw new UnrecognizedCCodeException("unkown binary operator", cfaEdge);
          }
        }
        else{
          throw new UnrecognizedCCodeException("unkown binary operator", cfaEdge);
        }
      }

      else if(val1 != null && val2 == null){
        if(lVarInBinaryExp instanceof CIdExpression){
          rVarName = ((CIdExpression)rVarInBinaryExp).getName();

          switch (binaryOperator) {

          case PLUS:
            constVal = val1.intValue();
            lVarCoef = 0;
            rVarCoef = 1;
            break;

          case MINUS:
            constVal = val1.intValue();
            lVarCoef = 0;
            rVarCoef = -1;
            break;

          case MULTIPLY:
            rVarCoef = val1.intValue();
            lVarCoef = 0;
            constVal = 0;
            break;

          default:
            throw new UnrecognizedCCodeException("unkown binary operator", cfaEdge);
          }
        }
        else{
          throw new UnrecognizedCCodeException("unkown binary operator", cfaEdge);
        }
      }

      else if(val1 == null && val2 == null){
        if(lVarInBinaryExp instanceof CIdExpression){
          lVarName = ((CIdExpression)lVarInBinaryExp).getName();
          rVarName = ((CIdExpression)rVarInBinaryExp).getName();

          switch (binaryOperator) {

          case PLUS:
            lVarCoef = 1;
            rVarCoef = 1;
            break;

          case MINUS:
            lVarCoef = 1;
            rVarCoef = -1;
            break;

          case MULTIPLY:
            return forget(pElement, assignedVar);

          default:
            throw new UnrecognizedCCodeException("unkown binary operator", cfaEdge);
          }
        }
        else{
          throw new UnrecognizedCCodeException("unkown binary operator", cfaEdge);
        }
      }

      return assignmentOfBinaryExp(pElement, assignedVar, getvarName(lVarName, functionName), lVarCoef, getvarName(rVarName, functionName), rVarCoef, constVal);

    case EQUALS:
    case NOT_EQUALS:

      Long lVal = getExpressionValue(pElement, lVarInBinaryExp, functionName, cfaEdge);
      Long rVal = getExpressionValue(pElement, rVarInBinaryExp, functionName, cfaEdge);

      // TODO handle more cases later

      if (lVal == null || rVal == null) {
        return forget(pElement, assignedVar);

      } else {
        // assign 1 if expression holds, 0 otherwise
        long result = (lVal.equals(rVal) ? 1 : 0);

        if (binaryOperator == BinaryOperator.NOT_EQUALS) {
          // negate
          result = 1 - result;
        }
        return assignConstant(pElement, assignedVar, result);
      }
      //      break;

    default:
      // TODO warning
      return forget(pElement, assignedVar);
    }
    // TODO ?
    //    return null;
  }

  //  // TODO modify this.
  private Long getExpressionValue(OctState pElement, CRightHandSide expression,
      String functionName, CFAEdge cfaEdge) throws UnrecognizedCCodeException {

    if (expression instanceof CLiteralExpression) {
      return parseLiteral((CLiteralExpression)expression, cfaEdge);

    } else if (expression instanceof CIdExpression) {
      return null;
    } else if (expression instanceof CCastExpression) {
      return getExpressionValue(pElement, ((CCastExpression)expression).getOperand(),
          functionName, cfaEdge);

    } else if (expression instanceof CUnaryExpression) {
      CUnaryExpression unaryExpression = (CUnaryExpression)expression;
      UnaryOperator unaryOperator = unaryExpression.getOperator();
      CExpression unaryOperand = unaryExpression.getOperand();

      switch (unaryOperator) {

      case MINUS:
        Long val = getExpressionValue(pElement, unaryOperand, functionName, cfaEdge);
        return (val != null) ? -val : null;

      case AMPER:
        return null; // valid expresion, but it's a pointer value

      default:
        throw new UnrecognizedCCodeException("unknown unary operator", cfaEdge, unaryExpression);
      }
    } else {
      // TODO fields, arrays
      throw new UnrecognizedCCodeException(cfaEdge, expression);
    }
  }

  private OctState assignmentOfBinaryExp(OctState pElement,
      String pAssignedVar, String pLeftVarName, int pLeftVarCoef,
      String pRightVarName, int pRightVarCoef, int pConstVal) {
    pElement.assignmentOfBinaryExp(pAssignedVar, pLeftVarName, pLeftVarCoef,
        pRightVarName, pRightVarCoef, pConstVal);
    return pElement;
  }

  private OctState handleAssignmentOfVariable(OctState pElement,
      String lParam, CExpression op2, String functionName, int coef)
  {
    String rParam = op2.toASTString();

    String leftVarName = getvarName(lParam, functionName);
    String rightVarName = getvarName(rParam, functionName);

    return assignVariable(pElement, leftVarName, rightVarName, coef);
  }

//  private OctState handleAssignmentOfReturnVariable(OctState pElement,
//      String lParam, String tempVarName, String functionName, int coef)
//  {
//    String leftVarName = getvarName(lParam, functionName);
//
//    return assignVariable(pElement, leftVarName, tempVarName, coef);
//  }

  private OctState assignVariable(OctState pElement, String pLeftVarName,
      String pRightVarName, int coef) {
    pElement.assignVariable(pLeftVarName, pRightVarName, coef);
    return pElement;
  }

  private OctState handleAssignmentOfLiteral(OctState pElement,
      String lParam, CLiteralExpression op2, String functionName, CFAEdge edge)
  throws UnrecognizedCCodeException
  {
    //    OctState newElement = element.clone();

    // op2 may be null if this is a "return;" statement
    Long val = (op2 == null ? Long.valueOf(0L) : parseLiteral(op2, edge));

    String assignedVar = getvarName(lParam, functionName);
    if (val != null) {
      return assignConstant(pElement, assignedVar, val);
    } else {
      return forget(pElement, assignedVar);
    }
    //TODO
    //    return null;
  }

  private Long parseLiteral(CLiteralExpression expression, CFAEdge edge) throws UnrecognizedCCodeException {
    if (expression instanceof CIntegerLiteralExpression) {
      return ((CIntegerLiteralExpression)expression).asLong();

    } else if (expression instanceof CFloatLiteralExpression) {
      return null;

    } else if (expression instanceof CCharLiteralExpression) {
      return (long)((CCharLiteralExpression)expression).getCharacter();

    } else if (expression instanceof CStringLiteralExpression) {
      return null;

    } else {
      throw new UnrecognizedCCodeException("unknown literal", edge, expression);
    }
  }

  private Long parseLiteralWithOppositeSign(CLiteralExpression expression, CFAEdge edge) throws UnrecognizedCCodeException {
    Long value = parseLiteral(expression, edge);
    if (value != null) {
      value = -value;
    }
    return value;
  }

  public String getvarName(String variableName, String functionName){
    if(variableName == null){
      return null;
    }
    if(globalVars.contains(variableName)){
      return variableName;
    }
    return functionName + "::" + variableName;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState element,
      List<AbstractState> otherElements, CFAEdge cfaEdge,
      Precision precision) {

    assert element instanceof OctState;
    OctState octagonElement = (OctState)element;

    for (AbstractState ae : otherElements) {
      if (ae instanceof PointerState) {
        return strengthen(octagonElement, (PointerState)ae, cfaEdge, precision);
      }
      else if(ae instanceof AssumptionStorageState){
        return strengthen(octagonElement, (AssumptionStorageState)ae, cfaEdge, precision);
      }
    }
    return null;


  }

  private Collection<? extends AbstractState> strengthen(
      OctState pOctagonElement, AssumptionStorageState pAe,
      CFAEdge pCfaEdge, Precision pPrecision) {
    // TODO Auto-generated method stub
    return null;
  }

  private Collection<? extends AbstractState> strengthen(
      OctState pOctagonElement, PointerState pAe, CFAEdge pCfaEdge,
      Precision pPrecision) {
    // TODO Auto-generated method stub
    return null;
  }
}
