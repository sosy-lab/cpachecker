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

import org.sosy_lab.cpachecker.cfa.ast.AInitializer;
import org.sosy_lab.cpachecker.cfa.ast.java.DefaultJExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayCreationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JAssignment;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.java.JCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JClassInstanceCreation;
import org.sosy_lab.cpachecker.cfa.ast.java.JDeclaration;
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
import org.sosy_lab.cpachecker.cfa.ast.java.JParameterDeclaration;
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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.java.JAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodCallEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JMethodSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.java.JStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.java.JBasicType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassType;
import org.sosy_lab.cpachecker.cfa.types.java.JReferenceType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

import java.util.List;

/**
 * Transfer Relation traversing the CFA and tracking Run Time Type Information
 * of Java Programs.
 */
public class RTTTransferRelation extends ForwardingTransferRelation<RTTState,RTTState,Precision> {

  private static final String NOT_IN_OBJECT_SCOPE = RTTState.NULL_REFERENCE;
  private static final int RETURN_EDGE = 0;

  // variable name for temporary storage of information
  private static final String TEMP_VAR_NAME = "___cpa_temp_result_var_";
  private static final String JAVA_ENUM_OBJECT_NAME = "java.lang.Enum";

  private static int nextFreeId = 0;

  private final static NameProvider nameProvider = NameProvider.getInstance();

  @Override
  protected RTTState handleDeclarationEdge(JDeclarationEdge cfaEdge, JDeclaration declaration)
      throws UnrecognizedCodeException {

    if (!(declaration instanceof JVariableDeclaration)) {
      // nothing interesting to see here, please move along
      return state;
    }

    JVariableDeclaration decl = (JVariableDeclaration) declaration;

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
            return state;
          default:
            // nothing to do here, TODO perhaps throw exceptions in other cases?
      }
    }

    final RTTState newState = RTTState.copyOf(state);

    // variables without initializer are set to null
    // until they are assigned a value
    String initialValue = RTTState.NULL_REFERENCE;

    // handle field variables
    if (decl instanceof JFieldDeclaration) {

      JFieldDeclaration fieldVariable = (JFieldDeclaration) decl;

      // if this is a  field, add to the list of field variables
      newState.addFieldVariable(fieldVariable);
    }

    // get initial value
    AInitializer init = decl.getInitializer();

    if (init instanceof JInitializerExpression) {
      JExpression exp = ((JInitializerExpression) init).getExpression();

      initialValue = getExpressionValue(newState, exp, functionName, cfaEdge);
    }

    // assign initial value
    String scopedVarName = nameProvider.getScopedVariableName(decl, functionName,
        newState.getClassObjectScope());

    if (initialValue == null) {
      newState.forget(scopedVarName);
    } else {
      newState.assignObject(scopedVarName, initialValue);
    }

    return newState;
  }

  /** returns the evaluated expression or NULL if the expression is unknown. */
  private String getExpressionValue(RTTState pState, JExpression expression,
          String methodName, CFAEdge edge) throws UnrecognizedCodeException {
    return expression.accept(
        new ExpressionValueVisitor(edge, pState, methodName));
  }

  @Override
  protected RTTState handleReturnStatementEdge(JReturnStatementEdge cfaEdge)
      throws UnrecognizedCodeException {

    if (cfaEdge.getExpression().isPresent()) {
      JExpression expression = cfaEdge.getExpression().get();

      RTTState newState = RTTState.copyOf(state);
      // In Case Of Class Instance Creation, return unique Object
      final String value;
      if (cfaEdge.getRawAST().get() instanceof JObjectReferenceReturn) {
        value = state.getClassObjectScope();
      } else {
        value = getExpressionValue(state, expression, functionName, cfaEdge);
      }

      final String assignedVar =
          nameProvider.getScopedVariableName(TEMP_VAR_NAME, functionName,
              state.getClassObjectScope(), state);
      if (value == null) {
        newState.forget(assignedVar);
      } else {
        newState.assignObject(assignedVar, value);
      }
      return newState;

    } else {
      return state;
    }
  }

  @Override
  protected RTTState handleStatementEdge(JStatementEdge cfaEdge, JStatement statement)
      throws UnrecognizedCodeException {

    // expression is a binary operation, e.g. a = b;
    if (statement instanceof JAssignment) {
      return handleAssignment((JAssignment) statement, cfaEdge);

      // external function call - do nothing
    } else if (statement instanceof JMethodOrConstructorInvocation) {

      // there is such a case
    } else if (statement instanceof JExpressionStatement) {

    } else {
      throw new UnrecognizedCodeException("unknown statement", cfaEdge, statement);
    }

    return state;
  }

  private RTTState handleAssignment(JAssignment assignExpression, CFAEdge edge)
      throws UnrecognizedCCodeException {

    JExpression op1 = assignExpression.getLeftHandSide();
    JRightHandSide op2 = assignExpression.getRightHandSide();

    if (op1 instanceof JIdExpression) {
      // a = ...

      JSimpleDeclaration declaration = ((JIdExpression) op1).getDeclaration();

      // If declaration could not be resolve, forget variable
      if (declaration == null) {

        String scopedName = nameProvider.getScopedVariableName(
                                            ((JIdExpression) op1).getName(),
                                            functionName,
                                            state.getClassObjectScope(),
                                            state);

        RTTState newState = RTTState.copyOf(state);
        newState.forget(scopedName);
        return newState;
      } else {
        return handleAssignmentToVariable((JIdExpression) op1, op2, edge);
      }
    }

    // we know nothing new, so return old state
    return state;
  }

  /** assigns the evaluated RightHandSide to the LeftHandSide if possible,
   *  or deletes its value. */
  private RTTState handleAssignmentToVariable(JIdExpression lParam, JRightHandSide exp, CFAEdge edge)
      throws UnrecognizedCCodeException {

    String lParamObjectScope = nameProvider.getObjectScope(state, functionName, lParam);
    String value = exp.accept(new ExpressionValueVisitor(edge, state, functionName));
    RTTState newState = RTTState.copyOf(state);

    if (!newState.isKnownAsStatic(lParam.getName()) && lParamObjectScope == null) {
      // can't resolve lParam variable, do nothing
      // TODO How to forget old Values?
      return state;
    }

    String assignedVar = nameProvider.getScopedVariableName(lParam.getDeclaration(),
        functionName, lParamObjectScope);

    if (value != null && (lParam.getExpressionType() instanceof JReferenceType)) {
      newState.assignObject(assignedVar, value);
    } else {
      newState.forget(assignedVar);
    }
    return newState;
  }

  @Override
  protected RTTState handleFunctionReturnEdge(JMethodReturnEdge cfaEdge,
      JMethodSummaryEdge fnkCall, JMethodOrConstructorInvocation summaryExpr,
      String callerFunctionName)
          throws UnrecognizedCodeException {

    RTTState newState  = RTTState.copyOf(state);

    // expression is an assignment operation, e.g. a = g(b);

    if (summaryExpr instanceof JMethodInvocationAssignmentStatement) {
      JMethodInvocationAssignmentStatement assignExp = ((JMethodInvocationAssignmentStatement)summaryExpr);
      JExpression op1 = assignExp.getLeftHandSide();

      // we expect left hand side of the expression to be a variable

      if ((op1 instanceof JIdExpression)) {

        String returnVarName = nameProvider.getScopedVariableName(TEMP_VAR_NAME,
                                                                  functionName,
                                                                  newState.getClassObjectScope(),
                                                                  newState);

        String assignedVarName = nameProvider.getScopedVariableName(((JIdExpression) op1).getDeclaration(),
                                                                    callerFunctionName,
                                                                    newState.getClassObjectStack().peek());


        JSimpleDeclaration decl = ((JIdExpression) op1).getDeclaration();

        //Ignore not reference Types
        if (state.contains(returnVarName) && (decl.getType() instanceof JReferenceType)) {
          newState.assignObject(assignedVarName, state.getUniqueObjectFor(returnVarName));
        } else {
          newState.forget(assignedVarName);
        }
      }
      // a[x] = b(); TODO: for now, nothing is done here, but cloning the current state
      else if (op1 instanceof JArraySubscriptExpression) {

      } else {
        throw new UnrecognizedCodeException("on function return", fnkCall, op1);
      }
    }

    newState.dropFrame(functionName);

    return newState;
  }

  @Override
  protected RTTState handleFunctionCallEdge(JMethodCallEdge cfaEdge,
      List<JExpression> arguments, List<JParameterDeclaration> parameters,
      String calledFunctionName)
      throws UnrecognizedCodeException {

    RTTState newState = RTTState.copyOf(state);

    FunctionEntryNode functionEntryNode = cfaEdge.getSuccessor();
    List<String> paramNames = functionEntryNode.getFunctionParameterNames();

    if (!cfaEdge.getSuccessor().getFunctionDefinition().getType().takesVarArgs()) {
      assert (paramNames.size() == arguments.size());
    }

    // get value of actual parameter in caller function context
    for (int i = 0; i < paramNames.size(); i++) {
      JExpression exp = arguments.get(i);

      // get the Object values of the actual parameters in caller function context
      String value = getExpressionValue(state, exp, functionName, cfaEdge);

      String formalParamName = nameProvider
                                           .getScopedVariableName(paramNames.get(i),
                                                                  calledFunctionName,
                                                                  newState.getClassObjectScope(),
                                                                  newState);

      if (value == null || !(exp.getExpressionType() instanceof JReferenceType)) {
        newState.forget(formalParamName);
      } else {
        newState.assignObject(formalParamName, value);
      }
    }

    JMethodInvocationExpression functionCall =
        cfaEdge.getSummaryEdge().getExpression().getFunctionCallExpression();


    // There are five possibilities when assigning this and the new object Scope.

    // A Object calls its super Constructor
    if (functionCall instanceof JSuperConstructorInvocation) {

      newState.assignThisAndNewObjectScope(state.getUniqueObjectFor(RTTState.KEYWORD_THIS));

   // A New Object is created, which is the new classObject scope
    } else if (functionCall instanceof JClassInstanceCreation) {

      JReturnStatementEdge returnEdge =  (JReturnStatementEdge) functionEntryNode.getExitNode().getEnteringEdge(RETURN_EDGE);
      String uniqueObject = returnEdge.getExpression().get().accept(new FunctionExitValueVisitor(returnEdge, newState, calledFunctionName));
      newState.assignThisAndNewObjectScope(uniqueObject);

      // A Referenced Method Invocation, the new scope is the unique Object
      // of its reference variable
    } else if (functionCall instanceof JReferencedMethodInvocationExpression) {
      JReferencedMethodInvocationExpression objectMethodInvocation = (JReferencedMethodInvocationExpression) functionCall;
      JSimpleDeclaration variableReference = objectMethodInvocation.getReferencedVariable().getDeclaration();

      String variableName = nameProvider
                                        .getScopedVariableName(variableReference, functionName,
                                            newState.getClassObjectScope());

      if (newState.contains(variableName)) {
        newState.assignThisAndNewObjectScope(newState.getUniqueObjectFor(variableName));
      } else {
        // When the object of the variable can't be found
        newState.assignThisAndNewObjectScope(NOT_IN_OBJECT_SCOPE);
      }
      //  a unreferenced Method Invocation
    } else {

      JMethodDeclaration decl = functionCall.getDeclaration();

      // If the method isn't static, the object  scope remains the same
      if (decl.isStatic()) {
        newState.assignThisAndNewObjectScope(NOT_IN_OBJECT_SCOPE);
      } else {
        newState.assignThisAndNewObjectScope(newState.getUniqueObjectFor(RTTState.KEYWORD_THIS));
      }
     //  the method Invocation can't be handled
    }
    return newState;
  }

  @Override
  protected RTTState handleAssumption(JAssumeEdge cfaEdge,
      JExpression expression, boolean truthAssumption)
          throws UnrecognizedCodeException {

    // get the value of the expression (either true[1L], false[0L], or unknown[null])
    String valueString = getExpressionValue(state, expression, functionName, cfaEdge);

    boolean value = Boolean.parseBoolean(valueString);

    RTTState newState = RTTState.copyOf(state);

    // value is null and therefore unknown
    // Investigate if right
    if (valueString == null) {

      AssigningValueVisitor visitor =
          new AssigningValueVisitor(newState, truthAssumption, functionName);

      // Try to derive Information from Assumption.
      expression.accept(visitor);

      return newState;
    } else if ((truthAssumption && value) || (!truthAssumption && !value)) {
      return state;
    } else {
      return null;
    }
  }

  private static class FunctionExitValueVisitor extends ExpressionValueVisitor {

    public FunctionExitValueVisitor(CFAEdge pEdge, RTTState pState, String pFunctionName) {
      super(pEdge, pState, pFunctionName);

    }

    @Override
    public String visit(JThisExpression thisExp) throws UnrecognizedCCodeException {
      return thisExp.getExpressionType().getName();
    }

  }



  private static class AssigningValueVisitor extends DefaultJExpressionVisitor<String, UnrecognizedCCodeException> {

    private final boolean truthAssumption;
    private final RTTState newState; // this state will be changed!
    private final String methodName;


    public AssigningValueVisitor(RTTState pNewState, boolean pTruthAssumption, String pMethodName) {
      truthAssumption = pTruthAssumption;
      newState = pNewState;
      methodName = pMethodName;
    }

    @Override
    public String visit(JVariableRunTimeType pE) throws UnrecognizedCCodeException {
      return nameProvider
          .getScopedVariableName(pE.getReferencedVariable().getDeclaration(), methodName,
                             newState.getKeywordThisUniqueObject());
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

      String reference  = pE.getRunTimeTypeExpression().accept(this);

      if (reference == null) {
        return null;
      }

      if (truthAssumption) {
        newState.assignAssumptionType(reference, assignableType);
      }

      return null;
    }

  }



  /** This visitor evaluates an expression and returns its content. */
  private static class ExpressionValueVisitor extends DefaultJExpressionVisitor<String, UnrecognizedCCodeException> implements JRightHandSideVisitor<String, UnrecognizedCCodeException> {

    protected final CFAEdge edge;
    protected final RTTState state; // only for read-access, do never change this state!
    protected final String functionName;


    public ExpressionValueVisitor(CFAEdge pEdge, RTTState pState, String pFunctionName) {
      edge = pEdge;
      state = pState;
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
      return "Character";
    }

    @Override
    public String visit(JStringLiteralExpression pPaStringLiteralExpression) throws UnrecognizedCCodeException {
      return "String";
    }

    @Override
    public String visit(JBinaryExpression binaryExpression) throws UnrecognizedCCodeException {
      final JExpression leftOperand = binaryExpression.getOperand1();
      final JExpression rightOperand = binaryExpression.getOperand2();

      // The only binary Expressions on Class Types is String + which is not yet supported and
      // object comparison.

      /*
       * For enums, we only have to compare the type of the variable. For 'casual' objects, we
       * have to compare the concrete references.
       */
      if (isObjectComparison(binaryExpression)) {
        if (isEnum((JClassType) leftOperand.getExpressionType())
            || isEnum((JClassType) rightOperand.getExpressionType())) {

          return handleEnumComparison(leftOperand, rightOperand, binaryExpression.getOperator());

        } else {
          return handleObjectComparison(leftOperand, rightOperand, binaryExpression.getOperator());
        }
      }

      return null;
    }

    private boolean isObjectComparison(JBinaryExpression pExpression) {
      final BinaryOperator operator = pExpression.getOperator();
      boolean isComparison = operator == BinaryOperator.EQUALS || operator == BinaryOperator.NOT_EQUALS;

      final JExpression leftOperand = pExpression.getOperand1();
      final JExpression rightOperand = pExpression.getOperand2();
      boolean isObject = leftOperand.getExpressionType() instanceof JClassType
          && rightOperand.getExpressionType() instanceof JClassType;

      return isComparison && isObject;
    }

    private boolean isEnum(JClassType pClassType) {
      List<JClassOrInterfaceType> superTypes = pClassType.getAllSuperTypesOfType();

      for (JClassOrInterfaceType currentType : superTypes) {
        if (currentType.getName().equals(JAVA_ENUM_OBJECT_NAME)) {
          return false;
        }
      }

      return true;
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
        break;
      default:
        throw new UnrecognizedCCodeException("unexpected enum comparison", edge);
      }

      return Boolean.toString(result);
    }

    private String handleObjectComparison(final JExpression pLeftOperand,
        final JExpression pRightOperand, final BinaryOperator pOperator)
        throws UnrecognizedCCodeException {
      String value1 = pLeftOperand.accept(this);
      String value2 = pRightOperand.accept(this);

      boolean result = pOperator == BinaryOperator.NOT_EQUALS ^ value1.equals(value2);
      return Boolean.toString(result);
    }

    @Override
    public String visit(JArrayCreationExpression pJArrayCreationExpression) throws UnrecognizedCCodeException {
      // TODO Support Array Class
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

      JSimpleDeclaration declaration = idExpression.getDeclaration();

      if (idExpression instanceof JFieldAccess) {

        JFieldAccess fiExpr = (JFieldAccess) idExpression;

        JType type = fiExpr.getExpressionType();

        JIdExpression qualifier = fiExpr.getReferencedVariable();

        String uniqueQualifierObject = qualifier.accept(this);
        String variableName =
            nameProvider.getScopedVariableName(declaration, functionName, uniqueQualifierObject);

        if (type instanceof JClassOrInterfaceType
            && state.contains(variableName)) {
          return state.getUniqueObjectFor(variableName);
        } else {
          return null;
        }
      } else {

        JType type = idExpression.getExpressionType();
        String variableName =
            nameProvider.getScopedVariableName(declaration, functionName, state.getClassObjectScope());

        if (type instanceof JClassOrInterfaceType
            && state.contains(variableName)) {
          return state.getUniqueObjectFor(variableName);
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
