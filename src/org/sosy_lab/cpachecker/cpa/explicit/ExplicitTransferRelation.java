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
package org.sosy_lab.cpachecker.cpa.explicit;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.DefaultExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.IASTArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTAssignment;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
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
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTInitializer;
import org.sosy_lab.cpachecker.cfa.ast.IASTInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.IASTStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.RightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.StorageClass;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.IASTEnumerationSpecifier.IASTEnumerator;
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
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.assumptions.storage.AssumptionStorageElement;
import org.sosy_lab.cpachecker.cpa.pointer.Memory;
import org.sosy_lab.cpachecker.cpa.pointer.Pointer;
import org.sosy_lab.cpachecker.cpa.pointer.PointerElement;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.util.assumptions.NumericTypes;

@Options(prefix="cpa.explicit")
public class ExplicitTransferRelation implements TransferRelation {


  public static Pair<AbstractElement, String> maxElem;
  private static int maxSize = 0;

  private final Set<String> globalVars = new HashSet<String>();

  @Option(description="threshold for amount of different values that "
    + "are tracked for one variable in ExplicitCPA (0 means infinitely)")
  private int threshold = 0;

  private String missingInformationLeftVariable = null;
  private String missingInformationLeftPointer  = null;
  private IASTRightHandSide missingInformationRightExpression = null;

  public static ExplicitPrecision currentPrecision = null;

  public ExplicitTransferRelation(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  @Override
  public Collection<AbstractElement> getAbstractSuccessors(
      AbstractElement element, Precision pPrecision, CFAEdge cfaEdge) throws CPATransferException {
    if (! (pPrecision instanceof ExplicitPrecision)) {
      throw new IllegalArgumentException("precision is no ExplicitPrecision");
    }
    ExplicitPrecision precision = (ExplicitPrecision) pPrecision;

    currentPrecision = precision;

    AbstractElement successor;
    ExplicitElement explicitElement = (ExplicitElement)element;

    // is there a fact associated with the current edge ..?
    if(precision.facts.get(cfaEdge) != null)
    {
//System.out.println("having fact at edge " + cfaEdge + ", namely " + precision.facts.get(cfaEdge));
      Map<String, Long> factsAtLocation = precision.facts.get(cfaEdge);

      for(Map.Entry<String, Long> factAtLocation : factsAtLocation.entrySet())
      {
        // ... and the variable associated with the fact is not already set ..?
        if(!explicitElement.contains(factAtLocation.getKey()))
        {
          // ... then set it!
          String factName = factAtLocation.getKey();
          Long factValue = factAtLocation.getValue();
//if(true)break;
//System.out.println("at edge " + cfaEdge + " setting " + factName + " to " + factValue);
          //if(factValue != null)
          //  explicitElement.assignFact(factName, factValue);
        }
      }
    }
//System.out.println("   at edge [" + cfaEdge.getEdgeType() + "] " + cfaEdge.getRawStatement() + ", elem = " + explicitElement);
    // check the type of the edge
    switch (cfaEdge.getEdgeType ()) {

    // if edge is a statement edge, e.g. a = b + c
    case StatementEdge: {
      StatementEdge statementEdge = (StatementEdge) cfaEdge;
      successor = handleStatement(explicitElement, statementEdge.getStatement(), cfaEdge, precision);
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
      break;
    }

    // edge is a declaration edge, e.g. int a;
    case DeclarationEdge: {
      DeclarationEdge declarationEdge = (DeclarationEdge) cfaEdge;
      successor = handleDeclaration(explicitElement, declarationEdge, precision);
      break;
    }

    // this is an assumption, e.g. if(a == b)
    case AssumeEdge: {
      AssumeEdge assumeEdge = (AssumeEdge) cfaEdge;
      successor = handleAssumption(explicitElement, assumeEdge.getExpression(), cfaEdge, assumeEdge.getTruthAssumption(), precision);
      break;
    }

    case BlankEdge: {
      successor = explicitElement.clone();
      break;
    }

    case FunctionCallEdge: {
      FunctionCallEdge functionCallEdge = (FunctionCallEdge) cfaEdge;
      successor = handleFunctionCall(explicitElement, functionCallEdge);
      break;
    }

    // this is a return edge from function, this is different from return statement
    // of the function. See case for statement edge for details
    case FunctionReturnEdge: {
      FunctionReturnEdge functionReturnEdge = (FunctionReturnEdge) cfaEdge;
      successor = handleFunctionReturn(explicitElement, functionReturnEdge);
      break;
    }

    default:
      throw new UnrecognizedCFAEdgeException(cfaEdge);
    }

    if (successor == null) {
      return Collections.emptySet();
    } else {
      ExplicitElement tempEl = ((ExplicitElement)successor);
      if(tempEl.getConstantsMap().size() > maxSize){
        maxSize = tempEl.getConstantsMap().size();
        maxElem = Pair.of(successor,
            cfaEdge.getRawStatement() + "@" + cfaEdge.getLineNumber());
      }
      return Collections.singleton(successor);
    }
  }

  /**
   * Handles return from one function to another function.
   * @param element previous abstract element.
   * @param functionReturnEdge return edge from a function to its call site.
   * @return new abstract element.
   */
  private ExplicitElement handleFunctionReturn(ExplicitElement element,
      FunctionReturnEdge functionReturnEdge)
  throws UnrecognizedCCodeException {

    CallToReturnEdge summaryEdge =
      functionReturnEdge.getSuccessor().getEnteringSummaryEdge();
    IASTFunctionCall exprOnSummary = summaryEdge.getExpression();
    // TODO get from stack
    ExplicitElement previousElem = element.getPreviousElement();
    ExplicitElement newElement = previousElem.clone();
    String callerFunctionName = functionReturnEdge.getSuccessor().getFunctionName();
    String calledFunctionName = functionReturnEdge.getPredecessor().getFunctionName();

    // copy global variables
    for(String globalVar:globalVars){
      if(element.getNoOfReferences().containsKey(globalVar) &&
          element.getNoOfReferences().get(globalVar).intValue() >= this.threshold){
        newElement.forget(globalVar);
        newElement.getNoOfReferences().put(globalVar, element.getNoOfReferences().get(globalVar));
      }
      else{
        if(element.contains(globalVar)){
          newElement.assignConstant(globalVar, element.getValueFor(globalVar), this.threshold);
          newElement.getNoOfReferences().put(globalVar, element.getNoOfReferences().get(globalVar));
        }
        else{
          newElement.forget(globalVar);
        }
      }
    }

    if (exprOnSummary instanceof IASTFunctionCallAssignmentStatement)
    {
      //expression is an assignment operation, e.g. a = g(b);

      IASTFunctionCallAssignmentStatement assignExp = ((IASTFunctionCallAssignmentStatement)exprOnSummary);
      IASTExpression op1 = assignExp.getLeftHandSide();

      //we expect left hand side of the expression to be a variable
      if((op1 instanceof IASTIdExpression) || (op1 instanceof IASTFieldReference)) {
        String returnVarName = getvarName("___cpa_temp_result_var_", calledFunctionName);

        String assignedVarName = getvarName(op1.getRawSignature(), callerFunctionName);

        if (element.contains(returnVarName)) {
          newElement.assignConstant(assignedVarName, element.getValueFor(returnVarName), this.threshold);
        } else {
          newElement.forget(assignedVarName);
        }
      }

      // a* = b(); TODO: for now, nothing is done here, but cloning the current element
      else if(op1 instanceof IASTUnaryExpression && ((IASTUnaryExpression)op1).getOperator() == UnaryOperator.STAR)
          return element.clone();

      else
      {
        throw new UnrecognizedCCodeException("on function return", summaryEdge, op1);
      }
    }

    return newElement;
  }

  private ExplicitElement handleFunctionCall(ExplicitElement element,
      FunctionCallEdge callEdge)
  throws UnrecognizedCCodeException {

    ExplicitElement newElement = new ExplicitElement(element);

    for(String globalVar:globalVars){
      if(element.contains(globalVar)){
        newElement.getConstantsMap().put(globalVar, element.getValueFor(globalVar));
        newElement.getNoOfReferences().put(globalVar, element.getNoOfReferences().get(globalVar));
      }
    }

    FunctionDefinitionNode functionEntryNode = callEdge.getSuccessor();
    String calledFunctionName = functionEntryNode.getFunctionName();
    String callerFunctionName = callEdge.getPredecessor().getFunctionName();

    List<String> paramNames = functionEntryNode.getFunctionParameterNames();
    List<IASTExpression> arguments = callEdge.getArguments();

    assert (paramNames.size() == arguments.size());

    // visitor for getting the values of the actual parameters in caller function context
    ExpressionValueVisitor v = new ExpressionValueVisitor(element, callerFunctionName);

    for (int i=0; i < arguments.size(); i++) {
      // get value of actual parameter in caller function context
      Long value = arguments.get(i).accept(v);

      String formalParamName = getvarName(paramNames.get(i), calledFunctionName);

      if (value == null) {
        newElement.forget(formalParamName);
      } else {
        newElement.assignConstant(formalParamName, value, this.threshold);
      }
    }

    return newElement;
  }

  private ExplicitElement handleExitFromFunction(ExplicitElement element,
      IASTExpression expression,
      ReturnStatementEdge returnEdge)
  throws UnrecognizedCCodeException {

    if (expression == null) {
      expression = NumericTypes.ZERO; // this is the default in C
    }

    String functionName = returnEdge.getPredecessor().getFunctionName();
    ExpressionValueVisitor v = new ExpressionValueVisitor(element, functionName);

    return handleAssignmentToVariable("___cpa_temp_result_var_", expression, v);
  }

  private AbstractElement handleAssumption(ExplicitElement element,
      IASTExpression expression, CFAEdge cfaEdge, boolean truthValue, ExplicitPrecision precision)
  throws UnrecognizedCFAEdgeException, UnrecognizedCCodeException {

    String functionName = cfaEdge.getPredecessor().getFunctionName();
    // Binary operation
    if (expression instanceof IASTBinaryExpression) {
      IASTBinaryExpression binExp = ((IASTBinaryExpression)expression);
      BinaryOperator opType = binExp.getOperator ();

      IASTExpression op1 = binExp.getOperand1();
      IASTExpression op2 = binExp.getOperand2();
      return propagateBooleanExpression(element, opType, op1, op2, functionName, truthValue, precision);
    }
    // Unary operation
    else if (expression instanceof IASTUnaryExpression)
    {
      IASTUnaryExpression unaryExp = ((IASTUnaryExpression)expression);

      switch (unaryExp.getOperator()) {
      case NOT:
        // ! exp
        return handleAssumption(element, unaryExp.getOperand(), cfaEdge, !truthValue, precision);

      case STAR:
        // *exp
        // don't know anything
        return element.clone();

      default:
        throw new UnrecognizedCCodeException("Unhandled case", cfaEdge, unaryExp);
      }
    }

    else if(expression instanceof IASTCastExpression){
      return handleAssumption(element, ((IASTCastExpression)expression).getOperand(), cfaEdge, truthValue, precision);
    }

    else if(expression instanceof IASTIdExpression
        || expression instanceof IASTFieldReference){
      return propagateBooleanExpression(element, null, expression, null, functionName, truthValue, precision);
    }

    else{
      throw new UnrecognizedCCodeException("Unhandled case", cfaEdge, expression);
    }

  }

  private AbstractElement propagateBooleanExpression(AbstractElement element,
      BinaryOperator opType,IASTExpression op1,
      IASTExpression op2, String functionName, boolean truthValue, ExplicitPrecision precision)
  throws UnrecognizedCCodeException {

    ExplicitElement newElement = ((ExplicitElement)element).clone();

    // a (bop) ?
    if(op1 instanceof IASTIdExpression ||
        op1 instanceof IASTFieldReference ||
        op1 instanceof IASTArraySubscriptExpression)
    {
      // [literal]
      if(op2 == null && opType == null){
        String varName = op1.getRawSignature();
        if(!newElement.contains(getvarName(varName, functionName)))
        //if(precision.isNotTracking(getvarName(varName, functionName)))
          return element;
        if(truthValue) {
          if(newElement.contains(getvarName(varName, functionName))){
            if(newElement.getValueFor(getvarName(varName, functionName)) == 0){
              return null;
            }
          }
          else{
          }
        }
        // ! [literal]
        else {
          if(newElement.contains(getvarName(varName, functionName))){
            if(newElement.getValueFor(getvarName(varName, functionName)) != 0){
              return null;
            }
          }
          else{
            newElement.assignConstant(getvarName(varName, functionName), 0L, this.threshold);
          }
        }
      }
      // a (bop) 9
      else if(op2 instanceof IASTLiteralExpression)
      {
        IASTLiteralExpression lop2 = (IASTLiteralExpression)op2;
        String varName = op1.getRawSignature();

        //if(!newElement.contains(getvarName(varName, functionName)))
        if(precision.isNotTracking(getvarName(varName, functionName)))
          return element;
        int typeOfLiteral = lop2.getKind();
        if( typeOfLiteral ==  IASTLiteralExpression.lk_integer_constant
            || typeOfLiteral == IASTLiteralExpression.lk_float_constant
            || typeOfLiteral == IASTLiteralExpression.lk_char_constant
        )
        {
          long valueOfLiteral = parseLiteral(lop2);
          // a == 9
          if(opType == BinaryOperator.EQUALS) {
            if(truthValue){
              if(newElement.contains(getvarName(varName, functionName))){
                if(newElement.getValueFor(getvarName(varName, functionName)) != valueOfLiteral){
                  return null;
                }
              }
              else{
                newElement.assignConstant(getvarName(varName, functionName), valueOfLiteral, this.threshold);
              }
            }
            // ! a == 9
            else {
              return propagateBooleanExpression(element, BinaryOperator.NOT_EQUALS, op1, op2, functionName, !truthValue, precision);
            }
          }
          // a != 9
          else if(opType == BinaryOperator.NOT_EQUALS)
          {
            if(truthValue){
              if(newElement.contains(getvarName(varName, functionName))){
                if(newElement.getValueFor(getvarName(varName, functionName)) == valueOfLiteral){
                  return null;
                }
              }
              else{
              }
            }
            // ! a != 9
            else {
              return propagateBooleanExpression(element, BinaryOperator.EQUALS, op1, op2, functionName, !truthValue, precision);
            }
          }

          // a > 9
          else if(opType == BinaryOperator.GREATER_THAN)
          {
            if(truthValue){
              if(newElement.contains(getvarName(varName, functionName))){
                if(newElement.getValueFor(getvarName(varName, functionName)) <= valueOfLiteral){
                  return null;
                }
              }
              else{
              }
            }
            else {
              return propagateBooleanExpression(element, BinaryOperator.LESS_EQUAL, op1, op2, functionName, !truthValue, precision);
            }
          }
          // a >= 9
          else if(opType == BinaryOperator.GREATER_EQUAL)
          {
            if(truthValue){
              if(newElement.contains(getvarName(varName, functionName))){
                if(newElement.getValueFor(getvarName(varName, functionName)) < valueOfLiteral){
                  return null;
                }
              }
              else{
              }
            }
            else {
              return propagateBooleanExpression(element, BinaryOperator.LESS_THAN, op1, op2, functionName, !truthValue, precision);
            }
          }
          // a < 9
          else if(opType == BinaryOperator.LESS_THAN)
          {
            if(truthValue){
              if(newElement.contains(getvarName(varName, functionName))){
                if(newElement.getValueFor(getvarName(varName, functionName)) >= valueOfLiteral){
                  return null;
                }
              }
              else{
              }
            }
            else {
              return propagateBooleanExpression(element, BinaryOperator.GREATER_EQUAL, op1, op2, functionName, !truthValue, precision);
            }
          }
          // a <= 9
          else if(opType == BinaryOperator.LESS_EQUAL)
          {
            if(truthValue){
              if(newElement.contains(getvarName(varName, functionName))){
                if(newElement.getValueFor(getvarName(varName, functionName)) > valueOfLiteral){
                  return null;
                }
              }
              else{
              }
            }
            else {
              return propagateBooleanExpression(element, BinaryOperator.GREATER_THAN, op1, op2, functionName, !truthValue, precision);
            }
          }
          // [a - 9]
          else if(opType == BinaryOperator.MINUS)
          {
            if(truthValue){
              if(newElement.contains(getvarName(varName, functionName))){
                if(newElement.getValueFor(getvarName(varName, functionName)) == valueOfLiteral){
                  return null;
                }
              }
              else{
              }
            }
            // ! a != 9
            else {
              return propagateBooleanExpression(element, BinaryOperator.EQUALS, op1, op2, functionName, !truthValue, precision);
            }
          }

          // [a + 9]
          else if(opType == BinaryOperator.PLUS)
          {
            if(truthValue){
              if(newElement.contains(getvarName(varName, functionName))){
                valueOfLiteral = parseLiteralWithOppositeSign(lop2);
                if(newElement.getValueFor(getvarName(varName, functionName)) == (valueOfLiteral)){
                  return null;
                }
              }
              else{
              }
            }
            else {
              if(newElement.contains(getvarName(varName, functionName))){
                valueOfLiteral = parseLiteralWithOppositeSign(lop2);
                if(newElement.getValueFor(getvarName(varName, functionName)) != (valueOfLiteral)){
                  return null;
                }
              }
              else{
                valueOfLiteral = parseLiteralWithOppositeSign(lop2);
                newElement.assignConstant(getvarName(varName, functionName), (valueOfLiteral), this.threshold);
              }
            }
          }

          // a & 9
          else if(opType == BinaryOperator.BINARY_AND)
          {
            if(!newElement.contains(getvarName(varName, functionName)))
              return newElement;
            else if(true)
              return newElement;

            Long r = newElement.getValueFor(getvarName(varName, functionName)) & valueOfLiteral;
            if((r == 0) == !truthValue)
            {
              System.out.println("return element");
            }
            else
              System.out.println("return null");

            if(((r != 0) && truthValue) || ((r == 0) && !truthValue))
            {
              return newElement;
            }
            else
            {
              return null;
            }
          }

          // TODO nothing
          else if(opType == BinaryOperator.BINARY_OR ||
              opType == BinaryOperator.BINARY_XOR){
            return newElement;
          }

          else{
            throw new UnrecognizedCCodeException("Unhandled case ");
          }
        }
        else{
          throw new UnrecognizedCCodeException("Unhandled case ");
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
        if (!newElement.contains(getvarName(leftVarName, functionName)) || !newElement.contains(getvarName(rightVarName, functionName)))
        //if(precision.isNotTracking(getvarName(leftVarName, functionName)) || precision.isNotTracking(getvarName(rightVarName, functionName)))
          return element;
        // a == b
        if(opType == BinaryOperator.EQUALS)
        {
          if(truthValue){
            if(newElement.contains(getvarName(leftVarName, functionName)) &&
                !newElement.contains(getvarName(rightVarName, functionName))){
              newElement.assignConstant(getvarName(rightVarName, functionName),
                  newElement.getValueFor(getvarName(leftVarName, functionName)), this.threshold);
            }
            else if(newElement.contains(getvarName(rightVarName, functionName)) &&
                !newElement.contains(getvarName(leftVarName, functionName))){
              newElement.assignConstant(getvarName(leftVarName, functionName),
                  newElement.getValueFor(getvarName(rightVarName, functionName)), this.threshold);
            }
            else if(newElement.contains(getvarName(rightVarName, functionName)) &&
                newElement.contains(getvarName(leftVarName, functionName))){
              if (!newElement.getValueFor(getvarName(rightVarName, functionName)).equals(
                   newElement.getValueFor(getvarName(leftVarName, functionName)))) {
                return null;
              }
            }
          }
          else{
            return propagateBooleanExpression(element, BinaryOperator.NOT_EQUALS, op1, op2, functionName, !truthValue, precision);
          }
        }
        // a != b
        else if(opType == BinaryOperator.NOT_EQUALS)
        {
          if(truthValue){
            if(newElement.contains(getvarName(rightVarName, functionName)) &&
                newElement.contains(getvarName(leftVarName, functionName))){
              if (newElement.getValueFor(getvarName(rightVarName, functionName)).equals(
                  newElement.getValueFor(getvarName(leftVarName, functionName)))) {
                return null;
              }
            }
            else{

            }
          }
          else{
            return propagateBooleanExpression(element, BinaryOperator.EQUALS, op1, op2, functionName, !truthValue, precision);
          }
        }
        // a > b
        else if(opType == BinaryOperator.GREATER_THAN)
        {
          if(truthValue){
            if(newElement.contains(getvarName(leftVarName, functionName)) &&
                newElement.contains(getvarName(rightVarName, functionName))){
              if(newElement.getValueFor(getvarName(leftVarName, functionName)) <=
                newElement.getValueFor(getvarName(rightVarName, functionName))){
                return null;
              }
            }
            else{

            }
          }
          else{
            return  propagateBooleanExpression(element, BinaryOperator.LESS_EQUAL, op1, op2, functionName, !truthValue, precision);
          }
        }
        // a >= b
        else if(opType == BinaryOperator.GREATER_EQUAL)
        {
          if(truthValue){
            if(newElement.contains(getvarName(leftVarName, functionName)) &&
                newElement.contains(getvarName(rightVarName, functionName))){
              if(newElement.getValueFor(getvarName(leftVarName, functionName)) <
                  newElement.getValueFor(getvarName(rightVarName, functionName))){
                return null;
              }
            }
            else{

            }
          }
          else{
            return propagateBooleanExpression(element, BinaryOperator.LESS_THAN, op1, op2, functionName, !truthValue, precision);
          }
        }
        // a < b
        else if(opType == BinaryOperator.LESS_THAN)
        {
          if(truthValue){
            if(newElement.contains(getvarName(leftVarName, functionName)) &&
                newElement.contains(getvarName(rightVarName, functionName))){
              if(newElement.getValueFor(getvarName(leftVarName, functionName)) >=
                newElement.getValueFor(getvarName(rightVarName, functionName))){
                return null;
              }
            }
            else{

            }
          }
          else{
            return propagateBooleanExpression(element, BinaryOperator.GREATER_EQUAL, op1, op2, functionName, !truthValue, precision);
          }
        }
        // a <= b
        else if(opType == BinaryOperator.LESS_EQUAL)
        {
          if(truthValue){
            if(newElement.contains(getvarName(leftVarName, functionName)) &&
                newElement.contains(getvarName(rightVarName, functionName))){
              if(newElement.getValueFor(getvarName(leftVarName, functionName)) >
              newElement.getValueFor(getvarName(rightVarName, functionName))){
                return null;
              }
            }
            else{

            }
          }
          else{
            return propagateBooleanExpression(element, BinaryOperator.GREATER_THAN, op1, op2, functionName, !truthValue, precision);
          }
        }
        else{
          throw new UnrecognizedCCodeException("Unhandled case ");
        }
      }
      else if(op2 instanceof IASTUnaryExpression)
      {
        String varName = op1.getRawSignature();
        if(!newElement.contains(getvarName(varName, functionName)))
        //if(precision.isNotTracking(getvarName(varName, functionName)))
          return element;
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

              // a == 9
              if(opType == BinaryOperator.EQUALS) {
                if(truthValue){
                  if(newElement.contains(getvarName(varName, functionName))){
                    if(newElement.getValueFor(getvarName(varName, functionName)) != valueOfLiteral){
                      return null;
                    }
                  }
                  else{
                    newElement.assignConstant(getvarName(varName, functionName), valueOfLiteral, this.threshold);
                  }
                }
                // ! a == 9
                else {
                  return propagateBooleanExpression(element, BinaryOperator.NOT_EQUALS, op1, op2, functionName, !truthValue, precision);
                }
              }
              // a != 9
              else if(opType == BinaryOperator.NOT_EQUALS)
              {
                if(truthValue){
                  if(newElement.contains(getvarName(varName, functionName))){
                    if(newElement.getValueFor(getvarName(varName, functionName)) == valueOfLiteral){
                      return null;
                    }
                  }
                  else{
                  }
                }
                // ! a != 9
                else {
                  return propagateBooleanExpression(element, BinaryOperator.EQUALS, op1, op2, functionName, !truthValue, precision);
                }
              }

              // a > 9
              else if(opType == BinaryOperator.GREATER_THAN)
              {
                if(truthValue){
                  if(newElement.contains(getvarName(varName, functionName))){
                    if(newElement.getValueFor(getvarName(varName, functionName)) <= valueOfLiteral){
                      return null;
                    }
                  }
                  else{
                  }
                }
                else {
                  return propagateBooleanExpression(element, BinaryOperator.LESS_EQUAL, op1, op2, functionName, !truthValue, precision);
                }
              }
              // a >= 9
              else if(opType == BinaryOperator.GREATER_EQUAL)
              {
                if(truthValue){
                  if(newElement.contains(getvarName(varName, functionName))){
                    if(newElement.getValueFor(getvarName(varName, functionName)) < valueOfLiteral){
                      return null;
                    }
                  }
                  else{
                  }
                }
                else {
                  return propagateBooleanExpression(element, BinaryOperator.LESS_THAN, op1, op2, functionName, !truthValue, precision);
                }
              }
              // a < 9
              else if(opType == BinaryOperator.LESS_THAN)
              {
                if(truthValue){
                  if(newElement.contains(getvarName(varName, functionName))){
                    if(newElement.getValueFor(getvarName(varName, functionName)) >= valueOfLiteral){
                      return null;
                    }
                  }
                  else{
                  }
                }
                else {
                  return propagateBooleanExpression(element, BinaryOperator.GREATER_EQUAL, op1, op2, functionName, !truthValue, precision);
                }
              }
              // a <= 9
              else if(opType == BinaryOperator.LESS_EQUAL)
              {
                if(truthValue){
                  if(newElement.contains(getvarName(varName, functionName))){
                    if(newElement.getValueFor(getvarName(varName, functionName)) > valueOfLiteral){
                      return null;
                    }
                  }
                  else{
                  }
                }
                else {
                  return propagateBooleanExpression(element, BinaryOperator.GREATER_THAN, op1, op2, functionName, !truthValue, precision);
                }
              }
              else{
                throw new UnrecognizedCCodeException("Unhandled case ");
              }
            }
            else{
              throw new UnrecognizedCCodeException("Unhandled case ");
            }
          }
          else{
            throw new UnrecognizedCCodeException("Unhandled case ");
          }
        }
        else{
          throw new UnrecognizedCCodeException("Unhandled case ");
        }
      }
      else if(op2 instanceof IASTBinaryExpression){
        String varName = op1.getRawSignature();
        if(!newElement.contains(getvarName(varName, functionName)))
        //if(precision.isNotTracking(getvarName(varName, functionName)))
          return element;
        // TODO forgetting
        newElement.forget(varName);
      }
      // right hand side is a cast exp
      else if(op2 instanceof IASTCastExpression){
        IASTCastExpression castExp = (IASTCastExpression)op2;
        IASTExpression exprInCastOp = castExp.getOperand();
        return propagateBooleanExpression(element, opType, op1, exprInCastOp, functionName, truthValue, precision);
      }
      else{
        String varName = op1.getRawSignature();
        // TODO forgetting
        newElement.forget(varName);
      }
    }
    else if(op1 instanceof IASTCastExpression){
      IASTCastExpression castExp = (IASTCastExpression) op1;
      IASTExpression castOperand = castExp.getOperand();
      return propagateBooleanExpression(element, opType, castOperand, op2, functionName, truthValue, precision);
    }
    else{
      String varName = op1.getRawSignature();
      if(!newElement.contains(getvarName(varName, functionName)))
      //if(precision.isNotTracking(getvarName(varName, functionName)))
        return element;
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
  //        case BinaryOperator.EQUALS:
  //          expressionValue = val1.equals(val2);
  //          break;
  //
  //        case BinaryOperator.NOT_EQUALS:
  //          expressionValue = !val1.equals(val2);
  //          break;
  //
  //        case BinaryOperator.GREATER_THAN:
  //          expressionValue = val1 > val2;
  //          break;
  //
  //        case BinaryOperator.GREATER_EQUAL:
  //          expressionValue = val1 >= val2;
  //          break;
  //
  //        case BinaryOperator.LESS_THAN:
  //          expressionValue = val1 < val2;
  //          break;
  //
  //        case BinaryOperator.LESS_EQUAL:
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

  private Set<DeclarationEdge> declared = new HashSet<DeclarationEdge>();
  private ExplicitElement handleDeclaration(ExplicitElement element,
      DeclarationEdge declarationEdge, ExplicitPrecision precision) throws UnrecognizedCCodeException {

    ExplicitElement newElement = element.clone();
    if ((declarationEdge.getName() == null)
        || (declarationEdge.getStorageClass() == StorageClass.TYPEDEF)
        || (declarationEdge.getDeclSpecifier() instanceof IASTFunctionTypeSpecifier)) {
      // nothing interesting to see here, please move along
      return newElement;
    }

    if(declared.add(declarationEdge))
      CPAAlgorithm.CPAStatistics.numberOfDeclarations++;

        // get the variable name in the declarator
        String varName = declarationEdge.getName();
        String functionName = declarationEdge.getPredecessor().getFunctionName();

        Long initialValue = null;

        // handle global variables
        if (declarationEdge.isGlobal()) {
          // if this is a global variable, add to the list of global variables
          globalVars.add(varName);

          // global variables without initializer are set to 0 in C
          initialValue = 0L;
        }

        // get initial value
        IASTInitializer init = declarationEdge.getInitializer();
        if (init instanceof IASTInitializerExpression) {
          IASTRightHandSide exp = ((IASTInitializerExpression)init).getExpression();

          initialValue = getExpressionValue(element, exp, functionName);
        }

        // assign initial value if necessary
        String scopedVarName = getvarName(varName, functionName);

        if (initialValue != null && precision.isTracking(scopedVarName)) {
          newElement.assignConstant(scopedVarName, initialValue, this.threshold);
        } else {
          newElement.forget(scopedVarName);
        }

    return newElement;
  }

  private ExplicitElement handleStatement(ExplicitElement element,
      IASTStatement expression, CFAEdge cfaEdge, ExplicitPrecision precision)
  throws UnrecognizedCCodeException {
    // expression is a binary operation, e.g. a = b;
    if (expression instanceof IASTAssignment) {
      return handleAssignment(element, (IASTAssignment)expression, cfaEdge, precision);
    }
    // external function call
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

  private ExplicitElement handleAssignment(ExplicitElement element,
      IASTAssignment assignExpression, CFAEdge cfaEdge, ExplicitPrecision precision)
  throws UnrecognizedCCodeException {

    IASTExpression op1 = assignExpression.getLeftHandSide();
    IASTRightHandSide op2 = assignExpression.getRightHandSide();

    if(op1 instanceof IASTIdExpression) {
      // a = ...
      if (precision.isNotTracking(getvarName(op1.getRawSignature(),cfaEdge.getPredecessor().getFunctionName())))
      {
        element.forget(getvarName(op1.getRawSignature(),cfaEdge.getPredecessor().getFunctionName()));
        return element;
      }
      else {
        String functionName = cfaEdge.getPredecessor().getFunctionName();
        ExpressionValueVisitor v = new ExpressionValueVisitor(element, functionName);

        return handleAssignmentToVariable(op1.getRawSignature(), op2, v);
      }
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
        missingInformationLeftPointer = op1.getRawSignature();
        missingInformationRightExpression = op2;

      } else {
        throw new UnrecognizedCCodeException("left operand of assignment has to be a variable", cfaEdge, op1);
      }
      return element.clone();

    } else if (op1 instanceof IASTFieldReference) {

      // a->b = ...
      if (precision.isNotTracking(getvarName(op1.getRawSignature(),cfaEdge.getPredecessor().getFunctionName())))
      {
        element.forget(getvarName(op1.getRawSignature(),cfaEdge.getPredecessor().getFunctionName()));
        return element.clone();
      }
      else {
        String functionName = cfaEdge.getPredecessor().getFunctionName();
        ExpressionValueVisitor v = new ExpressionValueVisitor(element, functionName);
        return handleAssignmentToVariable(op1.getRawSignature(), op2, v);
      }

     // return element.clone();

    } else if (op1 instanceof IASTArraySubscriptExpression) {
      // TODO assignment to array cell
      return element.clone();

    } else {
      throw new UnrecognizedCCodeException("left operand of assignment has to be a variable", cfaEdge, op1);
    }
  }

  private ExplicitElement handleAssignmentToVariable(String lParam,
      IASTRightHandSide exp, ExpressionValueVisitor v) throws UnrecognizedCCodeException {

    Long value = exp.accept(v);

    if (v.missingPointer) {
      missingInformationRightExpression = exp;
      assert value == null;
    }

    ExplicitElement newElement = v.element.clone();
    String assignedVar = getvarName(lParam, v.functionName);

    if (value == null) {
      newElement.forget(assignedVar);
    } else {
      newElement.assignConstant(assignedVar, value, this.threshold);
    }
    return newElement;
  }

  /**
   * Visitor that get's the value from an expression.
   * The result may be null, i.e., the value is unknown.
   */
  private class ExpressionValueVisitor extends DefaultExpressionVisitor<Long, UnrecognizedCCodeException>
                                       implements RightHandSideVisitor<Long, UnrecognizedCCodeException> {

    protected final ExplicitElement element;
    protected final String functionName;

    private boolean missingPointer = false;

    public ExpressionValueVisitor(ExplicitElement pElement, String pFunctionName) {
      element = pElement;
      functionName = pFunctionName;
    }

    // TODO fields, arrays

    @Override
    protected Long visitDefault(IASTExpression pExp) {
      return null;
    }

    @Override
    public Long visit(IASTBinaryExpression pE) throws UnrecognizedCCodeException {
      BinaryOperator binaryOperator = pE.getOperator();
      IASTExpression lVarInBinaryExp = pE.getOperand1();
      IASTExpression rVarInBinaryExp = pE.getOperand2();

      switch (binaryOperator) {
      case DIVIDE:
      case MODULO:
      case BINARY_AND:
      case BINARY_OR:
        // TODO check which cases can be handled (I think all)
        return null;

      case PLUS:
      case MINUS:
      case MULTIPLY: {

        Long lVal = lVarInBinaryExp.accept(this);
        if (lVal == null) {
          return null;
        }

        Long rVal = rVarInBinaryExp.accept(this);
        if (rVal == null) {
          return null;
        }

        switch (binaryOperator) {

        case PLUS:
          return lVal + rVal;

        case MINUS:
          return lVal - rVal;

        case MULTIPLY:
          return lVal * rVal;

        default:
          throw new AssertionError();
        }
      }

      case EQUALS:
      case NOT_EQUALS:
      case GREATER_THAN:
      case GREATER_EQUAL:
      case LESS_THAN:
      case LESS_EQUAL: {

        Long lVal = lVarInBinaryExp.accept(this);
        if (lVal == null) {
          return null;
        }

        Long rVal = rVarInBinaryExp.accept(this);
        if (rVal == null) {
          return null;
        }

        long l = lVal;
        long r = rVal;

        boolean result;
        switch (binaryOperator) {
        case EQUALS:
          result = (l == r);
          break;
        case NOT_EQUALS:
          result = (l != r);
          break;
        case GREATER_THAN:
          result = (l > r);
          break;
        case GREATER_EQUAL:
          result = (l >= r);
          break;
        case LESS_THAN:
          result = (l < r);
          break;
        case LESS_EQUAL:
          result = (l <= r);
          break;

        default:
          throw new AssertionError();
        }

        // return 1 if expression holds, 0 otherwise
        return (result ? 1L : 0L);
      }

      default:
        return null;
      }
    }

    @Override
    public Long visit(IASTCastExpression pE) throws UnrecognizedCCodeException {
      return pE.getOperand().accept(this);
    }

    @Override
    public Long visit(IASTFunctionCallExpression pIastFunctionCallExpression) throws UnrecognizedCCodeException {
      return null;
    }

    @Override
    public Long visit(IASTCharLiteralExpression pE) throws UnrecognizedCCodeException {
      return (long)pE.getCharacter();
    }

    @Override
    public Long visit(IASTFloatLiteralExpression pE) throws UnrecognizedCCodeException {
      return null;
    }

    @Override
    public Long visit(IASTIntegerLiteralExpression pE) throws UnrecognizedCCodeException {
      return pE.getValue().longValue();
    }

    @Override
    public Long visit(IASTStringLiteralExpression pE) throws UnrecognizedCCodeException {
      return null;
    }

    @Override
    public Long visit(IASTIdExpression idExp) throws UnrecognizedCCodeException {

      if (idExp.getDeclaration() instanceof IASTEnumerator) {
        IASTEnumerator enumerator = (IASTEnumerator)idExp.getDeclaration();
        if (enumerator.hasValue()) {
          return enumerator.getValue();
        } else {
          return null;
        }
      }

      String varName = getvarName(idExp.getName(), functionName);
      if (element.contains(varName)) {
        return element.getValueFor(varName);
      } else {
        return null;
      }
    }

    @Override
    public Long visit(IASTUnaryExpression unaryExpression) throws UnrecognizedCCodeException {
      UnaryOperator unaryOperator = unaryExpression.getOperator();
      IASTExpression unaryOperand = unaryExpression.getOperand();

      Long value = null;

      switch (unaryOperator) {

      case MINUS:
        value = unaryOperand.accept(this);
        return (value != null) ? -value : null;

      case NOT:
        value = unaryOperand.accept(this);

        if (value == null)
          return null;

        // if the value is 0, return 1, if it is anything other than 0, return 0
        else
          return (value == 0L) ? 1L : 0L;

      case AMPER:
        return null; // valid expression, but it's a pointer value

      case STAR: {
        missingPointer = true;
        return null;
      }

      default:
        throw new UnrecognizedCCodeException("unknown unary operator", null, unaryExpression);
      }
    }

    @Override
    public Long visit(IASTFieldReference fieldReferenceExpression) throws UnrecognizedCCodeException {

      String varName = getvarName(fieldReferenceExpression.getRawSignature(), functionName);
      if (element.contains(varName)) {
        return element.getValueFor(varName);
      } else {
        return null;
      }
    }
  }

  private class PointerExpressionValueVisitor extends ExpressionValueVisitor {

    private final PointerElement pointerElement;

    public PointerExpressionValueVisitor(ExplicitElement pElement,
        String pFunctionName, PointerElement pPointerElement) {
      super(pElement, pFunctionName);
      pointerElement = pPointerElement;
    }

    @Override
    public Long visit(IASTUnaryExpression unaryExpression) throws UnrecognizedCCodeException {

      if (unaryExpression.getOperator() != UnaryOperator.STAR) {
        return super.visit(unaryExpression);
      }

      // Cil produces code like
      // __cil_tmp8 = *((int *)__cil_tmp7);
      // so remove cast
      IASTExpression unaryOperand = unaryExpression.getOperand();
      if (unaryOperand instanceof IASTCastExpression) {
        unaryOperand = ((IASTCastExpression)unaryOperand).getOperand();
      }

      if (unaryOperand instanceof IASTIdExpression) {

        String rightVar = derefPointerToVariable(pointerElement, unaryOperand.getRawSignature());
        if (rightVar != null) {
          rightVar = getvarName(rightVar, functionName);
          if (element.contains(rightVar)) {
            return element.getValueFor(rightVar);
          }
        }

      } else {
        throw new UnrecognizedCCodeException("Pointer dereference of something that is not a variable", null, unaryExpression);
      }
      return null;
    }
  }

  private Long getExpressionValue(ExplicitElement element, IASTRightHandSide expression,
      String functionName) throws UnrecognizedCCodeException {

    ExpressionValueVisitor v = new ExpressionValueVisitor(element, functionName);
    return expression.accept(v);
  }

  private static Long parseLiteral(IASTLiteralExpression lexp) {
      // this should be a number...
      switch (lexp.getKind()) {
      case IASTLiteralExpression.lk_integer_constant:
        return ((IASTIntegerLiteralExpression)lexp).getValue().longValue();

      case IASTLiteralExpression.lk_char_constant:
        return (long)((IASTCharLiteralExpression)lexp).getCharacter();

      case IASTLiteralExpression.lk_float_constant:
      case IASTLiteralExpression.lk_string_literal:
        // can't handle
        return null;

      default:
        assert(false) : lexp;
        return null;
      }
  }

  private Long parseLiteralWithOppositeSign(IASTLiteralExpression lexp){
      // this should be a number...

      switch (lexp.getKind()) {
      case IASTLiteralExpression.lk_integer_constant: {
        long val = ((IASTIntegerLiteralExpression)lexp).getValue().longValue();
        return 0 - val;
      }

      case IASTLiteralExpression.lk_char_constant: {
        long val = ((IASTCharLiteralExpression)lexp).getCharacter();
        return 0 - val;
      }

      case IASTLiteralExpression.lk_float_constant:
      case IASTLiteralExpression.lk_string_literal:
        // can't handle
        return null;

      default:
        assert(false);
        return null;
      }
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
      CFAEdge cfaEdge, Precision precision) throws UnrecognizedCCodeException {

    assert element instanceof ExplicitElement;
    ExplicitElement explicitElement = (ExplicitElement)element;

    for (AbstractElement ae : elements) {
      if (ae instanceof PointerElement) {
        return strengthen(explicitElement, (PointerElement)ae, cfaEdge, precision);
      }
      else if(ae instanceof AssumptionStorageElement){
        return strengthen(explicitElement, (AssumptionStorageElement)ae, cfaEdge, precision);
      }
    }
    return null;
  }

  private Collection<? extends AbstractElement> strengthen(
      ExplicitElement pExplicitElement, AssumptionStorageElement pAe,
      CFAEdge pCfaEdge, Precision pPrecision) {


    return null;
  }

  private Collection<? extends AbstractElement> strengthen(ExplicitElement explicitElement,
      PointerElement pointerElement, CFAEdge cfaEdge, Precision precision) throws UnrecognizedCCodeException {

    try {
      if (missingInformationRightExpression != null) {
        String functionName = cfaEdge.getPredecessor().getFunctionName();
        ExpressionValueVisitor v = new PointerExpressionValueVisitor(explicitElement, functionName, pointerElement);

        if (missingInformationLeftVariable != null) {
          ExplicitElement newElement = handleAssignmentToVariable(missingInformationLeftVariable, missingInformationRightExpression, v);
          return Collections.singleton(newElement);

        } else if (missingInformationLeftPointer != null) {

          String leftVar = derefPointerToVariable(pointerElement, missingInformationLeftPointer);
          if (leftVar != null) {
            leftVar = getvarName(leftVar, functionName);
            ExplicitElement newElement = handleAssignmentToVariable(leftVar, missingInformationRightExpression, v);
            return Collections.singleton(newElement);
          }
        }
      }
      return null;

    } finally {
      missingInformationLeftVariable = null;
      missingInformationLeftPointer = null;
      missingInformationRightExpression = null;
    }
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

  public Set<String> getGlobalVars()
  {
    return globalVars;
  }
}
