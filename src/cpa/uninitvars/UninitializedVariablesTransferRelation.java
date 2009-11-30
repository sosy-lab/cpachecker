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
package cpa.uninitvars;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpressionList;
import org.eclipse.cdt.core.dom.ast.IASTFieldReference;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTInitializer;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;
import org.eclipse.cdt.core.dom.ast.IASTTypeIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTUnaryExpression;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.c.AssumeEdge;
import cfa.objectmodel.c.CallToReturnEdge;
import cfa.objectmodel.c.DeclarationEdge;
import cfa.objectmodel.c.GlobalDeclarationEdge;
import cfa.objectmodel.c.ReturnEdge;
import cfa.objectmodel.c.StatementEdge;
import cmdline.CPAMain;

import common.Pair;

import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.AbstractElementWithLocation;
import cpa.common.interfaces.Precision;
import cpa.common.interfaces.TransferRelation;
import exceptions.CPAException;
import exceptions.CPATransferException;
import exceptions.TransferRelationException;
import exceptions.UnrecognizedCFAEdgeException;

/**
 * @author Philipp Wendler
 */
public class UninitializedVariablesTransferRelation implements TransferRelation {

  private Set<String> globalVars; // set of all global variable names
  private boolean uninitializedFunctionReturn = false;
  
  private boolean printWarnings;
  private Set<Pair<Integer, String>> warnings;

  public UninitializedVariablesTransferRelation() {
    globalVars = new HashSet<String>();
    printWarnings = Boolean.parseBoolean(CPAMain.cpaConfig.getProperty("uninitVars.printWarnings", "false"));
    if (printWarnings) {
      warnings = new HashSet<Pair<Integer, String>>();
    }
  }
  
  @Override
  public AbstractElement getAbstractSuccessor(AbstractElement element,
                                              CFAEdge cfaEdge,
                                              Precision precision)
                                              throws CPATransferException {
    
    UninitializedVariablesElement successor = ((UninitializedVariablesElement)element).clone();
    
    switch (cfaEdge.getEdgeType()) {
    
    case DeclarationEdge:
      handleDeclaration(successor, (DeclarationEdge)cfaEdge);
      break;
      
    case StatementEdge:
      try {
        handleStatement(successor, ((StatementEdge)cfaEdge).getExpression(), cfaEdge);
      } catch (TransferRelationException e) {
        CPAMain.logManager.logException(Level.WARNING, e, "");
      }
      break;
    
    case ReturnEdge:
      successor.returnFromFunction(); // throw away local context
      
      // now handle the complete a = func(x) statement in the CallToReturnEdge
      ReturnEdge returnEdge = (ReturnEdge)cfaEdge;
      CallToReturnEdge ctrEdge = returnEdge.getSuccessor().getEnteringSummaryEdge();
      try {
        handleStatement(successor, ctrEdge.getExpression(), ctrEdge);
      } catch (TransferRelationException e) {
        CPAMain.logManager.logException(Level.WARNING, e, "");
      }
      break;
      
    case AssumeEdge:
      // just check if there are uninitialized variable usages
      if (printWarnings) {
        try {
          isExpressionUninitialized(successor, ((AssumeEdge)cfaEdge).getExpression(), cfaEdge);
        } catch (TransferRelationException e) {
          CPAMain.logManager.logException(Level.WARNING, e, "");
        }
      }
      break;
      
    case FunctionCallEdge:
      successor.callFunction(cfaEdge.getRawStatement());
      break;
      
    case BlankEdge:
      break;
      
    case CallToReturnEdge:
      // summary edge, handled from ReturnEdge
    case MultiStatementEdge:
    case MultiDeclarationEdge:
      assert false;
      break;
    
    default:
      try {
        throw new UnrecognizedCFAEdgeException("Unknown edge type");
      } catch (UnrecognizedCFAEdgeException e) {
        CPAMain.logManager.logException(Level.WARNING, e, "");
      }
    }
    
    return successor;
  }
  
  private void addWarning(CFAEdge edge, String variable) {
    if (printWarnings) {
      Integer lineNumber = edge.getSuccessor().getLineNumber();
      
      Pair<Integer, String> warningIndex = new Pair<Integer, String>(lineNumber, variable);
      if (!warnings.contains(warningIndex)) {
        warnings.add(warningIndex);
        System.out.println("uninitialized variable " + variable + " used in line "
            + lineNumber + ": " + edge.getRawStatement());
      }
    }
  }
  
  private void setUninitialized(UninitializedVariablesElement element, String varName) {
    if (globalVars.contains(varName)) {
      element.addGlobalVariable(varName);
    } else {
      element.addLocalVariable(varName);
    }
  }
  
  private void setInitialized(UninitializedVariablesElement element, String varName) {
    if (globalVars.contains(varName)) {
      element.removeGlobalVariable(varName);
    } else {
      element.removeLocalVariable(varName);
    }
  }
  
  private void handleDeclaration(UninitializedVariablesElement element,
                                 DeclarationEdge declaration) {
        
    for (IASTDeclarator declarator : declaration.getDeclarators()) {
      if (declarator != null) {

        // get the variable name in the declarator
        String varName = declarator.getName().toString();
        if (declaration instanceof GlobalDeclarationEdge) {
          globalVars.add(varName);
        }

        IASTInitializer initializer = declarator.getInitializer();
        // initializers in CIL are always constant, so no need to check if
        // initializer expression contains uninitialized variables
        if (initializer == null) {
          setUninitialized(element, varName);  
        }          
      }
    }
  }
  
  private void handleStatement(UninitializedVariablesElement element,
                               IASTExpression expression, CFAEdge cfaEdge)
                               throws TransferRelationException {
    
    if (cfaEdge.isJumpEdge()) {
      // this is the return-statement of a function
      uninitializedFunctionReturn = isExpressionUninitialized(element, expression, cfaEdge);
    
    } else if ((expression instanceof IASTUnaryExpression)
            || (expression instanceof IASTFunctionCallExpression)) {
      // this is either an unary operation (a++) or a mere function call (func(a))
      // all of these do not change the initialization status of variables

      // just check if there are uninitialized variable usages
      if (printWarnings) {
        isExpressionUninitialized(element, expression, cfaEdge);
      }
    
    } else if (expression instanceof IASTBinaryExpression) {
      // expression is a binary operation, e.g. a = b or a += b;
      
      IASTBinaryExpression binExpression = (IASTBinaryExpression)expression;
      
      int typeOfOperator = binExpression.getOperator();
      if (typeOfOperator == IASTBinaryExpression.op_assign) {
        // a = b
        handleAssign(element, binExpression, cfaEdge);
        
      } else if (
             typeOfOperator == IASTBinaryExpression.op_binaryAndAssign
          || typeOfOperator == IASTBinaryExpression.op_binaryOrAssign
          || typeOfOperator == IASTBinaryExpression.op_binaryXorAssign
          || typeOfOperator == IASTBinaryExpression.op_divideAssign
          || typeOfOperator == IASTBinaryExpression.op_minusAssign
          || typeOfOperator == IASTBinaryExpression.op_moduloAssign
          || typeOfOperator == IASTBinaryExpression.op_multiplyAssign
          || typeOfOperator == IASTBinaryExpression.op_plusAssign
          || typeOfOperator == IASTBinaryExpression.op_shiftLeftAssign
          || typeOfOperator == IASTBinaryExpression.op_shiftRightAssign
          ) {
        // a += b etc.
        
        String leftName = binExpression.getOperand1().getRawSignature();
        if (element.isUninitialized(leftName)) {
          // a +=5 where a is uninitialized -> everything stays the same
          if (printWarnings) {
            addWarning(cfaEdge, leftName);
            // check wether there are further uninitialized variables on right side
            isExpressionUninitialized(element, binExpression.getOperand2(), cfaEdge);
          }
          
        } else {
          handleAssign(element, binExpression, cfaEdge);
        }
      
      } else {
        // a + b etc.
        throw new TransferRelationException("Unhandled case " + cfaEdge.getRawStatement());
      }
    
    } else {
      throw new TransferRelationException("Unhandled case " + cfaEdge.getRawStatement());
    }
  }

  private void handleAssign(UninitializedVariablesElement element,
                            IASTBinaryExpression expression, CFAEdge cfaEdge)
                            throws TransferRelationException {
    
    IASTExpression op1 = expression.getOperand1();
    IASTExpression op2 = expression.getOperand2();
    
    if (op1 instanceof IASTIdExpression) {
      // assignment to simple variable
      
      String leftName = op1.getRawSignature();
      
      if (isExpressionUninitialized(element, op2, cfaEdge)) {
        setUninitialized(element, leftName);
      } else {
        setInitialized(element, leftName);
      }
    
    } else if (
           ((op1 instanceof IASTUnaryExpression) 
             && (((IASTUnaryExpression)op1).getOperator() == IASTUnaryExpression.op_star))
        || (op1 instanceof IASTFieldReference)
        || (op1 instanceof IASTArraySubscriptExpression)) {
      // assignment to the target of a pointer, a field or an array element,
      // this does not change the initialization status of the variable
      
      if (printWarnings) {
        isExpressionUninitialized(element, op1, cfaEdge);
        isExpressionUninitialized(element, op2, cfaEdge);
      }
    
    } else {
      throw new TransferRelationException("Unhandled case " + expression.getRawSignature());     
    }
  }

  private boolean isExpressionUninitialized(UninitializedVariablesElement element,
                                            IASTExpression expression,
                                            CFAEdge cfaEdge) throws TransferRelationException {
    if (expression == null) {
      // e.g. empty parameter list
      return false;
    
    } else if (expression instanceof IASTIdExpression) {
      String variable = expression.getRawSignature();
      if (element.isUninitialized(variable)) {
        addWarning(cfaEdge, variable);
        return true;
      } else {
        return false;
      }
            
    } else if (expression instanceof IASTTypeIdExpression) {
      // e.g. sizeof
      return false;
      
    } else if (expression instanceof IASTFieldReference) {
      // TODO: field access (needs types)
      System.out.println(expression.getRawSignature());

      return false;
    
    } else if (expression instanceof IASTArraySubscriptExpression) {
      IASTArraySubscriptExpression arrayExpression = (IASTArraySubscriptExpression)expression;
      return isExpressionUninitialized(element, arrayExpression.getArrayExpression(), cfaEdge)
           | isExpressionUninitialized(element, arrayExpression.getSubscriptExpression(), cfaEdge);
      
    } else if (expression instanceof IASTUnaryExpression) {
      IASTUnaryExpression unaryExpression = (IASTUnaryExpression)expression;
      
      int typeOfOperator = unaryExpression.getOperator(); 
      if (   (typeOfOperator == IASTUnaryExpression.op_amper)
          || (typeOfOperator == IASTUnaryExpression.op_sizeof)) {
        return false;
      
      } else {
        return isExpressionUninitialized(element, unaryExpression.getOperand(), cfaEdge);
      }
      
    } else if (expression instanceof IASTBinaryExpression) {
      IASTBinaryExpression binExpression = (IASTBinaryExpression) expression; 
      return isExpressionUninitialized(element, binExpression.getOperand1(), cfaEdge)
           | isExpressionUninitialized(element, binExpression.getOperand2(), cfaEdge);
    
    } else if (expression instanceof IASTCastExpression) {
      return isExpressionUninitialized(element, ((IASTCastExpression)expression).getOperand(), cfaEdge);
      
    } else if (expression instanceof IASTFunctionCallExpression) {
      IASTFunctionCallExpression funcExpression = (IASTFunctionCallExpression)expression;
      return uninitializedFunctionReturn
           | isExpressionUninitialized(element, funcExpression.getFunctionNameExpression(), cfaEdge)
           | isExpressionUninitialized(element, funcExpression.getParameterExpression(), cfaEdge);
    
    } else if (expression instanceof IASTExpressionList) {
      IASTExpressionList expressionList = (IASTExpressionList)expression;
      boolean result = false;
      for (IASTExpression exp : expressionList.getExpressions()) {
        if (isExpressionUninitialized(element, exp, cfaEdge)) {
          result = true;
        }
      }
      return result;
    
    } else if (expression instanceof IASTLiteralExpression) {
      return false;
      
    } else {
      throw new TransferRelationException("Unhandled case " + expression.getRawSignature());
    }
  }
  
  @Override
  public List<AbstractElementWithLocation> getAllAbstractSuccessors(
                                           AbstractElementWithLocation element,
                                           Precision precision)
                       throws CPAException, CPATransferException {
    throw new CPAException ("Cannot get all abstract successors from non-location domain");
  }

  @Override
  public AbstractElement strengthen(AbstractElement element,
                         List<AbstractElement> otherElements, CFAEdge cfaEdge,
                         Precision precision) { 
    UninitializedVariablesElement e = (UninitializedVariablesElement)element;
    System.out.println(e.toString());
    return null;
  }
}