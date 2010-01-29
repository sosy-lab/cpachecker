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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import cfa.objectmodel.c.AssumeEdge;
import cfa.objectmodel.c.CallToReturnEdge;
import cfa.objectmodel.c.DeclarationEdge;
import cfa.objectmodel.c.FunctionCallEdge;
import cfa.objectmodel.c.FunctionDefinitionNode;
import cfa.objectmodel.c.GlobalDeclarationEdge;
import cfa.objectmodel.c.ReturnEdge;
import cfa.objectmodel.c.StatementEdge;
import cmdline.CPAMain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import cpa.pointeranalysis.Memory;
import cpa.pointeranalysis.Pointer;
import cpa.pointeranalysis.PointerAnalysisElement;
import exceptions.CPATransferException;
import exceptions.UnrecognizedCCodeException;
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
  public Collection<AbstractElement> getAbstractSuccessors(
      AbstractElement element, Precision precision, CFAEdge cfaEdge) throws CPATransferException {
    AbstractElement successor;
    ExplicitAnalysisElement explicitElement = (ExplicitAnalysisElement)element;

    // check the type of the edge
    switch (cfaEdge.getEdgeType ()) {

    // if edge is a statement edge, e.g. a = b + c
    case StatementEdge: {
      StatementEdge statementEdge = (StatementEdge) cfaEdge;

      if (statementEdge.isJumpEdge()) {
        // this statement is a function return, e.g. return (a);
        // note that this is different from return edge
        // this is a statement edge which leads the function to the
        // last node of its CFA, where return edge is from that last node
        // to the return site of the caller function

        successor = handleExitFromFunction(explicitElement, statementEdge.getExpression(), statementEdge);
      } else {
        // this is a regular statement
        successor = handleStatement(explicitElement, statementEdge.getExpression(), cfaEdge);
      }
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

      if (functionCallEdge.isExternalCall()) {
        // call to an external function
        // TODO external function call
        throw new UnrecognizedCCodeException("external function calls not yet supported", functionCallEdge);
//      try {
//      handleExternalFunctionCall(expAnalysisElement, functionCallEdge);
//      } catch (ExplicitAnalysisTransferException e) {
//        CPAMain.logManager.logException(Level.WARNING, e, "");
//      }
      } else {
        successor = handleFunctionCall(explicitElement, functionCallEdge);
      }
      break;
    }

    // this is a return edge from function, this is different from return statement
    // of the function. See case for statement edge for details
    case ReturnEdge: {
      ReturnEdge functionReturnEdge = (ReturnEdge) cfaEdge;
      successor = handleFunctionReturn(explicitElement, functionReturnEdge);
      break;
    }

    default:
      throw new UnrecognizedCFAEdgeException(cfaEdge);
    }
    
    if (successor == null) {
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
   */
  private ExplicitAnalysisElement handleFunctionReturn(ExplicitAnalysisElement element,
      ReturnEdge functionReturnEdge) 
  throws UnrecognizedCCodeException {

    CallToReturnEdge summaryEdge =
      functionReturnEdge.getSuccessor().getEnteringSummaryEdge();
    IASTExpression exprOnSummary = summaryEdge.getExpression();
    // TODO get from stack
    ExplicitAnalysisElement previousElem = summaryEdge.extractAbstractElement(ExplicitAnalysisElement.class);
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

  private ExplicitAnalysisElement handleFunctionCall(ExplicitAnalysisElement element,
      FunctionCallEdge callEdge)  
  throws UnrecognizedCCodeException {

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
  throws UnrecognizedCCodeException {

    return handleAssignmentToVariable(element, "___cpa_temp_result_var_", expression, statementEdge);
  }

  private AbstractElement handleAssumption(ExplicitAnalysisElement element,
                  IASTExpression expression, CFAEdge cfaEdge, boolean truthValue)
                  throws UnrecognizedCCodeException {

    Boolean result = getBooleanExpressionValue(element, expression, cfaEdge, truthValue);
    
    if (result != null) {
      if (result) {
        return element.clone();
      } else {
        // return null for bottom element
        return null;
      }
    } else {
      // don't know
      return element.clone();
    }
  }

  private Boolean getBooleanExpressionValue(ExplicitAnalysisElement element,
                              IASTExpression expression, CFAEdge cfaEdge, boolean truthValue)
                              throws UnrecognizedCCodeException {
    if (expression instanceof IASTUnaryExpression) {
      // [!exp]
      IASTUnaryExpression unaryExp = ((IASTUnaryExpression)expression);
      
      switch (unaryExp.getOperator()) {
      
      case IASTUnaryExpression.op_bracketedPrimary: // [(exp)]
        return getBooleanExpressionValue(element, unaryExp.getOperand(), cfaEdge, truthValue);

      case IASTUnaryExpression.op_not: // [! exp]
        return getBooleanExpressionValue(element, unaryExp.getOperand(), cfaEdge, !truthValue);

      default:
        throw new UnrecognizedCCodeException(cfaEdge, unaryExp);
      }
    
    } else if (expression instanceof IASTIdExpression) {
      // [exp]
      String functionName = cfaEdge.getPredecessor().getFunctionName();
      String varName = getvarName(expression.getRawSignature(), functionName);
      
      if (element.contains(varName)) {
        boolean expressionValue = (element.getValueFor(varName) != 0); // != 0 is true, == 0 is false
        
        return expressionValue == truthValue;
        
      } else {
        return null;
      }
      
    } else if (expression instanceof IASTBinaryExpression) {
      // [exp1 == exp2]
      String functionName = cfaEdge.getPredecessor().getFunctionName();
      IASTBinaryExpression binExp = ((IASTBinaryExpression)expression);
      
      Long val1 = getExpressionValue(element, binExp.getOperand1(), functionName, cfaEdge);
      Long val2 = getExpressionValue(element, binExp.getOperand2(), functionName, cfaEdge);
      
      if (val1 != null && val2 != null) {
        boolean expressionValue;
        
        switch (binExp.getOperator()) {
        case IASTBinaryExpression.op_equals:
          expressionValue = val1.equals(val2);
          break;
          
        case IASTBinaryExpression.op_notequals:
          expressionValue = !val1.equals(val2);
          break;
          
        case IASTBinaryExpression.op_greaterThan:
          expressionValue = val1 > val2;
          break;

        case IASTBinaryExpression.op_greaterEqual:
          expressionValue = val1 >= val2;
          break;

        case IASTBinaryExpression.op_lessThan:
          expressionValue = val1 < val2;
          break;

        case IASTBinaryExpression.op_lessEqual:
          expressionValue = val1 <= val2;
          break;
          
        default:
          throw new UnrecognizedCCodeException(cfaEdge, binExp);
        }
        
        return expressionValue == truthValue;
        
      } else {
        return null;
      }
      
    } else {
      // TODO fields, arrays
      throw new UnrecognizedCCodeException(cfaEdge, expression);
    }
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

  private ExplicitAnalysisElement handleUnaryStatement(ExplicitAnalysisElement element,
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

      ExplicitAnalysisElement newElement = element.clone();
      if(newElement.contains(varName)){
        newElement.assignConstant(varName, newElement.getValueFor(varName) + shift, this.threshold);
      }
      return newElement;
      
    } else {
      throw new UnrecognizedCCodeException(cfaEdge, operand);
    }
  }

  private ExplicitAnalysisElement handleBinaryStatement(ExplicitAnalysisElement element,
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

  private ExplicitAnalysisElement handleOperationAndAssign(ExplicitAnalysisElement element,
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

  private ExplicitAnalysisElement handleAssignment(ExplicitAnalysisElement element,
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

  private ExplicitAnalysisElement handleAssignmentToVariable(ExplicitAnalysisElement element,
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
      ExplicitAnalysisElement newElement = element.clone();
      String lvarName = getvarName(lParam, functionName);
      newElement.forget(lvarName);
      return newElement;
    }
    else{
      throw new UnrecognizedCCodeException(cfaEdge, rightExp);
    }
  }
  
  private ExplicitAnalysisElement handleAssignmentOfCast(ExplicitAnalysisElement element,
                              String lParam, IASTCastExpression castExp, CFAEdge cfaEdge) 
                              throws UnrecognizedCCodeException 
  {
    IASTExpression castOperand = castExp.getOperand();
    return handleAssignmentToVariable(element, lParam, castOperand, cfaEdge);
  }

  private ExplicitAnalysisElement handleAssignmentOfUnaryExp(ExplicitAnalysisElement element,
                                      String lParam, IASTUnaryExpression unaryExp, CFAEdge cfaEdge) 
                                      throws UnrecognizedCCodeException {
    
    String functionName = cfaEdge.getPredecessor().getFunctionName();
    // name of the updated variable, so if a = -b is handled, lParam is a
    String assignedVar = getvarName(lParam, functionName);
    ExplicitAnalysisElement newElement = element.clone();
    
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
      
    } else {
      // a = -b or similar
      Long value = getExpressionValue(element, unaryOperand, functionName, cfaEdge);
      
      if (value != null) {
        newElement.assignConstant(assignedVar, value, this.threshold);
      } else {
        newElement.forget(assignedVar);
      }
    }

    return newElement;
  }

  private ExplicitAnalysisElement handleAssignmentOfBinaryExp(ExplicitAnalysisElement element,
                       String lParam, IASTExpression lVarInBinaryExp, IASTExpression rVarInBinaryExp,
                       int binaryOperator, CFAEdge cfaEdge)
                       throws UnrecognizedCCodeException {

    String functionName = cfaEdge.getPredecessor().getFunctionName();
    // name of the updated variable, so if a = b + c is handled, lParam is a
    String assignedVar = getvarName(lParam, functionName);
    ExplicitAnalysisElement newElement = element.clone();
    
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
    }
    return newElement;
  }

  private Long getExpressionValue(ExplicitAnalysisElement element, IASTExpression expression,
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
                        throws UnrecognizedCCodeException
  {
    ExplicitAnalysisElement newElement = element.clone();

    // op2 may be null if this is a "return;" statement
    Long val = (op2 == null ? 0 : parseLiteral(op2));

    String assignedVar = getvarName(lParam, functionName);
    if (val != null) {
      newElement.assignConstant(assignedVar, val, this.threshold);
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
        if(s.endsWith("L") || s.endsWith("U")){
          s = s.replace("L", "");
          s = s.replace("U", "");
        }
        try {
          return Long.valueOf(s);
        } catch (NumberFormatException e) {
          throw new UnrecognizedCCodeException("invalid integer literal", null, expression);
        }
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
                                    CFAEdge cfaEdge, Precision precision) throws UnrecognizedCCodeException {    

    assert element instanceof ExplicitAnalysisElement;
    ExplicitAnalysisElement explicitElement = (ExplicitAnalysisElement)element;
    
    for (AbstractElement ae : elements) {
      if (ae instanceof PointerAnalysisElement) {
        return strengthen(explicitElement, (PointerAnalysisElement)ae, cfaEdge, precision);
      }
    }
    return null;
  }

  private Collection<? extends AbstractElement> strengthen(ExplicitAnalysisElement explicitElement,
      PointerAnalysisElement pointerElement, CFAEdge cfaEdge, Precision precision) throws UnrecognizedCCodeException {
    
    List<ExplicitAnalysisElement> retList = new ArrayList<ExplicitAnalysisElement>();
    
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
      
  private String derefPointerToVariable(PointerAnalysisElement pointerElement,
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