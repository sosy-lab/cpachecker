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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.IASTArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTAssignment;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.IASTCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTPointerTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.IASTStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
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
import org.sosy_lab.cpachecker.cpa.assume.ConstrainedAssumeElement;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonPredicateElement;
import org.sosy_lab.cpachecker.cpa.interpreter.exceptions.AccessToUninitializedVariableException;
import org.sosy_lab.cpachecker.cpa.interpreter.exceptions.MissingInputException;
import org.sosy_lab.cpachecker.cpa.interpreter.exceptions.ReadingFromNondetVariableException;
import org.sosy_lab.cpachecker.cpa.pointer.Memory;
import org.sosy_lab.cpachecker.cpa.pointer.Pointer;
import org.sosy_lab.cpachecker.cpa.pointer.PointerElement;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.fshell.fql2.translators.cfa.ToFlleShAssumeEdgeTranslator;
import org.sosy_lab.cpachecker.util.ecp.ECPPredicate;

public class InterpreterTransferRelation implements TransferRelation {
  public static int TRCOUNT=0;
  public final static ArrayList<String> TRLIST = new ArrayList<String>();
  private final Set<String> globalVars = new HashSet<String>();

  private String missingInformationLeftVariable = null;
  private String missingInformationRightPointer = null;
  private String missingInformationLeftPointer  = null;
  private IASTExpression missingInformationRightExpression = null;

  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(
      AbstractElement element, Precision precision, CFAEdge cfaEdge) throws CPATransferException {
    AbstractElement successor;
    InterpreterElement explicitElement = (InterpreterElement)element;
    TRCOUNT++;
    TRLIST.add(cfaEdge.getRawStatement());
    // check the type of the edge
    switch (cfaEdge.getEdgeType ()) {

    // if edge is a statement edge, e.g. a = b + c
    case StatementEdge: {
      StatementEdge statementEdge = (StatementEdge) cfaEdge;
      successor = handleStatement(explicitElement, statementEdge.getStatement(), cfaEdge);

      // TODO remove
      if (successor == null) {
        throw new RuntimeException();
      }
      break;
    }

    case ReturnStatementEdge: {
      ReturnStatementEdge returnEdge = (ReturnStatementEdge)cfaEdge;
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
      DeclarationEdge declarationEdge = (DeclarationEdge) cfaEdge;
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
      FunctionCallEdge functionCallEdge = (FunctionCallEdge) cfaEdge;
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
      FunctionReturnEdge functionReturnEdge = (FunctionReturnEdge) cfaEdge;
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

    CallToReturnEdge summaryEdge =
      functionReturnEdge.getSuccessor().getEnteringSummaryEdge();
    IASTFunctionCall exprOnSummary = summaryEdge.getExpression();

    // TODO get from stack
    /*InterpreterElement previousElem = element.getPreviousElement();
    InterpreterElement newElement = previousElem.clone();

    newElement.setInputIndex(element.getInputIndex());*/

    InterpreterElement newElement = element.getUpdatedPreviousElement();

    String callerFunctionName = functionReturnEdge.getSuccessor().getFunctionName();
    String calledFunctionName = functionReturnEdge.getPredecessor().getFunctionName();

    //System.out.println(exprOnSummary.getRawSignature());
    //expression is an assignment operation, e.g. a = g(b);
    if (exprOnSummary instanceof IASTFunctionCallAssignmentStatement) {
      IASTFunctionCallAssignmentStatement binExp = ((IASTFunctionCallAssignmentStatement)exprOnSummary);
      IASTExpression op1 = binExp.getLeftHandSide();

      //we expect left hand side of the expression to be a variable
      if(op1 instanceof IASTIdExpression ||
          op1 instanceof IASTFieldReference)
      {
//      IASExpression leftHandSideVar = op1;
        String varName = op1.getRawSignature();
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
    else if (exprOnSummary instanceof IASTFunctionCallStatement)
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
      throw new UnrecognizedCCodeException("on function return", summaryEdge, exprOnSummary.asStatement());
    }

    return newElement;
  }

  private InterpreterElement handleFunctionCall(InterpreterElement element,
      FunctionCallEdge callEdge)
  throws UnrecognizedCCodeException, ReadingFromNondetVariableException, AccessToUninitializedVariableException {

    FunctionDefinitionNode functionEntryNode = callEdge.getSuccessor();
    String calledFunctionName = functionEntryNode.getFunctionName();
    String callerFunctionName = callEdge.getPredecessor().getFunctionName();

    List<String> paramNames = functionEntryNode.getFunctionParameterNames();
    List<IASTExpression> arguments = callEdge.getArguments();

    assert (paramNames.size() == arguments.size());

    InterpreterElement newElement = new InterpreterElement(element, element.getInputIndex(), element.getInputs());

    for(String globalVar:globalVars){
      if(element.contains(globalVar)){
        newElement.getConstantsMap().put(globalVar, element.getValueFor(globalVar));
      }
    }

    for(int i=0; i<arguments.size(); i++){
      IASTExpression arg = arguments.get(i);
      if (arg instanceof IASTCastExpression) {
        // ignore casts
        arg = ((IASTCastExpression)arg).getOperand();
      }

      String nameOfParam = paramNames.get(i);
      String formalParamName = getvarName(nameOfParam, calledFunctionName);
      if(arg instanceof IASTIdExpression){
        IASTIdExpression idExp = (IASTIdExpression) arg;
        String nameOfArg = idExp.getName();
        String actualParamName = getvarName(nameOfArg, callerFunctionName);

        if(element.contains(actualParamName)){
          newElement.assignConstant(formalParamName, element.getValueFor(actualParamName));
        }
      }

      else if(arg instanceof IASTLiteralExpression){
        Long val = parseLiteral(arg);

        if (val != null) {
          newElement.assignConstant(formalParamName, val);
        } else {
          // TODO forgetting
          newElement.forget(formalParamName);
        }
      }

      else if(arg instanceof IASTTypeIdExpression){
        newElement.forget(formalParamName);
      }

      else if(arg instanceof IASTUnaryExpression){
        IASTUnaryExpression unaryExp = (IASTUnaryExpression) arg;
        assert(unaryExp.getOperator() == UnaryOperator.STAR || unaryExp.getOperator() == UnaryOperator.AMPER);
      }

      else if(arg instanceof IASTFieldReference){
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
      IASTExpression expression,
      ReturnStatementEdge returnEdge)
  throws UnrecognizedCCodeException, ReadingFromNondetVariableException, MissingInputException, AccessToUninitializedVariableException {

    InterpreterElement lSuccessor = handleAssignmentToVariable(element, "___cpa_temp_result_var_", expression, returnEdge);

    if (lSuccessor == null) {
      throw new RuntimeException();
    }

    return lSuccessor;
  }

  private AbstractElement handleAssumption(InterpreterElement element,
                  IASTExpression expression, CFAEdge cfaEdge, boolean truthValue)
                  throws UnrecognizedCFAEdgeException, MissingInputException, ReadingFromNondetVariableException, AccessToUninitializedVariableException {

    String functionName = cfaEdge.getPredecessor().getFunctionName();
    // Binary operation
    if (expression instanceof IASTBinaryExpression) {
      IASTBinaryExpression binExp = ((IASTBinaryExpression)expression);
      BinaryOperator opType = binExp.getOperator ();

      IASTExpression op1 = binExp.getOperand1();
      IASTExpression op2 = binExp.getOperand2();

      AbstractElement lSuccessor = propagateBooleanExpression(element, opType, op1, op2, functionName, truthValue);

      if (lSuccessor == null) {
        throw new RuntimeException();
      }

      return lSuccessor;
    }
    // Unary operation
    else if (expression instanceof IASTUnaryExpression) {
      IASTUnaryExpression unaryExp = ((IASTUnaryExpression)expression);
      // ! exp
      if(unaryExp.getOperator() == UnaryOperator.NOT)
      {
        IASTExpression exp1 = unaryExp.getOperand();
        AbstractElement lSuccessor = handleAssumption(element, exp1, cfaEdge, !truthValue);

        if (lSuccessor == null) {
          throw new RuntimeException();
        }

        return lSuccessor;

      }
      else if(expression instanceof IASTCastExpression) {
        AbstractElement lSuccessor = handleAssumption(element, ((IASTCastExpression)expression).getOperand(), cfaEdge, truthValue);

        if (lSuccessor == null) {
          throw new RuntimeException();
        }

        return lSuccessor;
      }
      else {
        throw new UnrecognizedCFAEdgeException("Unhandled case " + cfaEdge.getRawStatement());
      }
    }
    else if(expression instanceof IASTIdExpression
        || expression instanceof IASTFieldReference) {
      AbstractElement lSuccessor = propagateBooleanExpression(element, null, expression, null, functionName, truthValue);

      if (lSuccessor == null) {
        throw new RuntimeException();
      }

      return lSuccessor;
    }

    else{
      throw new UnrecognizedCFAEdgeException("Unhandled case " + cfaEdge.getRawStatement());
    }

  }

  private AbstractElement propagateBooleanExpression(AbstractElement element,
      BinaryOperator opType,IASTExpression op1,
      IASTExpression op2, String functionName, boolean truthValue)
  throws UnrecognizedCFAEdgeException, ReadingFromNondetVariableException, AccessToUninitializedVariableException {

    InterpreterElement newElement = ((InterpreterElement)element).clone();

    // a (bop) ?
    if (op1 instanceof IASTIdExpression ||
        op1 instanceof IASTFieldReference ||
        op1 instanceof IASTArraySubscriptExpression)
    {
      // [literal]
      if (op2 == null && opType == null){
        String lVariableName = op1.getRawSignature();

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
      else if (op2 instanceof IASTLiteralExpression)
      {
        String varName = op1.getRawSignature();
        int typeOfLiteral = ((IASTLiteralExpression)op2).getKind();
        if ( typeOfLiteral ==  IASTLiteralExpression.lk_integer_constant
            //  || typeOfLiteral == IASTLiteralExpression.lk_float_constant
        )
        {
          String literalString = op2.getRawSignature();
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
              AbstractElement lSuccessor = propagateBooleanExpression(element, BinaryOperator.NOT_EQUALS, op1, op2, functionName, !truthValue);

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
              AbstractElement lSuccessor = propagateBooleanExpression(element, BinaryOperator.EQUALS, op1, op2, functionName, !truthValue);

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
              AbstractElement lSuccessor = propagateBooleanExpression(element, BinaryOperator.LESS_EQUAL, op1, op2, functionName, !truthValue);

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
              AbstractElement lSuccessor = propagateBooleanExpression(element, BinaryOperator.LESS_THAN, op1, op2, functionName, !truthValue);

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
              AbstractElement lSuccessor = propagateBooleanExpression(element, BinaryOperator.GREATER_EQUAL, op1, op2, functionName, !truthValue);

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
              AbstractElement lSuccessor = propagateBooleanExpression(element, BinaryOperator.GREATER_THAN, op1, op2, functionName, !truthValue);

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
              AbstractElement lSuccessor = propagateBooleanExpression(element, BinaryOperator.EQUALS, op1, op2, functionName, !truthValue);

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
            AbstractElement lSuccessor = propagateBooleanExpression(element, BinaryOperator.NOT_EQUALS, op1, op2, functionName, !truthValue);

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
            AbstractElement lSuccessor = propagateBooleanExpression(element, BinaryOperator.EQUALS, op1, op2, functionName, !truthValue);

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
            AbstractElement lSuccessor = propagateBooleanExpression(element, BinaryOperator.LESS_EQUAL, op1, op2, functionName, !truthValue);

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
            AbstractElement lSuccessor = propagateBooleanExpression(element, BinaryOperator.LESS_THAN, op1, op2, functionName, !truthValue);

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
            AbstractElement lSuccessor = propagateBooleanExpression(element, BinaryOperator.GREATER_EQUAL, op1, op2, functionName, !truthValue);

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
            AbstractElement lSuccessor = propagateBooleanExpression(element, BinaryOperator.GREATER_THAN, op1, op2, functionName, !truthValue);

            if (lSuccessor == null) {
              throw new RuntimeException();
            }

            return lSuccessor;
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
                //  || typeOfLiteral == IASTLiteralExpression.lk_float_constant
            )
            {
              String literalValue = op2.getRawSignature();
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
                  AbstractElement lSuccessor = propagateBooleanExpression(element, BinaryOperator.NOT_EQUALS, op1, op2, functionName, !truthValue);

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
                  AbstractElement lSuccessor = propagateBooleanExpression(element, BinaryOperator.EQUALS, op1, op2, functionName, !truthValue);

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
                  AbstractElement lSuccessor = propagateBooleanExpression(element, BinaryOperator.LESS_EQUAL, op1, op2, functionName, !truthValue);

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
                  AbstractElement lSuccessor = propagateBooleanExpression(element, BinaryOperator.LESS_THAN, op1, op2, functionName, !truthValue);

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
                  AbstractElement lSuccessor = propagateBooleanExpression(element, BinaryOperator.GREATER_EQUAL, op1, op2, functionName, !truthValue);

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
                  AbstractElement lSuccessor = propagateBooleanExpression(element, BinaryOperator.GREATER_THAN, op1, op2, functionName, !truthValue);

                  if (lSuccessor == null) {
                    throw new RuntimeException();
                  }

                  return lSuccessor;
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

          AbstractElement lSuccessor = propagateBooleanExpression(element, opType, op1, exprInCastOp, functionName, truthValue);

          if (lSuccessor == null) {
            throw new RuntimeException();
          }

          return lSuccessor;
        }
        else{
          throw new UnrecognizedCFAEdgeException("Unhandled case ");
        }
      }
      else if(op2 instanceof IASTBinaryExpression){
        String varName = op1.getRawSignature();
        // TODO forgetting
        newElement.forget(varName);
      }
      else{
      String varName = op1.getRawSignature();
      // TODO forgetting
      newElement.forget(varName);
//        System.out.println(op2);
//        System.out.println(op2.getRawSignature());
//        System.exit(0);
//        throw new UnrecognizedCFAEdgeException("Unhandled case ");
      }
    }
    else if(op1 instanceof IASTCastExpression){
      IASTCastExpression castExp = (IASTCastExpression) op1;
      IASTExpression castOperand = castExp.getOperand();

      AbstractElement lSuccessor = propagateBooleanExpression(element, opType, castOperand, op2, functionName, truthValue);

      if (lSuccessor == null) {
        throw new RuntimeException();
      }

      return lSuccessor;
    }
    else{

     System.out.println(op2.getRawSignature());
     System.out.println(op1.getRawSignature());

    String varName = op1.getRawSignature();
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
      DeclarationEdge declarationEdge) {

    InterpreterElement newElement = element.clone();
    if (declarationEdge.getName() != null) {

        // get the variable name in the declarator
        String varName = declarationEdge.getName().toString();

        // TODO check other types of variables later - just handle primitive
        // types for the moment
        // don't add pointer variables to the list since we don't track them
        if (declarationEdge.getDeclSpecifier() instanceof IASTPointerTypeSpecifier) {
          return newElement;
        }
        // if this is a global variable, add to the list of global variables
        if(declarationEdge.isGlobal())
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
      IASTStatement expression, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException, ReadingFromNondetVariableException, AccessToUninitializedVariableException, MissingInputException {
    // expression is an assignment operation, e.g. a = b;
    if (expression instanceof IASTAssignment) {
      return handleAssignment(element, (IASTAssignment)expression, cfaEdge);
    }
    else if(expression instanceof IASTFunctionCallStatement){
      // do nothing
      return element.clone();
    }
    // there is such a case
    else if(expression instanceof IASTExpressionStatement){
      return element.clone();
    }
    else{
      throw new UnrecognizedCCodeException(cfaEdge, expression);
    }
  }

  private InterpreterElement handleAssignment(InterpreterElement element,
                            IASTAssignment assignExpression, CFAEdge cfaEdge)
                            throws UnrecognizedCCodeException, MissingInputException, ReadingFromNondetVariableException, AccessToUninitializedVariableException {

    IASTExpression op1 = assignExpression.getLeftHandSide();
    IASTRightHandSide op2 = assignExpression.getRightHandSide();


    if(op1 instanceof IASTIdExpression) {
      // a = ...
      return handleAssignmentToVariable(element, op1.getRawSignature(), op2, cfaEdge);

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
                          String lParam, IASTRightHandSide rightExp, CFAEdge cfaEdge) throws UnrecognizedCCodeException, MissingInputException, ReadingFromNondetVariableException, AccessToUninitializedVariableException {
    String functionName = cfaEdge.getPredecessor().getFunctionName();

    // a = 8.2 or "return;" (when rightExp == null)
    if(rightExp == null || rightExp instanceof IASTLiteralExpression){
      return handleAssignmentOfLiteral(element, lParam, (IASTLiteralExpression)rightExp, functionName);
    }
    // a = b
    else if (rightExp instanceof IASTIdExpression){
      return handleAssignmentOfVariable(element, lParam, (IASTIdExpression)rightExp, functionName);
    }
    // a = (cast) ?
    else if(rightExp instanceof IASTCastExpression) {
      return handleAssignmentOfCast(element, lParam, (IASTCastExpression)rightExp, cfaEdge);
    }
    // a = -b
    else if(rightExp instanceof IASTUnaryExpression){
      return handleAssignmentOfUnaryExp(element, lParam, (IASTUnaryExpression)rightExp, cfaEdge);
    }
    // a = b op c
    else if(rightExp instanceof IASTBinaryExpression){
      IASTBinaryExpression binExp = (IASTBinaryExpression)rightExp;

      InterpreterElement lSuccessor = handleAssignmentOfBinaryExp(element, lParam, binExp.getOperand1(),
          binExp.getOperand2(), binExp.getOperator(), cfaEdge);

      if (lSuccessor == null) {
        throw new RuntimeException();
      }

      return lSuccessor;
    }
    else{
      throw new UnrecognizedCCodeException(cfaEdge, rightExp);
    }
  }

  private InterpreterElement handleAssignmentOfCast(InterpreterElement element,
                              String lParam, IASTCastExpression castExp, CFAEdge cfaEdge)
                              throws UnrecognizedCCodeException, MissingInputException, ReadingFromNondetVariableException, AccessToUninitializedVariableException
  {
    IASTExpression castOperand = castExp.getOperand();

    InterpreterElement lSuccessor = handleAssignmentToVariable(element, lParam, castOperand, cfaEdge);

    if (lSuccessor == null) {
      throw new RuntimeException();
    }

    return lSuccessor;
  }

  private InterpreterElement handleAssignmentOfUnaryExp(InterpreterElement element,
                                      String lParam, IASTUnaryExpression unaryExp, CFAEdge cfaEdge)
                                      throws UnrecognizedCCodeException, MissingInputException, ReadingFromNondetVariableException, AccessToUninitializedVariableException {

    String functionName = cfaEdge.getPredecessor().getFunctionName();
    // name of the updated variable, so if a = -b is handled, lParam is a
    String assignedVar = getvarName(lParam, functionName);
    InterpreterElement newElement = element.clone();

    IASTExpression unaryOperand = unaryExp.getOperand();
    UnaryOperator unaryOperator = unaryExp.getOperator();

    if (unaryOperator == UnaryOperator.STAR) {
      // a = * b
      newElement.forget(assignedVar);

      // Cil produces code like
      // __cil_tmp8 = *((int *)__cil_tmp7);
      // so remove cast
      if (unaryOperand instanceof IASTCastExpression) {
        unaryOperand = ((IASTCastExpression)unaryOperand).getOperand();
      }

      if (unaryOperand instanceof IASTIdExpression) {
        missingInformationLeftVariable = assignedVar;
        missingInformationRightPointer = unaryOperand.getRawSignature();
      } else{
        throw new UnrecognizedCCodeException(cfaEdge, unaryOperand);
      }
    }
    else {
      // a = -b or similar
      Long value = getExpressionValue(element, unaryExp, functionName, cfaEdge);
      if (value != null) {
        newElement.assignConstant(assignedVar, value);
      } else {
        System.out.println("FORGETTING: " + unaryExp.getRawSignature());
        newElement.forget(assignedVar);
      }
    }

    return newElement;
  }

  private InterpreterElement handleAssignmentOfBinaryExp(InterpreterElement element,
                       String lParam, IASTExpression lVarInBinaryExp, IASTExpression rVarInBinaryExp,
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

      if(lVarInBinaryExp instanceof IASTUnaryExpression
          && ((IASTUnaryExpression)lVarInBinaryExp).getOperator() == UnaryOperator.STAR) {
        // a = *b + c
        // TODO prepare for using strengthen operator to dereference pointer
        val1 = null;

        throw new RuntimeException();
      } else {
        val1 = getExpressionValue(element, lVarInBinaryExp, functionName, cfaEdge);
      }

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

  private Long getExpressionValue(InterpreterElement element, IASTExpression expression,
                                  String functionName, CFAEdge cfaEdge) throws UnrecognizedCCodeException, MissingInputException, ReadingFromNondetVariableException, AccessToUninitializedVariableException {

    if (expression instanceof IASTLiteralExpression) {
      return parseLiteral(expression);

    } else if (expression instanceof IASTIdExpression) {
      String varName = getvarName(expression.getRawSignature(), functionName);

      return element.getValueFor(varName);
    } else if (expression instanceof IASTCastExpression) {
      return getExpressionValue(element, ((IASTCastExpression)expression).getOperand(),
                                functionName, cfaEdge);

    } else if (expression instanceof IASTUnaryExpression) {
      IASTUnaryExpression unaryExpression = (IASTUnaryExpression)expression;
      UnaryOperator unaryOperator = unaryExpression.getOperator();
      IASTExpression unaryOperand = unaryExpression.getOperand();

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
      throw new UnrecognizedCCodeException(cfaEdge, expression);
    }
  }

  private InterpreterElement handleAssignmentOfVariable(InterpreterElement element,
      String lParam, IASTExpression op2, String functionName) throws MissingInputException, ReadingFromNondetVariableException, AccessToUninitializedVariableException
  {
    String rParam = op2.getRawSignature();

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
                        String lParam, IASTExpression op2, String functionName)
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

  private Long parseLiteral(IASTExpression expression) throws UnrecognizedCCodeException {
    if (expression instanceof IASTLiteralExpression) {

      int typeOfLiteral = ((IASTLiteralExpression)expression).getKind();
      if (typeOfLiteral == IASTLiteralExpression.lk_integer_constant) {

        String s = expression.getRawSignature();
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
      if (typeOfLiteral == IASTLiteralExpression.lk_string_literal) {
        return (long) expression.hashCode();
      }
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
  public Collection<? extends AbstractElement> strengthen(AbstractElement element,
                                    List<AbstractElement> elements,
                                    CFAEdge cfaEdge, Precision precision) throws UnrecognizedCCodeException, MissingInputException, ReadingFromNondetVariableException, AccessToUninitializedVariableException {

    assert element instanceof InterpreterElement;
    InterpreterElement explicitElement = (InterpreterElement)element;

    for (AbstractElement ae : elements) {
      if (ae instanceof PointerElement) {
        return strengthen(explicitElement, (PointerElement)ae, cfaEdge, precision);
      }
      else if (ae instanceof ConstrainedAssumeElement) {
        return strengthen(cfaEdge.getSuccessor(), explicitElement, (ConstrainedAssumeElement)ae, precision);
      }
      else if (ae instanceof GuardedEdgeAutomatonPredicateElement) {
        return strengthen(cfaEdge.getSuccessor(), explicitElement, (GuardedEdgeAutomatonPredicateElement)ae, precision);
      }
    }
    return null;
  }

  private Collection<? extends AbstractElement> strengthen(InterpreterElement explicitElement,
      PointerElement pointerElement, CFAEdge cfaEdge, Precision precision) throws UnrecognizedCCodeException, MissingInputException, ReadingFromNondetVariableException, AccessToUninitializedVariableException {

    List<InterpreterElement> retList = new ArrayList<InterpreterElement>();

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

  private String derefPointerToVariable(PointerElement pointerElement,
                                        String pointer) {
    Pointer p = pointerElement.lookupPointer(pointer);
    if (p != null && p.getNumberOfTargets() == 1) {
      Memory.PointerTarget target = p.getFirstTarget();
      if (target instanceof Memory.Variable) {
        return ((Memory.Variable)target).getVarName();
      } else if (target instanceof Memory.StackArrayCell) {
        return ((Memory.StackArrayCell)target).getVarName();
      }
    }
    return null;
  }

  public Collection<? extends AbstractElement> strengthen(CFANode pNode, InterpreterElement pElement, GuardedEdgeAutomatonPredicateElement pAutomatonElement, Precision pPrecision) {
    AbstractElement lResultElement = pElement;

    for (ECPPredicate lPredicate : pAutomatonElement) {
      AssumeEdge lEdge = ToFlleShAssumeEdgeTranslator.translate(pNode, lPredicate);

      try {
        Collection<? extends AbstractElement> lResult = getAbstractSuccessors(lResultElement, pPrecision, lEdge);

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

  public Collection<? extends AbstractElement> strengthen(CFANode pNode, InterpreterElement pElement, ConstrainedAssumeElement pAssumeElement, Precision pPrecision) {
    AssumeEdge lEdge = new AssumeEdge(pAssumeElement.getExpression().getRawSignature(), pNode.getLineNumber(), pNode, pNode, pAssumeElement.getExpression(), true);

    try {
      Collection<? extends AbstractElement> lResult = getAbstractSuccessors(pElement, pPrecision, lEdge);

      return lResult;
    } catch (CPATransferException e) {
      throw new RuntimeException(e);
    }
  }

}
