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
package org.sosy_lab.cpachecker.cpa.jort;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.AArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.IAssignment;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.Initializer;
import org.sosy_lab.cpachecker.cfa.ast.IAExpression;
import org.sosy_lab.cpachecker.cfa.ast.IARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.IASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IAStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.java.DefaultJExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayCreationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.java.JCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JClassInstanceCreation;
import org.sosy_lab.cpachecker.cfa.ast.java.JEnumConstantExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodInvocationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JNullLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JObjectReferenceReturn;
import org.sosy_lab.cpachecker.cfa.ast.java.JReferencedMethodInvocationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.java.JRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JRunTimeTypeEqualsType;
import org.sosy_lab.cpachecker.cfa.ast.java.JStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JSuperConstructorInvocation;
import org.sosy_lab.cpachecker.cfa.ast.java.JThisExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableRunTimeType;
import org.sosy_lab.cpachecker.cfa.ast.java.JVariableDeclaration;
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
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.java.JBasicType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;


public class JortTransferRelation implements TransferRelation {

  private static final String NOT_IN_OBJECT_SCOPE = JortState.NULL_REFERENCE;
  private static final int RETURN_EDGE = 0;
  private final Set<String> staticFieldVariables = new HashSet<String>();
  private final Set<String> nonStaticFieldVariables = new HashSet<String>();

  private static int  nextFreeId = 0;

 // private String missingInformationLeftVariable  = null;
 // private IARightHandSide missingInformationRightExpression = null;


  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(AbstractState element, Precision precision,
      CFAEdge cfaEdge) throws CPATransferException, InterruptedException {


    JortState jortState     = (JortState)element;

    //jortState.getToBeErased().clear();

    JortState successor;

    switch (cfaEdge.getEdgeType()) {
    // this is an assumption, e.g. if (a == b)
    case AssumeEdge:
      AssumeEdge assumeEdge = (AssumeEdge) cfaEdge;
      successor = handleAssumption(jortState.clone(), assumeEdge.getExpression(), cfaEdge, assumeEdge.getTruthAssumption());
      break;

    case FunctionCallEdge:
      FunctionCallEdge functionCallEdge = (FunctionCallEdge) cfaEdge;
      successor = handleFunctionCall(jortState, functionCallEdge);
      break;

    // this is a return edge from function, this is different from return statement
    // of the function. See case for statement edge for details
    case FunctionReturnEdge:
      FunctionReturnEdge functionReturnEdge = (FunctionReturnEdge) cfaEdge;
      successor = handleFunctionReturn(jortState, functionReturnEdge);

      successor.dropFrame(functionReturnEdge.getPredecessor().getFunctionName());
      break;

    default:
      successor = jortState.clone();
      handleSimpleEdge(successor, cfaEdge);
    }

    if (successor == null) {
      return Collections.emptySet();
    } else {
      return Collections.singleton(successor);
    }



  }

  private void handleSimpleEdge(JortState element, CFAEdge cfaEdge) throws UnrecognizedCFAEdgeException, UnrecognizedCCodeException {

    // check the type of the edge
    switch (cfaEdge.getEdgeType()) {
    // if edge is a statement edge, e.g. a = b + c
    case StatementEdge:
      AStatementEdge statementEdge = (AStatementEdge) cfaEdge;
      handleStatement(element, statementEdge.getStatement(), cfaEdge);
      break;

    case ReturnStatementEdge:
      AReturnStatementEdge returnEdge = (AReturnStatementEdge)cfaEdge;
      // this statement is a function return, e.g. return (a);
      // note that this is different from return edge
      // this is a statement edge which leads the function to the
      // last node of its CFA, where return edge is from that last node
      // to the return site of the caller function
      if(returnEdge.getExpression() != null){
        handleExitFromFunction(element, returnEdge.getExpression(), returnEdge);
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
      for (CFAEdge edge : (MultiEdge)cfaEdge) {
        handleSimpleEdge(element,  edge);
      }
      break;

    default:
      throw new UnrecognizedCFAEdgeException(cfaEdge);
    }

  }

  private void handleDeclaration(JortState newElement, ADeclarationEdge declarationEdge) throws UnrecognizedCCodeException {

    if (!(declarationEdge.getDeclaration() instanceof AVariableDeclaration)) {
      // nothing interesting to see here, please move along
      return;
    }

    JVariableDeclaration decl = (JVariableDeclaration) declarationEdge.getDeclaration();

    if(decl.getType() instanceof JSimpleType ) {

      JBasicType simpleType = ((JSimpleType)decl.getType()).getType();

          switch(simpleType){
          case BOOLEAN:
          case BYTE:
          case CHAR:
          case FLOAT:
          case DOUBLE:
          case INT:
          case LONG:
          case SHORT:
            // TODO Change with inclusion of Boxing, Unboxing
            // Unnecessary to track Primitive types.
            return;
      }

    }

    // get the variable name in the declarator
    String varName = decl.getName();
    String functionName = declarationEdge.getPredecessor().getFunctionName();

    String initialValue = null;

    // handle field variables
    if (decl.isGlobal() && decl instanceof JFieldDeclaration) {

      JFieldDeclaration fieldVariable = (JFieldDeclaration) decl;

      if(fieldVariable.isStatic()){
        // if this is a  field, add to the list of field variables
        staticFieldVariables.add(varName);
      } else {
        nonStaticFieldVariables.add(varName);
      }

      // field variables without initializer are set to null
      initialValue = JortState.NULL_REFERENCE;
    }

    // get initial value
    Initializer init = decl.getInitializer();

    if(init instanceof AInitializerExpression) {
      IAExpression exp = ((AInitializerExpression)init).getExpression();

      initialValue = getExpressionValue(newElement, exp, functionName, declarationEdge);
    }

    // assign initial value
    String scopedVarName = getScopedVariableName(varName, functionName, newElement.getClassObjectScope());

    if (initialValue != null) {
      newElement.assignObject(scopedVarName, initialValue);
    } else {
      // variable References without Objects are null
      newElement.assignObject(scopedVarName, JortState.NULL_REFERENCE);
    }
  }

  private String getExpressionValue(JortState element, IAExpression expression, String functionName,
      CFAEdge edge) throws UnrecognizedCCodeException {
   return ((JRightHandSide) expression).accept(new ExpressionValueVisitor(edge, element, functionName));
  }

  private void handleExitFromFunction(JortState newElement, IAExpression expression, AReturnStatementEdge returnEdge) throws UnrecognizedCCodeException {

    String functionName = returnEdge.getPredecessor().getFunctionName();

    // In Case Of Class Instance Creation, return unique Object
    if(returnEdge.getRawAST().get() instanceof JObjectReferenceReturn) {
      handleAssignmentToVariable("___cpa_temp_result_var_", expression, newElement.getClassObjectScope(), newElement, returnEdge.getSuccessor().getFunctionName());
    } else {
    handleAssignmentToVariable("___cpa_temp_result_var_", expression, new ExpressionValueVisitor(returnEdge, newElement, functionName));
    }
  }

  private void handleAssignmentToVariable(String lParam, IAExpression exp, String value, JortState newElement, String functionName) {

    String assignedVar = getScopedVariableName(lParam, functionName, newElement.getClassObjectScope());

    if (value == null) {
      newElement.forget(assignedVar);
    }

    else {
        newElement.assignObject(assignedVar, value);
    }
  }

  private void handleAssignmentToVariable(String lParam, IAExpression exp,
      ExpressionValueVisitor visitor) throws UnrecognizedCCodeException {

    String value;
    value = ((JRightHandSide) exp).accept(visitor);
    JortState newElement = visitor.state;

   handleAssignmentToVariable(lParam, exp, value, newElement, visitor.functionName);
  }

  private void handleStatement(JortState newElement, IAStatement expression, CFAEdge cfaEdge) throws UnrecognizedCCodeException {
    // expression is a binary operation, e.g. a = b;
    if (expression instanceof IAssignment) {
      handleAssignment(newElement, (IAssignment)expression, cfaEdge);

    // external function call - do nothing
    } else if (expression instanceof AFunctionCallStatement) {

    // there is such a case
    } else if (expression instanceof AExpressionStatement) {

    } else {
      throw new UnrecognizedCCodeException(cfaEdge, expression);
    }
  }

  private void handleAssignment(JortState newElement, IAssignment assignExpression, CFAEdge cfaEdge) throws UnrecognizedCCodeException {

    IAExpression op1    = assignExpression.getLeftHandSide();
    IARightHandSide op2 = assignExpression.getRightHandSide();


    if(op1 instanceof AIdExpression) {
      // a = ...
        String functionName = cfaEdge.getPredecessor().getFunctionName();
        handleAssignmentToVariable(op1.toASTString(), op2, new ExpressionValueVisitor(cfaEdge, newElement, functionName));
    }
  }

  private void handleAssignmentToVariable(String lParam, IARightHandSide exp,
      ExpressionValueVisitor visitor) throws UnrecognizedCCodeException {


    String value = ((JRightHandSide) exp).accept(visitor);

    JortState newElement = visitor.state;
    String assignedVar = getScopedVariableName(lParam, visitor.functionName, newElement.getClassObjectScope());

    if (value == null) {
      newElement.forget(assignedVar);
    }

    else {
        newElement.assignObject(assignedVar, value);
    }
  }

  private JortState handleFunctionReturn(JortState element, FunctionReturnEdge functionReturnEdge) throws UnrecognizedCCodeException {

    FunctionSummaryEdge summaryEdge    = functionReturnEdge.getSummaryEdge();
    AFunctionCall exprOnSummary  = summaryEdge.getExpression();

    JortState newElement  = element.clone();
    String callerFunctionName = functionReturnEdge.getSuccessor().getFunctionName();
    String calledFunctionName = functionReturnEdge.getPredecessor().getFunctionName();

    // expression is an assignment operation, e.g. a = g(b);

    if(exprOnSummary instanceof AFunctionCallAssignmentStatement) {
      AFunctionCallAssignmentStatement assignExp = ((AFunctionCallAssignmentStatement)exprOnSummary);
      IAExpression op1 = assignExp.getLeftHandSide();

      // we expect left hand side of the expression to be a variable

      if((op1 instanceof AIdExpression) || (op1 instanceof CFieldReference)) {
        String returnVarName = getScopedVariableName("___cpa_temp_result_var_", calledFunctionName, newElement.getClassObjectScope());

        String assignedVarName = getScopedVariableName(op1.toASTString(), callerFunctionName, newElement.getClassObjectStack().peek());

        if (element.contains(returnVarName)) {
          newElement.assignObject(assignedVarName, element.getUniqueObjectFor(returnVarName));
        } else {
          newElement.forget(assignedVarName);
        }
      }

      // a[x] = b(); TODO: for now, nothing is done here, but cloning the current element
      else if (op1 instanceof AArraySubscriptExpression) {
        return newElement;
      }

      else {
        throw new UnrecognizedCCodeException("on function return", summaryEdge, op1);
      }
    }

    return newElement;
  }

  private JortState handleFunctionCall(JortState element, FunctionCallEdge callEdge) throws UnrecognizedCCodeException {

    JortState newElement = element.clone();

    FunctionEntryNode functionEntryNode = callEdge.getSuccessor();
    String calledFunctionName = functionEntryNode.getFunctionName();
    String callerFunctionName = callEdge.getPredecessor().getFunctionName();


    List<String> paramNames = functionEntryNode.getFunctionParameterNames();
    List<? extends IAExpression> arguments = callEdge.getArguments();

    if (!callEdge.getSuccessor().getFunctionDefinition().getType().takesVarArgs()) {
      assert(paramNames.size() == arguments.size());
    }

    // visitor for getting the Object values of the actual parameters in caller function context
    ExpressionValueVisitor visitor = new ExpressionValueVisitor(callEdge, element, callerFunctionName);

    // get value of actual parameter in caller function context
    for (int i = 0; i < paramNames.size(); i++) {
      String value = null;
      IAExpression exp = arguments.get(i);

      if(exp instanceof JExpression){
        value = ((JExpression) arguments.get(i)).accept(visitor);
      }


      String formalParamName = getScopedVariableName(paramNames.get(i), calledFunctionName, newElement.getClassObjectScope());

      if (value == null) {
        newElement.forget(formalParamName);
      } else {
        newElement.assignObject(formalParamName, value);
      }
    }

    AFunctionCallExpression functionCall = callEdge.getSummaryEdge().getExpression().getFunctionCallExpression();


    // There are five possibilities when assigning this and the new object Scope.

    // A Object calls its super Constructor
    if(functionCall instanceof JSuperConstructorInvocation) {

      newElement.assignThisAndNewObjectScope(element.getUniqueObjectFor(JortState.KEYWORD_THIS));

   // A New Object is created, which is the new classObject scope
    } else if(functionCall instanceof JClassInstanceCreation) {



      AReturnStatementEdge returnEdge =  (AReturnStatementEdge) functionEntryNode.getExitNode().getEnteringEdge(RETURN_EDGE);
      String uniqueObject = ((JExpression) returnEdge.getExpression()).accept(  new FunctionExitValueVisitor(returnEdge, newElement, calledFunctionName));
      newElement.assignThisAndNewObjectScope(uniqueObject);

      // A Referenced Method Invocation, the new scope is the unique Object
      // of its reference variable
    } else if(functionCall instanceof JReferencedMethodInvocationExpression) {
      JReferencedMethodInvocationExpression objectMethodInvocation = (JReferencedMethodInvocationExpression) functionCall;
      IASimpleDeclaration variableReference = objectMethodInvocation.getReferencedVariable().getDeclaration();

      if( newElement.contains(getScopedVariableName(variableReference.getName(), callerFunctionName, newElement.getClassObjectScope()))){
        newElement.assignThisAndNewObjectScope( newElement.getUniqueObjectFor(getScopedVariableName(variableReference.getName(), callerFunctionName, newElement.getClassObjectScope())));
      } else {
        // When the object of the variable can't be found
        newElement.assignThisAndNewObjectScope(NOT_IN_OBJECT_SCOPE);
      }
      //  a unreferenced Method Invocation
    } else if (functionCall instanceof JMethodInvocationExpression) {

      JMethodDeclaration decl = ((JMethodInvocationExpression)functionCall).getDeclaration();

      // If the method isn't static, the object  scope remains the same
      if(decl.isStatic()) {
        newElement.assignThisAndNewObjectScope(NOT_IN_OBJECT_SCOPE);
      }else {
       newElement.assignThisAndNewObjectScope(newElement.getUniqueObjectFor(JortState.KEYWORD_THIS));
      }
     //  the method Invocation can't be handled
    } else {
      newElement.assignThisAndNewObjectScope(NOT_IN_OBJECT_SCOPE);
    }
    return newElement;
  }

  private String getScopedVariableName(String variableName, String functionName, String uniqueObject) {

    if(variableName.equals(JortState.KEYWORD_THIS)) {
      return variableName;
    }

    if (staticFieldVariables.contains(variableName)) {
      return variableName;
    }

    if( nonStaticFieldVariables.contains(variableName)){
      //TODO Exception for "" as ObjectScope
      return uniqueObject + "::" + variableName;
    }

    return functionName + "::" + variableName;
  }


  private JortState handleAssumption(JortState element, IAExpression expression, CFAEdge cfaEdge,
      boolean truthAssumption) throws UnrecognizedCCodeException {

    String functionName = cfaEdge.getPredecessor().getFunctionName();

    // get the value of the expression (either true[1L], false[0L], or unknown[null])
    String valueString = getExpressionValue(element, expression, functionName, cfaEdge);

    boolean value = Boolean.parseBoolean(valueString);

    // value is null and therefore unknown
    // Investigate if right
    if (valueString == null) {

      return element;
    } else if ((truthAssumption && value) || (!truthAssumption && !value)) {
      return element;
    } else {
      return null;
    }
  }

  private class FunctionExitValueVisitor extends ExpressionValueVisitor {

    public FunctionExitValueVisitor(CFAEdge pEdge, JortState pElement, String pFunctionName) {
      super(pEdge, pElement, pFunctionName);

    }

    @Override
    public String visit(JStringLiteralExpression functionTypeReturn) throws UnrecognizedCCodeException {
      return functionTypeReturn.getValue();
    }


  }

  private class ExpressionValueVisitor extends DefaultJExpressionVisitor<String, UnrecognizedCCodeException> implements JRightHandSideVisitor<String, UnrecognizedCCodeException>{


    //TODO FieldAccess needs  to be read

    @SuppressWarnings("unused")
    protected final CFAEdge edge;
    protected final JortState state;
    protected final String functionName;


    public ExpressionValueVisitor(CFAEdge pEdge, JortState pElement, String pFunctionName) {
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

      if((binaryExpression.getOperator() == BinaryOperator.EQUALS || binaryExpression.getOperator() == BinaryOperator.NOT_EQUALS) && (binaryExpression.getOperand1() instanceof JEnumConstantExpression ||  binaryExpression.getOperand2() instanceof JEnumConstantExpression)) {
        return handleEnumComparison(binaryExpression.getOperand1() , binaryExpression.getOperand2() , binaryExpression.getOperator());
      }
      return null;
    }

    private String handleEnumComparison(JExpression operand1, JExpression operand2, BinaryOperator operator) throws UnrecognizedCCodeException {

      String value1 = operand1.accept(this);
      String value2 = operand2.accept(this);

      if(state.getConstantsMap().containsValue(value1)) {
        value1 = state.getRunTimeClassOfUniqueObject(value1);
      }

      if(state.getConstantsMap().containsValue(value2)) {
        value2 = state.getRunTimeClassOfUniqueObject(value2);
      }

      boolean result = value1.equals(value2);

      switch(operator){
      case EQUALS:   break;
      case NOT_EQUALS: result = !result;
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
    public String visit(JVariableRunTimeType pJThisRunTimeType) throws UnrecognizedCCodeException {
      if(state.contains(getScopedVariableName(pJThisRunTimeType.getReferencedVariable().getName(), functionName, state.getClassObjectScope()))) {
       return state.getRunTimeClassFor( getScopedVariableName(pJThisRunTimeType.getReferencedVariable().getName(), functionName, state.getClassObjectScope()));
      } else {
       return null;
      }
    }

    @Override
    public String visit(JIdExpression idExpression) throws UnrecognizedCCodeException {

      //TODO Referenced Field Access...

       IASimpleDeclaration variable = idExpression.getDeclaration();

       Type type = variable.getType();

      if(type instanceof JClassOrInterfaceType && state.contains(getScopedVariableName(idExpression.getName() , functionName, state.getClassObjectScope()))){
        return state.getUniqueObjectFor( getScopedVariableName(idExpression.getName(), functionName, state.getClassObjectScope()));
      } else {
        return null;
      }
    }

    @Override
    public String visit(JRunTimeTypeEqualsType jRunTimeTypeEqualsType) throws UnrecognizedCCodeException {

      String jrunTimeType = jRunTimeTypeEqualsType.getRunTimeTypeExpression().accept(this);

      return Boolean.toString(jrunTimeType != null && jRunTimeTypeEqualsType.getTypeDef().getName().equals(jrunTimeType));

    }

    @Override
    public String visit(JClassInstanceCreation jClassInstanzeCreation) throws UnrecognizedCCodeException {
      return jClassInstanzeCreation.getExpressionType().getReturnType().getName();
    }

    @Override
    public String visit(JMethodInvocationExpression pAFunctionCallExpression) throws UnrecognizedCCodeException {
      return null;
    }

    @Override
    public String visit(JThisExpression thisExpression) {

      if(state.contains(JortState.KEYWORD_THIS)) {
        return state.getUniqueObjectFor(JortState.KEYWORD_THIS);
      } else {
        return null;
      }

    }

    @Override
    public String visit(JNullLiteralExpression pJNullLiteralExpression) throws UnrecognizedCCodeException {
      return JortState.NULL_REFERENCE;
    }

    @Override
    public String visit(JEnumConstantExpression e) throws UnrecognizedCCodeException {

      return e.getValue();
    }

  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState element, List<AbstractState> elements, CFAEdge cfaEdge, Precision precision) throws CPATransferException, InterruptedException {
    return null;
  }

  public static int nextId(){
    nextFreeId++;
    return nextFreeId;

  }

}