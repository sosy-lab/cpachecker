/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
import java.util.logging.Level;

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
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
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
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.types.Type;
import org.sosy_lab.cpachecker.cpa.types.Type.TypeClass;
import org.sosy_lab.cpachecker.cpa.types.TypesState;
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

  private LogManager logger;

  //needed for strengthen()
  private String lastAdded = null;
  //used to check if a warning message in strengthen() has been displayed if typesCPA is not present
  private boolean typesWarningAlreadyDisplayed = false;

  public UninitializedVariablesTransferRelation(String printWarnings, LogManager logger) {
    globalVars = new HashSet<>();
    this.printWarnings = Boolean.parseBoolean(printWarnings);
    this.logger = logger;
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
      handleStatement(successor, ctrEdge.getExpression().asStatement(), ctrEdge);
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

    lastAdded = varName;

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
      throw new UnrecognizedCCodeException(cfaEdge, expression);
    }
  }

  private void handleAssign(UninitializedVariablesState element,
                            CAssignment expression, CFAEdge cfaEdge)
                            throws UnrecognizedCCodeException {

    CExpression op1 = expression.getLeftHandSide().getExpression();
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

    } else if (

        ((op1 instanceof CUnaryExpression)
            && (((CUnaryExpression)op1).getOperator() == UnaryOperator.STAR))
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
      throw new UnrecognizedCCodeException(cfaEdge, expression);
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
  /**
   * strengthen() is only necessary when declaring field variables, so the underlying struct type
   * is properly associated. This can only be done here because information about types is needed, which can
   * only be provided by typesCPA.
   */
  public Collection<? extends AbstractState> strengthen(AbstractState element,
                          List<AbstractState> otherElements, CFAEdge cfaEdge,
                          Precision precision) {

    //only call for declarations. check for lastAdded prevents unnecessary repeated executions for the same statement
    boolean typesCPAPresent = false;

    if (cfaEdge.getEdgeType() == CFAEdgeType.DeclarationEdge && lastAdded != null) {
      CDeclarationEdge declEdge = (CDeclarationEdge)cfaEdge;

      for (AbstractState other : otherElements) {

        //only interested in the types here
        if (other instanceof TypesState) {
          typesCPAPresent = true;

          //find type of the item last added to the list of variables
          TypesState typeElem = (TypesState) other;
          Type t = findType(typeElem, cfaEdge, lastAdded);

          if (t != null) {
            //only need to do this for non-external structs: add a variable for each field of the struct
            //and set it uninitialized (since it is only declared at this point); do this recursively for all
            //fields that are structs themselves
            if (t.getTypeClass() == TypeClass.STRUCT) {

              handleStructDeclaration((UninitializedVariablesState)element, typeElem,
                                      (Type.CompositeType)t, lastAdded, lastAdded,
                                      declEdge.getDeclaration().isGlobal());
            }
          }
        }
      }

      if (!typesWarningAlreadyDisplayed && !typesCPAPresent && lastAdded != null) {
        //set typesWarningAlreadyDisplayed so this message only comes up once
        typesWarningAlreadyDisplayed = true;
        logger.log(Level.INFO,
        "TypesCPA not present - information about field references may be unreliable");
      }

      //set lastAdded to null to prevent unnecessary repeats
      lastAdded = null;
    }

    //the following deals with structs being assigned to other structs
    if (cfaEdge.getEdgeType() == CFAEdgeType.StatementEdge) {

      CStatement exp = ((CStatementEdge)cfaEdge).getStatement();

      if (exp instanceof CAssignment) {

          CExpression op1 = ((CAssignment) exp).getLeftHandSide().getExpression();
          CRightHandSide op2 = ((CAssignment) exp).getRightHandSide();

          String leftName = op1.toASTString();
          String rightName = op2.toASTString();

          for (AbstractState other : otherElements) {
            //only interested in the types here
            if (other instanceof TypesState) {
              typesCPAPresent = true;

              TypesState typeElem = (TypesState) other;

              Type t1 = checkForFieldReferenceType(op1, typeElem, cfaEdge);
              Type t2 = checkForFieldReferenceType(op2, typeElem, cfaEdge);

              if (t1 != null && t2 != null) {

                //only interested in structs being assigned to structs here
                if (t1.getTypeClass() == TypeClass.STRUCT
                    && t2.getTypeClass() == TypeClass.STRUCT) {

                  //only structs of the same type can be assigned to each other
                  assert t1.equals(t2);

                  //check all fields of the structures' type and set their status
                  initializeFields((UninitializedVariablesState)element, cfaEdge, op2, typeElem,
                                   (Type.CompositeType)t1, leftName, rightName, leftName, rightName);
                }
              }
            }
          }
      }
    }
    return null;
  }

  /**
   * recursively checks the initialization status of all fields of a struct being assigned to
   * another struct of the same type, setting the status of the assignee's fields accordingly
   */
  private void initializeFields(UninitializedVariablesState element,
                                CFAEdge cfaEdge, CRightHandSide exp,
                                TypesState typeElem, Type.CompositeType structType,
                                String leftName, String rightName,
                                String recursiveLeftName, String recursiveRightName) {

    Set<String> members = structType.getMembers();

    //check all members
    for (String member : members) {
      Type t = structType.getMemberType(member);
      //for a field that is itself a struct, repeat the whole process
      if (t != null && t.getTypeClass() == TypeClass.STRUCT) {
        initializeFields(element, cfaEdge, exp, typeElem, (Type.CompositeType)t, member, member,
                         recursiveLeftName + "." + member, recursiveRightName + "." + member);
      //else, check the initialization status of the assigned variable
      //and set the status of the assignee accordingly
      } else {
        if (element.isUninitialized(recursiveRightName + "." + member)) {
          if (printWarnings) {
            addWarning(cfaEdge, recursiveRightName + "." + member, exp, element);
          }
          setUninitialized(element, recursiveLeftName + "." + member);
        }
      }
    }
  }

  /**
   * recursively sets all fields of a struct uninitialized, except if the field is itself a struct
   */
  private void handleStructDeclaration(UninitializedVariablesState element,
                                       TypesState typeElem,
                                       Type.CompositeType structType,
                                       String varName,
                                       String recursiveVarName,
                                       boolean isGlobalDeclaration) {

    //structs themselves are always considered initialized
    setInitialized(element, recursiveVarName);

    Set<String> members = structType.getMembers();

    for (String member : members) {
      Type t = structType.getMemberType(member);
      //for a field that is itself a struct, repeat the whole process
      if (t != null && t.getTypeClass() == TypeClass.STRUCT) {
        handleStructDeclaration(element, typeElem, (Type.CompositeType)t, member,
                                recursiveVarName + "." + member, isGlobalDeclaration);
      } else {
        //set non structure fields uninitialized, since they have only just been declared
        if (isGlobalDeclaration) {
          globalVars.add(recursiveVarName + "." + member);
        }
        setUninitialized(element, recursiveVarName + "." + member);
      }
    }
  }

  /**
   * checks wether a given expression is a field reference;
   * if yes, find the type of the referenced field, if no, try to determine the type of the variable
   */
  private Type checkForFieldReferenceType(CRightHandSide exp, TypesState typeElem, CFAEdge cfaEdge) {

    String name = exp.toASTString();
    Type t = null;

    if (exp instanceof CFieldReference) {
      String[] s = name.split("[.]");
      t = findType(typeElem, cfaEdge, s[0]);
      int i = 1;

      //follow the field reference to its end
      while (t != null && t.getTypeClass() == TypeClass.STRUCT && i < s.length) {
        t = ((Type.CompositeType)t).getMemberType(s[i]);
        i++;
      }

    //if exp is not a field reference, simply try to find the type of the associated variable name
    } else {
      t = findType(typeElem, cfaEdge, name);
    }
    return t;
  }

  /**
   * checks all possible locations for type information of a given name
   */
  private Type findType(TypesState typeElem, CFAEdge cfaEdge, String varName) {
    Type t = null;
    //check type definitions
    t = typeElem.getTypedef(varName);
    //if this fails, check functions
    if (t == null) {
      t = typeElem.getFunction(varName);
    }
    //if this also fails, check variables for the global context
    if (t == null) {
      t = typeElem.getVariableType(null, varName);
    }
    try {
      //if again there was no result, check local variables and function parameters
      if (t == null) {
        t = typeElem.getVariableType(cfaEdge.getSuccessor().getFunctionName(), varName);
      }
    } catch (IllegalArgumentException e) {
      //if nothing at all can be found, just return null
    }
    return t;
  }

}