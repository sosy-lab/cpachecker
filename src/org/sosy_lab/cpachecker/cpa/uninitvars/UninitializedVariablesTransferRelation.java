/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.uninitvars;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.uninitvars.UninitializedVariablesState.ElementProperty;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;

/**
 * Needs typesCPA to properly deal with field references.
 * If run without typesCPA, uninitialized field references may not be detected.
 */
public class UninitializedVariablesTransferRelation implements TransferRelation {

  private Set<String> globalVars; // set of all global variable names

  private boolean printWarnings;

  public UninitializedVariablesTransferRelation(String printWarnings, LogManager logger) {
    globalVars = new HashSet<>();
    this.printWarnings = Boolean.parseBoolean(printWarnings);
  }

  private AbstractState getAbstractSuccessor(AbstractState element,
                                              CFAEdge cfaEdge,
                                              Precision precision)
                                              throws CPATransferException {

    UninitializedVariablesState successor = ((UninitializedVariablesState)element).clone();
    successor.clearProperties();

    switch (cfaEdge.getEdgeType()) {

    case DeclarationEdge:
      handleDeclaration(successor, (CDeclarationEdge)cfaEdge);
      break;

    case StatementEdge:
      handleStatement(successor, ((CStatementEdge)cfaEdge).getStatement(), cfaEdge);
      break;

    case ReturnStatementEdge:
      //this is the return-statement of a function
      //set a local variable tracking the return statement's initialization status
      if (isExpressionUninitialized(successor, ((CReturnStatementEdge)cfaEdge).getExpression(), cfaEdge)) {
        setUninitialized(successor, "CPAchecker_UninitVars_FunctionReturn");
      } else {
        setInitialized(successor, "CPAchecker_UninitVars_FunctionReturn");
      }
      break;

    case FunctionReturnEdge:
      //handle statement like a = func(x) in the CFunctionSummaryEdge
      CFunctionReturnEdge functionReturnEdge = (CFunctionReturnEdge)cfaEdge;
      CFunctionSummaryEdge ctrEdge = functionReturnEdge.getSummaryEdge();
      handleStatement(successor, ctrEdge.getExpression(), ctrEdge);
      break;

    case AssumeEdge:
      // just check if there are uninitialized variable usages
      if (printWarnings) {
        isExpressionUninitialized(successor, ((CAssumeEdge)cfaEdge).getExpression(), cfaEdge);
      }
      break;

    case FunctionCallEdge:
      //on calling a function, check initialization status of the parameters
      handleFunctionCall(successor, (CFunctionCallEdge)cfaEdge);
      break;

    case BlankEdge:
      break;

    default:
      throw new UnrecognizedCFAEdgeException(cfaEdge);
    }

    return successor;
  }

  private void addWarning(CFAEdge edge, String variable, CRightHandSide expression,
                                                      UninitializedVariablesState element) {

    if (printWarnings) {

      int lineNumber = edge.getLineNumber();
      String message;

      if (edge instanceof FunctionSummaryEdge && expression instanceof CFunctionCallExpression) {
        message = "uninitialized return value of function call " + variable + " in line "
        + lineNumber + ": " + edge.getDescription();
        element.addProperty(ElementProperty.UNINITIALIZED_RETURN_VALUE);
      } else {
        message = "uninitialized variable " + variable + " used in line "
        + lineNumber + ": " + edge.getDescription();
        element.addProperty(ElementProperty.UNINITIALIZED_VARIABLE_USED);
      }

      element.addWarning(lineNumber, variable, message);
    }
  }

  private void setUninitialized(UninitializedVariablesState element, String varName) {
    if (globalVars.contains(varName)) {
      element.addGlobalVariable(varName);
    } else {
      element.addLocalVariable(varName);
    }
  }

  private void setInitialized(UninitializedVariablesState element, String varName) {
    if (globalVars.contains(varName)) {
      element.removeGlobalVariable(varName);
    } else {
      element.removeLocalVariable(varName);
    }
  }

  private void handleDeclaration(UninitializedVariablesState element,
      CDeclarationEdge declarationEdge) {

    if (!(declarationEdge.getDeclaration() instanceof CVariableDeclaration)) {
      // typedefs etc. do not concern this CPA
      return;
    }
    CVariableDeclaration decl = (CVariableDeclaration)declarationEdge.getDeclaration();
    String varName = decl.getName();
    if (decl.isGlobal()) {
      globalVars.add(varName);
    }

    CType type = decl.getType();
    CInitializer initializer = decl.getInitializer();
    // initializers in CIL are always constant, so no need to check if
    // initializer expression contains uninitialized variables
    if (initializer == null &&
        !(decl.getCStorageClass() == CStorageClass.EXTERN) &&
        !(type instanceof CArrayType) &&
        !(type instanceof CFunctionType)) {
      setUninitialized(element, varName);
    } else {
      setInitialized(element, varName);
    }

    if (isStructType(type)) {
      //only need to do this for non-external structs: add a variable for each field of the struct
      //and set it uninitialized (since it is only declared at this point); do this recursively for all
      //fields that are structs themselves
      handleStructDeclaration(element,
                              (CCompositeType)type, varName, varName,
                              decl.isGlobal());
    }
  }

  private void handleFunctionCall(UninitializedVariablesState element, CFunctionCallEdge callEdge)
                                                                  throws UnrecognizedCCodeException {
    //find functions's parameters and arguments
    CFunctionEntryNode functionEntryNode = callEdge.getSuccessor();
    List<String> paramNames = functionEntryNode.getFunctionParameterNames();
    List<CExpression> arguments = callEdge.getArguments();

    if (!arguments.isEmpty()) {

      int numOfParams = paramNames.size();

      //if the following  is the case, this is a varargs function and thus can take any number of arguments
      if (numOfParams < arguments.size()) {
        //then, for unnamed parameters, only check for use of uninitialized variables
        for (int j = numOfParams; j < arguments.size(); j++) {
          isExpressionUninitialized(element, arguments.get(j), callEdge);
        }
      }

      LinkedList<String> uninitParameters = new LinkedList<>();
      LinkedList<String> initParameters = new LinkedList<>();

      //collect initialization status of the called function's parameters from the context of the calling function
      for (int i = 0; i < numOfParams; i++) {
        if (isExpressionUninitialized(element, arguments.get(i), callEdge)) {
          uninitParameters.add(paramNames.get(i));
        } else {
          initParameters.add(paramNames.get(i));
        }
      }

      //create local context of the called function
      element.callFunction(functionEntryNode.getFunctionName());

      //set initialization status of the function's parameters according to the arguments
      for (String param : uninitParameters) {
        setUninitialized(element, param);
      }
      for (String param : initParameters) {
        setInitialized(element, param);
      }

    } else {
      //if there are no parameters, just create the local context
      element.callFunction(functionEntryNode.getFunctionName());
    }
  }

  private void handleStatement(UninitializedVariablesState element,
                               CStatement expression, CFAEdge cfaEdge)
                               throws UnrecognizedCCodeException {

    if (expression instanceof CFunctionCallStatement) {
      //in case of a return edge, remove the local context of the function from which we returned
      if (cfaEdge instanceof FunctionSummaryEdge) {
        element.returnFromFunction();
      }
      //a mere function call (func(a)) does not change the initialization status of variables
      // just check if there are uninitialized variable usages
      if (printWarnings) {
        for (CExpression param : ((CFunctionCallStatement)expression).getFunctionCallExpression().getParameterExpressions()) {
          isExpressionUninitialized(element, param, cfaEdge);
        }
      }

    } else if (expression instanceof CExpressionStatement) {

      // just check if there are uninitialized variable usages
      if (printWarnings) {
        isExpressionUninitialized(element, ((CExpressionStatement)expression).getExpression(), cfaEdge);
      }

    } else if (expression instanceof CAssignment) {
      // expression is an assignment operation, e.g. a = b or a = a+b;

      CAssignment assignExpression = (CAssignment)expression;

      // a = b
      handleAssign(element, assignExpression, cfaEdge);

    } else {
      throw new UnrecognizedCCodeException("unknown statement", cfaEdge, expression);
    }
  }

  private void handleAssign(UninitializedVariablesState element,
                            CAssignment expression, CFAEdge cfaEdge)
                            throws UnrecognizedCCodeException {

    CExpression op1 = expression.getLeftHandSide();
    CRightHandSide op2 = expression.getRightHandSide();

    if (op1 instanceof CIdExpression) {
      // assignment to simple variable

      String leftName = ((CIdExpression)op1).getName();

      if (isExpressionUninitialized(element, op2, cfaEdge)) {
        setUninitialized(element, leftName);
      } else {
        setInitialized(element, leftName);
      }


    } else if (op1 instanceof CFieldReference) {
      //for field references, don't change the initialization status in case of a pointer dereference
      if (((CFieldReference) op1).isPointerDereference()) {
        if (printWarnings) {
          isExpressionUninitialized(element, op1, cfaEdge);
          isExpressionUninitialized(element, op2, cfaEdge);
        }
      } else {
        String leftName = op1.toASTString();
        if (isExpressionUninitialized(element, op2, cfaEdge)) {
          setUninitialized(element, leftName);
        } else {
          setInitialized(element, leftName);
        }
      }

    } else if ((op1 instanceof CPointerExpression)
            || (op1 instanceof CArraySubscriptExpression)) {
      // assignment to the target of a pointer or an array element,
      // this does not change the initialization status of the variable

      if (printWarnings) {
        isExpressionUninitialized(element, op1, cfaEdge);
        isExpressionUninitialized(element, op2, cfaEdge);
      }

    } else {
      throw new UnrecognizedCCodeException("unknown left hand side of an assignment", cfaEdge, op1);
    }

    String leftName = op1.toASTString();
    String rightName = op2.toASTString();

    CType t1 = op1.getExpressionType().getCanonicalType();
    CType t2 = op2.getExpressionType().getCanonicalType();

    //only interested in structs being assigned to structs here
    if (isStructType(t1) && isStructType(t2)) {

      //only structs of the same type can be assigned to each other
      assert t1.equals(t2);

      //check all fields of the structures' type and set their status
      initializeFields(element, cfaEdge, op2,
                       (CCompositeType)t1, leftName, rightName, leftName, rightName);
    }
  }

  private boolean isStructType(CType t) {
    return t instanceof CCompositeType
        && ((CCompositeType)t).getKind() == ComplexTypeKind.STRUCT;
  }

  private boolean isExpressionUninitialized(UninitializedVariablesState element,
                                            CRightHandSide expression,
                                            CFAEdge cfaEdge) throws UnrecognizedCCodeException {
    if (expression == null) {
      // e.g. empty parameter list
      return false;

    } else if (expression instanceof CIdExpression) {
      String variable = ((CIdExpression)expression).getName();
      if (element.isUninitialized(variable)) {
        addWarning(cfaEdge, variable, expression, element);
        return true;
      } else {
        return false;
      }

    } else if (expression instanceof CTypeIdExpression) {
      // e.g. sizeof
      return false;

    } else if (expression instanceof CFieldReference) {
      CFieldReference e = (CFieldReference) expression;
      if (e.isPointerDereference()) {
        return isExpressionUninitialized(element, e.getFieldOwner(), cfaEdge);
      } else {
        String variable = expression.toASTString();
        if (element.isUninitialized(variable)) {
          addWarning(cfaEdge, variable, expression, element);
          return true;
        } else {
          return false;
        }
      }

    } else if (expression instanceof CArraySubscriptExpression) {
      CArraySubscriptExpression arrayExpression = (CArraySubscriptExpression)expression;
      return isExpressionUninitialized(element, arrayExpression.getArrayExpression(), cfaEdge)
           | isExpressionUninitialized(element, arrayExpression.getSubscriptExpression(), cfaEdge);

    } else if (expression instanceof CUnaryExpression) {
      CUnaryExpression unaryExpression = (CUnaryExpression)expression;

      UnaryOperator typeOfOperator = unaryExpression.getOperator();
      if (   (typeOfOperator == UnaryOperator.AMPER)
          || (typeOfOperator == UnaryOperator.SIZEOF)) {
        return false;

      } else {
        return isExpressionUninitialized(element, unaryExpression.getOperand(), cfaEdge);
      }

    } else if (expression instanceof  CPointerExpression) {
      return isExpressionUninitialized(element, ((CPointerExpression)expression).getOperand(), cfaEdge);

    } else if (expression instanceof CBinaryExpression) {
      CBinaryExpression binExpression = (CBinaryExpression) expression;
      return isExpressionUninitialized(element, binExpression.getOperand1(), cfaEdge)
           | isExpressionUninitialized(element, binExpression.getOperand2(), cfaEdge);

    } else if (expression instanceof CCastExpression) {
      return isExpressionUninitialized(element, ((CCastExpression)expression).getOperand(), cfaEdge);

    } else if (expression instanceof CFunctionCallExpression) {
      CFunctionCallExpression funcExpression = (CFunctionCallExpression)expression;
      //if the FunctionCallExpression is associated with a statement edge, then this is
      //an external function call, and call to return edges for external calls are disabled.
      //since we can not know its return value's initialization status, only check the parameters
      if (cfaEdge instanceof CStatementEdge) {
        for (CExpression param : funcExpression.getParameterExpressions()) {
          isExpressionUninitialized(element, param, cfaEdge);
        }
        return false;

      } else {
        //for an internal function call, we can check the return value - for an external call
        //(with enabled call to return edges), the return value is always assumed to be initialized
        boolean returnUninit = element.isUninitialized("CPAchecker_UninitVars_FunctionReturn");
        if (printWarnings && returnUninit) {
          addWarning(cfaEdge, funcExpression.toASTString(), expression, element);
        }
        //get rid of the local context, as it is no longer needed and may be different on the next call.
        //only do this in case of an internal call.
        if (cfaEdge instanceof FunctionSummaryEdge) {
          element.returnFromFunction();
        }
        return returnUninit;
      }

    } else if (expression instanceof CLiteralExpression) {
      return false;

    } else {
      throw new UnrecognizedCCodeException("unknown expression", cfaEdge, expression);
    }
  }

  @Override
  public Collection<AbstractState> getAbstractSuccessors(
                                           AbstractState element,
                                           Precision precision, CFAEdge cfaEdge)
                       throws CPATransferException {
    return Collections.singleton(getAbstractSuccessor(element, cfaEdge, precision));
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState element,
                          List<AbstractState> otherElements, CFAEdge cfaEdge,
                          Precision precision) {

    return null;
  }

  /**
   * recursively checks the initialization status of all fields of a struct being assigned to
   * another struct of the same type, setting the status of the assignee's fields accordingly
   */
  private void initializeFields(UninitializedVariablesState element,
                                CFAEdge cfaEdge, CRightHandSide exp,
                                CCompositeType structType,
                                String leftName, String rightName,
                                String recursiveLeftName, String recursiveRightName) {

    //check all members
    for (CCompositeTypeMemberDeclaration member : structType.getMembers()) {
      CType t = member.getType().getCanonicalType();
      String name = member.getName();
      //for a field that is itself a struct, repeat the whole process
      if (isStructType(t)) {
        initializeFields(element, cfaEdge, exp, (CCompositeType)t, name, name,
                         recursiveLeftName + "." + name, recursiveRightName + "." + name);
      //else, check the initialization status of the assigned variable
      //and set the status of the assignee accordingly
      } else {
        if (element.isUninitialized(recursiveRightName + "." + name)) {
          if (printWarnings) {
            addWarning(cfaEdge, recursiveRightName + "." + name, exp, element);
          }
          setUninitialized(element, recursiveLeftName + "." + name);
        }
      }
    }
  }

  /**
   * recursively sets all fields of a struct uninitialized, except if the field is itself a struct
   */
  private void handleStructDeclaration(UninitializedVariablesState element,
                                       CCompositeType structType,
                                       String varName,
                                       String recursiveVarName,
                                       boolean isGlobalDeclaration) {

    //structs themselves are always considered initialized
    setInitialized(element, recursiveVarName);

    for (CCompositeTypeMemberDeclaration member : structType.getMembers()) {
      CType t = member.getType().getCanonicalType();
      String name = member.getName();
      //for a field that is itself a struct, repeat the whole process
      if (isStructType(t)) {
        handleStructDeclaration(element, (CCompositeType)t, name,
                                recursiveVarName + "." + name, isGlobalDeclaration);
      } else {
        //set non structure fields uninitialized, since they have only just been declared
        if (isGlobalDeclaration) {
          globalVars.add(recursiveVarName + "." + name);
        }
        setUninitialized(element, recursiveVarName + "." + name);
      }
    }
  }
}