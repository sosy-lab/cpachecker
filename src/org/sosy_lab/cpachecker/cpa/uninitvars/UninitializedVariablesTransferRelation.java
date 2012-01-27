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
import org.sosy_lab.cpachecker.cfa.ast.IASTArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTArrayTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTAssignment;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionTypeSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTInitializer;
import org.sosy_lab.cpachecker.cfa.ast.IASTLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.IASTStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.IType;
import org.sosy_lab.cpachecker.cfa.ast.StorageClass;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
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
import org.sosy_lab.cpachecker.cpa.types.Type;
import org.sosy_lab.cpachecker.cpa.types.Type.TypeClass;
import org.sosy_lab.cpachecker.cpa.types.TypesElement;
import org.sosy_lab.cpachecker.cpa.uninitvars.UninitializedVariablesElement.ElementProperty;
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
    globalVars = new HashSet<String>();
    this.printWarnings = Boolean.parseBoolean(printWarnings);
    this.logger = logger;
  }

  private AbstractElement getAbstractSuccessor(AbstractElement element,
                                              CFAEdge cfaEdge,
                                              Precision precision)
                                              throws CPATransferException {

    UninitializedVariablesElement successor = ((UninitializedVariablesElement)element).clone();
    successor.clearProperties();

    switch (cfaEdge.getEdgeType()) {

    case DeclarationEdge:
      handleDeclaration(successor, (DeclarationEdge)cfaEdge);
      break;

    case StatementEdge:
      handleStatement(successor, ((StatementEdge)cfaEdge).getStatement(), cfaEdge);
      break;

    case ReturnStatementEdge:
      //this is the return-statement of a function
      //set a local variable tracking the return statement's initialization status
      if (isExpressionUninitialized(successor, ((ReturnStatementEdge)cfaEdge).getExpression(), cfaEdge)) {
        setUninitialized(successor, "CPAChecker_UninitVars_FunctionReturn");
      } else {
        setInitialized(successor, "CPAChecker_UninitVars_FunctionReturn");
      }
      break;

    case FunctionReturnEdge:
      //handle statement like a = func(x) in the CallToReturnEdge
      FunctionReturnEdge functionReturnEdge = (FunctionReturnEdge)cfaEdge;
      CallToReturnEdge ctrEdge = functionReturnEdge.getSuccessor().getEnteringSummaryEdge();
      handleStatement(successor, ctrEdge.getExpression().asStatement(), ctrEdge);
      break;

    case AssumeEdge:
      // just check if there are uninitialized variable usages
      if (printWarnings) {
        isExpressionUninitialized(successor, ((AssumeEdge)cfaEdge).getExpression(), cfaEdge);
      }
      break;

    case FunctionCallEdge:
      //on calling a function, check initialization status of the parameters
      handleFunctionCall(successor, (FunctionCallEdge)cfaEdge);
      break;

    case BlankEdge:
      break;

    default:
      throw new UnrecognizedCFAEdgeException(cfaEdge);
    }

    return successor;
  }

  private void addWarning(CFAEdge edge, String variable, IASTRightHandSide expression,
                                                      UninitializedVariablesElement element) {

    if (printWarnings) {

      int lineNumber = edge.getLineNumber();
      String message;

      if (edge instanceof CallToReturnEdge && expression instanceof IASTFunctionCallExpression) {
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

    //typedefs do not concern this CPA
    if (declaration.getStorageClass() != StorageClass.TYPEDEF) {

      if (declaration.getName() != null) {
          String varName = declaration.getName();
          if (declaration.isGlobal()) {
            globalVars.add(varName);
          }

          lastAdded = varName;

          IType specifier = declaration.getDeclSpecifier();
          IASTInitializer initializer = declaration.getInitializer();
          // initializers in CIL are always constant, so no need to check if
          // initializer expression contains uninitialized variables
          if (initializer == null &&
              !(declaration.getStorageClass() == StorageClass.EXTERN) &&
              !(specifier instanceof IASTArrayTypeSpecifier) &&
              !(specifier instanceof IASTFunctionTypeSpecifier)) {
            setUninitialized(element, varName);
          } else {
            setInitialized(element, varName);
          }
      }
    }
  }

  private void handleFunctionCall(UninitializedVariablesElement element, FunctionCallEdge callEdge)
                                                                  throws UnrecognizedCCodeException {
    //find functions's parameters and arguments
    FunctionDefinitionNode functionEntryNode = callEdge.getSuccessor();
    List<String> paramNames = functionEntryNode.getFunctionParameterNames();
    List<IASTExpression> arguments = callEdge.getArguments();

    if (!arguments.isEmpty()) {

      int numOfParams = paramNames.size();

      //if the following  is the case, this is a varargs function and thus can take any number of arguments
      if (numOfParams < arguments.size()) {
        //then, for unnamed parameters, only check for use of uninitialized variables
        for (int j = numOfParams; j < arguments.size(); j++) {
          isExpressionUninitialized(element, arguments.get(j), callEdge);
        }
      }

      LinkedList<String> uninitParameters = new LinkedList<String>();
      LinkedList<String> initParameters = new LinkedList<String>();

      //collect initialization status of the called function's parameters from the context of the calling function
      for (int i = 0; i < numOfParams; i++) {
        if(isExpressionUninitialized(element, arguments.get(i), callEdge)) {
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

  private void handleStatement(UninitializedVariablesElement element,
                               IASTStatement expression, CFAEdge cfaEdge)
                               throws UnrecognizedCCodeException {

    if (expression instanceof IASTFunctionCallStatement) {
      //in case of a return edge, remove the local context of the function from which we returned
      if (cfaEdge instanceof CallToReturnEdge) {
        element.returnFromFunction();
      }
      //a mere function call (func(a)) does not change the initialization status of variables
      // just check if there are uninitialized variable usages
      if (printWarnings) {
        for (IASTExpression param : ((IASTFunctionCallStatement)expression).getFunctionCallExpression().getParameterExpressions()) {
          isExpressionUninitialized(element, param, cfaEdge);
        }
      }

    } else if (expression instanceof IASTExpressionStatement) {

      // just check if there are uninitialized variable usages
      if (printWarnings) {
        isExpressionUninitialized(element, ((IASTExpressionStatement)expression).getExpression(), cfaEdge);
      }

    } else if (expression instanceof IASTAssignment) {
      // expression is an assignment operation, e.g. a = b or a = a+b;

      IASTAssignment assignExpression = (IASTAssignment)expression;

      // a = b
      handleAssign(element, assignExpression, cfaEdge);

    } else {
      throw new UnrecognizedCCodeException(cfaEdge, expression);
    }
  }

  private void handleAssign(UninitializedVariablesElement element,
                            IASTAssignment expression, CFAEdge cfaEdge)
                            throws UnrecognizedCCodeException {

    IASTExpression op1 = expression.getLeftHandSide();
    IASTRightHandSide op2 = expression.getRightHandSide();

    if (op1 instanceof IASTIdExpression) {
      // assignment to simple variable

      String leftName = ((IASTIdExpression)op1).getName();

      if (isExpressionUninitialized(element, op2, cfaEdge)) {
        setUninitialized(element, leftName);
      } else {
        setInitialized(element, leftName);
      }


    } else if (op1 instanceof IASTFieldReference) {
      //for field references, don't change the initialization status in case of a pointer dereference
      if (((IASTFieldReference) op1).isPointerDereference()) {
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

        ((op1 instanceof IASTUnaryExpression)
            && (((IASTUnaryExpression)op1).getOperator() == UnaryOperator.STAR))
            || (op1 instanceof IASTArraySubscriptExpression)) {
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

  private boolean isExpressionUninitialized(UninitializedVariablesElement element,
                                            IASTRightHandSide expression,
                                            CFAEdge cfaEdge) throws UnrecognizedCCodeException {
    if (expression == null) {
      // e.g. empty parameter list
      return false;

    } else if (expression instanceof IASTIdExpression) {
      String variable = ((IASTIdExpression)expression).getName();
      if (element.isUninitialized(variable)) {
        addWarning(cfaEdge, variable, expression, element);
        return true;
      } else {
        return false;
      }

    } else if (expression instanceof IASTTypeIdExpression) {
      // e.g. sizeof
      return false;

    } else if (expression instanceof IASTFieldReference) {
      IASTFieldReference e = (IASTFieldReference) expression;
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

    } else if (expression instanceof IASTArraySubscriptExpression) {
      IASTArraySubscriptExpression arrayExpression = (IASTArraySubscriptExpression)expression;
      return isExpressionUninitialized(element, arrayExpression.getArrayExpression(), cfaEdge)
           | isExpressionUninitialized(element, arrayExpression.getSubscriptExpression(), cfaEdge);

    } else if (expression instanceof IASTUnaryExpression) {
      IASTUnaryExpression unaryExpression = (IASTUnaryExpression)expression;

      UnaryOperator typeOfOperator = unaryExpression.getOperator();
      if (   (typeOfOperator == UnaryOperator.AMPER)
          || (typeOfOperator == UnaryOperator.SIZEOF)) {
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
      //if the FunctionCallExpression is associated with a statement edge, then this is
      //an external function call, and call to return edges for external calls are disabled.
      //since we can not know its return value's initialization status, only check the parameters
      if (cfaEdge instanceof StatementEdge) {
        for (IASTExpression param : funcExpression.getParameterExpressions()) {
          isExpressionUninitialized(element, param, cfaEdge);
        }
        return false;

      } else {
        //for an internal function call, we can check the return value - for an external call
        //(with enabled call to return edges), the return value is always assumed to be initialized
        boolean returnUninit = element.isUninitialized("CPAChecker_UninitVars_FunctionReturn");
        if (printWarnings && returnUninit) {
          addWarning(cfaEdge, funcExpression.toASTString(), expression, element);
        }
        //get rid of the local context, as it is no longer needed and may be different on the next call.
        //only do this in case of an internal call.
        if (cfaEdge instanceof CallToReturnEdge &&
            !((CallToReturnEdge)cfaEdge).getRawStatement().equals("External Call")) {
          element.returnFromFunction();
        }
        return returnUninit;
      }

    } else if (expression instanceof IASTLiteralExpression) {
      return false;

    } else {
      throw new UnrecognizedCCodeException(cfaEdge, expression);
    }
  }

  @Override
  public Collection<AbstractElement> getAbstractSuccessors(
                                           AbstractElement element,
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
  public Collection<? extends AbstractElement> strengthen(AbstractElement element,
                          List<AbstractElement> otherElements, CFAEdge cfaEdge,
                          Precision precision) {

    //only call for declarations. check for lastAdded prevents unnecessary repeated executions for the same statement
    boolean typesCPAPresent = false;

    if (cfaEdge.getEdgeType() == CFAEdgeType.DeclarationEdge && lastAdded != null) {
      DeclarationEdge declEdge = (DeclarationEdge)cfaEdge;

      for (AbstractElement other : otherElements) {

        //only interested in the types here
        if (other instanceof TypesElement) {
          typesCPAPresent = true;

          //find type of the item last added to the list of variables
          TypesElement typeElem = (TypesElement) other;
          Type t = findType(typeElem, cfaEdge, lastAdded);

          if (t != null) {
            //only need to do this for non-external structs: add a variable for each field of the struct
            //and set it uninitialized (since it is only declared at this point); do this recursively for all
            //fields that are structs themselves
            if (t.getTypeClass() == TypeClass.STRUCT &&
                !(declEdge.getStorageClass() == StorageClass.EXTERN)) {

              handleStructDeclaration((UninitializedVariablesElement)element, typeElem,
                                      (Type.CompositeType)t, lastAdded, lastAdded,
                                      declEdge.isGlobal());
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

      IASTStatement exp = ((StatementEdge)cfaEdge).getStatement();

      if (exp instanceof IASTAssignment) {

          IASTExpression op1 = ((IASTAssignment) exp).getLeftHandSide();
          IASTRightHandSide op2 = ((IASTAssignment) exp).getRightHandSide();

          String leftName = op1.toASTString();
          String rightName = op2.toASTString();

          for (AbstractElement other : otherElements) {
            //only interested in the types here
            if (other instanceof TypesElement) {
              typesCPAPresent = true;

              TypesElement typeElem = (TypesElement) other;

              Type t1 = checkForFieldReferenceType(op1, typeElem, cfaEdge);
              Type t2 = checkForFieldReferenceType(op2, typeElem, cfaEdge);

              if (t1 != null && t2 != null) {

                //only interested in structs being assigned to structs here
                if (t1.getTypeClass() == TypeClass.STRUCT
                    && t2.getTypeClass() == TypeClass.STRUCT) {

                  //only structs of the same type can be assigned to each other
                  assert t1.equals(t2);

                  //check all fields of the structures' type and set their status
                  initializeFields((UninitializedVariablesElement)element, cfaEdge, op2, typeElem,
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
  private void initializeFields(UninitializedVariablesElement element,
                                CFAEdge cfaEdge, IASTRightHandSide exp,
                                TypesElement typeElem, Type.CompositeType structType,
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
  private void handleStructDeclaration(UninitializedVariablesElement element,
                                       TypesElement typeElem,
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
  private Type checkForFieldReferenceType(IASTRightHandSide exp, TypesElement typeElem, CFAEdge cfaEdge) {

    String name = exp.toASTString();
    Type t = null;

    if (exp instanceof IASTFieldReference) {
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
  private Type findType(TypesElement typeElem, CFAEdge cfaEdge, String varName) {
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