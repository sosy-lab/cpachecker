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

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.pointer.Memory;
import org.sosy_lab.cpachecker.cpa.pointer.Pointer;
import org.sosy_lab.cpachecker.cpa.pointer.PointerState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;

@Options(prefix="cpa.explicit")
public class ExplicitTransferRelation implements TransferRelation
{
  private final Set<String> globalVariables = new HashSet<String>();

  private String missingInformationLeftVariable = null;
  private String missingInformationLeftPointer  = null;

  private CRightHandSide missingInformationRightExpression = null;

  private ExplicitPrecision currentPrecision = null;

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
      CAssumeEdge assumeEdge = (CAssumeEdge) cfaEdge;
      successor = handleAssumption(explicitState.clone(), assumeEdge.getExpression(), cfaEdge, assumeEdge.getTruthAssumption(), explicitPrecision);
      break;

    case FunctionCallEdge:
      CFunctionCallEdge functionCallEdge = (CFunctionCallEdge) cfaEdge;
      successor = handleFunctionCall(explicitState, functionCallEdge);
      break;

    // this is a return edge from function, this is different from return statement
    // of the function. See case for statement edge for details
    case FunctionReturnEdge:
      CFunctionReturnEdge functionReturnEdge = (CFunctionReturnEdge) cfaEdge;
      successor = handleFunctionReturn(explicitState, functionReturnEdge);
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
      CStatementEdge statementEdge = (CStatementEdge) cfaEdge;
      handleStatement(element, statementEdge.getStatement(), cfaEdge, precision);
      break;

    case ReturnStatementEdge:
      CReturnStatementEdge returnEdge = (CReturnStatementEdge)cfaEdge;
      // this statement is a function return, e.g. return (a);
      // note that this is different from return edge
      // this is a statement edge which leads the function to the
      // last node of its CFA, where return edge is from that last node
      // to the return site of the caller function
      handleExitFromFunction(element, returnEdge.getExpression(), returnEdge);
      break;

    // edge is a declaration edge, e.g. int a;
    case DeclarationEdge:
      CDeclarationEdge declarationEdge = (CDeclarationEdge) cfaEdge;
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

  private ExplicitState handleFunctionCall(ExplicitState element, CFunctionCallEdge callEdge)
    throws UnrecognizedCCodeException {
    ExplicitState newElement = new ExplicitState(element);

    // copy global variables into the new state, to make them available in body of called function
    for (String globalVar : globalVariables) {
      if (element.contains(globalVar)) {
        newElement.assignConstant(globalVar, element.getValueFor(globalVar));
      }
    }

    CFunctionEntryNode functionEntryNode = callEdge.getSuccessor();
    String calledFunctionName = functionEntryNode.getFunctionName();
    String callerFunctionName = callEdge.getPredecessor().getFunctionName();

    List<String> paramNames = functionEntryNode.getFunctionParameterNames();
    List<CExpression> arguments = callEdge.getArguments();

    if (!callEdge.getSuccessor().getFunctionDefinition().getType().takesVarArgs()) {
      assert(paramNames.size() == arguments.size());
    }

    // visitor for getting the values of the actual parameters in caller function context
    ExpressionValueVisitor visitor = new ExpressionValueVisitor(callEdge, element, callerFunctionName);

    // get value of actual parameter in caller function context
    for (int i = 0; i < paramNames.size(); i++) {
      Long value = arguments.get(i).accept(visitor);

      String formalParamName = getScopedVariableName(paramNames.get(i), calledFunctionName);

      if (value == null) {
        newElement.forget(formalParamName);
      } else {
        newElement.assignConstant(formalParamName, value);
      }
    }

    return newElement;
  }

  private void handleExitFromFunction(ExplicitState newElement, CExpression expression, CReturnStatementEdge returnEdge)
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
  private ExplicitState handleFunctionReturn(ExplicitState element, CFunctionReturnEdge functionReturnEdge)
    throws UnrecognizedCCodeException {
    CFunctionSummaryEdge summaryEdge    = functionReturnEdge.getSummaryEdge();
    CFunctionCall exprOnSummary  = summaryEdge.getExpression();

    ExplicitState newElement      = element.getPreviousState().clone();
    String callerFunctionName       = functionReturnEdge.getSuccessor().getFunctionName();
    String calledFunctionName       = functionReturnEdge.getPredecessor().getFunctionName();

    // copy global variables back to the new state, to make them available in body of calling function
    for (String variableName : globalVariables) {
      if (element.contains(variableName)) {
        newElement.assignConstant(variableName, element.getValueFor(variableName));
      } else {
        newElement.forget(variableName);
      }
    }

    // expression is an assignment operation, e.g. a = g(b);
    if (exprOnSummary instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement assignExp = ((CFunctionCallAssignmentStatement)exprOnSummary);
      CExpression op1 = assignExp.getLeftHandSide();

      // we expect left hand side of the expression to be a variable
      if ((op1 instanceof CIdExpression) || (op1 instanceof CFieldReference)) {
        String returnVarName = getScopedVariableName("___cpa_temp_result_var_", calledFunctionName);

        String assignedVarName = getScopedVariableName(op1.toASTString(), callerFunctionName);

        if (currentPrecision.isTracking(assignedVarName) && element.contains(returnVarName)) {
          newElement.assignConstant(assignedVarName, element.getValueFor(returnVarName));
        } else {
          newElement.forget(assignedVarName);
        }
      }

      // a* = b(); TODO: for now, nothing is done here, but cloning the current element
      else if (op1 instanceof CUnaryExpression && ((CUnaryExpression)op1).getOperator() == UnaryOperator.STAR) {
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

  private ExplicitState handleAssumption(ExplicitState element, CExpression expression, CFAEdge cfaEdge, boolean truthValue, ExplicitPrecision precision)
    throws UnrecognizedCCodeException {
    // convert an expression like [a + 753 != 951] to [a != 951 - 753]
    expression = optimizeAssumeForEvaluation(expression);

    String functionName = cfaEdge.getPredecessor().getFunctionName();

    // get the value of the expression (either true[1L], false[0L], or unknown[null])
    Long value = getExpressionValue(element, expression, functionName, cfaEdge);

    // value is null, try to derive further information
    if (value == null) {
      AssigningValueVisitor avv = new AssigningValueVisitor(cfaEdge, element, functionName, truthValue);

      expression.accept(avv);

      return element;
    } else if ((truthValue && value == 1L) || (!truthValue && value == 0L)) {
      return element;
    } else {
      return null;
    }
  }

  private void handleDeclaration(ExplicitState newElement, CDeclarationEdge declarationEdge, ExplicitPrecision precision)
    throws UnrecognizedCCodeException {

    if (!(declarationEdge.getDeclaration() instanceof CVariableDeclaration)) {
      // nothing interesting to see here, please move along
      return;
    }

    CVariableDeclaration decl = (CVariableDeclaration)declarationEdge.getDeclaration();

    // get the variable name in the declarator
    String varName = decl.getName();
    String functionName = declarationEdge.getPredecessor().getFunctionName();

    Long initialValue = null;

    // handle global variables
    if (decl.isGlobal()) {
      // if this is a global variable, add to the list of global variables
      globalVariables.add(varName);

      // global variables without initializer are set to 0 in C
      initialValue = 0L;
    }

    // get initial value
    CInitializer init = decl.getInitializer();
    if (init instanceof CInitializerExpression) {
      CExpression exp = ((CInitializerExpression)init).getExpression();

      initialValue = getExpressionValue(newElement, exp, functionName, declarationEdge);
    }

    // assign initial value if necessary
    String scopedVarName = getScopedVariableName(varName, functionName);

    if (initialValue != null && precision.isTracking(scopedVarName)) {
      newElement.assignConstant(scopedVarName, initialValue);
    } else {
      newElement.forget(scopedVarName);
    }
  }

  private void handleStatement(ExplicitState newElement, CStatement expression, CFAEdge cfaEdge, ExplicitPrecision precision)
    throws UnrecognizedCCodeException {
    // expression is a binary operation, e.g. a = b;
    if (expression instanceof CAssignment) {
      handleAssignment(newElement, (CAssignment)expression, cfaEdge, precision);

    // external function call - do nothing
    } else if (expression instanceof CFunctionCallStatement) {

    // there is such a case
    } else if (expression instanceof CExpressionStatement) {

    } else {
      throw new UnrecognizedCCodeException(cfaEdge, expression);
    }
  }

  private void handleAssignment(ExplicitState newElement, CAssignment assignExpression, CFAEdge cfaEdge, ExplicitPrecision precision)
    throws UnrecognizedCCodeException {
    CExpression op1    = assignExpression.getLeftHandSide();
    CRightHandSide op2 = assignExpression.getRightHandSide();

    if (op1 instanceof CIdExpression) {
      // a = ...
      if (!precision.isOnBlacklist(getScopedVariableName(((CIdExpression)op1).getName(), cfaEdge.getPredecessor().getFunctionName()))) {
        String functionName = cfaEdge.getPredecessor().getFunctionName();

        handleAssignmentToVariable(op1.toASTString(), op2, new ExpressionValueVisitor(cfaEdge, newElement, functionName));
      }
    }

    else if (op1 instanceof CUnaryExpression && ((CUnaryExpression)op1).getOperator() == UnaryOperator.STAR) {
      // *a = ...

      op1 = ((CUnaryExpression)op1).getOperand();

      // Cil produces code like
      // *((int*)__cil_tmp5) = 1;
      // so remove cast
      if (op1 instanceof CCastExpression) {
        op1 = ((CCastExpression)op1).getOperand();
      }

      if (op1 instanceof CIdExpression) {
        missingInformationLeftPointer = ((CIdExpression)op1).getName();
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
    else if (op1 instanceof CArraySubscriptExpression) {

    } else {
      throw new UnrecognizedCCodeException("left operand of assignment has to be a variable", cfaEdge, op1);
    }
  }

  private void handleAssignmentToVariable(String lParam, CRightHandSide exp, ExpressionValueVisitor visitor)
    throws UnrecognizedCCodeException {
    Long value = exp.accept(visitor);

    if (visitor.missingPointer) {
      missingInformationRightExpression = exp;
      assert value == null;
    }

    ExplicitState newElement = visitor.state;
    String assignedVar = getScopedVariableName(lParam, visitor.functionName);

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

  /**
   * Visitor that get's the value from an expression.
   * The result may be null, i.e., the value is unknown.
   */
  private class ExpressionValueVisitor extends DefaultCExpressionVisitor<Long, UnrecognizedCCodeException>
                                       implements CRightHandSideVisitor<Long, UnrecognizedCCodeException> {
    protected final CFAEdge edge;
    protected final ExplicitState state;
    protected final String functionName;

    private boolean missingPointer = false;

    public ExpressionValueVisitor(CFAEdge pEdge, ExplicitState pElement, String pFunctionName) {
      edge = pEdge;
      state = pElement;
      functionName = pFunctionName;
    }

    // TODO fields, arrays

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

    private CExpression unwrap(CExpression expression) {
      // is this correct for e.g. [!a != !(void*)(int)(!b)] !?!?!
      if (expression instanceof CUnaryExpression) {
        CUnaryExpression exp = (CUnaryExpression)expression;
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

      lVarInBinaryExp = unwrap(lVarInBinaryExp);

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

      return super.visit(pE);
    }

    private boolean isAssignable(CExpression expression) {
      return expression instanceof CIdExpression || expression instanceof CFieldReference;
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
  }

  private Long getExpressionValue(ExplicitState element, CRightHandSide expression, String functionName, CFAEdge edge)
    throws UnrecognizedCCodeException {
    return expression.accept(new ExpressionValueVisitor(edge, element, functionName));
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
      }
    }

    return null;
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
  private CExpression optimizeAssumeForEvaluation(CExpression expression) {
    if (expression instanceof CBinaryExpression) {
      CBinaryExpression binaryExpression = (CBinaryExpression)expression;

      BinaryOperator operator = binaryExpression.getOperator();
      CExpression leftOperand = binaryExpression.getOperand1();
      CExpression riteOperand = binaryExpression.getOperand2();

      if (operator == BinaryOperator.EQUALS || operator == BinaryOperator.NOT_EQUALS) {
        if (leftOperand instanceof CBinaryExpression && riteOperand instanceof CLiteralExpression) {
          CBinaryExpression expr = (CBinaryExpression)leftOperand;

          BinaryOperator operation = expr.getOperator();
          CExpression leftAddend = expr.getOperand1();
          CExpression riteAddend = expr.getOperand2();

          // [(a + 753) != 951] => [a != 951 + 753]
          if (riteAddend instanceof CLiteralExpression && (operation == BinaryOperator.PLUS || operation == BinaryOperator.MINUS)) {
            BinaryOperator newOperation = (operation == BinaryOperator.PLUS) ? BinaryOperator.MINUS : BinaryOperator.PLUS;

            CBinaryExpression sum = new CBinaryExpression(expr.getFileLocation(),
                                                                expr.getExpressionType(),
                                                                riteOperand,
                                                                riteAddend,
                                                                newOperation);

            CBinaryExpression assume = new CBinaryExpression(expression.getFileLocation(),
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
