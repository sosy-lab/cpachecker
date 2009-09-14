/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.explicit;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTPointerOperator;
import org.eclipse.cdt.core.dom.ast.IASTTypeIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAErrorNode;
import cfa.objectmodel.c.AssumeEdge;
import cfa.objectmodel.c.CallToReturnEdge;
import cfa.objectmodel.c.DeclarationEdge;
import cfa.objectmodel.c.FunctionCallEdge;
import cfa.objectmodel.c.FunctionDefinitionNode;
import cfa.objectmodel.c.GlobalDeclarationEdge;
import cfa.objectmodel.c.ReturnEdge;
import cfa.objectmodel.c.StatementEdge;
import cmdline.CPAMain;
import cpa.common.CPAAlgorithm;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import cpa.pointeranalysis.Memory;
import cpa.pointeranalysis.Pointer;
import cpa.pointeranalysis.PointerAnalysisElement;
import exceptions.CPAException;
import exceptions.CPATransferException;
import exceptions.ExplicitAnalysisTransferException;
import exceptions.UnrecognizedCFAEdgeException;

public class ExplicitAnalysisTransferRelation implements TransferRelation {

  private ExplicitAnalysisDomain explicitAnalysisDomain;

  private Set<String> globalVars;

  private int threshold;

  private String missingInformationLeftVariable = null;
  private String missingInformationRightPointer = null;
  private String missingInformationLeftPointer  = null;
  private IASTExpression missingInformationRightExpression = null;

  public ExplicitAnalysisTransferRelation (ExplicitAnalysisDomain explicitAnalysisfUseDomain)
  {
    this.explicitAnalysisDomain = explicitAnalysisfUseDomain;
    globalVars = new HashSet<String>();
    threshold = Integer.parseInt(CPAMain.cpaConfig.getProperty("explicitAnalysis.threshold"));
  }

  @Override
  public AbstractElement getAbstractSuccessor(AbstractElement element,
      CFAEdge cfaEdge, Precision precision) 
  throws CPATransferException
  {
    AbstractElement successor = explicitAnalysisDomain.getBottomElement();
    ExplicitAnalysisElement explicitElement = (ExplicitAnalysisElement)element;

    if(cfaEdge.getSuccessor() instanceof CFAErrorNode){
      CPAAlgorithm.errorFound = true;
      return new ExplicitAnalysisElement();
    }

    // check the type of the edge
    switch (cfaEdge.getEdgeType ())
    {

    // if edge is a statement edge, e.g. a = b + c
    case StatementEdge:
    {
      StatementEdge statementEdge = (StatementEdge) cfaEdge;
      IASTExpression expression = statementEdge.getExpression ();
      // this statement is a function return, e.g. return (a);
      // note that this is different from return edge
      // this is a statement edge which leads the function to the
      // last node of its CFA, where return edge is from that last node
      // to the return site of the caller function
      if(statementEdge.isJumpEdge())
      {
        try {
          successor = handleExitFromFunction(explicitElement, expression, statementEdge);
        } catch (ExplicitAnalysisTransferException e) {
          e.printStackTrace();
        }
      }

      // this is a regular statement
      else{
        try {
          successor = handleStatement(explicitElement, expression, cfaEdge);
        } catch (ExplicitAnalysisTransferException e) {
          e.printStackTrace();
        }
      }
      break;
    }

    // edge is a declaration edge, e.g. int a;
    case DeclarationEdge:
    {
      DeclarationEdge declarationEdge = (DeclarationEdge) cfaEdge;
      successor = handleDeclaration(explicitElement, declarationEdge);
      break;
    }

    // this is an assumption, e.g. if(a == b)
    case AssumeEdge:
    {
      AssumeEdge assumeEdge = (AssumeEdge) cfaEdge;
      IASTExpression expression = assumeEdge.getExpression();
      try {
        successor = handleAssumption(explicitElement, expression, cfaEdge, assumeEdge.getTruthAssumption());
      } catch (ExplicitAnalysisTransferException e) {
        e.printStackTrace();
      }
      break;
    }

    case BlankEdge:
    {
      successor = explicitElement.clone();
      break;
    }

    case FunctionCallEdge:
    {
      FunctionCallEdge functionCallEdge = (FunctionCallEdge) cfaEdge;

      // call to an external function
      if(functionCallEdge.isExternalCall())
      {
        // TODO external function call
//      try {
//      handleExternalFunctionCall(expAnalysisElement, functionCallEdge);
//      } catch (ExplicitAnalysisTransferException e) {
//      e.printStackTrace();
//      }
      }
      else{
        try {
          successor = handleFunctionCall(explicitElement, functionCallEdge);
        } catch (ExplicitAnalysisTransferException e) {
          e.printStackTrace();
        }
      }
      break;
    }

    // this is a return edge from function, this is different from return statement
    // of the function. See case for statement edge for details
    case ReturnEdge:
    {
      ReturnEdge functionReturnEdge = (ReturnEdge) cfaEdge;
      try {
        successor = handleFunctionReturn(explicitElement, functionReturnEdge);
      } catch (ExplicitAnalysisTransferException e) {
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

    case MultiStatementEdge:
    {
      assert(false);
      break;
    }

    case MultiDeclarationEdge:
    {
      assert(false);
      break;
    }
    default:
      try {
        throw new UnrecognizedCFAEdgeException("Unknown edge type");
      } catch (UnrecognizedCFAEdgeException e) {
        e.printStackTrace();
      }
    }
    return successor;
  }

  /**
   * Handles return from one function to another function.
   * @param element previous abstract element.
   * @param functionReturnEdge return edge from a function to its call site.
   * @return new abstract element.
   * @throws ExplicitAnalysisTransferException the operation on the edge cannot be handled.
   */
  private ExplicitAnalysisElement handleFunctionReturn(ExplicitAnalysisElement element,
      ReturnEdge functionReturnEdge) 
  throws ExplicitAnalysisTransferException{

    CallToReturnEdge summaryEdge =
      functionReturnEdge.getSuccessor().getEnteringSummaryEdge();
    IASTExpression exprOnSummary = summaryEdge.getExpression();
    // TODO get from stack
    ExplicitAnalysisElement previousElem = (ExplicitAnalysisElement)summaryEdge.extractAbstractElement("ExplicitAnalysisElement");
    ExplicitAnalysisElement newElement = previousElem.clone();
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
        throw new ExplicitAnalysisTransferException("Unhandled case " + functionReturnEdge.getPredecessor().getNodeNumber());
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
      throw new ExplicitAnalysisTransferException("Unhandled case - return from function" + functionReturnEdge.getPredecessor().getNodeNumber());
    }

    return newElement;
  }

  private ExplicitAnalysisElement handleFunctionCall(ExplicitAnalysisElement element,
      FunctionCallEdge callEdge)  
  throws ExplicitAnalysisTransferException {

    FunctionDefinitionNode functionEntryNode = (FunctionDefinitionNode)callEdge.getSuccessor();
    String calledFunctionName = functionEntryNode.getFunctionName();
    String callerFunctionName = callEdge.getPredecessor().getFunctionName();

    List<String> paramNames = functionEntryNode.getFunctionParameterNames();
    IASTExpression[] arguments = callEdge.getArguments();

    if (arguments == null) {
      arguments = new IASTExpression[0];
    }

    assert (paramNames.size() == arguments.length);

    ExplicitAnalysisElement newElement = new ExplicitAnalysisElement();

    for(String globalVar:globalVars){
      if(element.contains(globalVar)){
        newElement.getConstantsMap().put(globalVar, element.getValueFor(globalVar));
        newElement.getNoOfReferences().put(globalVar, element.getNoOfReferences().get(globalVar));
      }
    }

    for(int i=0; i<arguments.length; i++){
      IASTExpression arg = arguments[i];
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
//      throw new ExplicitAnalysisTransferException("Unhandled case");
      }
    }

    return newElement;
  }

  private ExplicitAnalysisElement handleExitFromFunction(ExplicitAnalysisElement element,
      IASTExpression expression,
      StatementEdge statementEdge) 
  throws ExplicitAnalysisTransferException {

    return handleAssignmentToVariable(element, "___cpa_temp_result_var_", expression, statementEdge);
  }

  private AbstractElement handleAssumption(ExplicitAnalysisElement element,
      IASTExpression expression, CFAEdge cfaEdge, boolean truthValue)
  throws ExplicitAnalysisTransferException 
  {

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
        IASTExpression exp1 = unaryExp.getOperand();
        // ! unaryExp
        if(exp1 instanceof IASTUnaryExpression){
          IASTUnaryExpression unaryExp1 = ((IASTUnaryExpression)exp1);
          // (exp)
          if (unaryExp1.getOperator() == IASTUnaryExpression.op_bracketedPrimary){
            IASTExpression exp2 = unaryExp1.getOperand();
            // (binaryExp)
            if(exp2 instanceof IASTBinaryExpression){
              IASTBinaryExpression binExp2 = (IASTBinaryExpression)exp2;
              return handleAssumption(element, binExp2, cfaEdge, !truthValue);
            }
            else {
              throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
            }
          }
          else {
            throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
          }
        }

        if(exp1 instanceof IASTIdExpression ||
            exp1 instanceof IASTFieldReference){
          return handleAssumption(element, exp1, cfaEdge, !truthValue);
        }
        else {
          throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
        }
      }
      else if(unaryExp.getOperator() == IASTUnaryExpression.op_bracketedPrimary){
        return handleAssumption(element, unaryExp.getOperand(), cfaEdge, truthValue);
      }
      else if(unaryExp instanceof IASTCastExpression){
        return handleAssumption(element, ((IASTCastExpression)expression).getOperand(), cfaEdge, truthValue);
      }
      else {
        throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
      }
    }

    else if(expression instanceof IASTIdExpression
        || expression instanceof IASTFieldReference){
      return propagateBooleanExpression(element, -999, expression, null, functionName, truthValue);
    }

    else{
      throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
    }
  }

  private AbstractElement propagateBooleanExpression(ExplicitAnalysisElement element, 
      int opType,IASTExpression op1, 
      IASTExpression op2, String functionName, boolean truthValue) 
  throws ExplicitAnalysisTransferException {

    ExplicitAnalysisElement newElement = element.clone();

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
              return explicitAnalysisDomain.getBottomElement();
            }
          }
          else{
          }
        }
        // ! [literal]
        else {
          if(newElement.contains(getvarName(varName, functionName))){
            if(newElement.getValueFor(getvarName(varName, functionName)) != 0){
              return explicitAnalysisDomain.getBottomElement();
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
        Long val = parseLiteral(op2);
        
        if (val != null) {
          long valueOfLiteral = val; // unboxing
        
          // a == 9
          if(opType == IASTBinaryExpression.op_equals) {
            if(truthValue){
              if(newElement.contains(getvarName(varName, functionName))){
                if(newElement.getValueFor(getvarName(varName, functionName)) != valueOfLiteral){
                  return explicitAnalysisDomain.getBottomElement();
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
                  return explicitAnalysisDomain.getBottomElement();
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
                  return explicitAnalysisDomain.getBottomElement();
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
                  return explicitAnalysisDomain.getBottomElement();
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
                  return explicitAnalysisDomain.getBottomElement();
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
                  return explicitAnalysisDomain.getBottomElement();
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
                  return explicitAnalysisDomain.getBottomElement();
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
                if(newElement.getValueFor(getvarName(varName, functionName)) == (0 - valueOfLiteral)){
                  return explicitAnalysisDomain.getBottomElement();
                }
              }
              else{
              }
            }
            // ! a != 9
            else {
              if(newElement.contains(getvarName(varName, functionName))){
                if(newElement.getValueFor(getvarName(varName, functionName)) != (0 - valueOfLiteral)){
                  return explicitAnalysisDomain.getBottomElement();
                }
              }
              else{
                newElement.assignConstant(getvarName(varName, functionName), (0 - valueOfLiteral), this.threshold);
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
            throw new ExplicitAnalysisTransferException("Unhandled case ");
          }
        }
        else{
          throw new ExplicitAnalysisTransferException("Unhandled case ");
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
                return explicitAnalysisDomain.getBottomElement();
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
                return explicitAnalysisDomain.getBottomElement();
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
                return explicitAnalysisDomain.getBottomElement();
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
                return explicitAnalysisDomain.getBottomElement();
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
                return explicitAnalysisDomain.getBottomElement();
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
                return explicitAnalysisDomain.getBottomElement();
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
          throw new ExplicitAnalysisTransferException("Unhandled case ");
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
            Long val = parseLiteral(unaryExpOp);

            if (val != null) {
              long valueOfLiteral = val; // unboxing

              // a == 9
              if(opType == IASTBinaryExpression.op_equals) {
                if(truthValue){
                  if(newElement.contains(getvarName(varName, functionName))){
                    if(newElement.getValueFor(getvarName(varName, functionName)) != valueOfLiteral){
                      return explicitAnalysisDomain.getBottomElement();  
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
                      return explicitAnalysisDomain.getBottomElement();  
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
                      return explicitAnalysisDomain.getBottomElement();  
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
                      return explicitAnalysisDomain.getBottomElement();  
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
                      return explicitAnalysisDomain.getBottomElement();  
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
                      return explicitAnalysisDomain.getBottomElement();  
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
                throw new ExplicitAnalysisTransferException("Unhandled case ");
              }
            }
            else{
              throw new ExplicitAnalysisTransferException("Unhandled case ");
            }
          }
          else{
            throw new ExplicitAnalysisTransferException("Unhandled case ");
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
          throw new ExplicitAnalysisTransferException("Unhandled case ");
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
    }
    return newElement;
  }

  private ExplicitAnalysisElement handleDeclaration(ExplicitAnalysisElement element,
      DeclarationEdge declarationEdge) {

    ExplicitAnalysisElement newElement = element.clone();
    IASTDeclarator[] declarators = declarationEdge.getDeclarators();
    // IASTDeclSpecifier specifier = declarationEdge.getDeclSpecifier();

    for (IASTDeclarator declarator : declarators)
    {
      if(declarator != null)
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
        if(declarationEdge instanceof GlobalDeclarationEdge)
        {
          globalVars.add(varName);
        }
      }
    }
    return newElement;
  }

  private ExplicitAnalysisElement handleStatement(ExplicitAnalysisElement element,
      IASTExpression expression, CFAEdge cfaEdge) 
  throws ExplicitAnalysisTransferException {
    // expression is a binary operation, e.g. a = b;
    if (expression instanceof IASTBinaryExpression) {
      return handleBinaryExpression(element, expression, cfaEdge);
    }
    // expression is a unary operation, e.g. a++;
    else if (expression instanceof IASTUnaryExpression)
    {
      return handleUnaryExpression(element, expression, cfaEdge);
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
      throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
    }
  }

  private ExplicitAnalysisElement handleUnaryExpression(ExplicitAnalysisElement element,
      IASTExpression expression, CFAEdge cfaEdge) 
  throws ExplicitAnalysisTransferException {

    String functionName = cfaEdge.getPredecessor().getFunctionName();
    IASTUnaryExpression unaryExpression = (IASTUnaryExpression) expression;

    int operator = unaryExpression.getOperator ();

    String leftOpName = unaryExpression.getOperand().getRawSignature();
    String varName = getvarName(leftOpName, functionName); 

    // a++, ++a
    if (operator == IASTUnaryExpression.op_postFixIncr ||
        operator == IASTUnaryExpression.op_prefixIncr
    )
    {
      return addLiteralToVariable(element, cfaEdge, varName, varName, 1);
    }
    // a--, --a
    else if(operator == IASTUnaryExpression.op_prefixDecr ||
        operator == IASTUnaryExpression.op_postFixDecr)
    {
      return addLiteralToVariable(element, cfaEdge, varName, varName, -1);
    }

    else
    {
      throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
    }
  }

  private ExplicitAnalysisElement addLiteralToVariable(ExplicitAnalysisElement element, CFAEdge cfaEdge,
      String assignedVar, String varName, long val) 
  {
    ExplicitAnalysisElement newElement = element.clone();
    if(newElement.contains(varName)){
      newElement.assignConstant(assignedVar, newElement.getValueFor(varName) + val, this.threshold);
    }
    else{
      newElement.forget(assignedVar);
    }
    return newElement;
  }

  private ExplicitAnalysisElement subtractVariableFromLiteral(ExplicitAnalysisElement element, CFAEdge cfaEdge,
      String assignedVar, String varName, long val) 
  {
    ExplicitAnalysisElement newElement = element.clone();
    if(newElement.contains(varName)){
      newElement.assignConstant(assignedVar, val - newElement.getValueFor(varName), this.threshold);
    }
    else{
      newElement.forget(assignedVar);
    }
    return newElement;
  }

  private ExplicitAnalysisElement multiplyLiteralWithVariable(ExplicitAnalysisElement element, CFAEdge cfaEdge,
      String assignedVar, String varName, long val)
  {
    ExplicitAnalysisElement newElement = element.clone();
    if(newElement.contains(varName)){
      newElement.assignConstant(assignedVar, newElement.getValueFor(varName) * val, this.threshold);
    }
    else{
      newElement.forget(assignedVar);
    }
    return newElement;
  }

  private ExplicitAnalysisElement handleBinaryExpression(ExplicitAnalysisElement element,
      IASTExpression expression, CFAEdge cfaEdge)
  throws ExplicitAnalysisTransferException 
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
      throw new ExplicitAnalysisTransferException("Unhandled case " + binaryExpression.getRawSignature());
    }
  }

  private ExplicitAnalysisElement handleOperationAndAssign(ExplicitAnalysisElement element,
      IASTBinaryExpression binaryExpression, CFAEdge cfaEdge) 
  throws ExplicitAnalysisTransferException 
  {
    String functionName = cfaEdge.getPredecessor().getFunctionName();
    IASTExpression op1 = binaryExpression.getOperand1();
    IASTExpression op2 = binaryExpression.getOperand2();
    int typeOfOperator = binaryExpression.getOperator();

    // First operand is not an id expression
    if (!(op1 instanceof IASTIdExpression ||
        op1 instanceof IASTFieldReference ||
        (op1 instanceof IASTUnaryExpression && ((IASTUnaryExpression)op1).getOperator() == IASTUnaryExpression.op_star)))
    {
      System.out.println("First operand is not a proper variable");
      throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
    }
    // If first operand is an id expression or field reference
    else if(op1 instanceof IASTIdExpression ||
        op1 instanceof IASTFieldReference ||
        (op1 instanceof IASTUnaryExpression && ((IASTUnaryExpression)op1).getOperator() == IASTUnaryExpression.op_star))
    {
      String nameOfLVar = op1.getRawSignature();
      String varName = getvarName(nameOfLVar, functionName);
      // a op= 2
      if(op2 instanceof IASTLiteralExpression){
        Long val = parseLiteral(op2);
        
        if (val != null) {
          
          switch (typeOfOperator) {
            case IASTBinaryExpression.op_plusAssign: // a += 2
              return addLiteralToVariable(element, cfaEdge, varName, varName, val);

            case IASTBinaryExpression.op_minusAssign: // a -= 2
              long negVal = 0 - val;
              return addLiteralToVariable(element, cfaEdge, varName, varName,  negVal);
              
            case IASTBinaryExpression.op_multiplyAssign: // a *= 2
              return multiplyLiteralWithVariable(element, cfaEdge, varName, varName, val);

            case IASTBinaryExpression.op_shiftLeftAssign:
            case IASTBinaryExpression.op_shiftRightAssign:
            case IASTBinaryExpression.op_binaryAnd:
            case IASTBinaryExpression.op_binaryXor:
            case IASTBinaryExpression.op_binaryOr:
              ExplicitAnalysisElement newElement = element.clone();
              newElement.forget(varName);
              return newElement;
              
            default:   
              throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
          }
        }
        else{
          // not an integer literal
          ExplicitAnalysisElement newElement = element.clone();
          newElement.forget(varName);
          return newElement;
        }
      }
      // a op= b
      else if(op2 instanceof IASTIdExpression ||
          op2 instanceof IASTFieldReference){

        String nameOfRVar = op2.getRawSignature();
        String rightVar = getvarName(nameOfRVar, functionName);

        // a += b
        if(typeOfOperator == IASTBinaryExpression.op_plusAssign){
          return addTwoVariables(element, cfaEdge, varName, varName, rightVar);
        }
        // a -= b
        else if(typeOfOperator == IASTBinaryExpression.op_minusAssign){
          return subtractOneVariable(element, cfaEdge, varName, varName, rightVar);
        }
        // a *= b
        else if(typeOfOperator == IASTBinaryExpression.op_multiplyAssign){
          return multiplyTwoVariables(element, cfaEdge, varName, varName, rightVar);
        }
        // a |= b, binary ops
        else if(IASTBinaryExpression.op_shiftLeftAssign <= typeOfOperator &&
            IASTBinaryExpression.op_binaryOrAssign >= typeOfOperator){
          ExplicitAnalysisElement newElement = element.clone();
          newElement.forget(varName);
          return newElement;
        }
        else{
          throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
        }
      }
      // TODO forget for now
      else if(op2 instanceof IASTCastExpression){
        ExplicitAnalysisElement newElement = element.clone();
        newElement.forget(varName);
        return newElement;
      }
      // TODO forget for now
      else if(op2 instanceof IASTBinaryExpression){
        ExplicitAnalysisElement newElement = element.clone();
        newElement.forget(varName);
        return newElement;
      }
      else{
        throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
      }
    }
    else{
      throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
    }
  }

  private ExplicitAnalysisElement multiplyTwoVariables(ExplicitAnalysisElement element,
      CFAEdge cfaEdge, String assignedVar, String leftVar, String rightVar) 
  {
    ExplicitAnalysisElement newElement = element.clone();
    if(newElement.contains(leftVar) && newElement.contains(rightVar)){
      newElement.assignConstant(assignedVar, 
          (newElement.getValueFor(leftVar) * newElement.getValueFor(rightVar)), 
          this.threshold);
    }
    else{
      newElement.forget(assignedVar);
    }
    return newElement;
  }

  private ExplicitAnalysisElement subtractOneVariable(ExplicitAnalysisElement element,
      CFAEdge cfaEdge, String assignedVar, String leftVar, String rightVar)
  {
    ExplicitAnalysisElement newElement = element.clone();
    if(newElement.contains(leftVar) && newElement.contains(leftVar)){
      newElement.assignConstant(assignedVar, 
          (newElement.getValueFor(leftVar) - newElement.getValueFor(rightVar)), 
          this.threshold);
    }
    else{
      newElement.forget(assignedVar);
    }
    return newElement;
  }

  private ExplicitAnalysisElement addTwoVariables(ExplicitAnalysisElement element,
      CFAEdge cfaEdge, String assignedVar, String leftVar, String rightVar)
  {
    ExplicitAnalysisElement newElement = element.clone();
    if(newElement.contains(leftVar) && newElement.contains(leftVar)){
      newElement.assignConstant(assignedVar, 
          (newElement.getValueFor(leftVar) + newElement.getValueFor(rightVar)), 
          this.threshold);
    }
    else{
      newElement.forget(assignedVar);
    }
    return newElement;
  }

  private ExplicitAnalysisElement handleAssignment(ExplicitAnalysisElement element,
      IASTBinaryExpression binaryExpression, CFAEdge cfaEdge) 
  throws ExplicitAnalysisTransferException
  {
    IASTExpression op1 = binaryExpression.getOperand1();
    IASTExpression op2 = binaryExpression.getOperand2();

    if(op1 instanceof IASTIdExpression)
    {
      return handleAssignmentToVariable(element, op1.getRawSignature(), op2, cfaEdge);
    }
    else if (op1 instanceof IASTUnaryExpression) {
      IASTUnaryExpression unaryExpression = (IASTUnaryExpression)op1;
      ExplicitAnalysisElement newElement = element.clone();
 
      if (unaryExpression.getOperator() == IASTUnaryExpression.op_star) {
        // *a = ...
        if (unaryExpression.getOperand() instanceof IASTIdExpression) {
          missingInformationLeftPointer = unaryExpression.getOperand().getRawSignature();
          missingInformationRightExpression = binaryExpression.getOperand2();
        }
        return newElement;
      }
      else if (unaryExpression.getOperator() == IASTUnaryExpression.op_amper) {
        // & a = ...
        // do nothing
        return newElement;
      }
      else {
        throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
      }
    }
    else if (op1 instanceof IASTFieldReference) {
      // TODO do nothing
      ExplicitAnalysisElement newElement = element.clone();
      return newElement;
    }
    else if (op1 instanceof IASTArraySubscriptExpression) {
      // TODO do nothing
      ExplicitAnalysisElement newElement = element.clone();
      return newElement;
    }
    else {
      throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
    }
  }

  private ExplicitAnalysisElement handleAssignmentToVariable(ExplicitAnalysisElement element,
                          String lParam, IASTExpression rightExp, CFAEdge cfaEdge) throws ExplicitAnalysisTransferException {
    String functionName = cfaEdge.getPredecessor().getFunctionName();

    // a = 8.2
    if(rightExp instanceof IASTLiteralExpression){
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
    // a = b op c
    else if(rightExp instanceof IASTBinaryExpression){
      IASTBinaryExpression binExp = (IASTBinaryExpression)rightExp;
      
      return handleAssignmentOfBinaryExp(element, lParam, binExp.getOperand1(),
                            binExp.getOperand2(), binExp.getOperator(), cfaEdge);
    }
    // a = -b
    else if(rightExp instanceof IASTUnaryExpression){
      return handleAssignmentOfUnaryExp(element, lParam, (IASTUnaryExpression)rightExp, cfaEdge);
    }
    // a = extCall();  or  a = b->c;
    else if(rightExp instanceof IASTFunctionCallExpression
         || rightExp instanceof IASTFieldReference){
      ExplicitAnalysisElement newElement = element.clone();
      String lvarName = getvarName(lParam, functionName);
      newElement.forget(lvarName);
      return newElement;
    }
    else{
      throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
    }
  }
  
  private ExplicitAnalysisElement handleAssignmentOfCast(ExplicitAnalysisElement element,
                              String lParam, IASTCastExpression castExp, CFAEdge cfaEdge) 
                              throws ExplicitAnalysisTransferException 
  {
    IASTExpression castOperand = castExp.getOperand();
    return handleAssignmentToVariable(element, lParam, castOperand, cfaEdge);
  }

  private ExplicitAnalysisElement handleAssignmentOfUnaryExp(ExplicitAnalysisElement element,
      String lParam, IASTUnaryExpression unaryExp, CFAEdge cfaEdge) 
  throws ExplicitAnalysisTransferException 
  {
    ExplicitAnalysisElement newElement = element.clone();
    String functionName = cfaEdge.getPredecessor().getFunctionName();
    String assignedVar = getvarName(lParam , functionName);

    IASTExpression unaryOperand = unaryExp.getOperand();
    int operatorType = unaryExp.getOperator();
    // a = -b
    if(operatorType == IASTUnaryExpression.op_minus){
      if(unaryOperand instanceof IASTLiteralExpression){
        Long val = parseLiteral(unaryOperand);
        
        if (val != null) {
          newElement.assignConstant(assignedVar, (0 - val), this.threshold);
        }
        else{
          newElement.forget(assignedVar);
        }
      }
      else if(unaryOperand instanceof IASTIdExpression){
        String nameOfVar = unaryOperand.getRawSignature();
        String varName = getvarName(nameOfVar, functionName);
        if(newElement.contains(varName)){
          newElement.assignConstant(assignedVar, (0 - newElement.getValueFor(varName)), this.threshold);
        }
        else{
          newElement.forget(assignedVar);
        }
      }
      else{
        throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
      }
    }
    else if (operatorType == IASTUnaryExpression.op_star) {
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
        throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
      }
    }
    else if(operatorType == IASTUnaryExpression.op_amper) {
      // a = & b
      // ignore pointer operations
      newElement.forget(assignedVar);
    } else if(operatorType == IASTUnaryExpression.op_bracketedPrimary) {
      return handleAssignmentToVariable(element, lParam, unaryOperand, cfaEdge);
    }
    else {
      throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
    }
    return newElement;
  }

  private ExplicitAnalysisElement handleAssignmentOfBinaryExp(ExplicitAnalysisElement element, String lParam,
      IASTExpression lVarInBinaryExp, IASTExpression rVarInBinaryExp, 
      int binaryOperator, CFAEdge cfaEdge) throws ExplicitAnalysisTransferException{

    String functionName = cfaEdge.getPredecessor().getFunctionName();
    // name of the updated variable, so if a = b + c is handled, lParam is a
    String assignedVar = getvarName(lParam, functionName);

    switch (binaryOperator)
    {
    // operand in left hand side of expression is an addition
    case IASTBinaryExpression.op_plus:
    {
      // TODO: refactoring, replace lots of code with calls to getExpressionValue()
      
      // a = -b + ?, left variable in right hand side of the expression is unary
      if(lVarInBinaryExp instanceof IASTUnaryExpression){
        IASTUnaryExpression unaryExpression = (IASTUnaryExpression) lVarInBinaryExp;
        int operator = unaryExpression.getOperator ();
        // make sure that unary expression is minus operator
        if(operator == IASTUnaryExpression.op_minus){
          IASTExpression unaryOperand = unaryExpression.getOperand();
          if(unaryOperand instanceof IASTLiteralExpression){
            Long val = parseLiteral(unaryOperand);
            
            if (val != null) {
              long negVal = 0 - val;
              
              IASTIdExpression rvar = ((IASTIdExpression)rVarInBinaryExp);
              String nameOfRVar = rvar.getRawSignature();
              String rightVariable = getvarName(nameOfRVar, functionName);
              return addLiteralToVariable(element, cfaEdge, assignedVar, rightVariable, negVal);
            }
            else{
              throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
            }
          }
          else{
            throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
          }
        }
        else if(operator == IASTUnaryExpression.op_star ||
            operator == IASTUnaryExpression.op_amper ){

          String nameOfLVar = lVarInBinaryExp.getRawSignature();
          String nameOfLeftVarOfBinaryExp = getvarName(nameOfLVar, functionName);

          // a = b + 2
          if(rVarInBinaryExp instanceof IASTLiteralExpression){
            Long val = parseLiteral(rVarInBinaryExp);
            
            if (val != null) {
              return addLiteralToVariable(element, cfaEdge, assignedVar, nameOfLeftVarOfBinaryExp, val);
            } else {
              throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
            }
          }
          // a = b + c,
          else if(rVarInBinaryExp instanceof IASTIdExpression){
            IASTIdExpression rvar = ((IASTIdExpression)rVarInBinaryExp);
            String nameOfRVar = rvar.getRawSignature();
            String nameOfRightVarOfBinaryExp = getvarName(nameOfRVar, functionName);
            return addTwoVariables(element, cfaEdge, assignedVar, nameOfLeftVarOfBinaryExp, nameOfRightVarOfBinaryExp);
          }
          else if(rVarInBinaryExp instanceof IASTCastExpression){
            IASTCastExpression castExp = (IASTCastExpression)rVarInBinaryExp;
            IASTExpression expInCastOp = castExp.getOperand();
            return handleAssignmentOfBinaryExp(element, lParam, lVarInBinaryExp, expInCastOp, 
                binaryOperator ,cfaEdge);
          }
          else{
            throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
          }
        }
        else if(operator == IASTUnaryExpression.op_bracketedPrimary){
          IASTExpression expInParant = unaryExpression.getOperand();
          return handleAssignmentOfBinaryExp(element, lParam, expInParant, rVarInBinaryExp, binaryOperator, cfaEdge);
        }
        else if(lVarInBinaryExp instanceof IASTCastExpression){
          IASTCastExpression castExp = (IASTCastExpression) lVarInBinaryExp;
          IASTExpression expInCastOp = castExp.getOperand();
          return handleAssignmentOfBinaryExp(element, lParam, expInCastOp, rVarInBinaryExp, binaryOperator, cfaEdge);
        }
        // TODO forgetting for now
        else{
//        ExplicitAnalysisElement newElement = ((ExplicitAnalysisElement)element).clone();
//        newElement.forget(assignedVar);
//        return newElement;
          throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
        }
      }
      // a = b + ?, left variable in right hand side of the expression is a variable
      else if(lVarInBinaryExp instanceof IASTIdExpression 
          || lVarInBinaryExp instanceof IASTFieldReference){
//      IASTIdExpression lvar = ((IASTIdExpression)lVarInBinaryExp);
        String nameOfLVar = lVarInBinaryExp.getRawSignature();
        String nameOfLeftVarOfBinaryExp = getvarName(nameOfLVar, functionName);

        // a = b + 2
        if(rVarInBinaryExp instanceof IASTLiteralExpression){
          Long val = parseLiteral(rVarInBinaryExp);
          
          if (val != null) {
            return addLiteralToVariable(element, cfaEdge, assignedVar, nameOfLeftVarOfBinaryExp, val);
          }
          else{
            throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
          }
        }
        // a = b + c,
        else if(rVarInBinaryExp instanceof IASTIdExpression
            || lVarInBinaryExp instanceof IASTFieldReference){
          String nameOfRVar = rVarInBinaryExp.getRawSignature();
          String nameOfRightVarOfBinaryExp = getvarName(nameOfRVar, functionName);
          return addTwoVariables(element, cfaEdge, assignedVar, nameOfLeftVarOfBinaryExp, nameOfRightVarOfBinaryExp);
        }
        else{
          // TODO forgetting
          ExplicitAnalysisElement newElement = element.clone();
          newElement.forget(assignedVar);
          return newElement;
//          throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
        }
      }

      // a = 9 + ? left variable in right hand side of the expression is a literal
      else if(lVarInBinaryExp instanceof IASTLiteralExpression){
        Long val = parseLiteral(lVarInBinaryExp);
        
        if (val != null) {
          // a = 8 + b
          if(rVarInBinaryExp instanceof IASTIdExpression){
            IASTIdExpression rvar = ((IASTIdExpression)rVarInBinaryExp);
            String nameOfRVar = rvar.getRawSignature();
            String rightVarName = getvarName(nameOfRVar, functionName);
            return addLiteralToVariable(element, cfaEdge, assignedVar, rightVarName, val);
          }
          // a = 8 + 9
          else if(rVarInBinaryExp instanceof IASTLiteralExpression){
            //Cil eliminates this case
            throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
          }
        }
        else {
          throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
        }
      }
      // TODO forgetting
      else{
      ExplicitAnalysisElement newElement = element.clone();
      newElement.forget(assignedVar);
      return newElement;
//        throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
      }

      break;
    }

    // operand in left hand side of expression is a subtraction
    case IASTBinaryExpression.op_minus:
    {
      // a = -9 + ? left variable in right hand side of the expression is a unary expression
      if(lVarInBinaryExp instanceof IASTUnaryExpression){
        IASTUnaryExpression unaryExpression = (IASTUnaryExpression) lVarInBinaryExp;
        int operator = unaryExpression.getOperator ();
        // make sure it is minus op
        if(operator == IASTUnaryExpression.op_minus){
          IASTExpression unaryOperand = unaryExpression.getOperand();
          if(unaryOperand instanceof IASTLiteralExpression){
            Long val = parseLiteral(unaryOperand);
            
            if (val != null) {
              long negVal = 0 - val;

              IASTIdExpression rvar = ((IASTIdExpression)rVarInBinaryExp);
              String nameOfRVar = rvar.getRawSignature();
              String nameOfRightVariable = getvarName(nameOfRVar, functionName);
              return addLiteralToVariable(element, cfaEdge, assignedVar, nameOfRightVariable, negVal);
            }
            else{
              throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
            }
          }
          else{
            throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
          }
        }
        else{
          throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
        }

      }
      // a = b - ? left variable in right hand side of the expression is a variable
      else if(lVarInBinaryExp instanceof IASTIdExpression
          || lVarInBinaryExp instanceof IASTFieldReference){
        String nameOfLVar = lVarInBinaryExp.getRawSignature();
        String nameOfLeftVar = getvarName(nameOfLVar, functionName);
        // a = b - 2
        if(rVarInBinaryExp instanceof IASTLiteralExpression){
          Long val = parseLiteral(rVarInBinaryExp);
          
          if (val != null) {
            long negVal = 0 - val;
            return addLiteralToVariable(element, cfaEdge, assignedVar, nameOfLeftVar, negVal);
          }
          else{
            throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
          }
        }
        // a = b - c
        else if(rVarInBinaryExp instanceof IASTIdExpression){
          IASTIdExpression rvar = ((IASTIdExpression)rVarInBinaryExp);
          String nameOfRVar = rvar.getRawSignature();
          String nameOfRightVar = getvarName(nameOfRVar, functionName);
          return subtractOneVariable(element, cfaEdge, assignedVar, nameOfLeftVar, nameOfRightVar);
        }
        else{
          throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
        }
      }

      // a = 8 - ? left variable in right hand side of the expression is a literal
      else if(lVarInBinaryExp instanceof IASTLiteralExpression){
        Long val = parseLiteral(lVarInBinaryExp);
        
        if (val != null) {
          // a = 8 - b
          if(rVarInBinaryExp instanceof IASTIdExpression){
            IASTIdExpression rvar = ((IASTIdExpression)rVarInBinaryExp);
            String nameOfRVar = rvar.getRawSignature();
            String nameOfVar = getvarName(nameOfRVar, functionName);
            return subtractVariableFromLiteral(element, cfaEdge, assignedVar, nameOfVar, val);
          }
          // a = 8 - 7
          else if(rVarInBinaryExp instanceof IASTLiteralExpression){
            //Cil eliminates this case
            throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
          } 
        }
        else {
          throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
        }
      }
      else{
        throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
      }
      break;
    }

    // operand in left hand side of expression is a multiplication
    case IASTBinaryExpression.op_multiply:
    {
      // a = -2 * b
      if(lVarInBinaryExp instanceof IASTUnaryExpression){
        IASTUnaryExpression unaryExpression = (IASTUnaryExpression) lVarInBinaryExp;
        int operator = unaryExpression.getOperator ();

        if(operator == IASTUnaryExpression.op_minus){
          IASTExpression unaryOperand = unaryExpression.getOperand();
          if(unaryOperand instanceof IASTLiteralExpression){
            Long val = parseLiteral(unaryOperand);
            
            if (val != null) {
              long negVal = 0 - val;
              IASTIdExpression rvar = ((IASTIdExpression)rVarInBinaryExp);
              String nameOfRVar = rvar.getRawSignature();
              String nameOfVar = getvarName(nameOfRVar, functionName);
              return multiplyLiteralWithVariable(element, cfaEdge, assignedVar, nameOfVar, negVal);
            }
            else{
              throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
            }
          }
          else{
            throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
          }
        }
        else{
          throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
        }

      }
      // a = b * ?
      else if(lVarInBinaryExp instanceof IASTIdExpression){
        IASTIdExpression lvar = ((IASTIdExpression)lVarInBinaryExp);
        String nameOfLVar = lvar.getRawSignature();
        String nameOfVar = getvarName(nameOfLVar, functionName);

        // a = b * 2
        if(rVarInBinaryExp instanceof IASTLiteralExpression){
          Long val = parseLiteral(rVarInBinaryExp);
          
          if (val != null) {
            return multiplyLiteralWithVariable(element, cfaEdge, assignedVar, nameOfVar, val);
          }
          else{
            throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
          }
        }
        // a = b * -2
        else if(rVarInBinaryExp instanceof IASTUnaryExpression){
          IASTUnaryExpression unaryExpression = (IASTUnaryExpression) rVarInBinaryExp;
          int operator = unaryExpression.getOperator ();

          if(operator == IASTUnaryExpression.op_minus){
            IASTExpression unaryOperand = unaryExpression.getOperand();
            if(unaryOperand instanceof IASTLiteralExpression){
              Long val = parseLiteral(unaryOperand);
              
              if (val != null) {
                long negVal = 0 - val;
                return multiplyLiteralWithVariable(element, cfaEdge, assignedVar, nameOfVar, negVal);
              }
              else{
                throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
              }
            }
            else{
              throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
            }
          }
          else{
            throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
          }

        }

        // a = b * c
        else if(rVarInBinaryExp instanceof IASTIdExpression){
          String nameOfLeftVarOfBinaryExp = getvarName(nameOfLVar, functionName);
          String nameOfRightVarOfBinaryExp = getvarName(((IASTIdExpression)rVarInBinaryExp).getRawSignature(), functionName);
          return multiplyTwoVariables(element, cfaEdge, assignedVar, nameOfLeftVarOfBinaryExp, nameOfRightVarOfBinaryExp);
        }
        else{
          throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
        }
      }

      // a = 8 * ?
      else if(lVarInBinaryExp instanceof IASTLiteralExpression){
        Long val = parseLiteral(lVarInBinaryExp);
        
        if (val != null) {
          
          // a = 8 * b
          if(rVarInBinaryExp instanceof IASTIdExpression){
            IASTIdExpression rvar = ((IASTIdExpression)rVarInBinaryExp);
            String nameOfRVar = rvar.getRawSignature();
            String nameOfVar = getvarName(nameOfRVar, functionName);
            return multiplyLiteralWithVariable(element, cfaEdge, assignedVar, nameOfVar, val);
          }
          // a = 8 * 9
          else if(rVarInBinaryExp instanceof IASTLiteralExpression){
            //Cil eliminates this case
            //PW: it does not, if one of the operands was a sizeof()
            Long val2 = parseLiteral(rVarInBinaryExp);
            if (val2 != null) {
              ExplicitAnalysisElement newElement = element.clone();
              newElement.assignConstant(assignedVar, val * val2, this.threshold);
              return newElement; 

            } else {
              throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement()); 
            }
          }
        }
        else {
          throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
        }
      }

      else{
        throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
      }

      break;
    }
    // operand in left hand side of expression is a division
    // operand in left hand side of expression is modulo op
    case IASTBinaryExpression.op_divide:
    case IASTBinaryExpression.op_modulo:
    case IASTBinaryExpression.op_lessEqual:
    case IASTBinaryExpression.op_greaterEqual:
    case IASTBinaryExpression.op_binaryAnd:
    case IASTBinaryExpression.op_binaryOr:
    {
      ExplicitAnalysisElement newElement = element.clone();
      newElement.forget(assignedVar);
      return newElement;
    }

    default: throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
    }
    throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
  }

  private Long getExpressionValue(ExplicitAnalysisElement element, IASTExpression expression,
                                  String functionName, CFAEdge cfaEdge) throws ExplicitAnalysisTransferException {
    if (expression instanceof IASTLiteralExpression) {
      return parseLiteral(expression);
    
    } else if (expression instanceof IASTIdExpression) {
      String varName = getvarName(expression.getRawSignature(), functionName);
      if (element.contains(varName)) {
        return element.getValueFor(varName);
      } else {
        return null;
      }
      
    } else if (expression instanceof IASTUnaryExpression) {
      IASTUnaryExpression unaryExpression = (IASTUnaryExpression)expression;
      int unaryOperator = unaryExpression.getOperator();
      IASTExpression unaryOperand = unaryExpression.getOperand();
      
      if (unaryOperator == IASTUnaryExpression.op_minus) {
        Long val = getExpressionValue(element, unaryOperand, functionName, cfaEdge);
        
        return (val != null) ? -val : null;
      
      } else if (unaryOperator == IASTUnaryExpression.op_bracketedPrimary) {
        return getExpressionValue(element, unaryOperand, functionName, cfaEdge);

      } else {
        throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
      }
    } else {
      throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
    }
  }
  
  private ExplicitAnalysisElement handleAssignmentOfVariable(ExplicitAnalysisElement element,
      String lParam, IASTExpression op2, String functionName)
  {
    String rParam = op2.getRawSignature();

    String leftVarName = getvarName(lParam, functionName);
    String rightVarName = getvarName(rParam, functionName);

    ExplicitAnalysisElement newElement = element.clone();
    if(newElement.contains(rightVarName)){
      newElement.assignConstant(leftVarName, newElement.getValueFor(rightVarName), this.threshold);
    }
    else{
      newElement.forget(leftVarName);
    }
    return newElement;
  }

  private ExplicitAnalysisElement handleAssignmentOfLiteral(ExplicitAnalysisElement element,
                        String lParam, IASTExpression op2, String functionName)
                        throws ExplicitAnalysisTransferException
  {
    ExplicitAnalysisElement newElement = element.clone();

    String assignedVar = getvarName(lParam, functionName);
    Long val = parseLiteral(op2);

    if (val != null) {
      newElement.assignConstant(assignedVar, val, this.threshold);
    } else {
      newElement.forget(assignedVar);
    }
    
    return newElement;
  }

  private Long parseLiteral(IASTExpression expression) throws ExplicitAnalysisTransferException {
    if (expression instanceof IASTLiteralExpression) {

      int typeOfLiteral = ((IASTLiteralExpression)expression).getKind();
      if (typeOfLiteral == IASTLiteralExpression.lk_integer_constant) {
        
        String s = expression.getRawSignature();
        if(s.endsWith("L") || s.endsWith("U")){
          s = s.replace("L", "");
          s = s.replace("U", "");
        }
        try {
          return Long.valueOf(s);
        } catch (NumberFormatException e) {
          throw new ExplicitAnalysisTransferException("Invalid integer literal " + s);
        }
      }
    }
    return null;
  }
  
  @Override
  public List<AbstractElementWithLocation> getAllAbstractSuccessors(
      AbstractElementWithLocation element, Precision precision) throws CPAException, CPATransferException {
    throw new CPAException ("Cannot get all abstract successors from non-location domain");
  }

  public String getvarName(String variableName, String functionName){
    if(globalVars.contains(variableName)){
      return variableName;
    }
    return functionName + "::" + variableName;
  }

  @Override
  public AbstractElement strengthen(AbstractElement element,
                                    List<AbstractElement> elements,
                                    CFAEdge cfaEdge, Precision precision) {    
    
    if (!(element instanceof ExplicitAnalysisElement) || element == explicitAnalysisDomain.getBottomElement()) {
      return null;
    }
    ExplicitAnalysisElement explicitElement = (ExplicitAnalysisElement)element;
    
    for (AbstractElement ae : elements) {
      try {
        if (ae instanceof PointerAnalysisElement) {
          return strengthen(explicitElement, (PointerAnalysisElement)ae, cfaEdge, precision);
        }
      } catch (ExplicitAnalysisTransferException e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  private ExplicitAnalysisElement strengthen(ExplicitAnalysisElement explicitElement,
      PointerAnalysisElement pointerElement, CFAEdge cfaEdge, Precision precision) throws ExplicitAnalysisTransferException {
    
    if (missingInformationLeftVariable != null && missingInformationRightPointer != null) {
      
      String rightVar = derefPointerToVariable(pointerElement, missingInformationRightPointer);
      if (rightVar != null) {
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
        return handleAssignmentToVariable(explicitElement, leftVar, missingInformationRightExpression, cfaEdge);
      }
      
      missingInformationLeftPointer = null;
      missingInformationRightExpression = null;
    }
    return null;
  }
      
  private String derefPointerToVariable(PointerAnalysisElement pointerElement,
                                        String pointer) {
    Pointer p = pointerElement.lookupPointer(pointer);
    if (p != null && p.getNumberOfTargets() == 1) {
      Memory.PointerTarget target = p.getFirstTarget();
      if (target instanceof Memory.Variable) {
        return ((Memory.Variable)target).getVarName();
      }
    }
    return null;
  }
}