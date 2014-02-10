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
package org.sosy_lab.cpachecker.cpa.rtt;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.IAInitializer;
import org.sosy_lab.cpachecker.cfa.ast.java.DefaultJExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayCreationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JAssignment;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.java.JCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JClassInstanceCreation;
import org.sosy_lab.cpachecker.cfa.ast.java.JEnumConstantExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldAccess;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodInvocationAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodInvocationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodOrConstructorInvocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JNullLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JObjectReferenceReturn;
import org.sosy_lab.cpachecker.cfa.ast.java.JReferencedMethodInvocationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.java.JRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JRunTimeTypeEqualsType;
import org.sosy_lab.cpachecker.cfa.ast.java.JSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JStatement;
import org.sosy_lab.cpachecker.cfa.ast.java.JStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JSuperConstructorInvocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JThisExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableRunTimeType;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.types.java.JBasicType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JReferenceType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Transfer Relation traversing the the CFA an tracking Run Type Time Information
 * of Java Programs.
 *
 *
 */
public class RTTTransferRelation implements TransferRelation {

  private static final String NOT_IN_OBJECT_SCOPE = RTTState.NULL_REFERENCE;
  private static final int RETURN_EDGE = 0;
  private final Set<String> staticFieldVariables = new HashSet<>();
  private final Set<String> nonStaticFieldVariables = new HashSet<>();

  private static int nextFreeId = 0;

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState element, Precision precision, CFAEdge cfaEdge)
          throws CPATransferException, InterruptedException {

    RTTState rttState = (RTTState) element;

    RTTState successor;

    switch (cfaEdge.getEdgeType()) {
    // this is an assumption, e.g. if (a == b)
    case AssumeEdge:
      AssumeEdge assumeEdge = (AssumeEdge) cfaEdge;
      JExpression exp = (JExpression) assumeEdge.getExpression();

      successor =
          handleAssumption(RTTState.copyOf(rttState),
              exp, cfaEdge,
              assumeEdge.getTruthAssumption());
      break;

    case FunctionCallEdge:
      FunctionCallEdge functionCallEdge = (FunctionCallEdge) cfaEdge;
      successor = handleFunctionCall(rttState, functionCallEdge);
      break;

    // this is a return edge from function, this is different from return statement
    // of the function. See case for statement edge for details
    case FunctionReturnEdge:
      FunctionReturnEdge functionReturnEdge = (FunctionReturnEdge) cfaEdge;
      successor = handleFunctionReturn(rttState, functionReturnEdge);

      successor.dropFrame(functionReturnEdge.getPredecessor().getFunctionName());
      break;

    default:
      successor = RTTState.copyOf(rttState);
      handleSimpleEdge(successor, cfaEdge);
    }

    if (successor == null) {
      return Collections.emptySet();
    } else {
      return Collections.singleton(successor);
    }
  }

  private void handleSimpleEdge(RTTState element, CFAEdge cfaEdge)
        throws UnrecognizedCFAEdgeException, UnrecognizedCodeException {

    // check the type of the edge
    switch (cfaEdge.getEdgeType()) {
    // if edge is a statement edge, e.g. a = b + c
    case StatementEdge:
      AStatementEdge statementEdge = (AStatementEdge) cfaEdge;
      handleStatement(element, (JStatement) statementEdge.getStatement(), cfaEdge);
      break;

    case ReturnStatementEdge:
      AReturnStatementEdge returnEdge = (AReturnStatementEdge) cfaEdge;
      // this statement is a function return, e.g. return (a);
      // note that this is different from return edge
      // this is a statement edge which leads the function to the
      // last node of its CFA, where return edge is from that last node
      // to the return site of the caller function
      if (returnEdge.getExpression() != null) {
        JExpression exp = (JExpression) returnEdge.getExpression();
        handleExitFromFunction(element, exp, returnEdge);
      }
      break;

    // edge is a declaration edge, e.g. int a;
    case DeclarationEdge:
      ADeclarationEdge declarationEdge = (ADeclarationEdge) cfaEdge;
      handleDeclaration(element, declarationEdge);
      break;

    case BlankEdge:
      break;

    case MultiEdge:
      for (CFAEdge edge : (MultiEdge) cfaEdge) {
        handleSimpleEdge(element, edge);
      }
      break;

    default:
      throw new UnrecognizedCFAEdgeException(cfaEdge);
    }

  }

  private void handleDeclaration(RTTState newElement,
      ADeclarationEdge declarationEdge) throws UnrecognizedCodeException {

    if (!(declarationEdge.getDeclaration() instanceof JVariableDeclaration)) {
      // nothing interesting to see here, please move along
      return;
    }


    JVariableDeclaration decl =
        (JVariableDeclaration) declarationEdge.getDeclaration();

    if (decl.getType() instanceof JSimpleType) {

      JBasicType simpleType = ((JSimpleType)decl.getType()).getType();

          switch (simpleType) {
          case BOOLEAN:
          case BYTE:
          case CHAR:
          case FLOAT:
          case DOUBLE:
          case INT:
          case LONG:
          case SHORT:
          case UNSPECIFIED:
            // TODO Change with inclusion of Boxing, Unboxing
            // Unnecessary to track Primitive types.
            return;
      }

    }

    // get the variable name in the declarator
    String varName = decl.getName();
    String methodName = declarationEdge.getPredecessor().getFunctionName();

    // variables without initializer are set to null
    // until they are assigned a value
    String initialValue = RTTState.NULL_REFERENCE;

    // handle field variables
    if (decl instanceof JFieldDeclaration) {

      JFieldDeclaration fieldVariable = (JFieldDeclaration) decl;

      if (fieldVariable.isStatic()) {
        // if this is a  field, add to the list of field variables
        staticFieldVariables.add(varName);
      } else {
        nonStaticFieldVariables.add(varName);
      }
    }

    // get initial value
    IAInitializer init = decl.getInitializer();

    if (init instanceof JInitializerExpression) {
      JExpression exp = ((JInitializerExpression) init).getExpression();

      initialValue =
          getExpressionValue(newElement, exp, methodName, declarationEdge);

    }

    // assign initial value
    String scopedVarName =
        getScopedVariableName(varName, methodName,
                              newElement.getClassObjectScope());

    if (initialValue == null) {
      newElement.forget(scopedVarName);
    } else {
      newElement.assignObject(scopedVarName, initialValue);
    }

  }

  private String getExpressionValue(RTTState element, JExpression expression,
          String methodName, CFAEdge edge) throws UnrecognizedCodeException {
    return expression.accept(
        new ExpressionValueVisitor(edge, element, methodName));
  }

  private void handleExitFromFunction(RTTState newElement,
                  JExpression expression, AReturnStatementEdge returnEdge)
                                        throws UnrecognizedCodeException {

    String methodName = returnEdge.getPredecessor().getFunctionName();

    // In Case Of Class Instance Creation, return unique Object
    if (returnEdge.getRawAST().get() instanceof JObjectReferenceReturn) {
      handleAssignmentToVariable("___cpa_temp_result_var_", expression,
          newElement.getClassObjectScope(), newElement,
          methodName);
    } else {
      handleAssignmentToVariable("___cpa_temp_result_var_", expression,
          new ExpressionValueVisitor(returnEdge, newElement, methodName));
    }
  }

  private void handleAssignmentToVariable
                        (String lParam, JExpression exp, String value,
                            RTTState newElement, String functionName) {

    String assignedVar =
        getScopedVariableName(lParam, functionName,
            newElement.getClassObjectScope());

    if (value == null) {
      newElement.forget(assignedVar);
    } else {
      newElement.assignObject(assignedVar, value);
    }
  }

  private void handleAssignmentToVariable(String lParam, JExpression exp,
      ExpressionValueVisitor visitor) throws UnrecognizedCodeException {

    String value;
    value =  exp.accept(visitor);
    RTTState newElement = visitor.state;

   handleAssignmentToVariable(lParam, exp, value, newElement, visitor.functionName);
  }

  private void handleStatement(RTTState newElement,
      JStatement expression, CFAEdge cfaEdge) throws UnrecognizedCodeException {

    // expression is a binary operation, e.g. a = b;
    if (expression instanceof JAssignment) {
      handleAssignment(newElement, (JAssignment) expression, cfaEdge);

      // external function call - do nothing
    } else if (expression instanceof JMethodOrConstructorInvocation) {

      // there is such a case
    } else if (expression instanceof JExpressionStatement) {

    } else {
      throw new UnrecognizedCodeException("unknown statement", cfaEdge, expression);
    }
  }

  private void handleAssignment(RTTState newElement,
                                  JAssignment assignExpression, CFAEdge cfaEdge)
                                                throws UnrecognizedCCodeException {

    JExpression op1 = assignExpression.getLeftHandSide();
    JRightHandSide op2 = assignExpression.getRightHandSide();



    if (op1 instanceof JIdExpression) {
      // a = ...

      String methodName = cfaEdge.getPredecessor().getFunctionName();

      // If declaration could not be resolve, forget variable
      if (((JIdExpression) op1).getDeclaration() == null) {

        String scopedName = getScopedVariableName(
                                              ((JIdExpression) op1).getName(),
                                              methodName,
                                              newElement.getClassObjectScope());

        newElement.forget(scopedName);
        return;
      }

      handleAssignmentToVariable((JIdExpression) op1,
          op2, new ExpressionValueVisitor(cfaEdge, newElement, methodName));
    }
  }

  private void handleAssignmentToVariable(JIdExpression lParam, JRightHandSide exp,
      ExpressionValueVisitor visitor) throws UnrecognizedCCodeException {


    String lParamObjectScope = getObjectScope(visitor.state, visitor.functionName, lParam);
    String value = exp.accept(visitor);

    RTTState newElement = visitor.state;

    if (nonStaticFieldVariables.contains(lParam.getName()) && lParamObjectScope == null) {
      // can't resolve lParam variable, do nothing
      // TODO How to forget old Values?
      return;
    }

    String assignedVar
    = getScopedVariableName(lParam.getName(),
        visitor.functionName, lParamObjectScope);

    if (value != null && (lParam.getExpressionType() instanceof JReferenceType)) {
      newElement.assignObject(assignedVar, value);

    } else {
      newElement.forget(assignedVar);
    }
  }

  private String getObjectScope(RTTState rttState, String methodName,
      JIdExpression notScopedField) {

    // Could not resolve var
    if (notScopedField.getDeclaration() == null) {
      return null;
    }

    if (notScopedField instanceof JFieldAccess) {

      JIdExpression qualifier = ((JFieldAccess) notScopedField).getReferencedVariable();

      String qualifierScope = getObjectScope(rttState, methodName, qualifier);

      String scopedFieldName =
          getScopedVariableName(qualifier.getDeclaration().getName(), methodName, qualifierScope);

      if (rttState.contains(scopedFieldName)) {
        return rttState.getUniqueObjectFor(scopedFieldName);
      } else {
        return null;
      }
    } else {
      if (rttState.contains(RTTState.KEYWORD_THIS)) {
        return rttState.getUniqueObjectFor(RTTState.KEYWORD_THIS);
      } else {
        return null;
      }
    }
  }


  private RTTState handleFunctionReturn(
      RTTState element,
      FunctionReturnEdge functionReturnEdge) throws UnrecognizedCodeException {

    FunctionSummaryEdge summaryEdge    = functionReturnEdge.getSummaryEdge();
    JMethodOrConstructorInvocation exprOnSummary  = (JMethodOrConstructorInvocation) summaryEdge.getExpression();

    RTTState newElement  = RTTState.copyOf(element);
    String callerFunctionName = functionReturnEdge.getSuccessor().getFunctionName();
    String calledFunctionName = functionReturnEdge.getPredecessor().getFunctionName();

    // expression is an assignment operation, e.g. a = g(b);

    if (exprOnSummary instanceof JMethodInvocationAssignmentStatement) {
      JMethodInvocationAssignmentStatement assignExp = ((JMethodInvocationAssignmentStatement)exprOnSummary);
      JExpression op1 = assignExp.getLeftHandSide();

      // we expect left hand side of the expression to be a variable

      if ((op1 instanceof JIdExpression)) {

        String returnVarName = getScopedVariableName("___cpa_temp_result_var_", calledFunctionName, newElement.getClassObjectScope());

        String assignedVarName = getScopedVariableName(((JIdExpression) op1).getName(), callerFunctionName, newElement.getClassObjectStack().peek());


        JSimpleDeclaration decl = ((JIdExpression) op1).getDeclaration();

        //Ignore not reference Types
        if (element.contains(returnVarName) && (decl.getType() instanceof JReferenceType)) {
          newElement.assignObject(assignedVarName, element.getUniqueObjectFor(returnVarName));
        } else {
          newElement.forget(assignedVarName);
        }
      }

      // a[x] = b(); TODO: for now, nothing is done here, but cloning the current element
      else if (op1 instanceof JArraySubscriptExpression) {
        return newElement;
      }

      else {
        throw new UnrecognizedCodeException("on function return", summaryEdge, op1);
      }
    }

    return newElement;
  }

  private RTTState handleFunctionCall(RTTState element, FunctionCallEdge callEdge)
      throws UnrecognizedCCodeException {

    RTTState newElement = RTTState.copyOf(element);

    FunctionEntryNode functionEntryNode = callEdge.getSuccessor();
    String calledFunctionName = functionEntryNode.getFunctionName();
    String callerFunctionName = callEdge.getPredecessor().getFunctionName();


    List<String> paramNames = functionEntryNode.getFunctionParameterNames();

    @SuppressWarnings("unchecked")
    List<? extends JExpression> arguments = (List<? extends JExpression>) callEdge.getArguments();

    if (!callEdge.getSuccessor().getFunctionDefinition().getType().takesVarArgs()) {
      assert (paramNames.size() == arguments.size());
    }

    // visitor for getting the Object values of the actual parameters in caller function context
    ExpressionValueVisitor visitor = new ExpressionValueVisitor(callEdge, element, callerFunctionName);

    // get value of actual parameter in caller function context
    for (int i = 0; i < paramNames.size(); i++) {
      String value = null;
      JExpression exp = arguments.get(i);

      value = exp.accept(visitor);

      String formalParamName =
          getScopedVariableName(paramNames.get(i), calledFunctionName, newElement.getClassObjectScope());

      if (value == null || !(exp.getExpressionType() instanceof JReferenceType)) {
        newElement.forget(formalParamName);
      } else {
        newElement.assignObject(formalParamName, value);
      }
    }

    JMethodInvocationExpression functionCall = (JMethodInvocationExpression) callEdge.getSummaryEdge().getExpression().getFunctionCallExpression();


    // There are five possibilities when assigning this and the new object Scope.

    // A Object calls its super Constructor
    if (functionCall instanceof JSuperConstructorInvocation) {

      newElement.assignThisAndNewObjectScope(element.getUniqueObjectFor(RTTState.KEYWORD_THIS));

   // A New Object is created, which is the new classObject scope
    } else if (functionCall instanceof JClassInstanceCreation) {

      AReturnStatementEdge returnEdge =  (AReturnStatementEdge) functionEntryNode.getExitNode().getEnteringEdge(RETURN_EDGE);
      String uniqueObject = ((JExpression) returnEdge.getExpression()).accept(new FunctionExitValueVisitor(returnEdge, newElement, calledFunctionName));
      newElement.assignThisAndNewObjectScope(uniqueObject);

      // A Referenced Method Invocation, the new scope is the unique Object
      // of its reference variable
    } else if (functionCall instanceof JReferencedMethodInvocationExpression) {
      JReferencedMethodInvocationExpression objectMethodInvocation = (JReferencedMethodInvocationExpression) functionCall;
      JSimpleDeclaration variableReference = objectMethodInvocation.getReferencedVariable().getDeclaration();

      if (newElement.contains(getScopedVariableName(variableReference.getName(), callerFunctionName, newElement.getClassObjectScope()))) {
        newElement.assignThisAndNewObjectScope(newElement.getUniqueObjectFor(getScopedVariableName(variableReference.getName(), callerFunctionName, newElement.getClassObjectScope())));
      } else {
        // When the object of the variable can't be found
        newElement.assignThisAndNewObjectScope(NOT_IN_OBJECT_SCOPE);
      }
      //  a unreferenced Method Invocation
    } else {

      JMethodDeclaration decl = functionCall.getDeclaration();

      // If the method isn't static, the object  scope remains the same
      if (decl.isStatic()) {
        newElement.assignThisAndNewObjectScope(NOT_IN_OBJECT_SCOPE);
      } else {
       newElement.assignThisAndNewObjectScope(newElement.getUniqueObjectFor(RTTState.KEYWORD_THIS));
      }
     //  the method Invocation can't be handled
    }
    return newElement;
  }

  private String getScopedVariableName(String variableName, String functionName, String uniqueObject) {

    if (variableName.equals(RTTState.KEYWORD_THIS)) { return variableName; }

    if (staticFieldVariables.contains(variableName)) { return variableName; }

    if (nonStaticFieldVariables.contains(variableName)) { return uniqueObject + "::" + variableName; }

    return functionName + "::" + variableName;
  }

  private RTTState handleAssumption(RTTState element,
      JExpression expression, CFAEdge cfaEdge,
      boolean truthAssumption) throws UnrecognizedCodeException {

    String functionName = cfaEdge.getPredecessor().getFunctionName();

    // get the value of the expression (either true[1L], false[0L], or unknown[null])
    String valueString = getExpressionValue(element, expression, functionName, cfaEdge);

    boolean value = Boolean.parseBoolean(valueString);

    // value is null and therefore unknown
    // Investigate if right
    if (valueString == null) {

      AssigningValueVisitor visitor =
          new AssigningValueVisitor(element, truthAssumption, functionName);

      // Try to derive Information from Assumption.
      expression.accept(visitor);

      return element;
    } else if ((truthAssumption && value) || (!truthAssumption && !value)) {
      return element;
    } else {
      return null;
    }
  }

  private class FunctionExitValueVisitor extends ExpressionValueVisitor {

    public FunctionExitValueVisitor(CFAEdge pEdge, RTTState pElement, String pFunctionName) {
      super(pEdge, pElement, pFunctionName);

    }

    @Override
    public String visit(JThisExpression thisExp) throws UnrecognizedCCodeException {
      return thisExp.getExpressionType().getName();
    }

  }



  private class AssigningValueVisitor extends DefaultJExpressionVisitor<String, UnrecognizedCCodeException> {

    final private boolean truthAssumption;
    final private RTTState newState;
    private final String methodName;


    public AssigningValueVisitor(RTTState pNewState, boolean pTruthAssumption, String pMethodName) {
      truthAssumption = pTruthAssumption;
      newState = pNewState;
      methodName = pMethodName;
    }

    @Override
    public String visit(JVariableRunTimeType pE) throws UnrecognizedCCodeException {
      return getScopedVariableName(pE.getReferencedVariable().getName(), methodName, newState.getKeywordThisUniqueObject());
    }

    @Override
    protected String visitDefault(JExpression pE) {
      return null;
    }

    @Override
    public String visit(JThisExpression thisExpression) throws UnrecognizedCCodeException {
      return RTTState.KEYWORD_THIS;
    }

    @Override
    public String visit(JRunTimeTypeEqualsType pE) throws UnrecognizedCCodeException {

      JClassOrInterfaceType assignableType = pE.getTypeDef();

      String referenz  = pE.getRunTimeTypeExpression().accept(this);

      if (referenz == null) {
        return null;
      }

      if (truthAssumption == true) {
        newState.assignAssumptionType(referenz, assignableType);
      }

      return null;
    }

  }



  private class ExpressionValueVisitor extends DefaultJExpressionVisitor<String, UnrecognizedCCodeException> implements JRightHandSideVisitor<String, UnrecognizedCCodeException> {

    @SuppressWarnings("unused")
    protected final CFAEdge edge;
    protected final RTTState state;
    protected final String functionName;


    public ExpressionValueVisitor(CFAEdge pEdge, RTTState pElement, String pFunctionName) {
      edge = pEdge;
      state = pElement;
      functionName = pFunctionName;
    }

    @Override
    protected String visitDefault(JExpression pE) {
      return null;
    }

    @Override
    public String visit(JCastExpression pE) throws UnrecognizedCCodeException {

      return pE.getOperand().accept(this);
    }

    @Override
    public String visit(JCharLiteralExpression pPaCharLiteralExpression) throws UnrecognizedCCodeException {
      return "Charackter";
    }

    @Override
    public String visit(JStringLiteralExpression pPaStringLiteralExpression) throws UnrecognizedCCodeException {
      return "String";
    }

    @Override
    public String visit(JBinaryExpression binaryExpression) throws UnrecognizedCCodeException {

      // The only binary Expressions on Class Types is String + which is not yet supported and EnumConstant Assignment
      if ((binaryExpression.getOperator() == BinaryOperator.EQUALS || binaryExpression.getOperator() == BinaryOperator.NOT_EQUALS) && (binaryExpression.getOperand1() instanceof JEnumConstantExpression ||  binaryExpression.getOperand2() instanceof JEnumConstantExpression)) {
        return handleEnumComparison(binaryExpression.getOperand1(), binaryExpression.getOperand2(), binaryExpression.getOperator());
      }

      return null;
    }

    private String handleEnumComparison(JExpression operand1, JExpression operand2, BinaryOperator operator)
        throws UnrecognizedCCodeException {

      String value1 = operand1.accept(this);
      String value2 = operand2.accept(this);

      if (state.getConstantsMap().containsValue(value1)) {
        value1 = state.getRunTimeClassOfUniqueObject(value1);
      }

      if (state.getConstantsMap().containsValue(value2)) {
        value2 = state.getRunTimeClassOfUniqueObject(value2);
      }

      if (value1 == null || value2 == null) {
        return null;
      }

      boolean result = value1.equals(value2);

      switch (operator) {
      case EQUALS:
        break;
      case NOT_EQUALS:
        result = !result;
      }

      return Boolean.toString(result);
    }

    @Override
    public String visit(JArrayCreationExpression pJBooleanLiteralExpression) throws UnrecognizedCCodeException {
      // TODO Support Boolean Class
      return null;
    }

    @Override
    public String visit(JArraySubscriptExpression pAArraySubscriptExpression) throws UnrecognizedCCodeException {
      // TODO Support Arrays
      return null;
    }

    @Override
    public String visit(JVariableRunTimeType vrtT) throws UnrecognizedCCodeException {

      JIdExpression expr = vrtT.getReferencedVariable();

      String uniqueObject = expr.accept(this);

      String runTimeClass = state.getRunTimeClassOfUniqueObject(uniqueObject);

      if (runTimeClass == null) {
        return null;
      } else {
        return runTimeClass;
      }
    }

    @Override
    public String visit(JIdExpression idExpression) throws UnrecognizedCCodeException {

      if (idExpression.getDeclaration() == null) {
        // IDExpression could not be Resolved, return null.
        return null;
      }

      if (idExpression instanceof JFieldAccess) {

        JFieldAccess fiExpr = (JFieldAccess) idExpression;

        JType type = fiExpr.getExpressionType();

        JIdExpression qualifier = fiExpr.getReferencedVariable();

        String uniqueQualifierObject = qualifier.accept(this);

        if (type instanceof JClassOrInterfaceType
            && state.contains(getScopedVariableName(idExpression.getName(), functionName, uniqueQualifierObject))) {
          return state.getUniqueObjectFor(getScopedVariableName(idExpression.getName(), functionName,
              uniqueQualifierObject));
        } else {
          return null;
        }
      } else {

        JType type = idExpression.getExpressionType();

        if (type instanceof JClassOrInterfaceType
            && state.contains(getScopedVariableName(idExpression.getName(), functionName, state.getClassObjectScope()))) {
          return state.getUniqueObjectFor(getScopedVariableName(idExpression.getName(), functionName,
              state.getClassObjectScope()));
        } else {
          return null;
        }
      }
    }

    @Override
    public String visit(JRunTimeTypeEqualsType jRunTimeTypeEqualsType) throws UnrecognizedCCodeException {

      String jrunTimeType = jRunTimeTypeEqualsType.getRunTimeTypeExpression().accept(this);

      if (jrunTimeType == null) {
        return null;
      }

      return Boolean.toString(jRunTimeTypeEqualsType.getTypeDef().getName().equals(jrunTimeType));

    }

    @Override
    public String visit(JClassInstanceCreation jClassInstanzeCreation) throws UnrecognizedCCodeException {
      return jClassInstanzeCreation.getExpressionType().getName();
    }

    @Override
    public String visit(JMethodInvocationExpression pAFunctionCallExpression) throws UnrecognizedCCodeException {
      return null;
    }

    @Override
    public String visit(JThisExpression thisExpression) throws UnrecognizedCCodeException {

      if (state.contains(RTTState.KEYWORD_THIS)) {
        return state.getUniqueObjectFor(RTTState.KEYWORD_THIS);
      } else {
        return null;
      }
    }

    @Override
    public String visit(JNullLiteralExpression pJNullLiteralExpression) throws UnrecognizedCCodeException {
      return RTTState.NULL_REFERENCE;
    }

    @Override
    public String visit(JEnumConstantExpression e) throws UnrecognizedCCodeException {
      return e.getConstantName();
    }

  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState element, List<AbstractState> elements, CFAEdge cfaEdge, Precision precision) throws CPATransferException, InterruptedException {
    return null;
  }

  /**
   * Generates different IDs per Object
   *
   * @return id for object
   */
  public static int nextId() {
    nextFreeId++;
    return nextFreeId;

  }

}