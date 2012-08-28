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
package org.sosy_lab.cpachecker.cpa.explicit;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.AArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.ABinaryExpression.ABinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.ACharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.IAExpression;
import org.sosy_lab.cpachecker.cfa.ast.IARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.IASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IAStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayCreationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JArrayInitializer;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JBooleanLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JClassInstanzeCreation;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldAccess;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JMethodInvocationExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JNullLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.java.JRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JRunTimeTypeEqualsType;
import org.sosy_lab.cpachecker.cfa.ast.java.JThisRunTimeType;
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
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.java.JArrayType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.jort.JortState;
import org.sosy_lab.cpachecker.cpa.pointer.Memory;
import org.sosy_lab.cpachecker.cpa.pointer.Pointer;
import org.sosy_lab.cpachecker.cpa.pointer.PointerState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;

@Options(prefix="cpa.explicit")
public class ExplicitTransferRelation implements TransferRelation
{

  @Option(description = "if there is an assumption like (x!=0), "
      + "this option sets unknown (uninitialized) variables to 1L, "
      + "when the true-branch is handled.")
  private boolean initAssumptionVars = false;

  private final Set<String> globalVariables = new HashSet<String>();

  private final Set<String> javaNonStaticVariables = new HashSet<String>();

  private String missingInformationLeftVariable = null;
  private String missingInformationLeftPointer  = null;

  private IARightHandSide missingInformationRightExpression = null;

  private ExplicitPrecision currentPrecision = null;


  private JRightHandSide missingInformationRightJExpression = null;
  private String missingInformationLeftJVariable = null;

  private boolean missingFieldVariableObject;
  private Pair<String, Long> fieldNameAndInitialValue;

  private boolean missingScopedFieldName;
  private JIdExpression notScopedField;
  private Long notScopedFieldValue;

  private boolean missingAssumeInformation;




  public ExplicitTransferRelation(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  @Override
  public Collection<ExplicitState> getAbstractSuccessors(AbstractState element, Precision pPrecision, CFAEdge cfaEdge)
    throws CPATransferException {

    ExplicitState explicitState     = (ExplicitState)element;
    ExplicitPrecision explicitPrecision = (ExplicitPrecision)pPrecision;

    currentPrecision = explicitPrecision;
    currentPrecision.setLocation(cfaEdge.getSuccessor());

    ExplicitState successor;

    switch (cfaEdge.getEdgeType()) {
    // this is an assumption, e.g. if (a == b)
    case AssumeEdge:
      AssumeEdge assumeEdge = (AssumeEdge) cfaEdge;
      successor = handleAssumption(explicitState.clone(), assumeEdge.getExpression(), cfaEdge, assumeEdge.getTruthAssumption(), explicitPrecision);
      break;

    case FunctionCallEdge:
      FunctionCallEdge functionCallEdge = (FunctionCallEdge) cfaEdge;
      successor = handleFunctionCall(explicitState, functionCallEdge);
      break;

    // this is a return edge from function, this is different from return statement
    // of the function. See case for statement edge for details
    case FunctionReturnEdge:
      FunctionReturnEdge functionReturnEdge = (FunctionReturnEdge) cfaEdge;
      successor = handleFunctionReturn(explicitState, functionReturnEdge);

      successor.dropFrame(functionReturnEdge.getPredecessor().getFunctionName());
      break;

    default:
      successor = explicitState.clone();
      handleSimpleEdge(successor, explicitPrecision, cfaEdge);
    }

    if (successor == null) {
      return Collections.emptySet();
    } else {
      return Collections.singleton(successor);
    }
  }

  private void handleSimpleEdge(ExplicitState element, ExplicitPrecision precision, CFAEdge cfaEdge)
        throws CPATransferException {

    // let the precision know the current location
    currentPrecision.setLocation(cfaEdge.getSuccessor());

    // check the type of the edge
    switch (cfaEdge.getEdgeType()) {
    // if edge is a statement edge, e.g. a = b + c
    case StatementEdge:
      AStatementEdge statementEdge = (AStatementEdge) cfaEdge;
      handleStatement(element, statementEdge.getStatement(), cfaEdge, precision);
      break;

    case ReturnStatementEdge:
      AReturnStatementEdge returnEdge = (AReturnStatementEdge)cfaEdge;
      // this statement is a function return, e.g. return (a);
      // note that this is different from return edge
      // this is a statement edge which leads the function to the
      // last node of its CFA, where return edge is from that last node
      // to the return site of the caller function
      handleExitFromFunction(element, returnEdge.getExpression(), returnEdge);
      break;

    // edge is a declaration edge, e.g. int a;
    case DeclarationEdge:
      ADeclarationEdge declarationEdge = (ADeclarationEdge) cfaEdge;
      handleDeclaration(element, declarationEdge, precision);
      break;

    case BlankEdge:
      break;

    case MultiEdge:
      for (CFAEdge edge : (MultiEdge)cfaEdge) {
        handleSimpleEdge(element, precision, edge);
      }
      break;

    default:
      throw new UnrecognizedCFAEdgeException(cfaEdge);
    }
  }

  private ExplicitState handleFunctionCall(ExplicitState element, FunctionCallEdge callEdge)
    throws UnrecognizedCCodeException {
    ExplicitState newElement = element.clone();

    FunctionEntryNode functionEntryNode = callEdge.getSuccessor();
    String calledFunctionName = functionEntryNode.getFunctionName();
    String callerFunctionName = callEdge.getPredecessor().getFunctionName();

    List<String> paramNames = functionEntryNode.getFunctionParameterNames();
    List<? extends IAExpression> arguments = callEdge.getArguments();

    if (!callEdge.getSuccessor().getFunctionDefinition().getType().takesVarArgs()) {
      assert(paramNames.size() == arguments.size());
    }



    // visitor for getting the values of the actual parameters in caller function context
    ExpressionValueVisitor visitor = new ExpressionValueVisitor(callEdge, element, callerFunctionName);

    // get value of actual parameter in caller function context
    for (int i = 0; i < paramNames.size(); i++) {
      Long value;
      IAExpression exp = arguments.get(i);


      if(exp instanceof JExpression){
        value = ((JExpression) arguments.get(i)).accept(visitor);
      } else {
        value = ((CExpression) arguments.get(i)).accept(visitor);
      }


      String formalParamName = getScopedVariableName(paramNames.get(i), calledFunctionName);

      if (value == null) {
        newElement.forget(formalParamName);
      } else {
        newElement.assignConstant(formalParamName, value);
      }
    }

    return newElement;
  }

  private void handleExitFromFunction(ExplicitState newElement, IAExpression expression, AReturnStatementEdge returnEdge)
    throws UnrecognizedCCodeException {
    if (expression == null) {
      expression = CNumericTypes.ZERO; // this is the default in C
    }

    String functionName = returnEdge.getPredecessor().getFunctionName();




    handleAssignmentToVariable("___cpa_temp_result_var_", expression, new ExpressionValueVisitor(returnEdge, newElement, functionName));
  }

  /**
   * Handles return from one function to another function.
   * @param element previous abstract state
   * @param functionReturnEdge return edge from a function to its call site
   * @return new abstract state
   */
  private ExplicitState handleFunctionReturn(ExplicitState element, FunctionReturnEdge functionReturnEdge)
    throws UnrecognizedCCodeException {

    FunctionSummaryEdge summaryEdge    = functionReturnEdge.getSummaryEdge();
    AFunctionCall exprOnSummary  = summaryEdge.getExpression();

    ExplicitState newElement  = element.clone();
    String callerFunctionName = functionReturnEdge.getSuccessor().getFunctionName();
    String calledFunctionName = functionReturnEdge.getPredecessor().getFunctionName();

    // expression is an assignment operation, e.g. a = g(b);

    if(exprOnSummary instanceof AFunctionCallAssignmentStatement) {
      AFunctionCallAssignmentStatement assignExp = ((AFunctionCallAssignmentStatement)exprOnSummary);
      IAExpression op1 = assignExp.getLeftHandSide();

      // we expect left hand side of the expression to be a variable

      if((op1 instanceof AIdExpression) || (op1 instanceof CFieldReference)) {
        String returnVarName = getScopedVariableName("___cpa_temp_result_var_", calledFunctionName);

        String assignedVarName = getScopedVariableName(op1.toASTString(), callerFunctionName);

        if(!element.contains(returnVarName) || !currentPrecision.isTracking(assignedVarName)) {
          newElement.forget(assignedVarName);
        } else if(op1 instanceof JIdExpression && ((JIdExpression) op1).getDeclaration() instanceof JFieldDeclaration && !((JFieldDeclaration) ((JIdExpression) op1).getDeclaration()).isStatic()) {
          missingScopedFieldName = true;
          notScopedField = (JIdExpression) op1;
          notScopedFieldValue = element.getValueFor(returnVarName);

        } else {
          newElement.assignConstant(assignedVarName, element.getValueFor(returnVarName));
        }
      }

      // a* = b(); TODO: for now, nothing is done here, but cloning the current element
      else if(op1 instanceof AUnaryExpression && ((AUnaryExpression)op1).getOperator() == UnaryOperator.STAR) {
        return newElement;
      }

      // a[x] = b(); TODO: for now, nothing is done here, but cloning the current element
      else if (op1 instanceof CArraySubscriptExpression) {
        return newElement;
      }

      else {
        throw new UnrecognizedCCodeException("on function return", summaryEdge, op1);
      }
    }

    return newElement;
  }

  private ExplicitState handleAssumption(ExplicitState element, IAExpression expression, CFAEdge cfaEdge, boolean truthValue, ExplicitPrecision precision)
    throws UnrecognizedCCodeException {
    // convert an expression like [a + 753 != 951] to [a != 951 - 753]
    expression = optimizeAssumeForEvaluation(expression);

    String functionName = cfaEdge.getPredecessor().getFunctionName();

    // get the value of the expression (either true[1L], false[0L], or unknown[null])
    Long value = getExpressionValue(element, expression, functionName, cfaEdge);


    // value is null, try to derive further information
    if (value == null) {
      AssigningValueVisitor avv = new AssigningValueVisitor(cfaEdge, element, functionName, truthValue);

      if(expression instanceof JExpression && ! (expression instanceof CExpression)){
        ((JExpression) expression).accept(avv);

        if(avv.missingFieldAccessInformation) {
          missingInformationRightJExpression = (JRightHandSide) expression;
          missingAssumeInformation = true;
        }

      } else {
        ((CExpression)expression).accept(avv);
      }




      return element;
    } else if ((truthValue && value == 1L) || (!truthValue && value == 0L)) {
      return element;
    } else {
      return null;
    }
  }

  private void handleDeclaration(ExplicitState newElement, ADeclarationEdge declarationEdge, ExplicitPrecision precision)
    throws UnrecognizedCCodeException {

    if (!(declarationEdge.getDeclaration() instanceof AVariableDeclaration)) {
      // nothing interesting to see here, please move along
      return;
    }

    AVariableDeclaration decl = (AVariableDeclaration)declarationEdge.getDeclaration();

    // get the variable name in the declarator
    String varName = decl.getName();
    String functionName = declarationEdge.getPredecessor().getFunctionName();

    Long initialValue = null;

    // handle global variables
    if (decl.isGlobal()) {
      // if this is a global variable, add to the list of global variables
      globalVariables.add(varName);



      if(decl instanceof JFieldDeclaration && !((JFieldDeclaration)decl).isStatic()){
        missingFieldVariableObject = true;
        javaNonStaticVariables.add(varName);
      }

      // global variables without initializer are set to 0 in C
      initialValue = 0L;
    }

    // get initial value
    CInitializer init = decl.getInitializer();

    if(init instanceof AInitializerExpression) {
      IAExpression exp = ((AInitializerExpression)init).getExpression();

        initialValue = getExpressionValue(newElement, exp, functionName, declarationEdge);

    }

      String scopedVarName = getScopedVariableName(varName, functionName);

      boolean complexType = decl.getType() instanceof JClassOrInterfaceType || decl.getType() instanceof JArrayType;

   // assign initial value if necessary
    if (precision.isTracking(scopedVarName) && !complexType  && (missingInformationRightJExpression != null || initialValue != null)) {
      if (missingFieldVariableObject) {
        fieldNameAndInitialValue = Pair.of(varName, initialValue);
      } else if(missingInformationRightJExpression == null) {
        newElement.assignConstant(scopedVarName, initialValue);
      } else {
        missingInformationLeftJVariable = scopedVarName;
      }
    } else {
      // If variable not tracked, its Object is irrelevant
      missingFieldVariableObject = false;
      newElement.forget(scopedVarName);
    }
  }

  private void handleStatement(ExplicitState newElement, IAStatement expression, CFAEdge cfaEdge, ExplicitPrecision precision)
    throws UnrecognizedCCodeException {
    // expression is a binary operation, e.g. a = b;
    if (expression instanceof AAssignment) {
      handleAssignment(newElement, (AAssignment)expression, cfaEdge, precision);

    // external function call - do nothing
    } else if (expression instanceof AFunctionCallStatement) {

    // there is such a case
    } else if (expression instanceof AExpressionStatement) {

    } else {
      throw new UnrecognizedCCodeException(cfaEdge, expression);
    }
  }

  private void handleAssignment(ExplicitState newElement, AAssignment assignExpression, CFAEdge cfaEdge, ExplicitPrecision precision)
    throws UnrecognizedCCodeException {
    IAExpression op1    = assignExpression.getLeftHandSide();
    IARightHandSide op2 = assignExpression.getRightHandSide();


    if(op1 instanceof AIdExpression) {
      // a = ...
      if (!precision.isOnBlacklist(getScopedVariableName(((AIdExpression)op1).getName(), cfaEdge.getPredecessor().getFunctionName()))) {
        String functionName = cfaEdge.getPredecessor().getFunctionName();

        if(op1 instanceof JIdExpression && ((JIdExpression) op1).getDeclaration() instanceof JFieldDeclaration && !((JFieldDeclaration) ((JIdExpression) op1).getDeclaration()).isStatic()) {
          missingScopedFieldName = true;
          notScopedField = (JIdExpression) op1;
        }

        handleAssignmentToVariable(op1.toASTString(), op2, new ExpressionValueVisitor(cfaEdge, newElement, functionName));
      }
    }


    else if(op1 instanceof AUnaryExpression && ((AUnaryExpression)op1).getOperator() == UnaryOperator.STAR) {
      // *a = ...

      op1 = ((AUnaryExpression)op1).getOperand();

      // Cil produces code like
      // *((int*)__cil_tmp5) = 1;
      // so remove cast
      if (op1 instanceof CCastExpression) {
        op1 = ((CCastExpression)op1).getOperand();
      }


      if(op1 instanceof AIdExpression) {
        missingInformationLeftPointer = ((AIdExpression)op1).getName();
        missingInformationRightExpression = op2;
      }

      return;

    }

    else if (op1 instanceof CFieldReference) {
      // a->b = ...
      if (precision.isOnBlacklist(getScopedVariableName(op1.toASTString(),cfaEdge.getPredecessor().getFunctionName()))) {
        return;
      } else {
        String functionName = cfaEdge.getPredecessor().getFunctionName();

        handleAssignmentToVariable(op1.toASTString(), op2, new ExpressionValueVisitor(cfaEdge, newElement, functionName));
      }
    }

    // TODO assignment to array cell
    else if (op1 instanceof CArraySubscriptExpression || op1 instanceof AArraySubscriptExpression) {

    } else {
      throw new UnrecognizedCCodeException("left operand of assignment has to be a variable", cfaEdge, op1);
    }
  }

  private void handleAssignmentToVariable(String lParam, IARightHandSide exp, ExpressionValueVisitor visitor)
    throws UnrecognizedCCodeException {


    Long value;

    if(exp instanceof JRightHandSide && !(exp instanceof CRightHandSide )){
      // TODO Here might be missin
       value = ((JRightHandSide) exp).accept(visitor);
    }else {
       value = ((CRightHandSide) exp).accept(visitor);
    }



    if(visitor.missingPointer) {
      missingInformationRightExpression = exp;
      assert value == null;
    }


    ExplicitState newElement = visitor.state;
    String assignedVar = getScopedVariableName(lParam, visitor.functionName);

    if(visitor.missingFieldAccessInformation) {
      // This may happen if an object of class is created which could not be parsed,
      // In  such a case, forget about it
      if(value != null) {
        newElement.forget(lParam);
        return;
      } else {
        missingInformationRightJExpression =  (JRightHandSide) exp;
        if(!missingScopedFieldName) {
          missingInformationLeftJVariable = assignedVar;
        }
      }
    }

    if(missingScopedFieldName){
      notScopedFieldValue = value;
    }

    else {
      if (value == null) {
        newElement.forget(assignedVar);
      }

      else {
        if (currentPrecision.isTracking(assignedVar) || assignedVar.endsWith("___cpa_temp_result_var_")) {
          newElement.assignConstant(assignedVar, value);
        }
        else {
          newElement.forget(assignedVar);
        }
      }
    }
  }

  /**
   * Visitor that get's the value from an expression.
   * The result may be null, i.e., the value is unknown.
   */
  private class ExpressionValueVisitor extends DefaultCExpressionVisitor<Long, UnrecognizedCCodeException>
                                       implements CRightHandSideVisitor<Long, UnrecognizedCCodeException>
                                       , JRightHandSideVisitor<Long, UnrecognizedCCodeException>
                                       , JExpressionVisitor<Long, UnrecognizedCCodeException>  {
    protected final CFAEdge edge;
    protected final ExplicitState state;
    protected final String functionName;

    private boolean missingPointer = false;
    protected boolean missingFieldAccessInformation = false;

    public ExpressionValueVisitor(CFAEdge pEdge, ExplicitState pElement, String pFunctionName) {
      edge = pEdge;
      state = pElement;
      functionName = pFunctionName;
    }

    @Override
    protected Long visitDefault(CExpression pExp) {
      return null;
    }

    @Override
    public Long visit(CBinaryExpression pE) throws UnrecognizedCCodeException {
      BinaryOperator binaryOperator = pE.getOperator();
      CExpression lVarInBinaryExp = pE.getOperand1();
      CExpression rVarInBinaryExp = pE.getOperand2();

      switch (binaryOperator) {
      case PLUS:
      case MINUS:
      case DIVIDE:
      case MULTIPLY:
      case SHIFT_LEFT:
      case BINARY_AND:
      case BINARY_OR:
      case BINARY_XOR: {
        Long lVal = lVarInBinaryExp.accept(this);
        if (lVal == null) {
          return null;
        }

        Long rVal = rVarInBinaryExp.accept(this);
        if (rVal == null) {
          return null;
        }

        switch (binaryOperator) {
        case PLUS:
          return lVal + rVal;

        case MINUS:
          return lVal - rVal;

        case DIVIDE:
          // TODO maybe we should signal a division by zero error?
          if (rVal == 0) {
            return null;
          }

          return lVal / rVal;

        case MULTIPLY:
          return lVal * rVal;

        case SHIFT_LEFT:
          return lVal << rVal;

        case BINARY_AND:
          return lVal & rVal;

        case BINARY_OR:
          return lVal | rVal;

        case BINARY_XOR:
          return lVal ^ rVal;

        default:
          throw new AssertionError();
        }
      }

      case EQUALS:
      case NOT_EQUALS:
      case GREATER_THAN:
      case GREATER_EQUAL:
      case LESS_THAN:
      case LESS_EQUAL: {

        Long lVal = lVarInBinaryExp.accept(this);
        if (lVal == null) {
          return null;
        }

        Long rVal = rVarInBinaryExp.accept(this);
        if (rVal == null) {
          return null;
        }

        long l = lVal;
        long r = rVal;

        boolean result;
        switch (binaryOperator) {
        case EQUALS:
          result = (l == r);
          break;
        case NOT_EQUALS:
          result = (l != r);
          break;
        case GREATER_THAN:
          result = (l > r);
          break;
        case GREATER_EQUAL:
          result = (l >= r);
          break;
        case LESS_THAN:
          result = (l < r);
          break;
        case LESS_EQUAL:
          result = (l <= r);
          break;

        default:
          throw new AssertionError();
        }

        // return 1 if expression holds, 0 otherwise
        return (result ? 1L : 0L);
      }

      case MODULO:
      case SHIFT_RIGHT:
      default:
        // TODO check which cases can be handled (I think all)
        return null;
      }
    }

    @Override
    public Long visit(CCastExpression pE) throws UnrecognizedCCodeException {
      return pE.getOperand().accept(this);
    }

    @Override
    public Long visit(CFunctionCallExpression pIastFunctionCallExpression) throws UnrecognizedCCodeException {
      return null;
    }

    @Override
    public Long visit(CCharLiteralExpression pE) throws UnrecognizedCCodeException {
      return (long)pE.getCharacter();
    }

    @Override
    public Long visit(CFloatLiteralExpression pE) throws UnrecognizedCCodeException {
      return null;
    }

    @Override
    public Long visit(CIntegerLiteralExpression pE) throws UnrecognizedCCodeException {
      return pE.asLong();
    }

    @Override
    public Long visit(CStringLiteralExpression pE) throws UnrecognizedCCodeException {
      return null;
    }

    @Override
    public Long visit(CIdExpression idExp) throws UnrecognizedCCodeException {
      if (idExp.getDeclaration() instanceof CEnumerator) {
        CEnumerator enumerator = (CEnumerator)idExp.getDeclaration();
        if (enumerator.hasValue()) {
          return enumerator.getValue();
        } else {
          return null;
        }
      }

      String varName = getScopedVariableName(idExp.getName(), functionName);

      if (state.contains(varName)) {
        return state.getValueFor(varName);
      } else {
        return null;
      }
    }

    @Override
    public Long visit(CUnaryExpression unaryExpression) throws UnrecognizedCCodeException {
      UnaryOperator unaryOperator = unaryExpression.getOperator();
      CExpression unaryOperand = unaryExpression.getOperand();

      Long value = null;

      switch (unaryOperator) {
      case MINUS:
        value = unaryOperand.accept(this);
        return (value != null) ? -value : null;

      case NOT:
        value = unaryOperand.accept(this);

        if (value == null) {
          return null;
        } else {
          return (value == 0L) ? 1L : 0L;
        }

      case AMPER:
        return null; // valid expression, but it's a pointer value

      case STAR:
        missingPointer = true;
        return null;

      case SIZEOF:
      case TILDE:
      default:
        // TODO handle unimplemented operators
        return null;
      }
    }

    @Override
    public Long visit(CFieldReference fieldReferenceExpression) throws UnrecognizedCCodeException {
      String varName = getScopedVariableName(fieldReferenceExpression.toASTString(), functionName);

      if (state.contains(varName)) {
        return state.getValueFor(varName);
      } else {
        return null;
      }
    }

    @Override
    public Long visit(ACharLiteralExpression pE) throws UnrecognizedCCodeException {
      return (long)pE.getCharacter();
    }

    @Override
    public Long visit(AStringLiteralExpression pPaStringLiteralExpression) throws UnrecognizedCCodeException {
      return null;
    }

    @Override
    public Long visit(JBinaryExpression pE) throws UnrecognizedCCodeException {

      org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression.BinaryOperator binaryOperator = pE.getOperator();
      IAExpression lVarInBinaryExp = pE.getOperand1();
      IAExpression rVarInBinaryExp = pE.getOperand2();

      switch(binaryOperator) {
      case PLUS:
      case MINUS:
      case DIVIDE:
      case MULTIPLY:
      case SHIFT_LEFT:
      case BINARY_AND:
      case BINARY_OR:
      case BINARY_XOR: {
        Long lVal =    ((JExpression)lVarInBinaryExp).accept(this);
        if(lVal == null) {
          return null;
        }

        Long rVal = ((JExpression)rVarInBinaryExp).accept(this);
        if(rVal == null) {
          return null;
        }

        switch(binaryOperator) {
        case PLUS:
          return lVal + rVal;

        case MINUS:
          return lVal - rVal;

        case DIVIDE:
          // TODO maybe we should signal a division by zero error?
          if(rVal == 0) {
            return null;
          }

          return lVal / rVal;

        case MULTIPLY:
          return lVal * rVal;

        case SHIFT_LEFT:
          return lVal << rVal;

        case BINARY_AND:
          return lVal & rVal;

        case BINARY_OR:
          return lVal | rVal;

        case BINARY_XOR:
          return lVal ^ rVal;

        default:
          throw new AssertionError();
        }
      }

      case EQUALS:
      case NOT_EQUALS:
      case GREATER_THAN:
      case GREATER_EQUAL:
      case LESS_THAN:
      case LESS_EQUAL: {

        Long lVal = ((JExpression)lVarInBinaryExp).accept(this);
        if(lVal == null)
          return null;

        Long rVal = ((JExpression)rVarInBinaryExp).accept(this);
        if(rVal == null)
          return null;

        long l = lVal;
        long r = rVal;

        boolean result;
        switch(binaryOperator) {
        case EQUALS:
          result = (l == r);
          break;
        case NOT_EQUALS:
          result = (l != r);
          break;
        case GREATER_THAN:
          result = (l > r);
          break;
        case GREATER_EQUAL:
          result = (l >= r);
          break;
        case LESS_THAN:
          result = (l < r);
          break;
        case LESS_EQUAL:
          result = (l <= r);
          break;

        default:
          throw new AssertionError();
        }

        // return 1 if expression holds, 0 otherwise
        return (result ? 1L : 0L);
      }
      case MODULO:
      case SHIFT_RIGHT_SIGNED:
      case SHIFT_RIGHT_UNSIGNED:
      default:
        // TODO check which cases can be handled
        return null;
      }
    }

    @Override
    public Long visit(JIdExpression idExp) throws UnrecognizedCCodeException {


      IASimpleDeclaration decl = idExp.getDeclaration();

      if(decl instanceof JFieldDeclaration && !((JFieldDeclaration) decl).isStatic()) {
        missingFieldAccessInformation = true;
      }

      if(idExp.getDeclaration() instanceof CEnumerator) {
        CEnumerator enumerator = (CEnumerator)idExp.getDeclaration();
        if(enumerator.hasValue()) {
          return enumerator.getValue();
        } else {
          return null;
        }
      }


      String varName = getScopedVariableName(idExp.getName(), functionName);

      if(state.contains(varName)) {
        return state.getValueFor(varName);
      } else {
        return null;
      }
    }

    @Override
    public Long visit(AUnaryExpression unaryExpression) throws UnrecognizedCCodeException {

      //TODO Change with unary Expression

      UnaryOperator unaryOperator = unaryExpression.getOperator();
      IAExpression unaryOperand = unaryExpression.getOperand();

      Long value = null;

      switch(unaryOperator) {
      case MINUS:
        value = ((JExpression)unaryOperand).accept(this);
        return (value != null) ? -value : null;

      case NOT:
        value = ((JExpression)(unaryOperand)).accept(this);

        if(value == null)
          return null;

        // if the value is 0, return 1, if it is anything other than 0, return 0
        else
          return (value == 0L) ? 1L : 0L;

      case AMPER:
        return null; // valid expression, but it's a pointer value

      case STAR:
        missingPointer = true;
        return null;

      case SIZEOF:
      case TILDE:
      default:
        // TODO handle unimplemented operators
        return null;
      }
    }

    @Override
    public Long visit(JIntegerLiteralExpression pE) throws UnrecognizedCCodeException {
      return pE.asLong();
    }

    @Override
    public Long visit(JBooleanLiteralExpression pE) throws UnrecognizedCCodeException {
      return ((pE.getValue()) ? 1l : 0l);
    }

    @Override
    public Long visit(JFloatLiteralExpression pJBooleanLiteralExpression) throws UnrecognizedCCodeException {
      return null;
    }

    @Override
    public Long visit(JMethodInvocationExpression pAFunctionCallExpression) throws UnrecognizedCCodeException {
      return null;
    }

    @Override
    public Long visit(JArrayCreationExpression aCE) throws UnrecognizedCCodeException {
      return null;
    }

    @Override
    public Long visit(JArrayInitializer pJArrayInitializer) throws UnrecognizedCCodeException {
      return null;
    }

    @Override
    public Long visit(AArraySubscriptExpression pAArraySubscriptExpression) throws UnrecognizedCCodeException {
      return ((JExpression) pAArraySubscriptExpression.getSubscriptExpression()).accept(this);
    }

    @Override
    public Long visit(JClassInstanzeCreation pJClassInstanzeCreation) throws UnrecognizedCCodeException {
      return null;
    }

    @Override
    public Long visit(JThisRunTimeType pJThisRunTimeType) throws UnrecognizedCCodeException {
      return null;
    }

    @Override
    public Long visit(JRunTimeTypeEqualsType pJRunTimeTypeEqualsType) throws UnrecognizedCCodeException {
      return null;
    }

    @Override
    public Long visit(JNullLiteralExpression pJNullLiteralExpression) throws UnrecognizedCCodeException {
      return null;
    }
  }


  /**
   * Visitor that derives further information from an assume edge
   */
  private class AssigningValueVisitor extends ExpressionValueVisitor {
    protected boolean truthValue = false;

    public AssigningValueVisitor(CFAEdge pEdge, ExplicitState pElement, String pFunctionName, boolean truthValue) {
      super(pEdge, pElement, pFunctionName);

      this.truthValue = truthValue;
    }

    private IAExpression unwrap(IAExpression expression) {
      // is this correct for e.g. [!a != !(void*)(int)(!b)] !?!?!

      if(expression instanceof AUnaryExpression) {
        AUnaryExpression exp = (AUnaryExpression)expression;
        if (exp.getOperator() == UnaryOperator.NOT) {
          expression = exp.getOperand();
          truthValue = !truthValue;

          expression = unwrap(expression);
        }
      }

      if (expression instanceof CCastExpression) {
        CCastExpression exp = (CCastExpression)expression;
        expression = exp.getOperand();

        expression = unwrap(expression);
      }

      return expression;
    }

    @Override
    public Long visit(CBinaryExpression pE) throws UnrecognizedCCodeException {
      BinaryOperator binaryOperator   = pE.getOperator();

      CExpression lVarInBinaryExp  = pE.getOperand1();

      lVarInBinaryExp = (CExpression) unwrap(lVarInBinaryExp);

      CExpression rVarInBinaryExp  = pE.getOperand2();

      Long leftValue                  = lVarInBinaryExp.accept(this);
      Long rightValue                 = rVarInBinaryExp.accept(this);

      if ((binaryOperator == BinaryOperator.EQUALS && truthValue) || (binaryOperator == BinaryOperator.NOT_EQUALS && !truthValue)) {
        if (leftValue == null &&  rightValue != null && isAssignable(lVarInBinaryExp)) {
          String leftVariableName = getScopedVariableName(lVarInBinaryExp.toASTString(), functionName);
          if (currentPrecision.isTracking(leftVariableName)) {
            state.assignConstant(leftVariableName, rightValue);
          }
        }

        else if (rightValue == null && leftValue != null && isAssignable(rVarInBinaryExp)) {
          String rightVariableName = getScopedVariableName(rVarInBinaryExp.toASTString(), functionName);
          if (currentPrecision.isTracking(rightVariableName)) {
            state.assignConstant(rightVariableName, leftValue);
          }
        }
      }

      if (initAssumptionVars) {
        // x is unknown, a binaryOperation (x!=0), true-branch: set x=1L
        // x is unknown, a binaryOperation (x==0), false-branch: set x=1L
        if ((binaryOperator == BinaryOperator.NOT_EQUALS && truthValue)
            || (binaryOperator == BinaryOperator.EQUALS && !truthValue)) {
          if (leftValue == null && rightValue == 0L && isAssignable(lVarInBinaryExp)) {
            String leftVariableName = getScopedVariableName(lVarInBinaryExp.toASTString(), functionName);
            if (currentPrecision.isTracking(leftVariableName)) {
              state.assignConstant(leftVariableName, 1L);
            }
          }

          else if (rightValue == null && leftValue == 0L && isAssignable(rVarInBinaryExp)) {
            String rightVariableName = getScopedVariableName(rVarInBinaryExp.toASTString(), functionName);
            if (currentPrecision.isTracking(rightVariableName)) {
              state.assignConstant(rightVariableName, 1L);
            }
          }
        }
      }
      return super.visit(pE);
    }


    @Override
    public Long visit(JBinaryExpression pE) throws UnrecognizedCCodeException {
      JBinaryExpression.BinaryOperator binaryOperator   = pE.getOperator();

      IAExpression lVarInBinaryExp  = pE.getOperand1();

      lVarInBinaryExp = unwrap(lVarInBinaryExp);

      IAExpression rVarInBinaryExp  = pE.getOperand2();

      Long leftValue                  = ((JExpression) lVarInBinaryExp).accept(this);
      Long rightValue                 = ((JExpression) rVarInBinaryExp).accept(this);

      if((binaryOperator == JBinaryExpression.BinaryOperator.EQUALS && truthValue) || (binaryOperator == JBinaryExpression.BinaryOperator.NOT_EQUALS && !truthValue)) {
        if(leftValue == null &&  rightValue != null && isAssignable(lVarInBinaryExp)) {
          String leftVariableName = getScopedVariableName(lVarInBinaryExp.toASTString(), functionName);
          if (currentPrecision.isTracking(leftVariableName)) {
            state.assignConstant(leftVariableName, rightValue);
          }
        }

        else if (rightValue == null && leftValue != null && isAssignable(rVarInBinaryExp)) {
          String rightVariableName = getScopedVariableName(rVarInBinaryExp.toASTString(), functionName);
          if (currentPrecision.isTracking(rightVariableName)) {
            state.assignConstant(rightVariableName, leftValue);
          }
        }
      }

      if (initAssumptionVars) {
        // x is unknown, a binaryOperation (x!=0), true-branch: set x=1L
        // x is unknown, a binaryOperation (x==0), false-branch: set x=1L
        if ((binaryOperator == JBinaryExpression.BinaryOperator.NOT_EQUALS && truthValue)
            || (binaryOperator == JBinaryExpression.BinaryOperator.EQUALS && !truthValue)) {
          if (leftValue == null && rightValue == 0L && isAssignable(lVarInBinaryExp)) {
            String leftVariableName = getScopedVariableName(lVarInBinaryExp.toASTString(), functionName);
            if (currentPrecision.isTracking(leftVariableName)) {
              state.assignConstant(leftVariableName, 1L);
            }
          }

          else if (rightValue == null && leftValue == 0L && isAssignable(rVarInBinaryExp)) {
            String rightVariableName = getScopedVariableName(rVarInBinaryExp.toASTString(), functionName);
            if (currentPrecision.isTracking(rightVariableName)) {
              state.assignConstant(rightVariableName, 1L);
            }
          }
        }
      }
      return super.visit(pE);
    }


    private boolean isAssignable(IAExpression expression) {
      return expression instanceof AIdExpression || expression instanceof CFieldReference;
    }
  }

  private class PointerExpressionValueVisitor extends ExpressionValueVisitor {
    private final PointerState pointerState;

    public PointerExpressionValueVisitor(CFAEdge pEdge, ExplicitState pElement, String pFunctionName, PointerState pPointerState) {
      super(pEdge, pElement, pFunctionName);
      pointerState = pPointerState;
    }

    @Override
    public Long visit(CUnaryExpression unaryExpression) throws UnrecognizedCCodeException {
      if (unaryExpression.getOperator() != UnaryOperator.STAR) {
        return super.visit(unaryExpression);
      }

      // Cil produces code like
      // __cil_tmp8 = *((int *)__cil_tmp7);
      // so remove cast
      CExpression unaryOperand = unaryExpression.getOperand();
      if (unaryOperand instanceof CCastExpression) {
        unaryOperand = ((CCastExpression)unaryOperand).getOperand();
      }

      if (unaryOperand instanceof CIdExpression) {
        String rightVar = derefPointerToVariable(pointerState, ((CIdExpression)unaryOperand).getName());
        if (rightVar != null) {
          rightVar = getScopedVariableName(rightVar, functionName);

          if (state.contains(rightVar)) {
            return state.getValueFor(rightVar);
          }
        }
      } else {
        throw new UnrecognizedCCodeException("Pointer dereference of something that is not a variable", edge, unaryExpression);
      }

      return null;
    }

    @Override
    public Long visit(AUnaryExpression unaryExpression) throws UnrecognizedCCodeException {
      if(unaryExpression.getOperator() != UnaryOperator.STAR) {
        return super.visit(unaryExpression);
      }

      // Cil produces code like
      // __cil_tmp8 = *((int *)__cil_tmp7);
      // so remove cast
      IAExpression unaryOperand = unaryExpression.getOperand();
      if (unaryOperand instanceof CCastExpression) {
        unaryOperand = ((CCastExpression)unaryOperand).getOperand();
      }

      if(unaryOperand instanceof AIdExpression) {
        String rightVar = derefPointerToVariable(pointerState, ((AIdExpression)unaryOperand).getName());
        if (rightVar != null) {
          rightVar = getScopedVariableName(rightVar, functionName);

          if (state.contains(rightVar)) {
            return state.getValueFor(rightVar);
          }
        }
      } else {
        throw new UnrecognizedCCodeException("Pointer dereference of something that is not a variable", edge, unaryExpression);
      }

      return null;
    }

  }


  private class  FieldAccessExpressionValueVisitor extends ExpressionValueVisitor {
    private final JortState jortState;

    public FieldAccessExpressionValueVisitor(CFAEdge pEdge, ExplicitState pElement, String pFunctionName, JortState pJortState) {
      super(pEdge, pElement, pFunctionName);
      jortState = pJortState;
    }

    @Override
    public Long visit(JIdExpression idExp) throws UnrecognizedCCodeException {

      IASimpleDeclaration decl = idExp.getDeclaration();

      if(idExp.getDeclaration() instanceof CEnumerator) {
        CEnumerator enumerator = (CEnumerator)idExp.getDeclaration();
        if(enumerator.hasValue()) {
          return enumerator.getValue();
        } else {
          return null;
        }
      }

      String varName = null;

      String reference;

      if(idExp instanceof JFieldAccess) {

        JVariableDeclaration referenceDeclaration = ((JFieldAccess) idExp).getReferencedVariable();


        if(referenceDeclaration instanceof JFieldDeclaration) {

          if(((JFieldDeclaration) referenceDeclaration).isStatic()) {
            // reference is static, it is stored in global scope
            reference = referenceDeclaration.getName();
          } else {
            // reference is non static field, is stored in this object scope
            reference = getJortScopedVariableName(referenceDeclaration.getName() ,jortState.getUniqueObjectFor(JortState.KEYWORD_THIS));
          }

        } else {
          // Reference Variable is local, it is stored in function Scope
          reference = getScopedVariableName(referenceDeclaration.getName(), functionName);
        }
      } else {
        reference = JortState.KEYWORD_THIS;
      }

      if(decl instanceof JFieldDeclaration && !((JFieldDeclaration) decl).isStatic()){

        if(jortState.contains(reference)) {
          varName = getJortScopedVariableName(decl.getName(), jortState.getUniqueObjectFor(reference));
        }else {
          return null;
        }
      } else {
        varName = getScopedVariableName(idExp.getName(), functionName);
      }

      if(state.contains(varName)) {
        return state.getValueFor(varName);
      } else {
        return null;
      }
    }
  }



  private Long getExpressionValue(ExplicitState element, IAExpression expression, String functionName, CFAEdge edge)
    throws UnrecognizedCCodeException {
    if(expression instanceof JRightHandSide && !(expression instanceof CRightHandSide)){

        ExpressionValueVisitor evv = new ExpressionValueVisitor(edge, element, functionName);
        Long value =  ((JRightHandSide) expression).accept(evv);
        if(evv.missingFieldAccessInformation) {
          missingInformationRightJExpression = (JRightHandSide) expression;
          return null;
        } else {
          return value;
        }
    } else {
      return ((CRightHandSide) expression).accept(new ExpressionValueVisitor(edge, element, functionName));
    }
  }

  public String getScopedVariableName(String variableName, String functionName) {

    if (globalVariables.contains(variableName)) {
      return variableName;
    }

    return functionName + "::" + variableName;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState element, List<AbstractState> elements, CFAEdge cfaEdge, Precision precision)
    throws UnrecognizedCCodeException {
    assert element instanceof ExplicitState;
    ExplicitState explicitState = (ExplicitState)element;

    for (AbstractState ae : elements) {
      if (ae instanceof PointerState) {
        return strengthen(explicitState, (PointerState)ae, cfaEdge, precision);
      } else if(ae instanceof JortState) {
        return strengthen(explicitState, (JortState)ae, cfaEdge, precision);
      }
    }


    return null;
  }

  private Collection<? extends AbstractState> strengthen(ExplicitState explicitState, JortState jortState, CFAEdge cfaEdge,
      Precision precision) throws UnrecognizedCCodeException {

    if(missingFieldVariableObject) {

      ExplicitState newElement = explicitState.clone();
      newElement.assignConstant(getJortScopedVariableName(fieldNameAndInitialValue.getFirst(), jortState.getKeywordThisUniqueObject() ), fieldNameAndInitialValue.getSecond());
      missingFieldVariableObject = false;
      fieldNameAndInitialValue = null;
      return Collections.singleton(newElement);

    } else if(missingScopedFieldName){

      ExplicitState newElement = explicitState.clone();
      newElement = handleNotScopedVariable(jortState, newElement , cfaEdge);
      missingScopedFieldName = false;
      notScopedField = null;
      notScopedFieldValue = null;
      missingInformationRightJExpression = null;

      if(newElement != null) {
      return Collections.singleton(newElement);
      } else {
        return null;
      }
    } else if(missingAssumeInformation && missingInformationRightJExpression != null) {
      ExplicitState newElement = explicitState.clone();
      Long value = handleMissingInformationRightJExpression(jortState, newElement, cfaEdge);


      missingAssumeInformation = false;
      missingInformationRightJExpression = null;

      if(value == null) {
        return null;
      } else if ((((AssumeEdge) cfaEdge).getTruthAssumption() && value == 1L) || (!((AssumeEdge) cfaEdge).getTruthAssumption() && value == 0L)) {
        return Collections.singleton(newElement);
      } else {
        return Collections.singleton(newElement);
      }
    } else if(missingInformationRightJExpression != null) {

      ExplicitState newElement = explicitState.clone();
      Long value = handleMissingInformationRightJExpression(jortState , newElement , cfaEdge);

      if(value != null) {
        newElement.assignConstant(missingInformationLeftJVariable, value);
        missingInformationRightJExpression = null;
        missingInformationLeftJVariable = null;
        return Collections.singleton(newElement);
      } else {
        missingInformationRightJExpression = null;
        missingInformationLeftJVariable = null;
        return null;
      }
    }
    return null;
  }

  private Long handleMissingInformationRightJExpression(JortState pJortState , ExplicitState newElement, CFAEdge cfaEdge) throws UnrecognizedCCodeException {

    return missingInformationRightJExpression.accept(new FieldAccessExpressionValueVisitor(cfaEdge , newElement, cfaEdge.getPredecessor().getFunctionName() , pJortState));
  }

  private ExplicitState handleNotScopedVariable(JortState jortState , ExplicitState newElement, CFAEdge cfaEdge) throws UnrecognizedCCodeException {



    String reference = null;

    if(notScopedField instanceof JFieldAccess) {
      reference = ((JFieldAccess) notScopedField).getReferencedVariable().getName();
    } else {
      reference = JortState.KEYWORD_THIS;
    }

    if(jortState.contains(reference)) {
      String scopedFieldName = getJortScopedVariableName(notScopedField.getName(), jortState.getUniqueObjectFor(reference ));
      Long value = notScopedFieldValue;
      if(missingInformationRightJExpression != null){
        value = handleMissingInformationRightJExpression(jortState , newElement, cfaEdge);
      }

      if(value != null) {
        newElement.assignConstant(scopedFieldName, value);
        return newElement;
      } else {
        return null;
      }
    } else {
      //TODO Logging? Expression?
      return null;
    }
  }

  private String getJortScopedVariableName(String fieldName, String uniqueObject) {

    return  uniqueObject + "::"+ fieldName;
  }

  private Collection<? extends AbstractState> strengthen(ExplicitState explicitState, PointerState pointerElement, CFAEdge cfaEdge, Precision precision)
    throws UnrecognizedCCodeException {
    try {
      if (missingInformationRightExpression != null) {
        String functionName = cfaEdge.getPredecessor().getFunctionName();
        ExplicitState newElement = explicitState.clone();
        ExpressionValueVisitor v = new PointerExpressionValueVisitor(cfaEdge, newElement, functionName, pointerElement);

        if (missingInformationLeftVariable != null) {
          handleAssignmentToVariable(missingInformationLeftVariable, missingInformationRightExpression, v);

          return Collections.singleton(newElement);
        }
        else if (missingInformationLeftPointer != null) {
          String leftVar = derefPointerToVariable(pointerElement, missingInformationLeftPointer);
          if (leftVar != null) {
            leftVar = getScopedVariableName(leftVar, functionName);
            handleAssignmentToVariable(leftVar, missingInformationRightExpression, v);

            return Collections.singleton(newElement);
          }
        }
      }
      return null;
    }

    finally {
      missingInformationLeftVariable = null;
      missingInformationLeftPointer = null;
      missingInformationRightExpression = null;
    }
  }

  private String derefPointerToVariable(PointerState pointerElement, String pointer) {
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

  /**
   * This method converts an expression like [a + 753 != 951] to [a != 951 - 753], to be able to derive addition information easier with the current expression evaluation visitor.
   *
   * @param expression the expression to generalize
   * @return the generalized expression
   */

  private IAExpression optimizeAssumeForEvaluation(IAExpression expression) {
    if(expression instanceof ABinaryExpression) {
      ABinaryExpression binaryExpression = (ABinaryExpression)expression;

      ABinaryOperator operator = binaryExpression.getOperator();
      IAExpression leftOperand = binaryExpression.getOperand1();
      IAExpression riteOperand = binaryExpression.getOperand2();


      if(operator == BinaryOperator.EQUALS || operator == BinaryOperator.NOT_EQUALS) {
        if(leftOperand instanceof ABinaryExpression && riteOperand instanceof ALiteralExpression) {
          CBinaryExpression expr = (CBinaryExpression)leftOperand;

          BinaryOperator operation = expr.getOperator();
          IAExpression leftAddend = expr.getOperand1();
          IAExpression riteAddend = expr.getOperand2();

          // [(a + 753) != 951] => [a != 951 + 753]

          if(riteAddend instanceof ALiteralExpression && (operation == BinaryOperator.PLUS || operation == BinaryOperator.MINUS)) {
            BinaryOperator newOperation = (operation == BinaryOperator.PLUS) ? BinaryOperator.MINUS : BinaryOperator.PLUS;

            ABinaryExpression sum = new ABinaryExpression(expr.getFileLocation(),
                                                                expr.getExpressionType(),
                                                                riteOperand,
                                                                riteAddend,
                                                                newOperation);

            ABinaryExpression assume = new ABinaryExpression(expression.getFileLocation(),
                                                                   expression.getExpressionType(),
                                                                   leftAddend,
                                                                   sum,
                                                                   operator);
            return assume;
          }
        }
      }
    }

    return expression;
  }
}