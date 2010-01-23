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
package cpa.concrete;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTInitializerExpression;
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
import cpa.mustmay.MustMayAnalysisElement;
import exceptions.CPATransferException;
import exceptions.ExplicitAnalysisTransferException;

public class ConcreteAnalysisTransferRelation implements TransferRelation {

  private ConcreteAnalysisDomain mDomain;

  private Set<String> globalVars;

  public ConcreteAnalysisTransferRelation(ConcreteAnalysisDomain pDomain)
  {
    this.mDomain = pDomain;
    
    globalVars = new HashSet<String>();
  }

  @Override
  public Collection<? extends AbstractElement> getAbstractSuccessors(
      AbstractElement pElement, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException {
    ConcreteAnalysisDomainElement lSuccessor = getAbstractSuccessor((ConcreteAnalysisDomainElement)pElement, pCfaEdge, pPrecision);
    
    if (lSuccessor.equals(this.mDomain.getBottomElement())) {
      return Collections.emptySet();
    }
    else {
      return Collections.singleton(lSuccessor);      
    }
  }
  
  public ConcreteAnalysisDomainElement getAbstractSuccessor(
      ConcreteAnalysisDomainElement pCurrentElement, CFAEdge pCfaEdge,
      Precision pPrecision) throws CPATransferException {
    
    assert(pCurrentElement != null);
    assert(pCfaEdge != null);
    assert(pPrecision != null);
    
    if (pCurrentElement.equals(mDomain.getBottomElement())) {
      return mDomain.getBottomElement();
    }
    
    if (pCurrentElement.equals(mDomain.getTopElement())) {
      throw new UnsupportedOperationException("Top element is not allowed to occur during analysis!");
    }
    
    assert(pCurrentElement instanceof ConcreteAnalysisElement);
    
    ConcreteAnalysisElement lCurrentElement = (ConcreteAnalysisElement)pCurrentElement;
    
    
    // check the type of the edge
    switch (pCfaEdge.getEdgeType ())
    {
    
    case StatementEdge:
    {
      // edge is a statement edge, e.g. a = b + c
      
      StatementEdge lStatementEdge = (StatementEdge) pCfaEdge;
      IASTExpression lExpression = lStatementEdge.getExpression ();

      if(lStatementEdge.isJumpEdge())
      {
        // this statement is a function return, e.g. return (a);
        // note that this is different from return edge
        // this is a statement edge which leads the function to the
        // last node of its CFA, where return edge is from that last node
        // to the return site of the caller function

        if (lExpression != null) {
          // return (a);
          // modeled by an assignment to unique global variable
          
          return handleAssignmentToVariable(lCurrentElement, "___cpa_temp_result_var_", lExpression, lStatementEdge);
        }
        else {
          return new ConcreteAnalysisElement(lCurrentElement);
        }
      }
      else{
        // this is a regular statement
      
        return handleStatement(lCurrentElement, lExpression, pCfaEdge);
      }
    }

    case DeclarationEdge:
    {
      // edge is a declaration edge, e.g. int a;
    
      return handleDeclaration(lCurrentElement, (DeclarationEdge)pCfaEdge);
    }

    case AssumeEdge:
    {
      // this is an assumption, e.g. if(a == b)
    
      AssumeEdge assumeEdge = (AssumeEdge) pCfaEdge;
      IASTExpression expression = assumeEdge.getExpression();
      
      return handleAssumption(lCurrentElement, expression, pCfaEdge, assumeEdge.getTruthAssumption());
    }

    case BlankEdge:
    {
      return new ConcreteAnalysisElement(lCurrentElement);
    }

    case FunctionCallEdge:
    {
      FunctionCallEdge lFunctionCallEdge = (FunctionCallEdge) pCfaEdge;

      if(lFunctionCallEdge.isExternalCall())
      {
        // call to an external function
        // TODO external function call
        
        // TODO Are there external function calls in the statement case, too?

        CPAMain.logManager.log(Level.ALL, "Call to an external function not implemented!");
        
        return mDomain.getBottomElement();
      }
      else{
        return handleFunctionCall(lCurrentElement, lFunctionCallEdge);
      }
    }

    case ReturnEdge:
    {
      // this is a return edge from function, this is different from return statement
      // of the function. See case for statement edge for details
    
      return handleFunctionReturn(lCurrentElement, (ReturnEdge)pCfaEdge);
    }

    case CallToReturnEdge:
      // TODO: Summary edge, we handle this on function return, do nothing
    case MultiStatementEdge:
    case MultiDeclarationEdge:
    default:
      CPAMain.logManager.log(Level.ALL, "Unimplemented edge type: " + pCfaEdge.toString());
      
      // This transfer relation violates the overapproximation condition
      // of a CPA. This CPA applies underapproximation.
      return mDomain.getBottomElement();
    }
  }

  /**
   * Handles return from one function to another function.
   * @param pCurrentElement previous abstract element.
   * @param pFunctionReturnEdge return edge from a function to its call site.
   * @return new abstract element.
   */
  private ConcreteAnalysisDomainElement handleFunctionReturn(ConcreteAnalysisElement pCurrentElement,
      ReturnEdge pFunctionReturnEdge) {
    
    CallToReturnEdge lSummaryEdge = 
      pFunctionReturnEdge.getSuccessor().getEnteringSummaryEdge();
    IASTExpression exprOnSummary = lSummaryEdge.getExpression();
    
    // TODO get from stack
    // retrieve summaryEdge or predecessor from stack ???
    // TODO Post question on developer forum
    
    // CAUTION: This is not possible because of nested access!!!
    // TODO: query for ConcreteAnalysisDomainElement?
    ConcreteAnalysisElement lPreviousElement = lSummaryEdge.extractAbstractElement(ConcreteAnalysisElement.class);
        
    ConcreteAnalysisElement newElement = new ConcreteAnalysisElement(lPreviousElement);

    String callerFunctionName = pFunctionReturnEdge.getSuccessor().getFunctionName();
    String calledFunctionName = pFunctionReturnEdge.getPredecessor().getFunctionName();

    if (exprOnSummary instanceof IASTBinaryExpression) {
      //expression is a binary operation, e.g. a = g(b);
    
      IASTBinaryExpression binExp = ((IASTBinaryExpression)exprOnSummary);
      int opType = binExp.getOperator ();
      IASTExpression op1 = binExp.getOperand1();

      assert(opType == IASTBinaryExpression.op_assign);

      //we expect left hand side of the expression to be a variable
      if (op1 instanceof IASTIdExpression || op1 instanceof IASTFieldReference) {
        String varName = op1.getRawSignature();
        String returnVarName = calledFunctionName + "::" + "___cpa_temp_result_var_";

        assert(pCurrentElement.contains(returnVarName));
        
        for (String globalVar : globalVars) {
          if (globalVar.equals(varName)) {
            newElement.assignConstant(varName, pCurrentElement.getValueFor(returnVarName));
          }
          else {
            if (pCurrentElement.contains(globalVar)) {
              // global variable may be changed in called function and
              // therefore has to be set with value from pCurrentElement
              
              newElement.assignConstant(globalVar, pCurrentElement.getValueFor(globalVar));
            }
          }
        }

        if (!globalVars.contains(varName)) {
          // varName is a local variable
          
          String assignedVarName = getvarName(varName, callerFunctionName);
          
          newElement.assignConstant(assignedVarName, pCurrentElement.getValueFor(returnVarName));
        }
        
        return newElement;
      }
      else{
        CPAMain.logManager.logException(Level.ALL, new ExplicitAnalysisTransferException("Unhandled case " + pFunctionReturnEdge.getPredecessor().getNodeNumber()), "");
        
        return mDomain.getBottomElement();
      }
    }
    else if (exprOnSummary instanceof IASTUnaryExpression)
    {
      // TODO this is not called -- expression is a unary operation, e.g. g(b);
      
      // only globals
      for (String globalVar : globalVars) {
        if (pCurrentElement.contains(globalVar)) {
          newElement.assignConstant(globalVar, pCurrentElement.getValueFor(globalVar));
        }
      }
      
      return newElement;
    }
    else if (exprOnSummary instanceof IASTFunctionCallExpression)
    {
      // g(b)

      // only globals
      for(String globalVar : globalVars) {
        if(pCurrentElement.contains(globalVar)){
          newElement.assignConstant(globalVar, pCurrentElement.getValueFor(globalVar));
        }
      }
      
      return newElement;
    }
    else{
      CPAMain.logManager.logException(Level.ALL, new ExplicitAnalysisTransferException("Unhandled case - return from function" + pFunctionReturnEdge.getPredecessor().getNodeNumber()), "");
      
      return mDomain.getBottomElement();
    }
  }

  private ConcreteAnalysisDomainElement handleFunctionCall(ConcreteAnalysisElement pCurrentElement,
      FunctionCallEdge callEdge) {

    FunctionDefinitionNode functionEntryNode = (FunctionDefinitionNode)callEdge.getSuccessor();
    String calledFunctionName = functionEntryNode.getFunctionName();
    String callerFunctionName = callEdge.getPredecessor().getFunctionName();

    List<String> paramNames = functionEntryNode.getFunctionParameterNames();
    IASTExpression[] arguments = callEdge.getArguments();

    if (arguments == null) {
      arguments = new IASTExpression[0];
    }

    assert (paramNames.size() == arguments.length);

    ConcreteAnalysisElement newElement = new ConcreteAnalysisElement();

    for (String globalVar : globalVars) {
      if (pCurrentElement.contains(globalVar)) {
        newElement.assignConstant(globalVar, pCurrentElement.getValueFor(globalVar));
      }
    }

    for (int i=0; i<arguments.length; i++) {
      IASTExpression arg = arguments[i];
      
      if (arg instanceof IASTCastExpression) {
        // ignore casts
        //arg = ((IASTCastExpression)arg).getOperand();
        
        CPAMain.logManager.log(Level.ALL, "Unhandled cast operation: " + arg.toString());
        
        return mDomain.getBottomElement();
      }
      
      String nameOfParam = paramNames.get(i);
      String formalParamName = getvarName(nameOfParam, calledFunctionName);
      
      if (arg instanceof IASTIdExpression) {
        IASTIdExpression idExp = (IASTIdExpression) arg;
        String nameOfArg = idExp.getRawSignature();
        String actualParamName = getvarName(nameOfArg, callerFunctionName);

        if (pCurrentElement.contains(actualParamName)) {
          newElement.assignConstant(formalParamName, pCurrentElement.getValueFor(actualParamName));
        }
      }
      else if (arg instanceof IASTLiteralExpression) {
        Long val = parseLiteral(arg);
        
        if (val != null) {
          newElement.assignConstant(formalParamName, val);
        } else {
          CPAMain.logManager.log(Level.ALL, "Problem while literal parsing: " + arg.toString());
          
          return mDomain.getBottomElement();
        }
      }
      else if (arg instanceof IASTTypeIdExpression) {
        CPAMain.logManager.log(Level.ALL, "Unhandled case: " + arg.toString());
        
        return mDomain.getBottomElement();
      }
      else if (arg instanceof IASTUnaryExpression) {
        IASTUnaryExpression unaryExp = (IASTUnaryExpression) arg;
        assert(unaryExp.getOperator() == IASTUnaryExpression.op_star || unaryExp.getOperator() == IASTUnaryExpression.op_amper);
        
        // ???
        
        CPAMain.logManager.log(Level.ALL, "Unhandled case: " + arg.toString());
        
        return mDomain.getBottomElement();
      }
      else if (arg instanceof IASTFunctionCallExpression) {
        assert(false);
      
        CPAMain.logManager.log(Level.ALL, "Unhandled case: " + arg.toString());
        
        return mDomain.getBottomElement();
      }
      else if (arg instanceof IASTFieldReference) {
        CPAMain.logManager.log(Level.ALL, "Unhandled case: " + arg.toString());
        
        return mDomain.getBottomElement();
      }
      else {
        CPAMain.logManager.log(Level.ALL, "Unhandled case: " + arg.toString());
        
        return mDomain.getBottomElement();
      }
    }

    return newElement;
  }

  private ConcreteAnalysisDomainElement handleAssumption(ConcreteAnalysisElement pCurrentElement,
                  IASTExpression expression, CFAEdge cfaEdge, boolean truthValue) {

    Boolean result = getBooleanExpressionValue(pCurrentElement, expression, cfaEdge, truthValue);
    
    if (result != null && result) {
      return new ConcreteAnalysisElement(pCurrentElement);
    }
    
    // result is false or don't know
    return mDomain.getBottomElement();
  }

  private Boolean getBooleanExpressionValue(ConcreteAnalysisElement element,
                              IASTExpression expression, CFAEdge cfaEdge, boolean truthValue) {
    
    if (expression instanceof IASTUnaryExpression) {
      // [!exp]
      IASTUnaryExpression unaryExp = ((IASTUnaryExpression)expression);
      
      switch (unaryExp.getOperator()) {
      
      case IASTUnaryExpression.op_bracketedPrimary: // [(exp)]
        return getBooleanExpressionValue(element, unaryExp.getOperand(), cfaEdge, truthValue);

      case IASTUnaryExpression.op_not: // [! exp]
        return getBooleanExpressionValue(element, unaryExp.getOperand(), cfaEdge, !truthValue);

      default:
        CPAMain.logManager.logException(Level.ALL, new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement()), "");
        
        return null;
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
          CPAMain.logManager.logException(Level.ALL, new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement()), "");
          
          return null;
        }
        
        return expressionValue == truthValue;
        
      } else {
        return null;
      }
      
    } else {
      // TODO fields, arrays
      CPAMain.logManager.logException(Level.ALL, new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement()), "");
      
      return null;
    }
  }
  
  private ConcreteAnalysisDomainElement handleDeclaration(ConcreteAnalysisElement pCurrentElement,
      DeclarationEdge declarationEdge) {

    ConcreteAnalysisElement lNewElement = new ConcreteAnalysisElement(pCurrentElement);
    
    IASTDeclarator[] declarators = declarationEdge.getDeclarators();
    // IASTDeclSpecifier specifier = declarationEdge.getDeclSpecifier();

    for (IASTDeclarator declarator : declarators)
    {
      if (declarator != null)
      {
        // get the variable name in the declarator
        String varName = declarator.getName().toString();

        // TODO check other types of variables later - just handle primitive
        // types for the moment
        // get pointer operators of the declaration
        IASTPointerOperator[] pointerOps = declarator.getPointerOperators();
        // don't add pointer variables to the list since we don't track them
        if (pointerOps.length > 0) {
          //continue;
          
          CPAMain.logManager.log(Level.ALL, "Unhandled case of pointer variables: " + declarator.toString());
          
          return mDomain.getBottomElement();
        }
        
        // if this is a global variable, add to the list of global variables
        if(declarationEdge instanceof GlobalDeclarationEdge)
        {
          globalVars.add(varName);
          
          // cilly might initialize global variables 
          
          IASTInitializer lInitializer = declarator.getInitializer();
          
          if (lInitializer != null) {
            assert(lInitializer instanceof IASTInitializerExpression);
            
            IASTInitializerExpression lInitializerExpression = (IASTInitializerExpression)lInitializer;
            
            ConcreteAnalysisDomainElement lTmpElement = handleAssignmentToVariable(lNewElement, varName, lInitializerExpression.getExpression(), declarationEdge);
            
            if (lTmpElement.equals(mDomain.getBottomElement())) {
              return lTmpElement;
            }
            else {
              assert(lTmpElement instanceof ConcreteAnalysisElement);
              
              lNewElement = (ConcreteAnalysisElement)lTmpElement;
            }
          }
        }
      }
    }
    
    return lNewElement;
  }

  private ConcreteAnalysisDomainElement handleStatement(ConcreteAnalysisElement element,
      IASTExpression expression, CFAEdge cfaEdge) {

    if (expression instanceof IASTBinaryExpression) {
      // expression is a binary operation, e.g. a = b;

      return handleBinaryStatement(element, expression, cfaEdge);
    }
    else if (expression instanceof IASTUnaryExpression)
    {
      // expression is a unary operation, e.g. a++;

      return handleUnaryStatement(element, expression, cfaEdge);
    }
    else if(expression instanceof IASTFunctionCallExpression){
      // external function call

      // TODO ???
      // do nothing
      //return element.clone();
      
      CPAMain.logManager.log(Level.ALL, "Unhandled case: " + expression.toString());
      
      return mDomain.getBottomElement();
    }
    else if(expression instanceof IASTIdExpression){
      // there is such a case
      
      // TODO ???
      
      //return element.clone();
      
      CPAMain.logManager.log(Level.ALL, "Unhandled case: " + expression.toString());
      
      return mDomain.getBottomElement();
    }
    else{
      CPAMain.logManager.logException(Level.ALL, new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement()), "");
      
      return mDomain.getBottomElement();
    }
  }

  private ConcreteAnalysisDomainElement handleUnaryStatement(ConcreteAnalysisElement element,
      IASTExpression expression, CFAEdge cfaEdge) {

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
      CPAMain.logManager.logException(Level.ALL, new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement()), "");
      
      return mDomain.getBottomElement();
    }
    
    IASTExpression operand = unaryExpression.getOperand();
    if (operand instanceof IASTIdExpression) {
      String functionName = cfaEdge.getPredecessor().getFunctionName();
      String varName = getvarName(operand.getRawSignature(), functionName); 

      ConcreteAnalysisElement newElement = new ConcreteAnalysisElement(element);
      
      assert(newElement.contains(varName));
     
      newElement.assignConstant(varName, newElement.getValueFor(varName) + shift);
      
      return newElement;
      
    } else {
      CPAMain.logManager.logException(Level.ALL, new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement()), "");
      
      return mDomain.getBottomElement();
    }
  }

  private ConcreteAnalysisDomainElement handleBinaryStatement(ConcreteAnalysisElement element,
      IASTExpression expression, CFAEdge cfaEdge) {
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
      CPAMain.logManager.logException(Level.ALL, new ExplicitAnalysisTransferException("Unhandled case " + binaryExpression.getRawSignature()), "");
      
      return mDomain.getBottomElement();
    }
  }

  private ConcreteAnalysisDomainElement handleOperationAndAssign(ConcreteAnalysisElement element,
                                      IASTBinaryExpression binaryExpression, CFAEdge cfaEdge) {
    
    IASTExpression leftOp = binaryExpression.getOperand1();
    IASTExpression rightOp = binaryExpression.getOperand2();
    int operator = binaryExpression.getOperator();

    if (!(leftOp instanceof IASTIdExpression)) {
      // TODO handle fields, arrays
      
      CPAMain.logManager.logException(Level.ALL, new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement()), "");

      return mDomain.getBottomElement();
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
      CPAMain.logManager.logException(Level.ALL, new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement()), "");
      
      return mDomain.getBottomElement();
    }
    
    return handleAssignmentOfBinaryExp(element, leftOp.getRawSignature(), leftOp,
                                                  rightOp, newOperator, cfaEdge);
  }

  private ConcreteAnalysisDomainElement handleAssignment(ConcreteAnalysisElement element,
                            IASTBinaryExpression binaryExpression, CFAEdge cfaEdge) {
    
    IASTExpression op1 = binaryExpression.getOperand1();
    IASTExpression op2 = binaryExpression.getOperand2();

    if(op1 instanceof IASTIdExpression) {
      // a = ...
      return handleAssignmentToVariable(element, op1.getRawSignature(), op2, cfaEdge);
    
    } else if (op1 instanceof IASTUnaryExpression
        && ((IASTUnaryExpression)op1).getOperator() == IASTUnaryExpression.op_star) {
      // *a = ...
      
      CPAMain.logManager.logException(Level.ALL, new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement()), "");
      
      return mDomain.getBottomElement();
      
      /*op1 = ((IASTUnaryExpression)op1).getOperand();
      
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
        throw new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement());
      }
      return element.clone();*/
    
    } /*else if (op1 instanceof IASTFieldReference) {
      // TODO assignment to field
      return element.clone();
    
    } else if (op1 instanceof IASTArraySubscriptExpression) {
      // TODO assignment to array cell
      return element.clone();
    
    }*/ else {
      CPAMain.logManager.logException(Level.ALL, new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement()), "");
      
      return mDomain.getBottomElement();
    }
  }

  private ConcreteAnalysisDomainElement handleAssignmentToVariable(ConcreteAnalysisElement element,
                          String lParam, IASTExpression rightExp, CFAEdge cfaEdge) {
    assert(rightExp != null);
    
    String functionName = cfaEdge.getPredecessor().getFunctionName();
    
    if (rightExp instanceof IASTLiteralExpression) {
      // a = 8.2
      
      return handleAssignmentOfLiteral(element, lParam, rightExp, functionName);
    }
    else if (rightExp instanceof IASTIdExpression) {
      // a = b
      
      return handleAssignmentOfVariable(element, lParam, rightExp, functionName);
    }
    else if(rightExp instanceof IASTCastExpression) {
      // a = (cast) ?
      
      return handleAssignmentOfCast(element, lParam, (IASTCastExpression)rightExp, cfaEdge);
    }
    else if(rightExp instanceof IASTUnaryExpression) {
      // a = -b
      
      return handleAssignmentOfUnaryExp(element, lParam, (IASTUnaryExpression)rightExp, cfaEdge);
    }
    else if(rightExp instanceof IASTBinaryExpression) {
      // a = b op c
      
      IASTBinaryExpression binExp = (IASTBinaryExpression)rightExp;
      
      return handleAssignmentOfBinaryExp(element, lParam, binExp.getOperand1(),
                            binExp.getOperand2(), binExp.getOperator(), cfaEdge);
    }
    // a = extCall();  or  a = b->c;
    /*else if(rightExp instanceof IASTFunctionCallExpression
         || rightExp instanceof IASTFieldReference){
      ConcreteAnalysisElement newElement = element.clone();
      String lvarName = getvarName(lParam, functionName);
      newElement.forget(lvarName);
      return newElement;
    }*/
    else{
      CPAMain.logManager.logException(Level.ALL, new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement()), "");
      
      return mDomain.getBottomElement();
    }
  }
  
  private ConcreteAnalysisDomainElement handleAssignmentOfCast(ConcreteAnalysisElement element,
                              String lParam, IASTCastExpression castExp, CFAEdge cfaEdge) {
    
    IASTExpression castOperand = castExp.getOperand();
    return handleAssignmentToVariable(element, lParam, castOperand, cfaEdge);
  }

  private ConcreteAnalysisDomainElement handleAssignmentOfUnaryExp(ConcreteAnalysisElement pCurrentElement,
    String lParam, IASTUnaryExpression unaryExp, CFAEdge cfaEdge) {
    
    String functionName = cfaEdge.getPredecessor().getFunctionName();
    // name of the updated variable, so if a = -b is handled, lParam is a
    String assignedVar = getvarName(lParam, functionName);
    ConcreteAnalysisElement newElement = new ConcreteAnalysisElement(pCurrentElement);
    
    IASTExpression unaryOperand = unaryExp.getOperand();
    int unaryOperator = unaryExp.getOperator();
    
    if (unaryOperator == IASTUnaryExpression.op_star) {
      // a = * b
      /*newElement.forget(assignedVar);
      
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
      }*/
      
      CPAMain.logManager.log(Level.ALL, "Unhandeled case: " + cfaEdge.getRawStatement());
      
      return mDomain.getBottomElement();
    
    } else if (unaryOperator == IASTUnaryExpression.op_bracketedPrimary) {
      // a = (b + c)
      return handleAssignmentToVariable(pCurrentElement, lParam, unaryOperand, cfaEdge);
      
    } else {
      // a = -b or similar
      Long value = getExpressionValue(pCurrentElement, unaryOperand, functionName, cfaEdge);
      
      if (value != null) {
        newElement.assignConstant(assignedVar, value);
      } else {
        CPAMain.logManager.log(Level.ALL, "Problems while determining value of expression: " + unaryOperand.toString());
        
        return mDomain.getBottomElement();
      }
    }

    return newElement;
  }

  private ConcreteAnalysisDomainElement handleAssignmentOfBinaryExp(ConcreteAnalysisElement element,
                       String lParam, IASTExpression lVarInBinaryExp, IASTExpression rVarInBinaryExp,
                       int binaryOperator, CFAEdge cfaEdge) {

    String functionName = cfaEdge.getPredecessor().getFunctionName();
    // name of the updated variable, so if a = b + c is handled, lParam is a
    String assignedVar = getvarName(lParam, functionName);
    ConcreteAnalysisElement newElement = new ConcreteAnalysisElement(element);
    
    switch (binaryOperator) {
    case IASTBinaryExpression.op_divide:
    case IASTBinaryExpression.op_modulo:
    case IASTBinaryExpression.op_lessEqual:
    case IASTBinaryExpression.op_greaterEqual:
    case IASTBinaryExpression.op_binaryAnd:
    case IASTBinaryExpression.op_binaryOr:
      // TODO check which cases can be handled (I think all)
      CPAMain.logManager.log(Level.ALL, "Unhandled case: " + binaryOperator);
      
      return mDomain.getBottomElement();
    
    case IASTBinaryExpression.op_plus:
    case IASTBinaryExpression.op_minus:
    case IASTBinaryExpression.op_multiply:
  
      Long val1;
      Long val2;
      
      if(lVarInBinaryExp instanceof IASTUnaryExpression
          && ((IASTUnaryExpression)lVarInBinaryExp).getOperator() == IASTUnaryExpression.op_star) {
        // a = *b + c
        // TODO prepare for using strengthen operator to dereference pointer
        // val1 = null;
        
        CPAMain.logManager.log(Level.ALL, "Dereferencing not supported!");
        
        return mDomain.getBottomElement();
      } else {
        
        val1 = getExpressionValue(element, lVarInBinaryExp, functionName, cfaEdge);
      }  
  
      if (val1 != null) {
        val2 = getExpressionValue(element, rVarInBinaryExp, functionName, cfaEdge);
      } else {
        CPAMain.logManager.log(Level.ALL, "Problem with determining value of val1");
        
        return mDomain.getBottomElement();
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
          CPAMain.logManager.log(Level.ALL, "Coding error, missing case in inner switch statement!");
          
          return mDomain.getBottomElement();
        }
        
        newElement.assignConstant(assignedVar, value);
      } else {
        CPAMain.logManager.log(Level.ALL, "Problem with determining value of val2");
        
        return mDomain.getBottomElement();
      }
    }
    return newElement;
  }

  private Long getExpressionValue(ConcreteAnalysisElement element, IASTExpression expression,
                                  String functionName, CFAEdge cfaEdge) {

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
        return null; // valid expression, but it's a pointer value

      default:
        CPAMain.logManager.logException(Level.ALL, new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement()), "");
        
        return null;
      }
    } else {
      // TODO fields, arrays
      
      CPAMain.logManager.logException(Level.ALL, new ExplicitAnalysisTransferException("Unhandled case " + cfaEdge.getRawStatement()), "");
      
      return null;
    }
  }
  
  private ConcreteAnalysisDomainElement handleAssignmentOfVariable(ConcreteAnalysisElement pCurrentElement,
      String lParam, IASTExpression op2, String functionName) {
    
    String rParam = op2.getRawSignature();

    String leftVarName = getvarName(lParam, functionName);
    String rightVarName = getvarName(rParam, functionName);

    ConcreteAnalysisElement newElement = new ConcreteAnalysisElement(pCurrentElement);
    
    assert(newElement.contains(rightVarName));
    
    newElement.assignConstant(leftVarName, newElement.getValueFor(rightVarName));
    
    return newElement;
  }

  private ConcreteAnalysisDomainElement handleAssignmentOfLiteral(ConcreteAnalysisElement pCurrentElement,
                        String lParam, IASTExpression op2, String functionName) {
    
    ConcreteAnalysisElement newElement = new ConcreteAnalysisElement(pCurrentElement);

    assert(op2 != null);
    
    Long val = parseLiteral(op2);
    
    String assignedVar = getvarName(lParam, functionName);
    
    if (val != null) {
      newElement.assignConstant(assignedVar, val);
    } else {
      CPAMain.logManager.log(Level.ALL, "Problem with determining value of " + op2.toString());
      
      return mDomain.getBottomElement();
    }
    
    return newElement;
  }

  private Long parseLiteral(IASTExpression expression) {
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
          CPAMain.logManager.logException(Level.ALL, e, "");
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
                                    CFAEdge cfaEdge, Precision precision) {    
    
    return null;
  }

}