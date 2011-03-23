/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
import org.sosy_lab.cpachecker.cfa.ast.IASTAssignmentExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTInitializer;
import org.sosy_lab.cpachecker.cfa.ast.IASTInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTPointerTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression.BinaryOperator;
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
import org.sosy_lab.cpachecker.exceptions.OctagonTransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
/**
 * Handles transfer relation for Octagon abstract domain library.
 * See <a href="http://www.di.ens.fr/~mine/oct/">Octagon abstract domain library</a>
 * @author Erkan
 *
 */
public class OctTransferRelation implements TransferRelation{

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
  public Collection<? extends AbstractElement> getAbstractSuccessors (AbstractElement element, Precision prec, CFAEdge cfaEdge) throws OctagonTransferException 
  {

    System.out.println(cfaEdge);
    // octElement is the region of the current state
    // this state will be updated using the edge
    
    OctElement octElement = null;
    OctElement prevElement = (OctElement)element;
    try {
      octElement = (OctElement)((OctElement)element).clone();
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
    }

    assert(octElement != null);

    // check the type of the edge
    switch (cfaEdge.getEdgeType ())
    {

    // if edge is a statement edge, e.g. a = b + c
    case StatementEdge:
    {
      StatementEdge statementEdge = (StatementEdge) cfaEdge;
      IASTExpression expression = statementEdge.getExpression ();
      try {
        octElement = handleStatement (octElement, expression, cfaEdge);
      } catch (UnrecognizedCCodeException e) {
        e.printStackTrace();
      }
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
      try {
        octElement = handleExitFromFunction(octElement, statementEdge.getExpression(), statementEdge);
      } catch (UnrecognizedCCodeException e) {
        e.printStackTrace();
      }
      break;
    }

    // edge is a decleration edge, e.g. int a;
    case DeclarationEdge:
    {
      DeclarationEdge declarationEdge = (DeclarationEdge) cfaEdge;
      try {
        octElement = handleDeclaration (octElement, declarationEdge);
      } catch (UnrecognizedCCodeException e) {
        e.printStackTrace();
      }
      break;
    }

    // this is an assumption, e.g. if(a == b)
    case AssumeEdge:
    {
      AssumeEdge assumeEdge = (AssumeEdge) cfaEdge;
      IASTExpression expression = assumeEdge.getExpression();
      try {
        octElement = (OctElement)handleAssumption (octElement, expression, cfaEdge, assumeEdge.getTruthAssumption());
      } catch (UnrecognizedCFAEdgeException e) {
        e.printStackTrace();
      }
      break;

    }

    case BlankEdge:
    {
      break;
    }

    case FunctionCallEdge:
    {
      FunctionCallEdge functionCallEdge = (FunctionCallEdge) cfaEdge;
      try {

        octElement = handleFunctionCall(octElement, prevElement, functionCallEdge);
      } catch (UnrecognizedCCodeException e) {
        e.printStackTrace();
      }
      break;
    }

    // this is a return edge from function, this is different from return statement
    // of the function. See case for statement edge for details
    case FunctionReturnEdge:
    {
      FunctionReturnEdge functionReturnEdge = (FunctionReturnEdge) cfaEdge;
      try {
        octElement = handleFunctionReturn(octElement, functionReturnEdge);
      } catch (UnrecognizedCCodeException e) {
        e.printStackTrace();
      }
      break;
    }

    // Summary edge, we handle this on function return, do nothing
    case CallToReturnEdge:
    {
      assert(false);
      break;
    }
    }
    
//    System.out.println("------------------ " + cfaEdge);
    if (octElement == null || octElement.isEmpty()) {
      System.out.println("[ empty ]");
      return Collections.emptySet();
    }
    
//    octElement.printOctagon();
//    System.out.println("=======================");
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
    IASTExpression exprOnSummary = summaryEdge.getExpression();

    OctElement previousElem = element.getPreviousElement();

    String callerFunctionName = functionReturnEdge.getSuccessor().getFunctionName();
    String calledFunctionName = functionReturnEdge.getPredecessor().getFunctionName();

    //expression is an assignment operation, e.g. a = g(b);
    if (exprOnSummary instanceof IASTAssignmentExpression) {
      IASTAssignmentExpression binExp = ((IASTAssignmentExpression)exprOnSummary);
      IASTExpression op1 = binExp.getLeftHandSide();

      //we expect left hand side of the expression to be a variable
      if(op1 instanceof IASTIdExpression ||
          op1 instanceof IASTFieldReference)
      {
        String varName = op1.getRawSignature();
        String returnVarName = calledFunctionName + "::" + "___cpa_temp_result_var_";

        String assignedVarName = getvarName(varName, callerFunctionName);
        
        assignVariable(element, assignedVarName, returnVarName, 1);
      }
      else{
        throw new UnrecognizedCCodeException("on function return", summaryEdge, op1);
      }
    }
    // TODO this is not called -- expression is a unary operation, e.g. g(b);
    else if (exprOnSummary instanceof IASTUnaryExpression)
    {
      // do nothing
    }
    // g(b)
    else if (exprOnSummary instanceof IASTFunctionCallExpression)
    {
      // do nothing
    }
    else{
      throw new UnrecognizedCCodeException("on function return", summaryEdge, exprOnSummary);
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
      OctElement pPrevElement, FunctionCallEdge callEdge)
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
        String nameOfArg = idExp.getRawSignature();
        String actualParamName = getvarName(nameOfArg, callerFunctionName);

        assignVariable(octagonElement, formalParamName, actualParamName, 1);
      }

      else if(arg instanceof IASTLiteralExpression){
        Long val = parseLiteral(arg);

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

      else if(arg instanceof IASTFunctionCallExpression){
        assert(false);
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
  throws UnrecognizedCFAEdgeException {

    String functionName = cfaEdge.getPredecessor().getFunctionName();
    // Binary operation
    if (expression instanceof IASTBinaryExpression) {
      IASTBinaryExpression binExp = ((IASTBinaryExpression)expression);
      BinaryOperator opType = binExp.getOperator ();

      IASTExpression op1 = binExp.getOperand1();
      IASTExpression op2 = binExp.getOperand2();
      return propagateBooleanExpression(pElement, opType, op1, op2, functionName, truthValue);
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
      else if(expression instanceof IASTCastExpression){
        return handleAssumption(pElement, ((IASTCastExpression)expression).getOperand(), cfaEdge, truthValue);
      }
      else {
        throw new UnrecognizedCFAEdgeException("Unhandled case " + cfaEdge.getRawStatement());
      }
    }

    else if(expression instanceof IASTIdExpression
        || expression instanceof IASTFieldReference){
      return propagateBooleanExpression(pElement, null, expression, null, functionName, truthValue);
    }

    else{
      throw new UnrecognizedCFAEdgeException("Unhandled case " + cfaEdge.getRawStatement());
    }

  }

  private AbstractElement propagateBooleanExpression(OctElement pElement, 
      BinaryOperator opType,IASTExpression op1, 
      IASTExpression op2, String functionName, boolean truthValue) 
  throws UnrecognizedCFAEdgeException {

    // a (bop) ?
    if(op1 instanceof IASTIdExpression || 
        op1 instanceof IASTFieldReference ||
        op1 instanceof IASTArraySubscriptExpression)
    {
      // [literal]
      if(op2 == null && opType == null){
        String varName = op1.getRawSignature();
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
        String varName = op1.getRawSignature();
        String variableName = getvarName(varName, functionName);
        int typeOfLiteral = ((IASTLiteralExpression)op2).getKind();
        if( typeOfLiteral ==  IASTLiteralExpression.lk_integer_constant 
            || typeOfLiteral == IASTLiteralExpression.lk_float_constant
            || typeOfLiteral == IASTLiteralExpression.lk_char_constant
        )
        {
          long valueOfLiteral = parseLiteral(op2);
          // a == 9
          if(opType == BinaryOperator.EQUALS) {
            if(truthValue){
              return addEqConstraint(pElement, variableName, valueOfLiteral);
            }
            // ! a == 9
            else {
              return propagateBooleanExpression(pElement, BinaryOperator.NOT_EQUALS, op1, op2, functionName, !truthValue);
            }
          }
          // a != 9
          else if(opType == BinaryOperator.NOT_EQUALS)
          {
            //            System.out.println(" >>>>> " + varName + " op2 " + op2.getRawSignature());
            if(truthValue){
              return addIneqConstraint(pElement, variableName, valueOfLiteral);
            }
            // ! a != 9
            else {
              return propagateBooleanExpression(pElement, BinaryOperator.EQUALS, op1, op2, functionName, !truthValue);
            }
          }

          // a > 9
          else if(opType == BinaryOperator.GREATER_THAN)
          {
            if(truthValue){
              return addGreaterConstraint(pElement, variableName, valueOfLiteral);
            }
            else {
              return propagateBooleanExpression(pElement, BinaryOperator.LESS_EQUAL, op1, op2, functionName, !truthValue);
            }
          }
          // a >= 9
          else if(opType == BinaryOperator.GREATER_EQUAL)
          {
            if(truthValue){
              return addGreaterEqConstraint(pElement, variableName, valueOfLiteral);
            }
            else {
              return propagateBooleanExpression(pElement, BinaryOperator.LESS_THAN, op1, op2, functionName, !truthValue);
            }
          }
          // a < 9
          else if(opType == BinaryOperator.LESS_THAN)
          {
            if(truthValue){
              return addSmallerConstraint(pElement, variableName, valueOfLiteral);
            }
            else {
              return propagateBooleanExpression(pElement, BinaryOperator.GREATER_EQUAL, op1, op2, functionName, !truthValue);
            }
          }
          // a <= 9
          else if(opType == BinaryOperator.LESS_EQUAL)
          {
            if(truthValue){
              return addSmallerEqConstraint(pElement, variableName, valueOfLiteral);
            }
            else {
              return propagateBooleanExpression(pElement, BinaryOperator.GREATER_THAN, op1, op2, functionName, !truthValue);
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
              return propagateBooleanExpression(pElement, BinaryOperator.EQUALS, op1, op2, functionName, !truthValue);
            }
          }

          // [a + 9]
          else if(opType == BinaryOperator.PLUS)
          {
            valueOfLiteral = parseLiteralWithOppositeSign(op2);
            if(truthValue){
              return addIneqConstraint(pElement, variableName, valueOfLiteral);
            }
            else {
              valueOfLiteral = parseLiteralWithOppositeSign(op2);

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
            throw new UnrecognizedCFAEdgeException("Unhandled case ");
          }
        }
        else{
          throw new UnrecognizedCFAEdgeException("Unhandled case ");
        }
      }
      // a (bop) b
      else if(op2 instanceof IASTIdExpression ||
          (op2 instanceof IASTUnaryExpression && (
              (((IASTUnaryExpression)op2).getOperator() == UnaryOperator.AMPER) || 
              (((IASTUnaryExpression)op2).getOperator() == UnaryOperator.STAR))))
      {
        String leftVarName = op1.getRawSignature();
        String rightVarName = op2.getRawSignature();

        String leftVariableName = getvarName(leftVarName, functionName);
        String rightVariableName = getvarName(rightVarName, functionName);

        // a == b
        if(opType == BinaryOperator.EQUALS)
        {
          if(truthValue){
            return addEqConstraint(pElement, rightVariableName, leftVariableName);
          }
          else{
            return propagateBooleanExpression(pElement, BinaryOperator.NOT_EQUALS, op1, op2, functionName, !truthValue);
          }
        }
        // a != b
        else if(opType == BinaryOperator.NOT_EQUALS)
        {
          if(truthValue){
            return addIneqConstraint(pElement, rightVariableName, leftVariableName);
          }
          else{
            return propagateBooleanExpression(pElement, BinaryOperator.EQUALS, op1, op2, functionName, !truthValue);
          }
        }
        // a > b
        else if(opType == BinaryOperator.GREATER_THAN)
        {
          if(truthValue){
            return addGreaterConstraint(pElement, rightVariableName, leftVariableName);
          }
          else{
            return  propagateBooleanExpression(pElement, BinaryOperator.LESS_EQUAL, op1, op2, functionName, !truthValue);
          }
        }
        // a >= b
        else if(opType == BinaryOperator.GREATER_EQUAL)
        {
          if(truthValue){
            return addGreaterEqConstraint(pElement, rightVariableName, leftVariableName);
          }
          else{
            return propagateBooleanExpression(pElement, BinaryOperator.LESS_THAN, op1, op2, functionName, !truthValue);
          }
        }
        // a < b
        else if(opType == BinaryOperator.LESS_THAN)
        {
          if(truthValue){
            return addSmallerConstraint(pElement, rightVariableName, leftVariableName);
          }
          else{
            return propagateBooleanExpression(pElement, BinaryOperator.GREATER_EQUAL, op1, op2, functionName, !truthValue);
          }
        }
        // a <= b
        else if(opType == BinaryOperator.LESS_EQUAL)
        {
          if(truthValue){
            return addSmallerEqConstraint(pElement, rightVariableName, leftVariableName);
          }
          else{
            return propagateBooleanExpression(pElement, BinaryOperator.GREATER_THAN, op1, op2, functionName, !truthValue);
          }
        }
        else{
          throw new UnrecognizedCFAEdgeException("Unhandled case ");
        }
      }
      else if(op2 instanceof IASTUnaryExpression)
      {
        String varName = op1.getRawSignature();

        IASTUnaryExpression unaryExp = (IASTUnaryExpression)op2;
        IASTExpression unaryExpOp = unaryExp.getOperand();

        UnaryOperator operatorType = unaryExp.getOperator();
        // a == -8
        if(operatorType == UnaryOperator.MINUS){

          if(unaryExpOp instanceof IASTLiteralExpression){
            IASTLiteralExpression literalExp = (IASTLiteralExpression)unaryExpOp;
            int typeOfLiteral = literalExp.getKind();
            if( typeOfLiteral ==  IASTLiteralExpression.lk_integer_constant 
                || typeOfLiteral == IASTLiteralExpression.lk_float_constant
                || typeOfLiteral == IASTLiteralExpression.lk_char_constant
            )
            {
              long valueOfLiteral = parseLiteralWithOppositeSign(literalExp);
              String variableName = getvarName(varName, functionName);

              // a == 9
              if(opType == BinaryOperator.EQUALS) {
                if(truthValue){
                  return addEqConstraint(pElement, variableName, valueOfLiteral);
                }
                // ! a == 9
                else {
                  return propagateBooleanExpression(pElement, BinaryOperator.NOT_EQUALS, op1, op2, functionName, !truthValue);
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
                  return propagateBooleanExpression(pElement, BinaryOperator.EQUALS, op1, op2, functionName, !truthValue);
                }
              }

              // a > 9
              else if(opType == BinaryOperator.GREATER_THAN)
              {
                if(truthValue){
                  return addGreaterConstraint(pElement, variableName, valueOfLiteral);
                }
                else {
                  return propagateBooleanExpression(pElement, BinaryOperator.LESS_EQUAL, op1, op2, functionName, !truthValue);
                }
              }
              // a >= 9
              else if(opType == BinaryOperator.GREATER_EQUAL)
              {
                if(truthValue){
                  return addGreaterEqConstraint(pElement, variableName, valueOfLiteral);
                }
                else {
                  return propagateBooleanExpression(pElement, BinaryOperator.LESS_THAN, op1, op2, functionName, !truthValue);
                }
              }
              // a < 9
              else if(opType == BinaryOperator.LESS_THAN)
              {
                if(truthValue){
                  return addSmallerConstraint(pElement, variableName, valueOfLiteral);
                }
                else {
                  return propagateBooleanExpression(pElement, BinaryOperator.GREATER_EQUAL, op1, op2, functionName, !truthValue);
                }
              }
              // a <= 9
              else if(opType == BinaryOperator.LESS_EQUAL)
              {
                if(truthValue){
                  return addSmallerEqConstraint(pElement, variableName, valueOfLiteral);
                }
                else {
                  return propagateBooleanExpression(pElement, BinaryOperator.GREATER_THAN, op1, op2, functionName, !truthValue);
                }
              }
              else{
                throw new UnrecognizedCFAEdgeException("Unhandled case ");
              }
            }
            else{
              throw new UnrecognizedCFAEdgeException("Unhandled case ");
            }
          }
          else{
            throw new UnrecognizedCFAEdgeException("Unhandled case ");
          }
        }
        // right hand side is a cast exp
        else if(op2 instanceof IASTCastExpression){
          IASTCastExpression castExp = (IASTCastExpression)op2;
          IASTExpression exprInCastOp = castExp.getOperand();
          return propagateBooleanExpression(pElement, opType, op1, exprInCastOp, functionName, truthValue);
        }
        else{
          throw new UnrecognizedCFAEdgeException("Unhandled case ");
        }
      }
      else if(op2 instanceof IASTBinaryExpression){
        String varName = op1.getRawSignature();
        String variableName = getvarName(varName, functionName);
        return forgetElement(pElement, variableName);
      }
      else{
        String varName = op1.getRawSignature();
        String variableName = getvarName(varName, functionName);
        return forgetElement(pElement, variableName);
      }
    }
    else if(op1 instanceof IASTCastExpression){
      IASTCastExpression castExp = (IASTCastExpression) op1;
      IASTExpression castOperand = castExp.getOperand();
      return propagateBooleanExpression(pElement, opType, castOperand, op2, functionName, truthValue);
    }
    else{
      String varName = op1.getRawSignature();
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
    try {
      newElem1 = (OctElement)pElement.clone();
    } catch (CloneNotSupportedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
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
    try {
      newElem1 = (OctElement)pElement.clone();
    } catch (CloneNotSupportedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
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
    try {
      newElem1 = (OctElement)pElement.clone();
    } catch (CloneNotSupportedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
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
    try {
      newElem1 = (OctElement)pElement.clone();
    } catch (CloneNotSupportedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
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
      String varName = declarationEdge.getName().toString();

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
      IASTExpression expression, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException {
    // expression is a binary operation, e.g. a = b;
    if (expression instanceof IASTAssignmentExpression) {
      return handleAssignment(pElement, (IASTAssignmentExpression)expression, cfaEdge);
    }
    // expression is a unary operation, e.g. a++;
    else if (expression instanceof IASTUnaryExpression)
    {
      return handleUnaryStatement(pElement, expression, cfaEdge);
    }
    // external function call
    else if(expression instanceof IASTFunctionCallExpression){
      // do nothing
    }
    // there is such a case
    else if(expression instanceof IASTIdExpression){
      // do nothing
    }
    else{
      throw new UnrecognizedCCodeException(cfaEdge, expression);
    }
    assert(false);
    return null;
  }

  private OctElement handleUnaryStatement(OctElement pElement,
      IASTExpression expression, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException {

    IASTUnaryExpression unaryExpression = (IASTUnaryExpression) expression;
    UnaryOperator operator = unaryExpression.getOperator();

    int shift;
    if (operator == UnaryOperator.POSTFIX_INCREMENT ||
        operator == UnaryOperator.PREFIX_INCREMENT) {
      // a++, ++a
      shift = 1;

    } else if(operator == UnaryOperator.PREFIX_DECREMENT ||
        operator == UnaryOperator.POSTFIX_DECREMENT) {
      // a--, --a
      shift = -1;
    } else {
      throw new UnrecognizedCCodeException(cfaEdge, unaryExpression);
    }

    IASTExpression operand = unaryExpression.getOperand();
    if (operand instanceof IASTIdExpression) {
      String functionName = cfaEdge.getPredecessor().getFunctionName();
      String varName = getvarName(operand.getRawSignature(), functionName);

      return shiftConstant(pElement, varName, shift);

    } else {
      throw new UnrecognizedCCodeException(cfaEdge, operand);
    }
  }

  private OctElement shiftConstant(OctElement pElement, String pVarName,
      int pShift) {
    return assignmentOfBinaryExp(pElement, pVarName, pVarName, 1, null, -1, pShift);
  }

  private OctElement handleAssignment(OctElement pElement,
      IASTAssignmentExpression assignExpression, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException {

    IASTExpression op1 = assignExpression.getLeftHandSide();
    IASTExpression op2 = assignExpression.getRightHandSide();

    if(op1 instanceof IASTIdExpression) {
      // a = ...
      return handleAssignmentToVariable(pElement, op1.getRawSignature(), op2, cfaEdge);

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
      String lParam, IASTExpression rightExp, CFAEdge cfaEdge) throws UnrecognizedCCodeException {
    String functionName = cfaEdge.getPredecessor().getFunctionName();

    // a = 8.2 or "return;" (when rightExp == null)
    if(rightExp == null || rightExp instanceof IASTLiteralExpression){
      return handleAssignmentOfLiteral(pElement, lParam, rightExp, functionName);
    }
    // a = b
    else if (rightExp instanceof IASTIdExpression){
      return handleAssignmentOfVariable(pElement, lParam, rightExp, functionName, 1);
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
        String rVarName = unaryOperand.getRawSignature();
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
          lVarName = lVarInBinaryExp.getRawSignature();

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
          rVarName = rVarInBinaryExp.getRawSignature();

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
          lVarName = lVarInBinaryExp.getRawSignature();
          rVarName = rVarInBinaryExp.getRawSignature();
          
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
  private Long getExpressionValue(OctElement pElement, IASTExpression expression,
      String functionName, CFAEdge cfaEdge) throws UnrecognizedCCodeException {

    if (expression instanceof IASTLiteralExpression) {
      return parseLiteral(expression);

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
    String rParam = op2.getRawSignature();

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
      String lParam, IASTExpression op2, String functionName)
  throws UnrecognizedCCodeException
  {
    //    OctElement newElement = element.clone();

    // op2 may be null if this is a "return;" statement
    Long val = (op2 == null ? Long.valueOf(0L) : parseLiteral(op2));

    String assignedVar = getvarName(lParam, functionName);
    if (val != null) {
      return assignConstant(pElement, assignedVar, val);
    } else {
      return forget(pElement, assignedVar);
    }
    //TODO
    //    return null;
  }

  private Long parseLiteral(IASTExpression expression) {
    if (expression instanceof IASTLiteralExpression) {
      //      System.out.println("expr " + expression.getRawSignature());

      //      int typeOfLiteral = ((IASTLiteralExpression)expression).getKind();
      //      if (typeOfLiteral == IASTLiteralExpression.lk_integer_constant) {
      //
      //        String s = expression.getRawSignature();
      //        if(s.endsWith("L") || s.endsWith("U") || s.endsWith("UL")){
      //          s = s.replace("L", "");
      //          s = s.replace("U", "");
      //          s = s.replace("UL", "");
      //        }
      //        try {
      //          return Long.valueOf(s);
      //        } catch (NumberFormatException e) {
      //          throw new UnrecognizedCCodeException("invalid integer literal", null, expression);
      //        }
      //      }
      //      if (typeOfLiteral == IASTLiteralExpression.lk_string_literal) {
      //        return (long) expression.hashCode();
      //      }

      // this should be a number...
      IASTLiteralExpression lexp = (IASTLiteralExpression)expression;
      String num = lexp.getRawSignature();
      Long retVal = null;
      switch (lexp.getKind()) {
      case IASTLiteralExpression.lk_integer_constant:
      case IASTLiteralExpression.lk_float_constant:
        if (num.startsWith("0x")) {
          // this should be in hex format
          // we use Long instead of Integer to avoid getting negative
          // numbers (e.g. for 0xffffff we would get -1)
          num = Long.valueOf(num, 16).toString();
        } else {
          // this might have some modifiers attached (e.g. 0UL), we
          // have to get rid of them
          int pos = num.length()-1;
          while (!Character.isDigit(num.charAt(pos))) {
            --pos;
          }
          num = num.substring(0, pos+1);
          //          System.out.println("num is " + num);
        }
        break;
      case IASTLiteralExpression.lk_char_constant: {
        // we convert to a byte, and take the integer value
        String s = expression.getRawSignature();
        int length = s.length();
        assert(s.charAt(0) == '\'');
        assert(s.charAt(length-1) == '\'');
        int n;

        if (s.charAt(1) == '\\') {
          n = Integer.parseInt(s.substring(2, length-1));
        } else {
          assert (expression.getRawSignature().length() == 3);
          n = expression.getRawSignature().charAt(1);
        }
        num = "" + n;

      }
      break;
      case IASTLiteralExpression.lk_string_literal: {
        // can't handle
        return null;
      }
      default:
        assert(false) : expression;
        return null;
      }
      // TODO here we assume 32 bit integers!!! This is because CIL
      // seems to do so as well...
      try {
        int i = Integer.parseInt(num);
        retVal = (long)i;
      } catch (NumberFormatException nfe) {
        //        System.out.print("catching ");
        long l = Long.parseLong(num);
        //        System.out.println(l);
        if (l < 0) {
          retVal = Integer.MAX_VALUE + l;
        } else {
          //retVal = (l - ((long)Integer.MAX_VALUE + 1)*2);
          retVal = l;
        }
      }
      return retVal;
    }
    return null;
  }

  private Long parseLiteralWithOppositeSign(IASTExpression expression){
    if (expression instanceof IASTLiteralExpression) {
      // this should be a number...
      IASTLiteralExpression lexp = (IASTLiteralExpression)expression;
      String num = lexp.getRawSignature();
      Long retVal = null;
      boolean isUnsigned = false;
      switch (lexp.getKind()) {
      case IASTLiteralExpression.lk_integer_constant:
      case IASTLiteralExpression.lk_float_constant:
        if (num.startsWith("0x")) {
          // this should be in hex format
          // we use Long instead of Integer to avoid getting negative
          // numbers (e.g. for 0xffffff we would get -1)
          num = Long.valueOf(num, 16).toString();
        } else {
          // this might have some modifiers attached (e.g. 0UL), we
          // have to get rid of them
          int pos = num.length()-1;
          if(num.contains("U")){
            isUnsigned = true;
          }
          while (!Character.isDigit(num.charAt(pos))) {
            --pos;
          }
          num = num.substring(0, pos+1);
        }
        break;
      case IASTLiteralExpression.lk_char_constant: {
        // we convert to a byte, and take the integer value
        String s = expression.getRawSignature();
        int length = s.length();
        assert(s.charAt(0) == '\'');
        assert(s.charAt(length-1) == '\'');
        int n;

        if (s.charAt(1) == '\\') {
          n = Integer.parseInt(s.substring(2, length-1));
        } else {
          assert (expression.getRawSignature().length() == 3);
          n = expression.getRawSignature().charAt(1);
        }
        num = "" + n;

      }
      break;
      default:
        assert(false);
        return null;
      }
      // TODO here we assume 32 bit integers!!! This is because CIL
      // seems to do so as well...
      try {
        int i = Integer.parseInt(num);
        retVal = 0 - (long)i;
        if(isUnsigned){
          if (retVal < 0) {
            retVal = ((long)Integer.MAX_VALUE + 1)*2 + retVal;
          } else {
            retVal = (retVal - ((long)Integer.MAX_VALUE + 1)*2);
          }
        }
      } catch (NumberFormatException nfe) {
        long l = Long.parseLong(num);
        l = 0 - l;
        if (l < 0) {
          retVal = Integer.MAX_VALUE + l;
        } else {
          //retVal = (l - ((long)Integer.MAX_VALUE + 1)*2);
          retVal = l;
        }
      }
      return retVal;
    }
    return null;

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
