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
package org.sosy_lab.cpachecker.cpa.explicit;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.DefaultExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.IASTArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTAssignment;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.IASTEnumerationSpecifier.IASTEnumerator;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.IASTStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.IASTCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.IASTFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTInitializer;
import org.sosy_lab.cpachecker.cfa.ast.IASTInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTPointerTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.RightHandSideVisitor;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
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

  @Option
  private int threshold = 0;

  private String missingInformationLeftVariable = null;
  private String missingInformationLeftPointer  = null;
  private IASTRightHandSide missingInformationRightExpression = null;

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
    
    AbstractElement successor;
    ExplicitElement explicitElement = (ExplicitElement)element;
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
    //System.out.println(exprOnSummary.getRawSignature());
    //expression is an assignment operation, e.g. a = g(b);
    if (exprOnSummary instanceof IASTFunctionCallAssignmentStatement) {
      IASTFunctionCallAssignmentStatement assignExp = ((IASTFunctionCallAssignmentStatement)exprOnSummary);
      IASTExpression op1 = assignExp.getLeftHandSide();

      //we expect left hand side of the expression to be a variable
      if(op1 instanceof IASTIdExpression ||
          op1 instanceof IASTFieldReference)
      {
        //      IASExpression leftHandSideVar = op1;
        String varName = op1.getRawSignature();
        String returnVarName = calledFunctionName + "::" + "___cpa_temp_result_var_";

        for(String globalVar:globalVars){
          if(globalVar.equals(varName)){
            if(element.getNoOfReferences().containsKey(globalVar) &&
                element.getNoOfReferences().get(globalVar).intValue() >= this.threshold){
              newElement.forget(globalVar);
              newElement.getNoOfReferences().put(globalVar, element.getNoOfReferences().get(globalVar));
            }
            else{
              if(element.contains(returnVarName)){
                newElement.assignConstant(varName, element.getValueFor(returnVarName), this.threshold);
              }
              else{
                newElement.forget(varName);
              }
            }
          }
          else{
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
        }

        if(!globalVars.contains(varName)){
          String assignedVarName = getvarName(varName, callerFunctionName);
          if(element.contains(returnVarName)){
            newElement.assignConstant(assignedVarName, element.getValueFor(returnVarName), this.threshold);
          }
          else{
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
    }
    else{
      throw new UnrecognizedCCodeException("on function return", summaryEdge, exprOnSummary.asStatement());
    }

    return newElement;
  }

  private ExplicitElement handleFunctionCall(ExplicitElement element,
      FunctionCallEdge callEdge)
  throws UnrecognizedCCodeException {

    FunctionDefinitionNode functionEntryNode = callEdge.getSuccessor();
    String calledFunctionName = functionEntryNode.getFunctionName();
    String callerFunctionName = callEdge.getPredecessor().getFunctionName();

    List<String> paramNames = functionEntryNode.getFunctionParameterNames();
    List<IASTExpression> arguments = callEdge.getArguments();

    assert (paramNames.size() == arguments.size());

    ExplicitElement newElement = new ExplicitElement(element);

    for(String globalVar:globalVars){
      if(element.contains(globalVar)){
        newElement.getConstantsMap().put(globalVar, element.getValueFor(globalVar));
        newElement.getNoOfReferences().put(globalVar, element.getNoOfReferences().get(globalVar));
      }
    }

    for (int i=0; i<arguments.size(); i++){
      IASTExpression arg = arguments.get(i);
      if (arg instanceof IASTCastExpression) {
        // ignore casts
        arg = ((IASTCastExpression)arg).getOperand();
      }

      String nameOfParam = paramNames.get(i);
      String formalParamName = getvarName(nameOfParam, calledFunctionName);
      if(arg instanceof IASTIdExpression){
        IASTIdExpression idExp = (IASTIdExpression) arg;
        String nameOfArg = idExp.getRawSignature();
        String actualParamName = getvarName(nameOfArg, callerFunctionName);

        if(element.contains(actualParamName)){
          newElement.assignConstant(formalParamName, element.getValueFor(actualParamName), this.threshold);
        }
      }

      else if(arg instanceof IASTLiteralExpression){
        Long val = parseLiteral((IASTLiteralExpression)arg);

        if (val != null) {
          newElement.assignConstant(formalParamName, val, this.threshold);
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
  throws UnrecognizedCFAEdgeException {

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
        throw new UnrecognizedCFAEdgeException("Unhandled case " + cfaEdge.getRawStatement());
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
      throw new UnrecognizedCFAEdgeException("Unhandled case " + cfaEdge.getRawStatement());
    }

  }

  private AbstractElement propagateBooleanExpression(AbstractElement element, 
      BinaryOperator opType,IASTExpression op1, 
      IASTExpression op2, String functionName, boolean truthValue, ExplicitPrecision precision) 
  throws UnrecognizedCFAEdgeException {

    ExplicitElement newElement = ((ExplicitElement)element).clone();

    // a (bop) ?
    if(op1 instanceof IASTIdExpression || 
        op1 instanceof IASTFieldReference ||
        op1 instanceof IASTArraySubscriptExpression)
    {
      // [literal]
      if(op2 == null && opType == null){
        String varName = op1.getRawSignature();
        if (precision.isOnBlacklist(getvarName(varName,functionName)))
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
            newElement.assignConstant(getvarName(varName, functionName), 0, this.threshold);
          }
        }
      }
      // a (bop) 9
      else if(op2 instanceof IASTLiteralExpression)
      {
        IASTLiteralExpression lop2 = (IASTLiteralExpression)op2;
        String varName = op1.getRawSignature();
        if (precision.isOnBlacklist(varName))
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
//            System.out.println(" >>>>> " + varName + " op2 " + op2.getRawSignature());
            if(truthValue){
              if(newElement.contains(getvarName(varName, functionName))){
//                System.out.println("here 17: " + newElement.getValueFor(getvarName(varName, functionName)));
//                System.out.println("lit val: " + valueOfLiteral);
                if(newElement.getValueFor(getvarName(varName, functionName)) == valueOfLiteral){
//                  System.out.println("here 18");
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

          // TODO nothing
          else if(opType == BinaryOperator.BINARY_AND ||
              opType == BinaryOperator.BINARY_OR ||
              opType == BinaryOperator.BINARY_XOR){
            return newElement;
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
        if (precision.isOnBlacklist(leftVarName) || precision.isOnBlacklist(rightVarName))
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
              if(newElement.getValueFor(getvarName(rightVarName, functionName)) != 
                newElement.getValueFor(getvarName(leftVarName, functionName))){
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
              if(newElement.getValueFor(getvarName(rightVarName, functionName)) == 
                newElement.getValueFor(getvarName(leftVarName, functionName))){
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
          throw new UnrecognizedCFAEdgeException("Unhandled case ");
        }
      }
      else if(op2 instanceof IASTUnaryExpression)
      {
        String varName = op1.getRawSignature();
        if (precision.isOnBlacklist(varName))
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
          return propagateBooleanExpression(element, opType, op1, exprInCastOp, functionName, truthValue, precision);
        }
        else{
          throw new UnrecognizedCFAEdgeException("Unhandled case ");
        }
      }
      else if(op2 instanceof IASTBinaryExpression){
        String varName = op1.getRawSignature();
        if (precision.isOnBlacklist(varName))
          return element;
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
      return propagateBooleanExpression(element, opType, castOperand, op2, functionName, truthValue, precision);
    }
    else{
      String varName = op1.getRawSignature();
      if (precision.isOnBlacklist(varName)) {
        return element;
      }
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

  private ExplicitElement handleDeclaration(ExplicitElement element,
      DeclarationEdge declarationEdge, ExplicitPrecision precision) throws UnrecognizedCCodeException {

    ExplicitElement newElement = element.clone();
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
        boolean onBlacklist = precision.isOnBlacklist(getvarName(varName,declarationEdge.getPredecessor().getFunctionName()));
        if(declarationEdge.isGlobal() && ! onBlacklist)
        {
          globalVars.add(varName);
          
          Long v;

          IASTInitializer init = declarationEdge.getInitializer();
          if (init != null) {
            if (init instanceof IASTInitializerExpression) {
              IASTExpression exp = ((IASTInitializerExpression)init).getExpression();

              v = getExpressionValue(element, exp, varName, declarationEdge);
            } else {
              // TODO show warning
              v = null;
            }
          } else {
            // global variables without initializer are set to 0 in C
            v = 0L;
          }
          if (v != null) {
            newElement.assignConstant(varName, v, this.threshold);
          }
        }
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
      if (precision.isOnBlacklist(getvarName(op1.getRawSignature(),cfaEdge.getPredecessor().getFunctionName()))) 
        return element;
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
      // TODO assignment to field
      return element.clone();

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
      case LESS_EQUAL:
      case GREATER_EQUAL:
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
          throw new UnrecognizedCCodeException("unkown binary operator", null, pE);
        }
      }
      
      case EQUALS:
      case NOT_EQUALS: {

        Long lVal = lVarInBinaryExp.accept(this);
        if (lVal == null) {
          return null;
        }
        
        Long rVal = rVarInBinaryExp.accept(this);
        if (rVal == null) {
          return null;
        }

        // assign 1 if expression holds, 0 otherwise
        long result = (lVal.equals(rVal) ? 1 : 0);
        
        if (binaryOperator == BinaryOperator.NOT_EQUALS) {
          // negate
          result = 1 - result;
        }
        return result;
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
      return parseLiteral(pE);
    }
    
    @Override
    public Long visit(IASTFloatLiteralExpression pE) throws UnrecognizedCCodeException {
      return parseLiteral(pE);
    }
    
    @Override
    public Long visit(IASTIntegerLiteralExpression pE) throws UnrecognizedCCodeException {
      return parseLiteral(pE);
    }
    
    @Override
    public Long visit(IASTStringLiteralExpression pE) throws UnrecognizedCCodeException {
      return parseLiteral(pE);
    }
    
    @Override
    public Long visit(IASTIdExpression idExp) throws UnrecognizedCCodeException {

      if (idExp.getDeclaration() instanceof IASTEnumerator) {
        return ((IASTEnumerator)idExp.getDeclaration()).getValue();
      }
      
      String varName = getvarName(idExp.getRawSignature(), functionName);
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
      
      switch (unaryOperator) {

      case MINUS:
        Long val = unaryOperand.accept(this);
        return (val != null) ? -val : null;
      
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
  
  private Long getExpressionValue(ExplicitElement element, IASTExpression expression,
      String functionName, CFAEdge cfaEdge) throws UnrecognizedCCodeException {

    ExpressionValueVisitor v = new ExpressionValueVisitor(element, functionName);
    return expression.accept(v);
  }

  private static Long parseLiteral(IASTLiteralExpression lexp) {
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
        String s = lexp.getRawSignature();
        int length = s.length();
        assert(s.charAt(0) == '\'');
        assert(s.charAt(length-1) == '\'');
        int n;

        if (s.charAt(1) == '\\') {
          n = Integer.parseInt(s.substring(2, length-1));
        } else {
          assert (lexp.getRawSignature().length() == 3);
          n = lexp.getRawSignature().charAt(1);
        }
        num = "" + n;

      }
      break;
      case IASTLiteralExpression.lk_string_literal: {
        // can't handle
        return null;
      }
      default:
        assert(false) : lexp;
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

  private Long parseLiteralWithOppositeSign(IASTLiteralExpression lexp){
      // this should be a number...
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
        String s = lexp.getRawSignature();
        int length = s.length();
        assert(s.charAt(0) == '\'');
        assert(s.charAt(length-1) == '\'');
        int n;

        if (s.charAt(1) == '\\') {
          n = Integer.parseInt(s.substring(2, length-1));
        } else {
          assert (lexp.getRawSignature().length() == 3);
          n = lexp.getRawSignature().charAt(1);
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
}
