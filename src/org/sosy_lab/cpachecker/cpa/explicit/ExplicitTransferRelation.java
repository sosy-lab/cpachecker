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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.IASTArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTPointerOperator;
import org.eclipse.cdt.core.dom.ast.IASTTypeIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;
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

@Options(prefix="cpa.explicit")
public class ExplicitTransferRelation implements TransferRelation {


  public static Pair<AbstractElement, String> maxElem;
  private static int maxSize = 0;

  private final Set<String> globalVars = new HashSet<String>();

  @Option
  private int threshold = 0;

  private String missingInformationLeftVariable = null;
  private String missingInformationRightPointer = null;
  private String missingInformationLeftPointer  = null;
  private IASTExpression missingInformationRightExpression = null;

  public ExplicitTransferRelation(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  @Override
  public Collection<AbstractElement> getAbstractSuccessors(
      AbstractElement element, Precision precision, CFAEdge cfaEdge) throws CPATransferException {
    AbstractElement successor;
    ExplicitElement explicitElement = (ExplicitElement)element;
    // check the type of the edge
    switch (cfaEdge.getEdgeType ()) {

    // if edge is a statement edge, e.g. a = b + c
    case StatementEdge: {
      StatementEdge statementEdge = (StatementEdge) cfaEdge;
      successor = handleStatement(explicitElement, statementEdge.getExpression(), cfaEdge);
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
      successor = handleDeclaration(explicitElement, declarationEdge);
      break;
    }

    // this is an assumption, e.g. if(a == b)
    case AssumeEdge: {
      AssumeEdge assumeEdge = (AssumeEdge) cfaEdge;
      successor = handleAssumption(explicitElement, assumeEdge.getExpression(), cfaEdge, assumeEdge.getTruthAssumption());
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
    IASTExpression exprOnSummary = summaryEdge.getExpression();
    // TODO get from stack
    ExplicitElement previousElem = element.getPreviousElement();
    ExplicitElement newElement = previousElem.clone();
    String callerFunctionName = functionReturnEdge.getSuccessor().getFunctionName();
    String calledFunctionName = functionReturnEdge.getPredecessor().getFunctionName();
    //System.out.println(exprOnSummary.getRawSignature());
    //expression is a binary operation, e.g. a = g(b);
    if (exprOnSummary instanceof IASTBinaryExpression) {
      IASTBinaryExpression binExp = ((IASTBinaryExpression)exprOnSummary);
      int opType = binExp.getOperator ();
      IASTExpression op1 = binExp.getOperand1();

      assert(opType == IASTBinaryExpression.op_assign);

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
                newElement.forget(varName);
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
    // TODO this is not called -- expression is a unary operation, e.g. g(b);
    else if (exprOnSummary instanceof IASTUnaryExpression)
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
    // g(b)
    else if (exprOnSummary instanceof IASTFunctionCallExpression)
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
      throw new UnrecognizedCCodeException("on function return", summaryEdge, exprOnSummary);
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
        Long val = parseLiteral(arg);

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
        assert(unaryExp.getOperator() == IASTUnaryExpression.op_star || unaryExp.getOperator() == IASTUnaryExpression.op_amper);
      }

      else if(arg instanceof IASTFunctionCallExpression){
        assert(false);
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

    return handleAssignmentToVariable(element, "___cpa_temp_result_var_", expression, returnEdge);
  }

  private AbstractElement handleAssumption(ExplicitElement element,
      IASTExpression expression, CFAEdge cfaEdge, boolean truthValue)
  throws UnrecognizedCFAEdgeException {

    String functionName = cfaEdge.getPredecessor().getFunctionName();
    // Binary operation
    if (expression instanceof IASTBinaryExpression) {
      IASTBinaryExpression binExp = ((IASTBinaryExpression)expression);
      int opType = binExp.getOperator ();

      IASTExpression op1 = binExp.getOperand1();
      IASTExpression op2 = binExp.getOperand2();
      return propagateBooleanExpression(element, opType, op1, op2, functionName, truthValue);
    }
    // Unary operation
    else if (expression instanceof IASTUnaryExpression)
    {
      IASTUnaryExpression unaryExp = ((IASTUnaryExpression)expression);
      // ! exp
      if(unaryExp.getOperator() == IASTUnaryExpression.op_not)
      {
//        System.out.println("here " + expression.getRawSignature() + " tv " + truthValue);
        IASTExpression exp1 = unaryExp.getOperand();
        // ! unaryExp
        if(exp1 instanceof IASTUnaryExpression){
//          System.out.println("here2 " + expression.getRawSignature() + " tv " + truthValue);
          IASTUnaryExpression unaryExp1 = ((IASTUnaryExpression)exp1);
          // (exp)
          if (unaryExp1.getOperator() == IASTUnaryExpression.op_bracketedPrimary){
//            System.out.println("here3 " + expression.getRawSignature() + " tv " + truthValue);
            IASTExpression exp2 = unaryExp1.getOperand();
            // (binaryExp)
            if(exp2 instanceof IASTBinaryExpression){
//              System.out.println("here4 " + expression.getRawSignature() + " tv " + truthValue);
              IASTBinaryExpression binExp2 = (IASTBinaryExpression)exp2;
              return handleAssumption(element, binExp2, cfaEdge, !truthValue);
            }
            else {
              throw new UnrecognizedCFAEdgeException("Unhandled case " + cfaEdge.getRawStatement());
            }
          }
          else {
            throw new UnrecognizedCFAEdgeException("Unhandled case " + cfaEdge.getRawStatement());
          }
        }

        if(exp1 instanceof IASTIdExpression ||
            exp1 instanceof IASTFieldReference){
          return handleAssumption(element, exp1, cfaEdge, !truthValue);
        }
        else {
          throw new UnrecognizedCFAEdgeException("Unhandled case " + cfaEdge.getRawStatement());
        }
      }
      else if(unaryExp.getOperator() == IASTUnaryExpression.op_bracketedPrimary){
        return handleAssumption(element, unaryExp.getOperand(), cfaEdge, truthValue);
      }
      else if(unaryExp instanceof IASTCastExpression){
        return handleAssumption(element, ((IASTCastExpression)expression).getOperand(), cfaEdge, truthValue);
      }
      else {
        throw new UnrecognizedCFAEdgeException("Unhandled case " + cfaEdge.getRawStatement());
      }
    }

    else if(expression instanceof IASTIdExpression
        || expression instanceof IASTFieldReference){
      return propagateBooleanExpression(element, -999, expression, null, functionName, truthValue);
    }

    else{
      throw new UnrecognizedCFAEdgeException("Unhandled case " + cfaEdge.getRawStatement());
    }

  }

  private AbstractElement propagateBooleanExpression(AbstractElement element, 
      int opType,IASTExpression op1, 
      IASTExpression op2, String functionName, boolean truthValue) 
  throws UnrecognizedCFAEdgeException {

    ExplicitElement newElement = ((ExplicitElement)element).clone();

    // a (bop) ?
    if(op1 instanceof IASTIdExpression || 
        op1 instanceof IASTFieldReference ||
        op1 instanceof IASTArraySubscriptExpression)
    {
      // [literal]
      if(op2 == null && opType == -999){
        String varName = op1.getRawSignature();
        if(truthValue){
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
        String varName = op1.getRawSignature();
        int typeOfLiteral = ((IASTLiteralExpression)op2).getKind();
        if( typeOfLiteral ==  IASTLiteralExpression.lk_integer_constant 
            || typeOfLiteral == IASTLiteralExpression.lk_float_constant
            || typeOfLiteral == IASTLiteralExpression.lk_char_constant
        )
        {
          long valueOfLiteral = parseLiteral(op2);
          // a == 9
          if(opType == IASTBinaryExpression.op_equals) {
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
              return propagateBooleanExpression(element, IASTBinaryExpression.op_notequals, op1, op2, functionName, !truthValue);
            }
          }
          // a != 9
          else if(opType == IASTBinaryExpression.op_notequals)
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
              return propagateBooleanExpression(element, IASTBinaryExpression.op_equals, op1, op2, functionName, !truthValue);
            }
          }

          // a > 9
          else if(opType == IASTBinaryExpression.op_greaterThan)
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
              return propagateBooleanExpression(element, IASTBinaryExpression.op_lessEqual, op1, op2, functionName, !truthValue);
            }
          }
          // a >= 9
          else if(opType == IASTBinaryExpression.op_greaterEqual)
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
              return propagateBooleanExpression(element, IASTBinaryExpression.op_lessThan, op1, op2, functionName, !truthValue);
            }
          }
          // a < 9
          else if(opType == IASTBinaryExpression.op_lessThan)
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
              return propagateBooleanExpression(element, IASTBinaryExpression.op_greaterEqual, op1, op2, functionName, !truthValue);
            }
          }
          // a <= 9
          else if(opType == IASTBinaryExpression.op_lessEqual)
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
              return propagateBooleanExpression(element, IASTBinaryExpression.op_greaterThan, op1, op2, functionName, !truthValue);
            }
          }
          // [a - 9]
          else if(opType == IASTBinaryExpression.op_minus)
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
              return propagateBooleanExpression(element, IASTBinaryExpression.op_equals, op1, op2, functionName, !truthValue);
            }
          }

          // [a + 9]
          else if(opType == IASTBinaryExpression.op_plus)
          {
            if(truthValue){
              if(newElement.contains(getvarName(varName, functionName))){
                valueOfLiteral = parseLiteralWithOppositeSign(op2);
                if(newElement.getValueFor(getvarName(varName, functionName)) == (valueOfLiteral)){
                  return null;
                }
              }
              else{
              }
            }
            else {
              if(newElement.contains(getvarName(varName, functionName))){
                valueOfLiteral = parseLiteralWithOppositeSign(op2);
                if(newElement.getValueFor(getvarName(varName, functionName)) != (valueOfLiteral)){
                  return null;
                }
              }
              else{
                valueOfLiteral = parseLiteralWithOppositeSign(op2);
                newElement.assignConstant(getvarName(varName, functionName), (valueOfLiteral), this.threshold);
              }
            }
          }

          // TODO nothing
          else if(opType == IASTBinaryExpression.op_binaryAnd ||
              opType == IASTBinaryExpression.op_binaryOr ||
              opType == IASTBinaryExpression.op_binaryXor){
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
              (((IASTUnaryExpression)op2).getOperator() == IASTUnaryExpression.op_amper) || 
              (((IASTUnaryExpression)op2).getOperator() == IASTUnaryExpression.op_star))))
      {
        String leftVarName = op1.getRawSignature();
        String rightVarName = op2.getRawSignature();

        // a == b
        if(opType == IASTBinaryExpression.op_equals)
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
            return propagateBooleanExpression(element, IASTBinaryExpression.op_notequals, op1, op2, functionName, !truthValue);
          }
        }
        // a != b
        else if(opType == IASTBinaryExpression.op_notequals)
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
            return propagateBooleanExpression(element, IASTBinaryExpression.op_equals, op1, op2, functionName, !truthValue);
          }
        }
        // a > b
        else if(opType == IASTBinaryExpression.op_greaterThan)
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
            return  propagateBooleanExpression(element, IASTBinaryExpression.op_lessEqual, op1, op2, functionName, !truthValue);
          }
        }
        // a >= b
        else if(opType == IASTBinaryExpression.op_greaterEqual)
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
            return propagateBooleanExpression(element, IASTBinaryExpression.op_lessThan, op1, op2, functionName, !truthValue);
          }
        }
        // a < b
        else if(opType == IASTBinaryExpression.op_lessThan)
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
            return propagateBooleanExpression(element, IASTBinaryExpression.op_greaterEqual, op1, op2, functionName, !truthValue);
          }
        }
        // a <= b
        else if(opType == IASTBinaryExpression.op_lessEqual)
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
            return propagateBooleanExpression(element, IASTBinaryExpression.op_greaterThan, op1, op2, functionName, !truthValue);
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

        int operatorType = unaryExp.getOperator();
        // a == -8
        if(operatorType == IASTUnaryExpression.op_minus){

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
              if(opType == IASTBinaryExpression.op_equals) {
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
                  return propagateBooleanExpression(element, IASTBinaryExpression.op_notequals, op1, op2, functionName, !truthValue);
                }
              }
              // a != 9
              else if(opType == IASTBinaryExpression.op_notequals)
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
                  return propagateBooleanExpression(element, IASTBinaryExpression.op_equals, op1, op2, functionName, !truthValue);
                }
              }

              // a > 9
              else if(opType == IASTBinaryExpression.op_greaterThan)
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
                  return propagateBooleanExpression(element, IASTBinaryExpression.op_lessEqual, op1, op2, functionName, !truthValue);
                }
              }
              // a >= 9
              else if(opType == IASTBinaryExpression.op_greaterEqual)
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
                  return propagateBooleanExpression(element, IASTBinaryExpression.op_lessThan, op1, op2, functionName, !truthValue);
                }
              }
              // a < 9
              else if(opType == IASTBinaryExpression.op_lessThan)
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
                  return propagateBooleanExpression(element, IASTBinaryExpression.op_greaterEqual, op1, op2, functionName, !truthValue);
                }
              }
              // a <= 9
              else if(opType == IASTBinaryExpression.op_lessEqual)
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
                  return propagateBooleanExpression(element, IASTBinaryExpression.op_greaterThan, op1, op2, functionName, !truthValue);
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
        else if(operatorType == IASTUnaryExpression.op_bracketedPrimary){
          IASTUnaryExpression unaryExprInPar = (IASTUnaryExpression)op2;
          IASTExpression exprInParanhesis = unaryExprInPar.getOperand();
          return propagateBooleanExpression(element, opType, op1, exprInParanhesis, functionName, truthValue);
        }
        // right hand side is a cast exp
        else if(unaryExp instanceof IASTCastExpression){
          IASTCastExpression castExp = (IASTCastExpression)unaryExp;
          IASTExpression exprInCastOp = castExp.getOperand();
          return propagateBooleanExpression(element, opType, op1, exprInCastOp, functionName, truthValue);
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
      return propagateBooleanExpression(element, opType, castOperand, op2, functionName, truthValue);
    }
    else{
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
  //      case IASTUnaryExpression.op_not: // [! exp]
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
  //        case IASTBinaryExpression.op_equals:
  //          expressionValue = val1.equals(val2);
  //          break;
  //
  //        case IASTBinaryExpression.op_notequals:
  //          expressionValue = !val1.equals(val2);
  //          break;
  //
  //        case IASTBinaryExpression.op_greaterThan:
  //          expressionValue = val1 > val2;
  //          break;
  //
  //        case IASTBinaryExpression.op_greaterEqual:
  //          expressionValue = val1 >= val2;
  //          break;
  //
  //        case IASTBinaryExpression.op_lessThan:
  //          expressionValue = val1 < val2;
  //          break;
  //
  //        case IASTBinaryExpression.op_lessEqual:
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
      DeclarationEdge declarationEdge) throws UnrecognizedCCodeException {

    ExplicitElement newElement = element.clone();
    List<IASTDeclarator> declarators = declarationEdge.getDeclarators();
    // IASTDeclSpecifier specifier = declarationEdge.getDeclSpecifier();

    for (IASTDeclarator declarator : declarators)
    {
        // get the variable name in the declarator
        String varName = declarator.getName().toString();

        // TODO check other types of variables later - just handle primitive
        // types for the moment
        // get pointer operators of the declaration
        IASTPointerOperator[] pointerOps = declarator.getPointerOperators();
        // don't add pointer variables to the list since we don't track them
        if(pointerOps.length > 0)
        {
          continue;
        }
        // if this is a global variable, add to the list of global variables
        if(declarationEdge.isGlobal())
        {
          globalVars.add(varName);
          
          Long v;

          IASTInitializer init = declarator.getInitializer();
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
      IASTExpression expression, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException {
    // expression is a binary operation, e.g. a = b;
    if (expression instanceof IASTBinaryExpression) {
      return handleBinaryStatement(element, expression, cfaEdge);
    }
    // expression is a unary operation, e.g. a++;
    else if (expression instanceof IASTUnaryExpression)
    {
      return handleUnaryStatement(element, expression, cfaEdge);
    }
    // external function call
    else if(expression instanceof IASTFunctionCallExpression){
      // do nothing
      return element.clone();
    }
    // there is such a case
    else if(expression instanceof IASTIdExpression){
      return element.clone();
    }
    else{
      throw new UnrecognizedCCodeException(cfaEdge, expression);
    }
  }

  private ExplicitElement handleUnaryStatement(ExplicitElement element,
      IASTExpression expression, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException {

    IASTUnaryExpression unaryExpression = (IASTUnaryExpression) expression;
    int operator = unaryExpression.getOperator();

    int shift;
    if (operator == IASTUnaryExpression.op_postFixIncr ||
        operator == IASTUnaryExpression.op_prefixIncr) {
      // a++, ++a
      shift = 1;

    } else if(operator == IASTUnaryExpression.op_prefixDecr ||
        operator == IASTUnaryExpression.op_postFixDecr) {
      // a--, --a
      shift = -1;
    } else {
      throw new UnrecognizedCCodeException(cfaEdge, unaryExpression);
    }

    IASTExpression operand = unaryExpression.getOperand();
    if (operand instanceof IASTIdExpression) {
      String functionName = cfaEdge.getPredecessor().getFunctionName();
      String varName = getvarName(operand.getRawSignature(), functionName);

      ExplicitElement newElement = element.clone();
      if(newElement.contains(varName)){
        newElement.assignConstant(varName, newElement.getValueFor(varName) + shift, this.threshold);
      }
      return newElement;

    } else {
      throw new UnrecognizedCCodeException(cfaEdge, operand);
    }
  }

  private ExplicitElement handleBinaryStatement(ExplicitElement element,
      IASTExpression expression, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException
  {
    IASTBinaryExpression binaryExpression = (IASTBinaryExpression) expression;
    switch (binaryExpression.getOperator ())
    {
    // a = ?
    case IASTBinaryExpression.op_assign:
    {
      return handleAssignment(element, binaryExpression, cfaEdge);
    }
    // a += 2
    case IASTBinaryExpression.op_plusAssign:
    case IASTBinaryExpression.op_minusAssign:
    case IASTBinaryExpression.op_multiplyAssign:
    case IASTBinaryExpression.op_shiftLeftAssign:
    case IASTBinaryExpression.op_shiftRightAssign:
    case IASTBinaryExpression.op_binaryAndAssign:
    case IASTBinaryExpression.op_binaryXorAssign:
    case IASTBinaryExpression.op_binaryOrAssign:
    {
      return handleOperationAndAssign(element, binaryExpression, cfaEdge);
    }
    default:
      throw new UnrecognizedCCodeException(cfaEdge, binaryExpression);
    }
  }

  private ExplicitElement handleOperationAndAssign(ExplicitElement element,
      IASTBinaryExpression binaryExpression, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException {

    IASTExpression leftOp = binaryExpression.getOperand1();
    IASTExpression rightOp = binaryExpression.getOperand2();
    int operator = binaryExpression.getOperator();

    if (!(leftOp instanceof IASTIdExpression)) {
      // TODO handle fields, arrays
      throw new UnrecognizedCCodeException("left operand of assignment has to be a variable", cfaEdge, leftOp);
    }

    int newOperator;
    switch (operator) {
    case IASTBinaryExpression.op_plusAssign:
      newOperator = IASTBinaryExpression.op_plus;
      break;
    case IASTBinaryExpression.op_minusAssign:
      newOperator = IASTBinaryExpression.op_minus;
      break;
    case IASTBinaryExpression.op_multiplyAssign:
      newOperator = IASTBinaryExpression.op_multiply;
      break;
    case IASTBinaryExpression.op_shiftLeftAssign:
      newOperator = IASTBinaryExpression.op_shiftLeft;
      break;
    case IASTBinaryExpression.op_shiftRightAssign:
      newOperator = IASTBinaryExpression.op_shiftRight;
      break;
    case IASTBinaryExpression.op_binaryAndAssign:
      newOperator = IASTBinaryExpression.op_binaryAnd;
      break;
    case IASTBinaryExpression.op_binaryXorAssign:
      newOperator = IASTBinaryExpression.op_binaryXor;
      break;
    case IASTBinaryExpression.op_binaryOrAssign:
      newOperator = IASTBinaryExpression.op_binaryOr;
      break;
    default:
      throw new UnrecognizedCCodeException("unknown binary operator", cfaEdge, binaryExpression);
    }

    return handleAssignmentOfBinaryExp(element, leftOp.getRawSignature(), leftOp,
        rightOp, newOperator, cfaEdge);
  }

  private ExplicitElement handleAssignment(ExplicitElement element,
      IASTBinaryExpression binaryExpression, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException {

    IASTExpression op1 = binaryExpression.getOperand1();
    IASTExpression op2 = binaryExpression.getOperand2();

    if(op1 instanceof IASTIdExpression) {
      // a = ...
      return handleAssignmentToVariable(element, op1.getRawSignature(), op2, cfaEdge);

    } else if (op1 instanceof IASTUnaryExpression
        && ((IASTUnaryExpression)op1).getOperator() == IASTUnaryExpression.op_star) {
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
      return element.clone();

    } else if (op1 instanceof IASTArraySubscriptExpression) {
      // TODO assignment to array cell
      return element.clone();

    } else {
      throw new UnrecognizedCCodeException("left operand of assignment has to be a variable", cfaEdge, op1);
    }
  }

  private ExplicitElement handleAssignmentToVariable(ExplicitElement element,
      String lParam, IASTExpression rightExp, CFAEdge cfaEdge) throws UnrecognizedCCodeException {
    String functionName = cfaEdge.getPredecessor().getFunctionName();

    // a = 8.2 or "return;" (when rightExp == null)
    if(rightExp == null || rightExp instanceof IASTLiteralExpression){
      return handleAssignmentOfLiteral(element, lParam, rightExp, functionName);
    }
    // a = b
    else if (rightExp instanceof IASTIdExpression){
      return handleAssignmentOfVariable(element, lParam, rightExp, functionName);
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

      return handleAssignmentOfBinaryExp(element, lParam, binExp.getOperand1(),
          binExp.getOperand2(), binExp.getOperator(), cfaEdge);
    }
    // a = extCall();  or  a = b->c;
    else if(rightExp instanceof IASTFunctionCallExpression
        || rightExp instanceof IASTFieldReference){
      ExplicitElement newElement = element.clone();
      String lvarName = getvarName(lParam, functionName);
      newElement.forget(lvarName);
      return newElement;
    }
    else{
      throw new UnrecognizedCCodeException(cfaEdge, rightExp);
    }
  }

  private ExplicitElement handleAssignmentOfCast(ExplicitElement element,
      String lParam, IASTCastExpression castExp, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException
  {
    IASTExpression castOperand = castExp.getOperand();
    return handleAssignmentToVariable(element, lParam, castOperand, cfaEdge);
  }

  private ExplicitElement handleAssignmentOfUnaryExp(ExplicitElement element,
      String lParam, IASTUnaryExpression unaryExp, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException {

    String functionName = cfaEdge.getPredecessor().getFunctionName();
    // name of the updated variable, so if a = -b is handled, lParam is a
    String assignedVar = getvarName(lParam, functionName);
    ExplicitElement newElement = element.clone();

    IASTExpression unaryOperand = unaryExp.getOperand();
    int unaryOperator = unaryExp.getOperator();

    if (unaryOperator == IASTUnaryExpression.op_star) {
      // a = * b
      newElement.forget(assignedVar);

      // Cil produces code like
      // __cil_tmp8 = *((int *)__cil_tmp7);
      // so remove parentheses and cast
      if (unaryOperand instanceof IASTUnaryExpression
          && ((IASTUnaryExpression)unaryOperand).getOperator() == IASTUnaryExpression.op_bracketedPrimary) {
        unaryOperand = ((IASTUnaryExpression)unaryOperand).getOperand();
      }
      if (unaryOperand instanceof IASTCastExpression) {
        unaryOperand = ((IASTCastExpression)unaryOperand).getOperand();
      }

      if (unaryOperand instanceof IASTIdExpression) {
        missingInformationLeftVariable = assignedVar;
        missingInformationRightPointer = unaryOperand.getRawSignature();
      } else{
        throw new UnrecognizedCCodeException(cfaEdge, unaryOperand);
      }

    } else if (unaryOperator == IASTUnaryExpression.op_bracketedPrimary) {
      // a = (b + c)
      return handleAssignmentToVariable(element, lParam, unaryOperand, cfaEdge);

    } 
    else {
      // a = -b or similar
      Long value = getExpressionValue(element, unaryExp, functionName, cfaEdge);
      if (value != null) {
        newElement.assignConstant(assignedVar, value, this.threshold);
      } else {
        newElement.forget(assignedVar);
      }
    }

    return newElement;
  }

  private ExplicitElement handleAssignmentOfBinaryExp(ExplicitElement element,
      String lParam, IASTExpression lVarInBinaryExp, IASTExpression rVarInBinaryExp,
      int binaryOperator, CFAEdge cfaEdge)
  throws UnrecognizedCCodeException {

    String functionName = cfaEdge.getPredecessor().getFunctionName();
    // name of the updated variable, so if a = b + c is handled, lParam is a
    String assignedVar = getvarName(lParam, functionName);
    ExplicitElement newElement = element.clone();

    switch (binaryOperator) {
    case IASTBinaryExpression.op_divide:
    case IASTBinaryExpression.op_modulo:
    case IASTBinaryExpression.op_lessEqual:
    case IASTBinaryExpression.op_greaterEqual:
    case IASTBinaryExpression.op_binaryAnd:
    case IASTBinaryExpression.op_binaryOr:
      // TODO check which cases can be handled (I think all)
      newElement.forget(assignedVar);
      break;

    case IASTBinaryExpression.op_plus:
    case IASTBinaryExpression.op_minus:
    case IASTBinaryExpression.op_multiply:

      Long val1;
      Long val2;

      if(lVarInBinaryExp instanceof IASTUnaryExpression
          && ((IASTUnaryExpression)lVarInBinaryExp).getOperator() == IASTUnaryExpression.op_star) {
        // a = *b + c
        // TODO prepare for using strengthen operator to dereference pointer
        val1 = null;
      } else {

        val1 = getExpressionValue(element, lVarInBinaryExp, functionName, cfaEdge);
      }

      if (val1 != null) {
        val2 = getExpressionValue(element, rVarInBinaryExp, functionName, cfaEdge);
      } else {
        val2 = null;
      }

      if (val2 != null) { // this implies val1 != null

        long value;
        switch (binaryOperator) {

        case IASTBinaryExpression.op_plus:
          value = val1 + val2;
          break;

        case IASTBinaryExpression.op_minus:
          value = val1 - val2;
          break;

        case IASTBinaryExpression.op_multiply:
          value = val1 * val2;
          break;

        default:
          throw new UnrecognizedCCodeException("unkown binary operator", cfaEdge, rVarInBinaryExp.getParent());
        }

        newElement.assignConstant(assignedVar, value, this.threshold);
      } else {
        newElement.forget(assignedVar);
      }
      break;
      
    case IASTBinaryExpression.op_equals:
    case IASTBinaryExpression.op_notequals:

      Long lVal = getExpressionValue(element, lVarInBinaryExp, functionName, cfaEdge);
      Long rVal = getExpressionValue(element, rVarInBinaryExp, functionName, cfaEdge);
      
      if (lVal == null || rVal == null) {
        newElement.forget(assignedVar);
      
      } else {
        // assign 1 if expression holds, 0 otherwise
        long result = (lVal.equals(rVal) ? 1 : 0);
        
        if (binaryOperator == IASTBinaryExpression.op_notequals) {
          // negate
          result = 1 - result;
        }
        newElement.assignConstant(assignedVar, result, this.threshold);
      }
      break;
      
    default:
      // TODO warning
      newElement.forget(assignedVar);
    }
    return newElement;
  }

  private Long getExpressionValue(ExplicitElement element, IASTExpression expression,
      String functionName, CFAEdge cfaEdge) throws UnrecognizedCCodeException {

    if (expression instanceof IASTLiteralExpression) {
      return parseLiteral(expression);

    } else if (expression instanceof IASTIdExpression) {
      String varName = getvarName(expression.getRawSignature(), functionName);
      if (element.contains(varName)) {
        return element.getValueFor(varName);
      } else {
        return null;
      }

    } else if (expression instanceof IASTCastExpression) {
      return getExpressionValue(element, ((IASTCastExpression)expression).getOperand(),
          functionName, cfaEdge);

    } else if (expression instanceof IASTUnaryExpression) {
      IASTUnaryExpression unaryExpression = (IASTUnaryExpression)expression;
      int unaryOperator = unaryExpression.getOperator();
      IASTExpression unaryOperand = unaryExpression.getOperand();

      switch (unaryOperator) {

      case IASTUnaryExpression.op_minus:
        Long val = getExpressionValue(element, unaryOperand, functionName, cfaEdge);
        return (val != null) ? -val : null;

      case IASTUnaryExpression.op_bracketedPrimary:
        return getExpressionValue(element, unaryOperand, functionName, cfaEdge);

      case IASTUnaryExpression.op_amper:
        return null; // valid expresion, but it's a pointer value

      default:
        throw new UnrecognizedCCodeException("unknown unary operator", cfaEdge, unaryExpression);
      }
    } else {
      // TODO fields, arrays
      throw new UnrecognizedCCodeException(cfaEdge, expression);
    }
  }

  private ExplicitElement handleAssignmentOfVariable(ExplicitElement element,
      String lParam, IASTExpression op2, String functionName)
  {
    String rParam = op2.getRawSignature();

    String leftVarName = getvarName(lParam, functionName);
    String rightVarName = getvarName(rParam, functionName);

    ExplicitElement newElement = element.clone();
    if(newElement.contains(rightVarName)){
      newElement.assignConstant(leftVarName, newElement.getValueFor(rightVarName), this.threshold);
    }
    else{
      newElement.forget(leftVarName);
    }
    return newElement;
  }

  private ExplicitElement handleAssignmentOfLiteral(ExplicitElement element,
      String lParam, IASTExpression op2, String functionName)
  throws UnrecognizedCCodeException
  {
    ExplicitElement newElement = element.clone();

    // op2 may be null if this is a "return;" statement
    Long val = (op2 == null ? Long.valueOf(0L) : parseLiteral(op2));

    String assignedVar = getvarName(lParam, functionName);
    if (val != null) {
      newElement.assignConstant(assignedVar, val, this.threshold);
    } else {
      newElement.forget(assignedVar);
    }

    return newElement;
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

    List<ExplicitElement> retList = new ArrayList<ExplicitElement>();

    if (missingInformationLeftVariable != null && missingInformationRightPointer != null) {

      String rightVar = derefPointerToVariable(pointerElement, missingInformationRightPointer);
      if (rightVar != null) {
        rightVar = getvarName(rightVar, cfaEdge.getPredecessor().getFunctionName());
        if (explicitElement.contains(rightVar)) {
          explicitElement.assignConstant(missingInformationLeftVariable,
              explicitElement.getValueFor(rightVar), this.threshold);
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
}