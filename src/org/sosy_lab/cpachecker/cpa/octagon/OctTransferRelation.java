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

import org.sosy_lab.cpachecker.cfa.ast.IASTArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTAssignment;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.IASTCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.IASTFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTInitializer;
import org.sosy_lab.cpachecker.cfa.ast.IASTInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTPointerTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.IASTStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CallToReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.ReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageElement;
import org.sosy_lab.cpachecker.cpa.pointer.PointerElement;
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
//  private IASTExpression missingInformationRightExpression = null;

  /**
   * Class constructor.
   */
  public OctTransferRelation ()
  {
    globalVars = new ArrayList<String>();
  }

  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors (AbstractElement element, Precision prec, CFAEdge cfaEdge) throws UnrecognizedCCodeException
  {

    // octElement is the region of the current state
    // this state will be updated using the edge

    OctElement octElement = null;
    OctElement prevElement = (OctElement)element;
    octElement = ((OctElement)element).clone();

    assert(octElement != null);

    // check the type of the edge
    switch (cfaEdge.getEdgeType ())
    {

    // if edge is a statement edge, e.g. a = b + c
    case StatementEdge:
    {
      StatementEdge statementEdge = (StatementEdge) cfaEdge;
      IASTStatement expression = statementEdge.getStatement();
      octElement = handleStatement (octElement, expression, cfaEdge);
      break;
    }

    case ReturnStatementEdge:
    {
      ReturnStatementEdge statementEdge = (ReturnStatementEdge) cfaEdge;
      // this statement is a function return, e.g. return (a);
      // note that this is different from return edge
      // this is a statement edge which leads the function to the
      // last node of its CFA, where return edge is from that last node
      // to the return site of the caller function
      octElement = handleExitFromFunction(octElement, statementEdge.getExpression(), statementEdge);
      break;
    }

    // edge is a decleration edge, e.g. int a;
    case DeclarationEdge:
    {
      DeclarationEdge declarationEdge = (DeclarationEdge) cfaEdge;
      octElement = handleDeclaration (octElement, declarationEdge);
      break;
    }

    // this is an assumption, e.g. if(a == b)
    case AssumeEdge:
    {
      AssumeEdge assumeEdge = (AssumeEdge) cfaEdge;
      IASTExpression expression = assumeEdge.getExpression();
      octElement = (OctElement)handleAssumption (octElement, expression, cfaEdge, assumeEdge.getTruthAssumption());
      break;

    }

    case BlankEdge:
    {
      break;
    }

    case FunctionCallEdge:
    {
      FunctionCallEdge functionCallEdge = (FunctionCallEdge) cfaEdge;
      octElement = handleFunctionCall(octElement, prevElement, functionCallEdge, cfaEdge);
      break;
    }

    // this is a return edge from function, this is different from return statement
    // of the function. See case for statement edge for details
    case FunctionReturnEdge:
    {
      FunctionReturnEdge functionReturnEdge = (FunctionReturnEdge) cfaEdge;
      octElement = handleFunctionReturn(octElement, functionReturnEdge);
      break;
    }

    // Summary edge, we handle this on function return, do nothing
    case CallToReturnEdge:
    {
      assert(false);
      break;
    }
    }

    if (octElement == null || octElement.isEmpty()) {
      return Collections.emptySet();
    }

    return Collections.singleton(octElement);
  }

  /**
   * Handles return from one function to another function.
   * @param element previous abstract element.
   * @param functionReturnEdge return edge from a function to its call site.
   * @return new abstract element.
   */
  private OctElement handleFunctionReturn(OctElement element,
      FunctionReturnEdge functionReturnEdge)
  throws UnrecognizedCCodeException {

    CallToReturnEdge summaryEdge =
      functionReturnEdge.getSuccessor().getEnteringSummaryEdge();
    IASTFunctionCall exprOnSummary = summaryEdge.getExpression();

    OctElement previousElem = element.getPreviousElement();

    String callerFunctionName = functionReturnEdge.getSuccessor().getFunctionName();
    String calledFunctionName = functionReturnEdge.getPredecessor().getFunctionName();

    //expression is an assignment operation, e.g. a = g(b);
    if (exprOnSummary instanceof IASTFunctionCallAssignmentStatement) {
      IASTFunctionCallAssignmentStatement binExp = ((IASTFunctionCallAssignmentStatement)exprOnSummary);
      IASTExpression op1 = binExp.getLeftHandSide();

      //we expect left hand side of the expression to be a variable
      if(op1 instanceof IASTIdExpression ||
          op1 instanceof IASTFieldReference)
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
    else if (exprOnSummary instanceof IASTFunctionCallStatement)
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

  private OctElement handleExitFromFunction(OctElement element,
      IASTExpression expression,
      ReturnStatementEdge returnEdge)
  throws UnrecognizedCCodeException {
    String tempVarName = getvarName("___cpa_temp_result_var_", returnEdge.getSuccessor().getFunctionName());
    element.declareVariable(tempVarName);
    return handleAssignmentToVariable(element, "___cpa_temp_result_var_", expression, returnEdge);
  }

  private OctElement handleFunctionCall(OctElement octagonElement,
      OctElement pPrevElement, FunctionCallEdge callEdge, CFAEdge edge)
  throws UnrecognizedCCodeException {

    octagonElement.setPreviousElement(pPrevElement);

    FunctionDefinitionNode functionEntryNode = callEdge.getSuccessor();
    String calledFunctionName = functionEntryNode.getFunctionName();
    String callerFunctionName = callEdge.getPredecessor().getFunctionName();

    List<String> paramNames = functionEntryNode.getFunctionParameterNames();
    List<IASTExpression> arguments = callEdge.getArguments();

    assert (paramNames.size() == arguments.size());

    for (int i=0; i<arguments.size(); i++){
      IASTExpression arg = arguments.get(i);
      if (arg instanceof IASTCastExpression) {
        // ignore casts
        arg = ((IASTCastExpression)arg).getOperand();
      }

      String nameOfParam = paramNames.get(i);
      String formalParamName = getvarName(nameOfParam, calledFunctionName);

      declareVariable(octagonElement, formalParamName);

      if(arg instanceof IASTIdExpression){
        IASTIdExpression idExp = (IASTIdExpression) arg;
        String nameOfArg = idExp.getName();
        String actualParamName = getvarName(nameOfArg, callerFunctionName);

        assignVariable(octagonElement, formalParamName, actualParamName, 1);
      }

      else if(arg instanceof IASTLiteralExpression){
        Long val = parseLiteral((IASTLiteralExpression)arg, edge);

        if (val != null) {
          octagonElement.assignConstant(formalParamName, val);
        }
      }

      else if(arg instanceof IASTTypeIdExpression){
        // do nothing
      }

      else if(arg instanceof IASTUnaryExpression){
        IASTUnaryExpression unaryExp = (IASTUnaryExpression) arg;
        assert(unaryExp.getOperator() == UnaryOperator.STAR || unaryExp.getOperator() == UnaryOperator.AMPER);
      }

      else if(arg instanceof IASTFieldReference){
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

  private AbstractElement handleAssumption (OctElement pElement,
      IASTExpression expression, CFAEdge cfaEdge, boolean truthValue)
  throws UnrecognizedCCodeException {

    String functionName = cfaEdge.getPredecessor().getFunctionName();
    // Binary operation
    if (expression instanceof IASTBinaryExpression) {
      IASTBinaryExpression binExp = ((IASTBinaryExpression)expression);
      BinaryOperator opType = binExp.getOperator ();

      IASTExpression op1 = binExp.getOperand1();
      IASTExpression op2 = binExp.getOperand2();
      return propagateBooleanExpression(pElement, opType, op1, op2, functionName, truthValue, cfaEdge);
    }
    // Unary operation
    else if (expression instanceof IASTUnaryExpression)
    {
      IASTUnaryExpression unaryExp = ((IASTUnaryExpression)expression);
      // ! exp
      if(unaryExp.getOperator() == UnaryOperator.NOT)
      {
        IASTExpression exp1 = unaryExp.getOperand();
        return handleAssumption(pElement, exp1, cfaEdge, !truthValue);
      }
      else {
        throw new UnrecognizedCCodeException("Unknown unary operator in assumption", cfaEdge, expression);
      }
    }

    else if(expression instanceof IASTIdExpression
        || expression instanceof IASTFieldReference){
      return propagateBooleanExpression(pElement, null, expression, null, functionName, truthValue, cfaEdge);
    }

    else if(expression instanceof IASTCastExpression){
      return handleAssumption(pElement, ((IASTCastExpression)expression).getOperand(), cfaEdge, truthValue);
    }

    else{
      throw new UnrecognizedCCodeException("Unknown expression type in assumption", cfaEdge, expression);
    }

  }

  private AbstractElement propagateBooleanExpression(OctElement pElement,
      BinaryOperator opType,IASTExpression op1,
      IASTExpression op2, String functionName, boolean truthValue, CFAEdge edge)
  throws UnrecognizedCCodeException {

    // a (bop) ?
    if(op1 instanceof IASTIdExpression ||
        op1 instanceof IASTFieldReference ||
        op1 instanceof IASTArraySubscriptExpression)
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
      else if(op2 instanceof IASTLiteralExpression)
      {
        IASTLiteralExpression literalExp = (IASTLiteralExpression)op2;
        String varName = op1.toASTString();
        String variableName = getvarName(varName, functionName);

        if (literalExp instanceof IASTIntegerLiteralExpression
            || literalExp instanceof IASTCharLiteralExpression) {
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
      else if(op2 instanceof IASTIdExpression ||
          (op2 instanceof IASTUnaryExpression && (
              (((IASTUnaryExpression)op2).getOperator() == UnaryOperator.AMPER) ||
              (((IASTUnaryExpression)op2).getOperator() == UnaryOperator.STAR))))
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
      else if(op2 instanceof IASTUnaryExpression)
      {
        String varName = op1.toASTString();

        IASTUnaryExpression unaryExp = (IASTUnaryExpression)op2;
        IASTExpression unaryExpOp = unaryExp.getOperand();

        UnaryOperator operatorType = unaryExp.getOperator();
        // a == -8
        if(operatorType == UnaryOperator.MINUS){

          if(unaryExpOp instanceof IASTLiteralExpression){
            IASTLiteralExpression literalExp = (IASTLiteralExpression)unaryExpOp;

            if (literalExp instanceof IASTIntegerLiteralExpression
                || literalExp instanceof IASTCharLiteralExpression) {
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
      else if(op2 instanceof IASTBinaryExpression){
        String varName = op1.toASTString();
        String variableName = getvarName(varName, functionName);
        return forgetElement(pElement, variableName);
      }
      // right hand side is a cast exp
      else if(op2 instanceof IASTCastExpression){
        IASTCastExpression castExp = (IASTCastExpression)op2;
        IASTExpression exprInCastOp = castExp.getOperand();
        return propagateBooleanExpression(pElement, opType, op1, exprInCastOp, functionName, truthValue, edge);
      }
      else{
        String varName = op1.toASTString();
        String variableName = getvarName(varName, functionName);
        return forgetElement(pElement, variableName);
      }
    }
    else if(op1 instanceof IASTCastExpression){
      IASTCastExpression castExp = (IASTCastExpression) op1;
      IASTExpression castOperand = castExp.getOperand();
      return propagateBooleanExpression(pElement, opType, castOperand, op2, functionName, truthValue, edge);
    }
    else{
      String varName = op1.toASTString();
      String variableName = getvarName(varName, functionName);
      return forgetElement(pElement, variableName);
    }
  }


  private AbstractElement forgetElement(OctElement pElement,
      String pVariableName) {
    pElement.forget(pVariableName);
    return pElement;
  }

  private AbstractElement addSmallerEqConstraint(OctElement pElement,
      String pRightVariableName, String pLeftVariableName) {
    int rVarIdx = pElement.getVariableIndexFor(pRightVariableName);
    int lVarIdx = pElement.getVariableIndexFor(pLeftVariableName);
    pElement.addConstraint(3, lVarIdx, rVarIdx, 0);
    return pElement;
  }

  // Note that this only works if both variables are integers
  private AbstractElement addSmallerConstraint(OctElement pElement,
      String pRightVariableName, String pLeftVariableName) {
    int rVarIdx = pElement.getVariableIndexFor(pRightVariableName);
    int lVarIdx = pElement.getVariableIndexFor(pLeftVariableName);
    pElement.addConstraint(3, lVarIdx, rVarIdx, -1);
    return pElement;
  }


  private AbstractElement addGreaterEqConstraint(OctElement pElement,
      String pRightVariableName, String pLeftVariableName) {
    int rVarIdx = pElement.getVariableIndexFor(pRightVariableName);
    int lVarIdx = pElement.getVariableIndexFor(pLeftVariableName);
    pElement.addConstraint(4, lVarIdx, rVarIdx, 0);
    return pElement;
  }

  // Note that this only works if both variables are integers
  private AbstractElement addGreaterConstraint(OctElement pElement,
      String pRightVariableName, String pLeftVariableName) {
    int rVarIdx = pElement.getVariableIndexFor(pRightVariableName);
    int lVarIdx = pElement.getVariableIndexFor(pLeftVariableName);
    pElement.addConstraint(4, lVarIdx, rVarIdx, -1);
    return pElement;
  }

  // Note that this only works if both variables are integers
  private AbstractElement addIneqConstraint(OctElement pElement,
      String pRightVariableName, String pLeftVariableName) {
    OctElement newElem1 = null;
    newElem1 = pElement.clone();
    addEqConstraint(newElem1, pLeftVariableName, pRightVariableName);
    if(! newElem1.isEmpty()){
      return null;
    }
    else{
      return pElement;
    }
  }

  private AbstractElement addEqConstraint(OctElement pElement,
      String pRightVariableName, String pLeftVariableName) {
//    addSmallerEqConstraint(pElement, pRightVariableName, pLeftVariableName);
//    addGreaterEqConstraint(pElement, pRightVariableName, pLeftVariableName);
//    return pElement;

    OctElement newElem1 = null;
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

  private AbstractElement addSmallerEqConstraint(OctElement pElement,
      String pVariableName, long pValueOfLiteral) {
    int varIdx = pElement.getVariableIndexFor(pVariableName);
    pElement.addConstraint(0, varIdx, 0, (int)pValueOfLiteral);
    return pElement;
  }

  // Note that this only works if both variables are integers
  private AbstractElement addSmallerConstraint(OctElement pElement,
      String pVariableName, long pValueOfLiteral) {
    int varIdx = pElement.getVariableIndexFor(pVariableName);
    pElement.addConstraint(0, varIdx, -1, (int)pValueOfLiteral-1);
    return pElement;
  }

  private AbstractElement addGreaterEqConstraint(OctElement pElement,
      String pVariableName, long pValueOfLiteral) {
    int varIdx = pElement.getVariableIndexFor(pVariableName);
    pElement.addConstraint(1, varIdx, 0, (0 - (int)pValueOfLiteral));
    return pElement;
  }

  // Note that this only works if both variables are integers
  private AbstractElement addGreaterConstraint(OctElement pElement,
      String pVariableName, long pValueOfLiteral) {
    int varIdx = pElement.getVariableIndexFor(pVariableName);
    pElement.addConstraint(1, varIdx, 0, (-1 - (int)pValueOfLiteral));
    return pElement;
  }

  private AbstractElement addEqConstraint(OctElement pElement,
      String pVariableName, long pI) {
//    addGreaterEqConstraint(pElement, pVariableName, pI);
//    addSmallerEqConstraint(pElement, pVariableName, pI);
//    return pElement;

    OctElement newElem1 = null;
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
  private AbstractElement addIneqConstraint(OctElement pElement,
      String pVariableName, long pI) {
    OctElement newElem1 = null;
    newElem1 = pElement.clone();
    addEqConstraint(newElem1, pVariableName, pI);
    if(! newElem1.isEmpty()){
      return pElement;
    }
    else{
      return pElement;
    }
  }

  private OctElement handleDeclaration(OctElement pElement,
      DeclarationEdge declarationEdge) throws UnrecognizedCCodeException {

    if (declarationEdge.getName() != null) {

      // get the variable name in the declarator
      String varName = declarationEdge.getName();

      // TODO check other types of variables later - just handle primitive
      // types for the moment
      // don't add pointer variables to the list since we don't track them
      if (declarationEdge.getDeclSpecifier() instanceof IASTPointerTypeSpecifier) {
        return pElement;
      }
      // if this is a global variable, add to the list of global variables
      if(declarationEdge.isGlobal())
      {
        globalVars.add(varName);

        Long v;

        IASTInitializer init = declarationEdge.getInitializer();
        if (init != null) {
          if (init instanceof IASTInitializerExpression) {
            IASTExpression exp = ((IASTInitializerExpression)init).getExpression();

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

  private OctElement declareVariable(OctElement pElement, String pVariableName) {
    pElement.declareVariable(pVariableName);
    return pElement;
  }

  private OctElement assignConstant(OctElement pElement, String pVarName,
      long pLongValue) {
    pElement.assignConstant(pVarName, pLongValue);
    return pElement;
  }

  private OctElement handleStatement(OctElement pElement,
      IASTStatement expression, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException {
    // expression is a binary operation, e.g. a = b;
    if (expression instanceof IASTAssignment) {
      return handleAssignment(pElement, (IASTAssignment)expression, cfaEdge);
    }
    // external function call
    else if(expression instanceof IASTFunctionCallStatement){
      // do nothing
    }
    // there is such a case
    else if(expression instanceof IASTExpressionStatement){
      // do nothing
    }
    else{
      throw new UnrecognizedCCodeException(cfaEdge, expression);
    }
    assert(false);
    return null;
  }

  private OctElement handleAssignment(OctElement pElement,
      IASTAssignment assignExpression, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException {

    IASTExpression op1 = assignExpression.getLeftHandSide();
    IASTRightHandSide op2 = assignExpression.getRightHandSide();

    if(op1 instanceof IASTIdExpression) {
      // a = ...
      return handleAssignmentToVariable(pElement, ((IASTIdExpression)op1).getName(), op2, cfaEdge);

    } else if (op1 instanceof IASTUnaryExpression
        && ((IASTUnaryExpression)op1).getOperator() == UnaryOperator.STAR) {
      // *a = ...

      op1 = ((IASTUnaryExpression)op1).getOperand();

      // Cil produces code like
      // *((int*)__cil_tmp5) = 1;
      // so remove cast
      if (op1 instanceof IASTCastExpression) {
        op1 = ((IASTCastExpression)op1).getOperand();
      }

      if (op1 instanceof IASTIdExpression) {
//        missingInformationLeftPointer = op1.getRawSignature();
//        missingInformationRightExpression = op2;

      } else {
        throw new UnrecognizedCCodeException("left operand of assignment has to be a variable", cfaEdge, op1);
      }
            return pElement;

    } else if (op1 instanceof IASTFieldReference) {
      // TODO assignment to field
            return pElement;

    } else if (op1 instanceof IASTArraySubscriptExpression) {
      // TODO assignment to array cell
            return pElement;

    } else {
      throw new UnrecognizedCCodeException("left operand of assignment has to be a variable", cfaEdge, op1);
    }
  }

  private OctElement handleAssignmentToVariable(OctElement pElement,
      String lParam, IASTRightHandSide rightExp, CFAEdge cfaEdge) throws UnrecognizedCCodeException {
    String functionName = cfaEdge.getPredecessor().getFunctionName();

    // a = 8.2 or "return;" (when rightExp == null)
    if(rightExp == null || rightExp instanceof IASTLiteralExpression){
      return handleAssignmentOfLiteral(pElement, lParam, (IASTLiteralExpression)rightExp, functionName, cfaEdge);
    }
    // a = b
    else if (rightExp instanceof IASTIdExpression){
      return handleAssignmentOfVariable(pElement, lParam, (IASTIdExpression)rightExp, functionName, 1);
    }
    // a = (cast) ?
    else if(rightExp instanceof IASTCastExpression) {
      return handleAssignmentOfCast(pElement, lParam, (IASTCastExpression)rightExp, cfaEdge);
    }
    // a = -b
    else if(rightExp instanceof IASTUnaryExpression){
      return handleAssignmentOfUnaryExp(pElement, lParam, (IASTUnaryExpression)rightExp, cfaEdge);
    }
    // a = b op c
    else if(rightExp instanceof IASTBinaryExpression){
      IASTBinaryExpression binExp = (IASTBinaryExpression)rightExp;

      return handleAssignmentOfBinaryExp(pElement, lParam, binExp.getOperand1(),
          binExp.getOperand2(), binExp.getOperator(), cfaEdge);
    }
    // a = extCall();  or  a = b->c;
    else if(rightExp instanceof IASTFunctionCallExpression
        || rightExp instanceof IASTFieldReference){
      //      OctElement newElement = element.clone();
      String lvarName = getvarName(lParam, functionName);
      return forget(pElement, lvarName);
    }
    else{
      throw new UnrecognizedCCodeException(cfaEdge, rightExp);
    }
  }

  private OctElement forget(OctElement pElement, String pLvarName) {
    pElement.forget(pLvarName);
    return pElement;
  }

  private OctElement handleAssignmentOfCast(OctElement pElement,
      String lParam, IASTCastExpression castExp, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException
  {
    IASTExpression castOperand = castExp.getOperand();
    return handleAssignmentToVariable(pElement, lParam, castOperand, cfaEdge);
  }

  private OctElement handleAssignmentOfUnaryExp(OctElement pElement,
      String lParam, IASTUnaryExpression unaryExp, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException {

    String functionName = cfaEdge.getPredecessor().getFunctionName();
    // name of the updated variable, so if a = -b is handled, lParam is a
    String assignedVar = getvarName(lParam, functionName);
    //    OctElement newElement = element.clone();

    IASTExpression unaryOperand = unaryExp.getOperand();
    UnaryOperator unaryOperator = unaryExp.getOperator();

    if (unaryOperator == UnaryOperator.STAR) {
      // a = * b
      // TODO what is this?
//      OctElement newElement = forget(pElement, assignedVar);

      // Cil produces code like
      // __cil_tmp8 = *((int *)__cil_tmp7);
      // so remove cast
      if (unaryOperand instanceof IASTCastExpression) {
        unaryOperand = ((IASTCastExpression)unaryOperand).getOperand();
      }

      if (unaryOperand instanceof IASTIdExpression) {
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

  private OctElement handleAssignmentOfBinaryExp(OctElement pElement,
      String lParam, IASTExpression lVarInBinaryExp, IASTExpression rVarInBinaryExp,
      BinaryOperator binaryOperator, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException {

    String functionName = cfaEdge.getPredecessor().getFunctionName();
    // name of the updated variable, so if a = b + c is handled, lParam is a
    String assignedVar = getvarName(lParam, functionName);
    //    OctElement newElement = element.clone();

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
        if(lVarInBinaryExp instanceof IASTIdExpression){
          lVarName = ((IASTIdExpression)lVarInBinaryExp).getName();

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
        if(lVarInBinaryExp instanceof IASTIdExpression){
          rVarName = ((IASTIdExpression)rVarInBinaryExp).getName();

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
        if(lVarInBinaryExp instanceof IASTIdExpression){
          lVarName = ((IASTIdExpression)lVarInBinaryExp).getName();
          rVarName = ((IASTIdExpression)rVarInBinaryExp).getName();

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
  private Long getExpressionValue(OctElement pElement, IASTRightHandSide expression,
      String functionName, CFAEdge cfaEdge) throws UnrecognizedCCodeException {

    if (expression instanceof IASTLiteralExpression) {
      return parseLiteral((IASTLiteralExpression)expression, cfaEdge);

    } else if (expression instanceof IASTIdExpression) {
      return null;
    } else if (expression instanceof IASTCastExpression) {
      return getExpressionValue(pElement, ((IASTCastExpression)expression).getOperand(),
          functionName, cfaEdge);

    } else if (expression instanceof IASTUnaryExpression) {
      IASTUnaryExpression unaryExpression = (IASTUnaryExpression)expression;
      UnaryOperator unaryOperator = unaryExpression.getOperator();
      IASTExpression unaryOperand = unaryExpression.getOperand();

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

  private OctElement assignmentOfBinaryExp(OctElement pElement,
      String pAssignedVar, String pLeftVarName, int pLeftVarCoef,
      String pRightVarName, int pRightVarCoef, int pConstVal) {
    pElement.assignmentOfBinaryExp(pAssignedVar, pLeftVarName, pLeftVarCoef,
        pRightVarName, pRightVarCoef, pConstVal);
    return pElement;
  }

  private OctElement handleAssignmentOfVariable(OctElement pElement,
      String lParam, IASTExpression op2, String functionName, int coef)
  {
    String rParam = op2.toASTString();

    String leftVarName = getvarName(lParam, functionName);
    String rightVarName = getvarName(rParam, functionName);

    return assignVariable(pElement, leftVarName, rightVarName, coef);
  }

//  private OctElement handleAssignmentOfReturnVariable(OctElement pElement,
//      String lParam, String tempVarName, String functionName, int coef)
//  {
//    String leftVarName = getvarName(lParam, functionName);
//
//    return assignVariable(pElement, leftVarName, tempVarName, coef);
//  }

  private OctElement assignVariable(OctElement pElement, String pLeftVarName,
      String pRightVarName, int coef) {
    pElement.assignVariable(pLeftVarName, pRightVarName, coef);
    return pElement;
  }

  private OctElement handleAssignmentOfLiteral(OctElement pElement,
      String lParam, IASTLiteralExpression op2, String functionName, CFAEdge edge)
  throws UnrecognizedCCodeException
  {
    //    OctElement newElement = element.clone();

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

  private Long parseLiteral(IASTLiteralExpression expression, CFAEdge edge) throws UnrecognizedCCodeException {
    if (expression instanceof IASTIntegerLiteralExpression) {
      return ((IASTIntegerLiteralExpression)expression).asLong();

    } else if (expression instanceof IASTFloatLiteralExpression) {
      return null;

    } else if (expression instanceof IASTCharLiteralExpression) {
      return (long)((IASTCharLiteralExpression)expression).getCharacter();

    } else if (expression instanceof IASTStringLiteralExpression) {
      return null;

    } else {
      throw new UnrecognizedCCodeException("unknown literal", edge, expression);
    }
  }

  private Long parseLiteralWithOppositeSign(IASTLiteralExpression expression, CFAEdge edge) throws UnrecognizedCCodeException {
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
  public Collection<? extends AbstractElement> strengthen(AbstractElement element,
      List<AbstractElement> otherElements, CFAEdge cfaEdge,
      Precision precision) {

    assert element instanceof OctElement;
    OctElement octagonElement = (OctElement)element;

    for (AbstractElement ae : otherElements) {
      if (ae instanceof PointerElement) {
        return strengthen(octagonElement, (PointerElement)ae, cfaEdge, precision);
      }
      else if(ae instanceof AssumptionStorageElement){
        return strengthen(octagonElement, (AssumptionStorageElement)ae, cfaEdge, precision);
      }
    }
    return null;


  }

  private Collection<? extends AbstractElement> strengthen(
      OctElement pOctagonElement, AssumptionStorageElement pAe,
      CFAEdge pCfaEdge, Precision pPrecision) {
    // TODO Auto-generated method stub
    return null;
  }

  private Collection<? extends AbstractElement> strengthen(
      OctElement pOctagonElement, PointerElement pAe, CFAEdge pCfaEdge,
      Precision pPrecision) {
    // TODO Auto-generated method stub
    return null;
  }
}
