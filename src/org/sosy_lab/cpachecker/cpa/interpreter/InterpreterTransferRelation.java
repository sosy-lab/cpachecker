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
package org.sosy_lab.cpachecker.cpa.interpreter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.IAExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.assume.ConstrainedAssumeState;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonPredicateElement;
import org.sosy_lab.cpachecker.cpa.interpreter.exceptions.AccessToUninitializedVariableException;
import org.sosy_lab.cpachecker.cpa.interpreter.exceptions.MissingInputException;
import org.sosy_lab.cpachecker.cpa.interpreter.exceptions.ReadingFromNondetVariableException;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.tiger.fql.ecp.ECPPredicate;
import org.sosy_lab.cpachecker.tiger.fql.translators.cfa.ToTigerAssumeEdgeTranslator;

public class InterpreterTransferRelation implements TransferRelation {

  private final Set<String> globalVars = new HashSet<>();

  private String missingInformationLeftVariable = null;
  private String missingInformationRightPointer = null;
  private String missingInformationLeftPointer  = null;
  private CExpression missingInformationRightExpression = null;

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState element, Precision precision, CFAEdge cfaEdge) throws CPATransferException {
    AbstractState successor;
    InterpreterElement explicitElement = (InterpreterElement)element;

    // check the type of the edge
    switch (cfaEdge.getEdgeType ()) {

    // if edge is a statement edge, e.g. a = b + c
    case StatementEdge: {
      CStatementEdge statementEdge = (CStatementEdge) cfaEdge;
      successor = handleStatement(explicitElement, statementEdge.getStatement(), cfaEdge);

      // TODO remove
      if (successor == null) {
        throw new RuntimeException();
      }
      break;
    }

    case ReturnStatementEdge: {
      CReturnStatementEdge returnEdge = (CReturnStatementEdge)cfaEdge;
      // this statement is a function return, e.g. return (a);
      // note that this is different from return edge
      // this is a statement edge which leads the function to the
      // last node of its CFA, where return edge is from that last node
      // to the return site of the caller function
      successor = handleExitFromFunction(explicitElement, returnEdge.getExpression(), returnEdge);

      // TODO remove
      if (successor == null) {
        throw new RuntimeException();
      }
      break;
    }

    // edge is a declaration edge, e.g. int a;
    case DeclarationEdge: {
      CDeclarationEdge declarationEdge = (CDeclarationEdge) cfaEdge;
      successor = handleDeclaration(explicitElement, declarationEdge);

      // TODO remove
      if (successor == null) {
        throw new RuntimeException();
      }
      break;
    }

    // this is an assumption, e.g. if(a == b)
    case AssumeEdge: {
      AssumeEdge assumeEdge = (AssumeEdge) cfaEdge;
      successor = handleAssumption(explicitElement, assumeEdge.getExpression(), cfaEdge, assumeEdge.getTruthAssumption());

      // TODO remove
      if (successor == null) {
        throw new RuntimeException();
      }
      break;
    }

    case BlankEdge: {
      successor = explicitElement.clone();

      // TODO remove
      if (successor == null) {
        throw new RuntimeException();
      }
      break;
    }

    case FunctionCallEdge: {
      CFunctionCallEdge functionCallEdge = (CFunctionCallEdge) cfaEdge;
      successor = handleFunctionCall(explicitElement, functionCallEdge);

      // TODO remove
      if (successor == null) {
        throw new RuntimeException();
      }
      break;
    }

    // this is a return edge from function, this is different from return statement
    // of the function. See case for statement edge for details
    case FunctionReturnEdge: {
      CFunctionReturnEdge functionReturnEdge = (CFunctionReturnEdge) cfaEdge;
      successor = handleFunctionReturn(explicitElement, functionReturnEdge);

      // TODO remove
      if (successor == null) {
        throw new RuntimeException();
      }

      InterpreterElement lSuccessor = (InterpreterElement)successor;

      if (lSuccessor.getInputIndex() != explicitElement.getInputIndex()) {
        throw new RuntimeException();
      }

      break;
    }

    default:
      throw new UnrecognizedCFAEdgeException(cfaEdge);
    }

    // TODO implement a debugger like interpreter
    /*try {
      System.out.println(cfaEdge);
      System.in.read();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }*/

    /*System.out.println("INTERPRETER: ");
    System.out.println("(" + element.toString() + ", " + precision.toString() + ")");
    System.out.println("--[" + cfaEdge.toString() + "]->");
    System.out.println(successor.toString());
    System.out.println("--------------");*/

    if (InterpreterBottomElement.INSTANCE.equals(successor)) {
      return Collections.emptySet();
    } else {
      return Collections.singleton(successor);
    }
  }

  /**
   * Handles return from one function to another function.
   * @param element previous abstract element.
   * @param functionReturnEdge return edge from a function to its call site.
   * @return new abstract element.
   * @throws MissingInputException
   * @throws ReadingFromNondetVariableException
   * @throws AccessToUninitializedVariableException
   */
  private InterpreterElement handleFunctionReturn(InterpreterElement element,
      FunctionReturnEdge functionReturnEdge)
  throws UnrecognizedCCodeException, ReadingFromNondetVariableException, AccessToUninitializedVariableException {

    //CallToReturnEdge summaryEdge =
    FunctionSummaryEdge summaryEdge =
      functionReturnEdge.getSuccessor().getEnteringSummaryEdge();
    AFunctionCall exprOnSummary = summaryEdge.getExpression();

    // TODO get from stack
    /*InterpreterElement previousElem = element.getPreviousElement();
    InterpreterElement newElement = previousElem.clone();

    newElement.setInputIndex(element.getInputIndex());*/

    InterpreterElement newElement = element.getUpdatedPreviousElement();

    String callerFunctionName = functionReturnEdge.getSuccessor().getFunctionName();
    String calledFunctionName = functionReturnEdge.getPredecessor().getFunctionName();

    //System.out.println(exprOnSummary.getRawSignature());
    //expression is an assignment operation, e.g. a = g(b);
    if (exprOnSummary instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement binExp = ((CFunctionCallAssignmentStatement)exprOnSummary);
      CLeftHandSide op1 = binExp.getLeftHandSide();

      //we expect left hand side of the expression to be a variable
      if(op1 instanceof CIdExpression ||
          op1 instanceof CFieldReference)
      {
//      IASExpression leftHandSideVar = op1;
        String varName = op1.toASTString();
        String returnVarName = calledFunctionName + "::" + "___cpa_temp_result_var_";

        for(String globalVar:globalVars){
          if(globalVar.equals(varName)){
              if(element.contains(returnVarName)){
                newElement.assignConstant(varName, element.getValueFor(returnVarName));
              }
              else{
                System.out.println("FORGETTING: " + exprOnSummary);
                newElement.forget(varName);
              }
          }
          else{
              if(element.contains(globalVar)){
                newElement.assignConstant(globalVar, element.getValueFor(globalVar));
              }
              else{
                System.out.println("FORGETTING: " + exprOnSummary);
                newElement.forget(varName);
              }
          }
        }

        if(!globalVars.contains(varName)){
          String assignedVarName = getvarName(varName, callerFunctionName);
          if(element.contains(returnVarName)){
            newElement.assignConstant(assignedVarName, element.getValueFor(returnVarName));
          }
          else{
            System.out.println("FORGETTING: " + exprOnSummary);
            newElement.forget(assignedVarName);
          }
        }
      }
      else{
        throw new UnrecognizedCCodeException("on function return", summaryEdge, op1);
      }
    }
    // g(b)
    else if (exprOnSummary instanceof CFunctionCallStatement)
    {
      // only globals
      for(String globalVar:globalVars){
          if(element.contains(globalVar)){
            newElement.assignConstant(globalVar, element.getValueFor(globalVar));
          }
          else{
            newElement.forget(globalVar);
          }
      }
    }
    else{
      throw new UnrecognizedCCodeException(summaryEdge.getRawStatement(), summaryEdge);
    }

    return newElement;
  }

  private InterpreterElement handleFunctionCall(InterpreterElement element,
      CFunctionCallEdge callEdge)
  throws UnrecognizedCCodeException, ReadingFromNondetVariableException, AccessToUninitializedVariableException {

    CFunctionEntryNode functionEntryNode = callEdge.getSuccessor();
    String calledFunctionName = functionEntryNode.getFunctionName();
    String callerFunctionName = callEdge.getPredecessor().getFunctionName();

    List<String> paramNames = functionEntryNode.getFunctionParameterNames();
    List<CExpression> arguments = callEdge.getArguments();

    assert (paramNames.size() == arguments.size());

    InterpreterElement newElement = new InterpreterElement(element, element.getInputIndex(), element.getInputs());

    for(String globalVar:globalVars){
      if(element.contains(globalVar)){
        newElement.getConstantsMap().put(globalVar, element.getValueFor(globalVar));
      }
    }

    for(int i=0; i<arguments.size(); i++){
      CExpression arg = arguments.get(i);
      if (arg instanceof CCastExpression) {
        // ignore casts
        arg = ((CCastExpression)arg).getOperand();
      }

      String nameOfParam = paramNames.get(i);
      String formalParamName = getvarName(nameOfParam, calledFunctionName);
      if(arg instanceof CIdExpression){
        CIdExpression idExp = (CIdExpression) arg;
        String nameOfArg = idExp.getName();
        String actualParamName = getvarName(nameOfArg, callerFunctionName);

        if(element.contains(actualParamName)){
          newElement.assignConstant(formalParamName, element.getValueFor(actualParamName));
        }
      }

      else if(arg instanceof CLiteralExpression){
        Long val = parseLiteral(arg);

        if (val != null) {
          newElement.assignConstant(formalParamName, val);
        } else {
          // TODO forgetting
          newElement.forget(formalParamName);
        }
      }

      else if(arg instanceof CTypeIdExpression){
        newElement.forget(formalParamName);
      }

      else if(arg instanceof CUnaryExpression){
        CUnaryExpression unaryExp = (CUnaryExpression) arg;
        //assert(unaryExp.getOperator() == UnaryOperator.STAR || unaryExp.getOperator() == UnaryOperator.AMPER);
        // TODO for .STAR we have to support CPointerExpression!
        assert(unaryExp.getOperator() == UnaryOperator.AMPER);
      }

      else if(arg instanceof CFieldReference){
        newElement.forget(formalParamName);
      }

      else{
        // TODO forgetting
        newElement.forget(formalParamName);
//      throw new ExplicitTransferException("Unhandled case");
      }
    }

    return newElement;
  }

  private InterpreterElement handleExitFromFunction(InterpreterElement element,
      CExpression expression,
      CReturnStatementEdge returnEdge)
  throws UnrecognizedCCodeException, ReadingFromNondetVariableException, MissingInputException, AccessToUninitializedVariableException {

    InterpreterElement lSuccessor = handleAssignmentToVariable(element, "___cpa_temp_result_var_", expression, returnEdge);

    if (lSuccessor == null) {
      throw new RuntimeException();
    }

    return lSuccessor;
  }

  private AbstractState handleAssumption(InterpreterElement element,
                  IAExpression expression, CFAEdge cfaEdge, boolean truthValue)
                  throws UnrecognizedCFAEdgeException, MissingInputException, ReadingFromNondetVariableException, AccessToUninitializedVariableException {

    String functionName = cfaEdge.getPredecessor().getFunctionName();
    // Binary operation
    if (expression instanceof CBinaryExpression) {
      CBinaryExpression binExp = ((CBinaryExpression)expression);
      BinaryOperator opType = binExp.getOperator();

      CExpression op1 = binExp.getOperand1();
      CExpression op2 = binExp.getOperand2();

      AbstractState lSuccessor = propagateBooleanExpression(element, opType, op1, op2, functionName, truthValue);

      if (lSuccessor == null) {
        throw new RuntimeException();
      }

      return lSuccessor;
    }
    // Unary operation
    else if (expression instanceof CUnaryExpression) {
      CUnaryExpression unaryExp = ((CUnaryExpression)expression);
      // ! exp
      if(unaryExp.getOperator() == UnaryOperator.NOT)
      {
        CExpression exp1 = unaryExp.getOperand();
        AbstractState lSuccessor = handleAssumption(element, exp1, cfaEdge, !truthValue);

        if (lSuccessor == null) {
          throw new RuntimeException();
        }

        return lSuccessor;

      }
      else if(expression instanceof CCastExpression) {
        AbstractState lSuccessor = handleAssumption(element, ((CCastExpression)expression).getOperand(), cfaEdge, truthValue);

        if (lSuccessor == null) {
          throw new RuntimeException();
        }

        return lSuccessor;
      }
      else {
        throw new UnrecognizedCFAEdgeException(cfaEdge);
      }
    }
    else if(expression instanceof CIdExpression
        || expression instanceof CFieldReference) {
      AbstractState lSuccessor = propagateBooleanExpression(element, null, expression, null, functionName, truthValue);

      if (lSuccessor == null) {
        throw new RuntimeException();
      }

      return lSuccessor;
    }
    else if (expression instanceof CIntegerLiteralExpression) {
      CIntegerLiteralExpression lIntegerLiteral = (CIntegerLiteralExpression)expression;
      BigInteger lValue = lIntegerLiteral.getValue();

      if (lValue.equals(BigInteger.ZERO)) {
        return InterpreterBottomElement.INSTANCE;
      }
      else {
        return element.clone();
      }
    }
    else{
      throw new UnrecognizedCFAEdgeException(cfaEdge);
    }

  }

  private AbstractState propagateBooleanExpression(AbstractState element,
      BinaryOperator opType,IAExpression op1,
      IAExpression op2, String functionName, boolean truthValue)
  throws UnrecognizedCFAEdgeException, ReadingFromNondetVariableException, AccessToUninitializedVariableException {

    InterpreterElement newElement = ((InterpreterElement)element).clone();

    // a (bop) ?
    if (op1 instanceof CIdExpression ||
        op1 instanceof CFieldReference ||
        op1 instanceof CArraySubscriptExpression)
    {
      // [literal]
      if (op2 == null && opType == null){
        String lVariableName = op1.toASTString();

        String lScopedVariableName = getvarName(lVariableName, functionName);

        if (truthValue) {
          if (newElement.getValueFor(lScopedVariableName) == 0) {
            return InterpreterBottomElement.INSTANCE;
          }
        }
        // ! [literal]
        else {
          if (newElement.getValueFor(lScopedVariableName) != 0) {
            return InterpreterBottomElement.INSTANCE;
          }
        }

      }
      // a (bop) 9
      else if (op2 instanceof CLiteralExpression)
      {
        String varName = op1.toASTString();
        CLiteralExpression literalExpression = (CLiteralExpression)op2;
        CType expressionType = literalExpression.getExpressionType();

        if (expressionType instanceof CSimpleType && ((CSimpleType)expressionType).getType() == CBasicType.INT) {
/*
        }

        int typeOfLiteral = ((CLiteralExpression)op2).getKind();
        if ( typeOfLiteral ==  CLiteralExpression.lk_integer_constant
            //  || typeOfLiteral == IASTLiteralExpression.lk_float_constant
        )
        {*/
          String literalString = op2.toASTString();
          if (literalString.contains("L") || literalString.contains("U")){
            literalString = literalString.replace("L", "");
            literalString = literalString.replace("U", "");
          }
          int valueOfLiteral = Integer.valueOf(literalString).intValue();

          // a == 9
          if (opType == BinaryOperator.EQUALS) {
            if (truthValue) {
              if (newElement.getValueFor(getvarName(varName, functionName)) != valueOfLiteral) {
                return InterpreterBottomElement.INSTANCE;
              }
            }
            // ! a == 9
            else {
              AbstractState lSuccessor = propagateBooleanExpression(element, BinaryOperator.NOT_EQUALS, op1, op2, functionName, !truthValue);

              if (lSuccessor == null) {
                throw new RuntimeException();
              }

              return lSuccessor;
            }
          }
          // a != 9
          else if (opType == BinaryOperator.NOT_EQUALS)
          {
            if (truthValue){
              if (newElement.getValueFor(getvarName(varName, functionName)) == valueOfLiteral){
                return InterpreterBottomElement.INSTANCE;
              }
            }
            // ! a != 9
            else {
              AbstractState lSuccessor = propagateBooleanExpression(element, BinaryOperator.EQUALS, op1, op2, functionName, !truthValue);

              if (lSuccessor == null) {
                throw new RuntimeException();
              }

              return lSuccessor;
            }
          }

          // a > 9
          else if(opType == BinaryOperator.GREATER_THAN)
          {
            if(truthValue){
              if(newElement.getValueFor(getvarName(varName, functionName)) <= valueOfLiteral){
                return InterpreterBottomElement.INSTANCE;
              }
            }
            else {
              AbstractState lSuccessor = propagateBooleanExpression(element, BinaryOperator.LESS_EQUAL, op1, op2, functionName, !truthValue);

              if (lSuccessor == null) {
                throw new RuntimeException();
              }

              return lSuccessor;
            }
          }
          // a >= 9
          else if(opType == BinaryOperator.GREATER_EQUAL)
          {
            if(truthValue){
              if(newElement.getValueFor(getvarName(varName, functionName)) < valueOfLiteral){
                return InterpreterBottomElement.INSTANCE;
              }
            }
            else {
              AbstractState lSuccessor = propagateBooleanExpression(element, BinaryOperator.LESS_THAN, op1, op2, functionName, !truthValue);

              if (lSuccessor == null) {
                throw new RuntimeException();
              }

              return lSuccessor;
            }
          }
          // a < 9
          else if(opType == BinaryOperator.LESS_THAN)
          {
            if(truthValue) {
              if(newElement.getValueFor(getvarName(varName, functionName)) >= valueOfLiteral) {
                return InterpreterBottomElement.INSTANCE;
              }
            }
            else {
              AbstractState lSuccessor = propagateBooleanExpression(element, BinaryOperator.GREATER_EQUAL, op1, op2, functionName, !truthValue);

              if (lSuccessor == null) {
                throw new RuntimeException();
              }

              return lSuccessor;
            }
          }
          // a <= 9
          else if(opType == BinaryOperator.LESS_EQUAL)
          {
            if(truthValue) {
              if(newElement.getValueFor(getvarName(varName, functionName)) > valueOfLiteral) {
                return InterpreterBottomElement.INSTANCE;
              }
            }
            else {
              AbstractState lSuccessor = propagateBooleanExpression(element, BinaryOperator.GREATER_THAN, op1, op2, functionName, !truthValue);

              if (lSuccessor == null) {
                throw new RuntimeException();
              }

              return lSuccessor;
            }
          }
          // [a - 9]
          else if(opType == BinaryOperator.MINUS)
          {
            if(truthValue) {
              if(newElement.getValueFor(getvarName(varName, functionName)) == valueOfLiteral) {
                return InterpreterBottomElement.INSTANCE;
              }
            }
            else { // ! a - 9
              AbstractState lSuccessor = propagateBooleanExpression(element, BinaryOperator.EQUALS, op1, op2, functionName, !truthValue);

              if (lSuccessor == null) {
                throw new RuntimeException();
              }

              return lSuccessor;
            }
          }

          // [a + 9]
          else if (opType == BinaryOperator.PLUS) {
            if (truthValue) {
              if (newElement.getValueFor(getvarName(varName, functionName)) == (0 - valueOfLiteral)) {
                return InterpreterBottomElement.INSTANCE;
              }
            }
            else { // ! [a + 9]
              if (newElement.getValueFor(getvarName(varName, functionName)) != (0 - valueOfLiteral)) {
                return InterpreterBottomElement.INSTANCE;
              }
            }
          }

          // TODO nothing
          else if(opType == BinaryOperator.BINARY_AND ||
              opType == BinaryOperator.BINARY_OR ||
              opType == BinaryOperator.BINARY_XOR){
            //return newElement;

            throw new RuntimeException();
          }

          else{
            //throw new UnrecognizedCFAEdgeException("Unhandled case ");
            throw new RuntimeException("Unhandled Case"); // TODO improve type of exception
          }
        }
        else{
          //throw new UnrecognizedCFAEdgeException("Unhandled case ");
          throw new RuntimeException("Unhandled Case"); // TODO improve type of exception
        }
      }
      // a (bop) b
      else if(op2 instanceof CIdExpression ||
          (op2 instanceof CUnaryExpression && (
              (((CUnaryExpression)op2).getOperator() == UnaryOperator.AMPER)))) /* ||
              (((CUnaryExpression)op2).getOperator() == UnaryOperator.STAR))))*/ // TODO we have to deal with PointerExpressions!
      {
        String leftVarName = op1.toASTString();
        String rightVarName = op2.toASTString();

        // a == b
        if(opType == BinaryOperator.EQUALS)
        {
          if(truthValue) {
            if(newElement.getValueFor(getvarName(rightVarName, functionName)) !=
              newElement.getValueFor(getvarName(leftVarName, functionName))) {

              return InterpreterBottomElement.INSTANCE;
            }
          }
          else{
            AbstractState lSuccessor = propagateBooleanExpression(element, BinaryOperator.NOT_EQUALS, op1, op2, functionName, !truthValue);

            if (lSuccessor == null) {
              throw new RuntimeException();
            }

            return lSuccessor;
          }
        }
        // a != b
        else if(opType == BinaryOperator.NOT_EQUALS) {
          if(truthValue) {
            if(newElement.getValueFor(getvarName(rightVarName, functionName)) ==
              newElement.getValueFor(getvarName(leftVarName, functionName))) {

              return InterpreterBottomElement.INSTANCE;
            }
          }
          else {
            AbstractState lSuccessor = propagateBooleanExpression(element, BinaryOperator.EQUALS, op1, op2, functionName, !truthValue);

            if (lSuccessor == null) {
              throw new RuntimeException();
            }

            return lSuccessor;
          }
        }
        // a > b
        else if(opType == BinaryOperator.GREATER_THAN) {
          if(truthValue) {
            if(newElement.getValueFor(getvarName(leftVarName, functionName)) <=
              newElement.getValueFor(getvarName(rightVarName, functionName))) {

              return InterpreterBottomElement.INSTANCE;
            }
          }
          else {
            AbstractState lSuccessor = propagateBooleanExpression(element, BinaryOperator.LESS_EQUAL, op1, op2, functionName, !truthValue);

            if (lSuccessor == null) {
              throw new RuntimeException();
            }

            return lSuccessor;
          }
        }
        // a >= b
        else if(opType == BinaryOperator.GREATER_EQUAL) {
          if(truthValue) {
            if(newElement.getValueFor(getvarName(leftVarName, functionName)) <
              newElement.getValueFor(getvarName(rightVarName, functionName))) {

              return InterpreterBottomElement.INSTANCE;
            }
          }
          else {
            AbstractState lSuccessor = propagateBooleanExpression(element, BinaryOperator.LESS_THAN, op1, op2, functionName, !truthValue);

            if (lSuccessor == null) {
              throw new RuntimeException();
            }

            return lSuccessor;
          }
        }
        // a < b
        else if(opType == BinaryOperator.LESS_THAN) {
          if(truthValue) {
            if(newElement.getValueFor(getvarName(leftVarName, functionName)) >=
              newElement.getValueFor(getvarName(rightVarName, functionName))) {

              return InterpreterBottomElement.INSTANCE;
            }
          }
          else {
            AbstractState lSuccessor = propagateBooleanExpression(element, BinaryOperator.GREATER_EQUAL, op1, op2, functionName, !truthValue);

            if (lSuccessor == null) {
              throw new RuntimeException();
            }

            return lSuccessor;
          }
        }
        // a <= b
        else if(opType == BinaryOperator.LESS_EQUAL) {
          if(truthValue) {
            if(newElement.getValueFor(getvarName(leftVarName, functionName)) >
              newElement.getValueFor(getvarName(rightVarName, functionName))) {

              return InterpreterBottomElement.INSTANCE;
            }
          }
          else {
            AbstractState lSuccessor = propagateBooleanExpression(element, BinaryOperator.GREATER_THAN, op1, op2, functionName, !truthValue);

            if (lSuccessor == null) {
              throw new RuntimeException();
            }

            return lSuccessor;
          }
        }
        else{
          //throw new UnrecognizedCFAEdgeException("Unhandled case ");
          throw new RuntimeException("Unhandled Case");
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

            CType expressionType = literalExp.getExpressionType();

            if (expressionType instanceof CSimpleType && ((CSimpleType)expressionType).getType() == CBasicType.INT)
            /*
            int typeOfLiteral = literalExp.getKind();
            if( typeOfLiteral ==  CLiteralExpression.lk_integer_constant
                //  || typeOfLiteral == IASTLiteralExpression.lk_float_constant
            )*/
            {
              String literalValue = op2.toASTString();
              if(literalValue.contains("L") || literalValue.contains("U")){
                literalValue = literalValue.replace("L", "");
                literalValue = literalValue.replace("U", "");
              }

              int valueOfLiteral = Integer.valueOf(literalValue).intValue();

              // a == 9
              if(opType == BinaryOperator.EQUALS) {
                if(truthValue){
                  String lVariableName = getvarName(varName, functionName);

                  if(newElement.contains(lVariableName)){
                    if(newElement.getValueFor(lVariableName) != valueOfLiteral){
                      return InterpreterBottomElement.INSTANCE;
                    }
                  }
                  else{
                    newElement.assignConstant(getvarName(varName, functionName), valueOfLiteral);
                    throw new RuntimeException();
                  }
                }
                // ! a == 9
                else {
                  AbstractState lSuccessor = propagateBooleanExpression(element, BinaryOperator.NOT_EQUALS, op1, op2, functionName, !truthValue);

                  if (lSuccessor == null) {
                    throw new RuntimeException();
                  }

                  return lSuccessor;
                }
              }
              // a != 9
              else if(opType == BinaryOperator.NOT_EQUALS)
              {
                if(truthValue){
                  String lVariableName = getvarName(varName, functionName);

                  if(newElement.contains(lVariableName)){
                    if(newElement.getValueFor(lVariableName) == valueOfLiteral){
                      return InterpreterBottomElement.INSTANCE;
                    }
                  }
                  else{
                  }
                }
                // ! a != 9
                else {
                  AbstractState lSuccessor = propagateBooleanExpression(element, BinaryOperator.EQUALS, op1, op2, functionName, !truthValue);

                  if (lSuccessor == null) {
                    throw new RuntimeException();
                  }

                  return lSuccessor;
                }
              }

              // a > 9
              else if(opType == BinaryOperator.GREATER_THAN)
              {
                if(truthValue){
                  if(newElement.contains(getvarName(varName, functionName))){
                    if(newElement.getValueFor(getvarName(varName, functionName)) <= valueOfLiteral){
                      throw new RuntimeException();
                      //return null;
                    }
                  }
                  else{
                  }
                }
                else {
                  AbstractState lSuccessor = propagateBooleanExpression(element, BinaryOperator.LESS_EQUAL, op1, op2, functionName, !truthValue);

                  if (lSuccessor == null) {
                    throw new RuntimeException();
                  }

                  return lSuccessor;
                }
              }
              // a >= 9
              else if(opType == BinaryOperator.GREATER_EQUAL)
              {
                if(truthValue){
                  if(newElement.contains(getvarName(varName, functionName))){
                    if(newElement.getValueFor(getvarName(varName, functionName)) < valueOfLiteral){
                      throw new RuntimeException();
                      //return null;
                    }
                  }
                  else{
                  }
                }
                else {
                  AbstractState lSuccessor = propagateBooleanExpression(element, BinaryOperator.LESS_THAN, op1, op2, functionName, !truthValue);

                  if (lSuccessor == null) {
                    throw new RuntimeException();
                  }

                  return lSuccessor;
                }
              }
              // a < 9
              else if(opType == BinaryOperator.LESS_THAN)
              {
                if(truthValue){
                  if(newElement.contains(getvarName(varName, functionName))){
                    if(newElement.getValueFor(getvarName(varName, functionName)) >= valueOfLiteral){
                      throw new RuntimeException();
                      //return null;
                    }
                  }
                  else{
                  }
                }
                else {
                  AbstractState lSuccessor = propagateBooleanExpression(element, BinaryOperator.GREATER_EQUAL, op1, op2, functionName, !truthValue);

                  if (lSuccessor == null) {
                    throw new RuntimeException();
                  }

                  return lSuccessor;
                }
              }
              // a <= 9
              else if(opType == BinaryOperator.LESS_EQUAL)
              {
                if(truthValue){
                  if(newElement.contains(getvarName(varName, functionName))){
                    if(newElement.getValueFor(getvarName(varName, functionName)) > valueOfLiteral){
                      throw new RuntimeException();
                      //return null;
                    }
                  }
                  else{
                  }
                }
                else {
                  AbstractState lSuccessor = propagateBooleanExpression(element, BinaryOperator.GREATER_THAN, op1, op2, functionName, !truthValue);

                  if (lSuccessor == null) {
                    throw new RuntimeException();
                  }

                  return lSuccessor;
                }
              }
              else{
                //throw new UnrecognizedCFAEdgeException("Unhandled case ");
                throw new RuntimeException("Unhandled Case"); // TODO improve type of exception
              }
            }
            else{
              //throw new UnrecognizedCFAEdgeException("Unhandled case ");
              throw new RuntimeException("Unhandled Case"); // TODO improve type of exception
            }
          }
          else{
            //throw new UnrecognizedCFAEdgeException("Unhandled case ");
            throw new RuntimeException("Unhandled Case"); // TODO improve type of exception
          }
        }
        // right hand side is a cast exp
        else if(op2 instanceof CCastExpression){
          CCastExpression castExp = (CCastExpression)op2;
          CExpression exprInCastOp = castExp.getOperand();

          AbstractState lSuccessor = propagateBooleanExpression(element, opType, op1, exprInCastOp, functionName, truthValue);

          if (lSuccessor == null) {
            throw new RuntimeException();
          }

          return lSuccessor;
        }
        else{
          //throw new UnrecognizedCFAEdgeException("Unhandled case ");
          throw new RuntimeException("Unhandled Case"); // TODO improve type of exception
        }
      }
      else if(op2 instanceof CBinaryExpression){
        String varName = op1.toASTString();
        // TODO forgetting
        newElement.forget(varName);
      }
      else{
      String varName = op1.toASTString();
      // TODO forgetting
      newElement.forget(varName);
//        System.out.println(op2);
//        System.out.println(op2.getRawSignature());
//        System.exit(0);
//        throw new UnrecognizedCFAEdgeException("Unhandled case ");
      }
    }
    else if(op1 instanceof CCastExpression){
      CCastExpression castExp = (CCastExpression) op1;
      CExpression castOperand = castExp.getOperand();

      AbstractState lSuccessor = propagateBooleanExpression(element, opType, castOperand, op2, functionName, truthValue);

      if (lSuccessor == null) {
        throw new RuntimeException();
      }

      return lSuccessor;
    }
    else{

     System.out.println(op2.toASTString());
     System.out.println(op1.toASTString());

    String varName = op1.toASTString();
    // TODO forgetting
    newElement.forget(varName);
//      throw new UnrecognizedCFAEdgeException("Unhandled case " );
    }

    return newElement;
  }

//  private Boolean getBooleanExpressionValue(ExplicitElement element,
//                              IASTExpression expression, CFAEdge cfaEdge, boolean truthValue)
//                              throws UnrecognizedCCodeException {
//    if (expression instanceof IASTUnaryExpression) {
//      // [!exp]
//      IASTUnaryExpression unaryExp = ((IASTUnaryExpression)expression);
//
//      switch (unaryExp.getOperator()) {
//
//      case IASTUnaryExpression.op_bracketedPrimary: // [(exp)]
//        return getBooleanExpressionValue(element, unaryExp.getOperand(), cfaEdge, truthValue);
//
//      case IASTUnaryExpression.NOT: // [! exp]
//        return getBooleanExpressionValue(element, unaryExp.getOperand(), cfaEdge, !truthValue);
//
//      default:
//        throw new UnrecognizedCCodeException(cfaEdge, unaryExp);
//      }
//
//    } else if (expression instanceof IASTIdExpression) {
//      // [exp]
//      String functionName = cfaEdge.getPredecessor().getFunctionName();
//      String varName = getvarName(expression.getRawSignature(), functionName);
//
//      if (element.contains(varName)) {
//        boolean expressionValue = (element.getValueFor(varName) != 0); // != 0 is true, == 0 is false
//
//        return expressionValue == truthValue;
//
//      } else {
//        return null;
//      }
//
//    } else if (expression instanceof IASTBinaryExpression) {
//      // [exp1 == exp2]
//      String functionName = cfaEdge.getPredecessor().getFunctionName();
//      IASTBinaryExpression binExp = ((IASTBinaryExpression)expression);
//
//      Long val1 = getExpressionValue(element, binExp.getOperand1(), functionName, cfaEdge);
//      Long val2 = getExpressionValue(element, binExp.getOperand2(), functionName, cfaEdge);
//
//      if (val1 != null && val2 != null) {
//        boolean expressionValue;
//
//        switch (binExp.getOperator()) {
//        case equals:
//          expressionValue = val1.equals(val2);
//          break;
//
//        case notequals:
//          expressionValue = !val1.equals(val2);
//          break;
//
//        case greaterThan:
//          expressionValue = val1 > val2;
//          break;
//
//        case greaterEqual:
//          expressionValue = val1 >= val2;
//          break;
//
//        case lessThan:
//          expressionValue = val1 < val2;
//          break;
//
//        case lessEqual:
//          expressionValue = val1 <= val2;
//          break;
//
//        default:
//          throw new UnrecognizedCCodeException(cfaEdge, binExp);
//        }
//
//        return expressionValue == truthValue;
//
//      } else {
//        return null;
//      }
//
//    } else if (expression instanceof IASTCastExpression) {
//        IASTCastExpression castExpr = (IASTCastExpression) expression;
//        return getBooleanExpressionValue(element, castExpr.getOperand(), cfaEdge, truthValue);
//
//    }
//
//  {
//      // TODO fields, arrays
//      throw new UnrecognizedCCodeException(cfaEdge, expression);
//    }
//  }

  private InterpreterElement handleDeclaration(InterpreterElement element,
      CDeclarationEdge declarationEdge) {

    InterpreterElement newElement = element.clone();

    if (declarationEdge.getDeclaration().getName() != null) {

        // get the variable name in the declarator
        String varName = declarationEdge.getDeclaration().getName().toString();

        // TODO check other types of variables later - just handle primitive
        // types for the moment
        // don't add pointer variables to the list since we don't track them

        if (declarationEdge.getDeclaration().getType() instanceof CPointerType) {
          return newElement;
        }
        // if this is a global variable, add to the list of global variables
        if(declarationEdge.getDeclaration().isGlobal())
        {
          globalVars.add(varName);
          // global declarations are set to 0
          // FIXME this forgets initializers!
          newElement.assignConstant(varName, 0);
        }
    }
    return newElement;
  }

  private InterpreterElement handleStatement(InterpreterElement element,
      CStatement expression, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException, ReadingFromNondetVariableException, AccessToUninitializedVariableException, MissingInputException {
    // expression is an assignment operation, e.g. a = b;
    if (expression instanceof CAssignment) {
      return handleAssignment(element, (CAssignment)expression, cfaEdge);
    }
    else if(expression instanceof CFunctionCallStatement){
      // do nothing
      return element.clone();
    }
    // there is such a case
    else if(expression instanceof CExpressionStatement){
      return element.clone();
    }
    else{
      throw new UnrecognizedCCodeException(expression.toASTString(), cfaEdge);
    }
  }

  private InterpreterElement handleAssignment(InterpreterElement element,
                            CAssignment assignExpression, CFAEdge cfaEdge)
                            throws UnrecognizedCCodeException, MissingInputException, ReadingFromNondetVariableException, AccessToUninitializedVariableException {

    CExpression op1 = assignExpression.getLeftHandSide();
    CRightHandSide op2 = assignExpression.getRightHandSide();


    if(op1 instanceof CIdExpression) {
      // a = ...
      return handleAssignmentToVariable(element, op1.toASTString(), op2, cfaEdge);

    } /*else if (op1 instanceof IASTUnaryExpression
        && ((IASTUnaryExpression)op1).getOperator() == IASTUnaryExpression.STAR) {
      // *a = ...

      op1 = ((IASTUnaryExpression)op1).getOperand();

      // Cil produces code like
      // *((int*)__cil_tmp5) = 1;
      // so remove parentheses and cast
      if (op1 instanceof IASTUnaryExpression
          && ((IASTUnaryExpression)op1).getOperator() == IASTUnaryExpression.op_bracketedPrimary) {

        op1 = ((IASTUnaryExpression)op1).getOperand();
      }
      if (op1 instanceof IASTCastExpression) {
        op1 = ((IASTCastExpression)op1).getOperand();
      }

      if (op1 instanceof IASTIdExpression) {
        missingInformationLeftPointer = op1.getRawSignature();
        missingInformationRightExpression = op2;

      } else {
        throw new UnrecognizedCCodeException("left operand of assignment has to be a variable", cfaEdge, op1);
      }

      return element.clone();

    } else if (op1 instanceof IASTFieldReference) {
      // TODO assignment to field
      //return element.clone();

      throw new RuntimeException();
    } else if (op1 instanceof IASTArraySubscriptExpression) {
      // TODO assignment to array cell
      //return element.clone();

      throw new RuntimeException();
    }*/ else {
      throw new UnrecognizedCCodeException("left operand of assignment has to be a variable", cfaEdge, op1);
    }
  }

  private InterpreterElement handleAssignmentToVariable(InterpreterElement element,
                          String lParam, CRightHandSide rightExp, CFAEdge cfaEdge) throws UnrecognizedCCodeException, MissingInputException, ReadingFromNondetVariableException, AccessToUninitializedVariableException {
    String functionName = cfaEdge.getPredecessor().getFunctionName();

    // a = 8.2 or "return;" (when rightExp == null)
    if(rightExp == null || rightExp instanceof CLiteralExpression){
      return handleAssignmentOfLiteral(element, lParam, (CLiteralExpression)rightExp, functionName);
    }
    // a = b
    else if (rightExp instanceof CIdExpression){
      return handleAssignmentOfVariable(element, lParam, (CIdExpression)rightExp, functionName);
    }
    // a = (cast) ?
    else if(rightExp instanceof CCastExpression) {
      return handleAssignmentOfCast(element, lParam, (CCastExpression)rightExp, cfaEdge);
    }
    // a = -b
    else if(rightExp instanceof CUnaryExpression){
      return handleAssignmentOfUnaryExp(element, lParam, (CUnaryExpression)rightExp, cfaEdge);
    }
    // a = b op c
    else if(rightExp instanceof CBinaryExpression){
      CBinaryExpression binExp = (CBinaryExpression)rightExp;

      InterpreterElement lSuccessor = handleAssignmentOfBinaryExp(element, lParam, binExp.getOperand1(),
          binExp.getOperand2(), binExp.getOperator(), cfaEdge);

      if (lSuccessor == null) {
        throw new RuntimeException();
      }

      return lSuccessor;
    }
    else{
      throw new UnrecognizedCCodeException(rightExp.toASTString(), cfaEdge);
    }
  }

  private InterpreterElement handleAssignmentOfCast(InterpreterElement element,
                              String lParam, CCastExpression castExp, CFAEdge cfaEdge)
                              throws UnrecognizedCCodeException, MissingInputException, ReadingFromNondetVariableException, AccessToUninitializedVariableException
  {
    CExpression castOperand = castExp.getOperand();

    InterpreterElement lSuccessor = handleAssignmentToVariable(element, lParam, castOperand, cfaEdge);

    if (lSuccessor == null) {
      throw new RuntimeException();
    }

    return lSuccessor;
  }

  private InterpreterElement handleAssignmentOfUnaryExp(InterpreterElement element,
                                      String lParam, CUnaryExpression unaryExp, CFAEdge cfaEdge)
                                      throws UnrecognizedCCodeException, MissingInputException, ReadingFromNondetVariableException, AccessToUninitializedVariableException {

    String functionName = cfaEdge.getPredecessor().getFunctionName();
    // name of the updated variable, so if a = -b is handled, lParam is a
    String assignedVar = getvarName(lParam, functionName);
    InterpreterElement newElement = element.clone();

    // TODO handle PointerExpressions !!!
    /*CExpression unaryOperand = unaryExp.getOperand();
    UnaryOperator unaryOperator = unaryExp.getOperator();

    if (unaryOperator == UnaryOperator.STAR) {
      // a = * b
      newElement.forget(assignedVar);

      // Cil produces code like
      // __cil_tmp8 = *((int *)__cil_tmp7);
      // so remove cast
      if (unaryOperand instanceof CCastExpression) {
        unaryOperand = ((CCastExpression)unaryOperand).getOperand();
      }

      if (unaryOperand instanceof CIdExpression) {
        missingInformationLeftVariable = assignedVar;
        missingInformationRightPointer = unaryOperand.toASTString();
      } else{
        throw new UnrecognizedCCodeException(unaryOperand.toASTString(), cfaEdge);
      }
    }
    else {*/
      // a = -b or similar
      Long value = getExpressionValue(element, unaryExp, functionName, cfaEdge);
      if (value != null) {
        newElement.assignConstant(assignedVar, value);
      } else {
        System.out.println("FORGETTING: " + unaryExp.toASTString());
        newElement.forget(assignedVar);
      }
    //}

    return newElement;
  }

  private InterpreterElement handleAssignmentOfBinaryExp(InterpreterElement element,
                       String lParam, CExpression lVarInBinaryExp, CExpression rVarInBinaryExp,
                       BinaryOperator binaryOperator, CFAEdge cfaEdge)
                       throws UnrecognizedCCodeException, MissingInputException, ReadingFromNondetVariableException, AccessToUninitializedVariableException {

    String functionName = cfaEdge.getPredecessor().getFunctionName();
    // name of the updated variable, so if a = b + c is handled, lParam is a
    String assignedVar = getvarName(lParam, functionName);
    InterpreterElement newElement = element.clone();

    switch (binaryOperator) {
    case PLUS:
    case MINUS:
    case MULTIPLY:
    case GREATER_THAN:
    case GREATER_EQUAL:
    case LESS_THAN:
    case LESS_EQUAL:
    case EQUALS:
    case NOT_EQUALS:
    case SHIFT_RIGHT:
    case SHIFT_LEFT:
    case DIVIDE:
    case MODULO:
    case BINARY_AND:
    case BINARY_OR:


      Long val1;
      Long val2;

      // TODO handle PointerExpressions!
      /*if(lVarInBinaryExp instanceof CUnaryExpression
          && ((CUnaryExpression)lVarInBinaryExp).getOperator() == UnaryOperator.STAR) {
        // a = *b + c
        // TODO prepare for using strengthen operator to dereference pointer
        val1 = null;

        throw new RuntimeException();
      } else {*/
        val1 = getExpressionValue(element, lVarInBinaryExp, functionName, cfaEdge);
      //}

      if (val1 != null) {
        val2 = getExpressionValue(element, rVarInBinaryExp, functionName, cfaEdge);
      } else {
        val2 = null;
      }

      if (val2 != null) { // this implies val1 != null

        long lValue1 = val1.longValue();
        long lValue2 = val2.longValue();

        long value;

        switch (binaryOperator) {

        case PLUS:
          value = lValue1 + lValue2;
          break;

        case MINUS:
          value = lValue1 - lValue2;
          break;

        case MULTIPLY:
          value = lValue1 * lValue2;
          break;

        case GREATER_THAN:
          value = (lValue1 > lValue2)?1:0;
          break;

        case GREATER_EQUAL:
          value = (lValue1 >= lValue2)?1:0;
          break;

        case LESS_THAN:
          value = (lValue1 < lValue2)?1:0;
          break;

        case LESS_EQUAL:
          value = (lValue1 <= lValue2)?1:0;
          break;

        case EQUALS:
          value = (lValue1 == lValue2)?1:0;
          break;

        case NOT_EQUALS:
          value = (lValue1 != lValue2)?1:0;
          break;

        case SHIFT_RIGHT:
          value = lValue1 >> lValue2;
          break;

        case SHIFT_LEFT:
          value = lValue1 << lValue2;
          break;

        case DIVIDE:
          value = lValue1 / lValue2;
          break;

        case MODULO:
          value = lValue1 % lValue2;
          break;

        case BINARY_AND:
          value = lValue1 & lValue2;
          break;

        case BINARY_OR:
          value = lValue1 | lValue2;
          break;

        default:
          throw new UnrecognizedCCodeException("unkown binary operator", cfaEdge);
        }

        newElement.assignConstant(assignedVar, value);
      } else {
        throw new RuntimeException();
      }
      break;
    default:
      {
        throw new UnrecognizedCCodeException("unkown binary operator", cfaEdge);
      }
    }
    return newElement;
  }

  private Long getExpressionValue(InterpreterElement element, CExpression expression,
                                  String functionName, CFAEdge cfaEdge) throws UnrecognizedCCodeException, MissingInputException, ReadingFromNondetVariableException, AccessToUninitializedVariableException {

    if (expression instanceof CLiteralExpression) {
      return parseLiteral(expression);

    } else if (expression instanceof CIdExpression) {
      String varName = getvarName(expression.toASTString(), functionName);

      return element.getValueFor(varName);
    } else if (expression instanceof CCastExpression) {
      return getExpressionValue(element, ((CCastExpression)expression).getOperand(),
                                functionName, cfaEdge);

    } else if (expression instanceof CUnaryExpression) {
      CUnaryExpression unaryExpression = (CUnaryExpression)expression;
      UnaryOperator unaryOperator = unaryExpression.getOperator();
      CExpression unaryOperand = unaryExpression.getOperand();

      switch (unaryOperator) {

      case MINUS:
        Long val = getExpressionValue(element, unaryOperand, functionName, cfaEdge);
        return (val != null) ? -val : null;

      case AMPER:
        return null; // valid expresion, but it's a pointer value

      default:
        throw new UnrecognizedCCodeException("unknown unary operator", cfaEdge, unaryExpression);
      }
    } else {
      // TODO fields, arrays
      throw new UnrecognizedCCodeException(expression.toASTString(), cfaEdge);
    }
  }

  private InterpreterElement handleAssignmentOfVariable(InterpreterElement element,
      String lParam, CExpression op2, String functionName) throws MissingInputException, ReadingFromNondetVariableException, AccessToUninitializedVariableException
  {
    String rParam = op2.toASTString();

    String leftVarName = getvarName(lParam, functionName);

    if (rParam.equals("__BLAST_NONDET")) {
      InterpreterElement newElement = element.nextInputElement();

      // We return the input of the current element, the successor element
      // will refer to the next input.
      newElement.assignConstant(leftVarName, element.getCurrentInput());

      return newElement;
    }
    else {
      String rightVarName = getvarName(rParam, functionName);

      InterpreterElement newElement = element.clone();
      newElement.assignConstant(leftVarName, newElement.getValueFor(rightVarName));

      return newElement;
    }
  }

  private InterpreterElement handleAssignmentOfLiteral(InterpreterElement element,
                        String lParam, CExpression op2, String functionName)
                        throws UnrecognizedCCodeException
  {
    InterpreterElement newElement = element.clone();

    // op2 may be null if this is a "return;" statement
    Long val = (op2 == null ? 0L : parseLiteral(op2));

    String assignedVar = getvarName(lParam, functionName);
    if (val != null) {
      newElement.assignConstant(assignedVar, val);
    } else {
      newElement.forget(assignedVar);
    }

    return newElement;
  }

  private Long parseLiteral(CExpression expression) throws UnrecognizedCCodeException {
    if (expression instanceof CLiteralExpression) {

      CLiteralExpression literalExpression = (CLiteralExpression)expression;
      CType expressionType = literalExpression.getExpressionType();
      if (expressionType instanceof CSimpleType && ((CSimpleType)expressionType).getType() == CBasicType.INT) {

      /*int typeOfLiteral = ((CLiteralExpression)expression).getKind();
      if (typeOfLiteral == CLiteralExpression.lk_integer_constant) {*/

        String s = expression.toASTString();
        if(s.endsWith("L") || s.endsWith("U") || s.endsWith("UL")){
          s = s.replace("L", "");
          s = s.replace("U", "");
          s = s.replace("UL", "");
        }
        try {
          return Long.valueOf(s);
        } catch (NumberFormatException e) {
          throw new UnrecognizedCCodeException("invalid integer literal", null, expression);
        }
      }
      else { // TODO fix
        throw new RuntimeException();
      }
      /*
      if (typeOfLiteral == CLiteralExpression.lk_string_literal) {
        return (long) expression.hashCode();
      }*/
    }
    return null;
  }

  public String getvarName(String variableName, String functionName){
    if(globalVars.contains(variableName)){
      return variableName;
    }
    return functionName + "::" + variableName;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState element,
                                    List<AbstractState> elements,
                                    CFAEdge cfaEdge, Precision precision) throws UnrecognizedCCodeException, MissingInputException, ReadingFromNondetVariableException, AccessToUninitializedVariableException {

    assert element instanceof InterpreterElement;
    InterpreterElement explicitElement = (InterpreterElement)element;

    for (AbstractState ae : elements) {
      if (ae instanceof PointerState) {
        return strengthen(explicitElement, (PointerState)ae, cfaEdge, precision);
      }
      else if (ae instanceof ConstrainedAssumeState) {
        return strengthen(cfaEdge.getSuccessor(), explicitElement, (ConstrainedAssumeState)ae, precision);
      }
      else if (ae instanceof GuardedEdgeAutomatonPredicateElement) {
        return strengthen(cfaEdge.getSuccessor(), explicitElement, (GuardedEdgeAutomatonPredicateElement)ae, precision);
      }
    }
    return null;
  }

  private Collection<? extends AbstractState> strengthen(InterpreterElement explicitElement,
      PointerState pointerElement, CFAEdge cfaEdge, Precision precision) throws UnrecognizedCCodeException, MissingInputException, ReadingFromNondetVariableException, AccessToUninitializedVariableException {

    List<InterpreterElement> retList = new ArrayList<>();

    if (missingInformationLeftVariable != null && missingInformationRightPointer != null) {

      String rightVar = derefPointerToVariable(pointerElement, missingInformationRightPointer);
      if (rightVar != null) {
        rightVar = getvarName(rightVar, cfaEdge.getPredecessor().getFunctionName());
        if (explicitElement.contains(rightVar)) {
          explicitElement.assignConstant(missingInformationLeftVariable,
              explicitElement.getValueFor(rightVar));
        }
      }
      missingInformationLeftVariable = null;
      missingInformationRightPointer = null;

    } else if (missingInformationLeftPointer != null && missingInformationRightExpression != null) {

      String leftVar = derefPointerToVariable(pointerElement, missingInformationLeftPointer);
      if (leftVar != null) {
        leftVar = getvarName(leftVar, cfaEdge.getPredecessor().getFunctionName());
        retList.add(handleAssignmentToVariable(explicitElement, leftVar, missingInformationRightExpression, cfaEdge));
        return retList;
      }

      missingInformationLeftPointer = null;
      missingInformationRightExpression = null;
    }
    return null;
  }

  private String derefPointerToVariable(PointerState pointerElement,
                                        String pointer) {
    throw new RuntimeException();
    // TODO fix
    /*
    Pointer p = pointerElement.lookupPointer(pointer);
    if (p != null && p.getNumberOfTargets() == 1) {
      Memory.PointerTarget target = p.getFirstTarget();
      if (target instanceof Memory.Variable) {
        return ((Memory.Variable)target).getVarName();
      } else if (target instanceof Memory.StackArrayCell) {
        return ((Memory.StackArrayCell)target).getVarName();
      }
    }
    return null;*/
  }

  public Collection<? extends AbstractState> strengthen(CFANode pNode, InterpreterElement pElement, GuardedEdgeAutomatonPredicateElement pAutomatonElement, Precision pPrecision) {
    AbstractState lResultElement = pElement;

    for (ECPPredicate lPredicate : pAutomatonElement) {
      AssumeEdge lEdge = ToTigerAssumeEdgeTranslator.translate(pNode, lPredicate);

      try {
        Collection<? extends AbstractState> lResult = getAbstractSuccessors(lResultElement, pPrecision, lEdge);

        if (lResult.size() == 0) {
          return Collections.emptySet();
        }
        else if (lResult.size() == 1) {
          lResultElement = lResult.iterator().next();
        }
        else {
          throw new RuntimeException();
        }

        return lResult;
      } catch (CPATransferException e) {
        throw new RuntimeException(e);
      }
    }

    return Collections.singleton(lResultElement);
  }

  public Collection<? extends AbstractState> strengthen(CFANode pNode, InterpreterElement pElement, ConstrainedAssumeState pAssumeElement, Precision pPrecision) {
    AssumeEdge lEdge = new AssumeEdge(pAssumeElement.getExpression().toASTString(), pNode.getLineNumber(), pNode, pNode, pAssumeElement.getExpression(), true);

    try {
      Collection<? extends AbstractState> lResult = getAbstractSuccessors(pElement, pPrecision, lEdge);

      return lResult;
    } catch (CPATransferException e) {
      throw new RuntimeException(e);
    }
  }

}
