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
package org.sosy_lab.cpachecker.cpa.invariants;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ExpressionToFormulaVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ExpressionToFormulaVisitor.VariableNameExtractor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaEvaluationVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormulaManager;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;

enum InvariantsTransferRelation implements TransferRelation {

  INSTANCE;

  /**
   * Base name of the variable that is introduced to pass results from
   * returning function calls.
   */
  private static final String RETURN_VARIABLE_BASE_NAME = "___cpa_temp_result_var_";

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pElement, Precision pPrecision, CFAEdge edge)
      throws CPATransferException {

    InvariantsState element = (InvariantsState)pElement;

    switch (edge.getEdgeType()) {
    case BlankEdge:
      break;
    case FunctionReturnEdge:
      element = handleFunctionReturn(element, (CFunctionReturnEdge) edge);
      break;
    case ReturnStatementEdge:
      element = handleReturnStatement(element, (CReturnStatementEdge) edge);
      break;
    case AssumeEdge:
      element = handleAssume(element, (CAssumeEdge)edge);
      break;

    case DeclarationEdge:
      element = handleDeclaration(element, (CDeclarationEdge)edge);
      break;

    case FunctionCallEdge:
      element = handleFunctionCall(element, (CFunctionCallEdge)edge);
      break;

    case StatementEdge:
      element = handleStatement(element, (CStatementEdge)edge);
      break;

    default:
      throw new UnrecognizedCFAEdgeException(edge);
    }

    if (element == null) {
      return Collections.emptySet();
    } else {
      return Collections.singleton(element);
    }
  }

  private InvariantsState handleAssume(InvariantsState pElement, CAssumeEdge pEdge) throws UnrecognizedCCodeException {
    FormulaEvaluationVisitor<CompoundState> resolver = pElement.getFormulaResolver(pEdge.toString());
    CExpression expression = pEdge.getExpression();
    // Create a formula representing the edge expression
    InvariantsFormula<CompoundState> expressionFormula = expression.accept(getExpressionToFormulaVisitor(pEdge));

    // Evaluate the state of the assume edge expression
    CompoundState expressionState = expressionFormula.accept(resolver);
    /*
     * If the expression definitely evaluates to false when truth is assumed or
     * the expression definitely evaluates to true when falsehood is assumed,
     * the state is unreachable.
     */
    if (pEdge.getTruthAssumption() && expressionState.isDefinitelyFalse()
        || !pEdge.getTruthAssumption() && expressionState.isDefinitelyTrue()) {
      return null;
    }
    /*
     * Assume the state of the expression:
     * If truth is assumed, any non-zero value, otherwise zero.
     */
    if (!pEdge.getTruthAssumption()) {
      expressionFormula = InvariantsFormulaManager.INSTANCE.logicalNot(expressionFormula);
    }
    return pElement.assume(expressionFormula, pEdge.toString());
  }

  private InvariantsState handleDeclaration(InvariantsState pElement, CDeclarationEdge pEdge) throws UnrecognizedCCodeException {
    if (!(pEdge.getDeclaration() instanceof CVariableDeclaration)) {
      return pElement;
    }

    CVariableDeclaration decl = (CVariableDeclaration) pEdge.getDeclaration();

    String varName = decl.getName();
    if (!decl.isGlobal()) {
      varName = scope(varName, pEdge.getSuccessor().getFunctionName());
    }

    /*
    CompoundState value = CompoundState.top();
    if (decl.getInitializer() != null && decl.getInitializer() instanceof CInitializerExpression) {
      CExpression init = ((CInitializerExpression)decl.getInitializer()).getExpression();
      value = init.accept(createExpressionToStateVisitor(pElement, pEdge));
    }
    */

    final InvariantsFormula<CompoundState> value;
    if (decl.getInitializer() != null && decl.getInitializer() instanceof CInitializerExpression) {
      CExpression init = ((CInitializerExpression)decl.getInitializer()).getExpression();
      value = init.accept(getExpressionToFormulaVisitor(pEdge));
    } else {
      value = InvariantsFormulaManager.INSTANCE.asConstant(CompoundState.top());
    }

    return pElement.assign(varName, value);
  }

  private InvariantsState handleFunctionCall(InvariantsState pElement, CFunctionCallEdge pEdge) throws UnrecognizedCCodeException {

    InvariantsState newElement = pElement;
    List<String> formalParams = pEdge.getSuccessor().getFunctionParameterNames();
    List<CExpression> actualParams = pEdge.getArguments();

    for (Pair<String, CExpression> param : Pair.zipList(formalParams, actualParams)) {
      CExpression actualParam = param.getSecond();

      InvariantsFormula<CompoundState> value = actualParam.accept(getExpressionToFormulaVisitor(pEdge));

      String formalParam = scope(param.getFirst(), pEdge.getSuccessor().getFunctionName());
      newElement = newElement.assign(formalParam, value);
    }

    return newElement;
  }

  private InvariantsState handleStatement(InvariantsState pElement, CStatementEdge pEdge) throws UnrecognizedCCodeException {

    if (pEdge.getStatement() instanceof CAssignment) {
      CAssignment assignment = (CAssignment)pEdge.getStatement();

      CExpression leftHandSide = assignment.getLeftHandSide();
      if (leftHandSide instanceof CIdExpression) {
        // a = ...

        String varName = getVarName((CIdExpression)leftHandSide, pEdge);
        InvariantsFormula<CompoundState> value = assignment.getRightHandSide().accept(getExpressionToFormulaVisitor(pEdge));
        return pElement.assign(varName, value);
      } else {
        throw new UnrecognizedCCodeException("unknown left-hand side of assignment", pEdge, leftHandSide);
      }

    }

    return pElement;
  }

  private InvariantsState handleReturnStatement(InvariantsState pElement, CReturnStatementEdge pEdge) throws UnrecognizedCCodeException {
    String calledFunctionName = pEdge.getPredecessor().getFunctionName();
    CExpression returnedExpression = pEdge.getExpression();
    InvariantsFormula<CompoundState> returnedState = returnedExpression.accept(getExpressionToFormulaVisitor(pEdge));
    String returnValueName = scope(RETURN_VARIABLE_BASE_NAME, calledFunctionName);
    return pElement.assign(returnValueName, returnedState);
  }

  private InvariantsState handleFunctionReturn(InvariantsState element, CFunctionReturnEdge functionReturnEdge)
      throws UnrecognizedCCodeException {
      CFunctionSummaryEdge summaryEdge = functionReturnEdge.getSummaryEdge();

      CFunctionCall expression = summaryEdge.getExpression();

      String callerFunctionName = functionReturnEdge.getSuccessor().getFunctionName();
      String calledFunctionName = functionReturnEdge.getPredecessor().getFunctionName();

      String returnValueName = scope(RETURN_VARIABLE_BASE_NAME, calledFunctionName);

      InvariantsFormula<CompoundState> value = InvariantsFormulaManager.INSTANCE.asVariable(returnValueName);

      // expression is an assignment operation, e.g. a = g(b);
      if (expression instanceof CFunctionCallAssignmentStatement) {
        CFunctionCallAssignmentStatement funcExp = (CFunctionCallAssignmentStatement)expression;

        CExpression operand1 = funcExp.getLeftHandSide();

        // left hand side of the expression has to be a variable
        if (operand1 instanceof CIdExpression) {
          final String varName = scope(((CIdExpression) operand1).getName(), callerFunctionName);
          element = element.assign(varName, value);
        }
      }
      return element;
  }

  static String getVarName(CIdExpression var, CFAEdge edge) throws UnrecognizedCCodeException {
    String varName = var.getName();
    if (var.getDeclaration() != null) {
      CSimpleDeclaration decl = var.getDeclaration();

      if (!(decl instanceof CDeclaration || decl instanceof CParameterDeclaration)) {
        throw new UnrecognizedCCodeException("unknown variable declaration", edge, var);
      }

      if (decl instanceof CDeclaration && ((CDeclaration)decl).isGlobal()) {

      } else {
        varName = scope(varName, edge.getPredecessor().getFunctionName());
      }
    }
    return varName;
  }

  private static String scope(String var, String function) {
    return function + "::" + var;
  }

  private static class PushStateExpressionVisitor extends DefaultCExpressionVisitor<InvariantsState, UnrecognizedCCodeException> {

    private final ExpressionToStateVisitor expressionVisitor;

    private final CompoundState stateToPush;

    public PushStateExpressionVisitor(ExpressionToStateVisitor expressionVisitor, CompoundState stateToPush) {
      this.expressionVisitor = expressionVisitor;
      if (stateToPush.isBottom()) {
        this.expressionVisitor.setBaseState(null);
      }
      this.stateToPush = stateToPush;
    }

    @Override
    public InvariantsState visit(CIdExpression pE) throws UnrecognizedCCodeException {
      if (!this.expressionVisitor.hasBaseState()) {
        return null;
      }
      CompoundState varState = this.expressionVisitor.getVarState(pE);
      varState = varState.intersectWith(this.stateToPush);
      if (varState.isBottom()) {
        return null;
      }
      return this.expressionVisitor.copyAndSetBaseState(pE, varState);
    }

    @Override
    protected InvariantsState visitDefault(CExpression pExp) throws UnrecognizedCCodeException {
      return this.expressionVisitor.getBaseState();
    }

    @Override
    public InvariantsState visit(CUnaryExpression pE) throws UnrecognizedCCodeException {
      switch (pE.getOperator()) {
      case MINUS:
        return pE.getOperand().accept(new PushStateExpressionVisitor(this.expressionVisitor, this.stateToPush.negate()));
      case NOT:
        return pE.getOperand().accept(new PushStateExpressionVisitor(this.expressionVisitor, this.stateToPush.logicalNot()));
      case PLUS:
        return pE.getOperand().accept(this);
      case TILDE:
        return pE.getOperand().accept(new PushStateExpressionVisitor(this.expressionVisitor, this.stateToPush.binaryNot()));
      default:
        return visitDefault(null);
      }
    }

    @Override
    public InvariantsState visit(CBinaryExpression pE) throws UnrecognizedCCodeException {
      if (!this.expressionVisitor.hasBaseState()) {
        return null;
      }
      CompoundState prevLhsState = pE.getOperand1().accept(this.expressionVisitor);
      CompoundState prevRhsState = pE.getOperand2().accept(this.expressionVisitor);
      CompoundState lhsState = null;
      CompoundState rhsState = null;
      while (!prevLhsState.equals(lhsState) || !prevRhsState.equals(rhsState)) {
        lhsState = prevLhsState;
        rhsState = prevRhsState;
        CompoundState pushLhsState;
        CompoundState pushRhsState;
        switch (pE.getOperator()) {
          case DIVIDE:
            pushLhsState = this.stateToPush.multiply(rhsState);
            pushRhsState = lhsState.divide(rhsState);
            break;
          case EQUALS:
            if (this.stateToPush.equals(CompoundState.logicalTrue())) {
              pushLhsState = pushRhsState = lhsState.intersectWith(rhsState);
            } else if (this.stateToPush.equals(CompoundState.logicalFalse())) {
              pushLhsState = rhsState.invert();
              pushRhsState = lhsState.invert();
            } else {
              return this.expressionVisitor.getBaseState();
            }
            break;
          case GREATER_EQUAL:
            if (this.stateToPush.equals(CompoundState.logicalTrue())) {
              if (rhsState.hasLowerBound()) {
                pushLhsState = CompoundState.of(SimpleInterval.singleton(rhsState.getLowerBound()).extendToPositiveInfinity());
              } else {
                pushLhsState = rhsState;
              }
              if (lhsState.hasUpperBound()) {
                pushRhsState = CompoundState.of(SimpleInterval.singleton(lhsState.getUpperBound()).extendToNegativeInfinity());
              } else {
                pushRhsState = lhsState;
              }
            } else if (this.stateToPush.equals(CompoundState.logicalFalse())) {
              if (rhsState.hasLowerBound()) {
                pushLhsState = CompoundState.of(SimpleInterval.singleton(rhsState.getLowerBound().subtract(BigInteger.ONE)).extendToNegativeInfinity());
              } else {
                pushLhsState = CompoundState.bottom();
              }
              if (lhsState.hasUpperBound()) {
                pushRhsState = CompoundState.of(SimpleInterval.singleton(lhsState.getUpperBound().add(BigInteger.ONE)).extendToPositiveInfinity());
              } else {
                pushRhsState = CompoundState.bottom();
              }
            } else {
              return this.expressionVisitor.getBaseState();
            }
            break;
          case GREATER_THAN:
            if (this.stateToPush.equals(CompoundState.logicalTrue())) {
              if (rhsState.hasUpperBound()) {
                pushLhsState = CompoundState.of(SimpleInterval.singleton(rhsState.getUpperBound().add(BigInteger.ONE)).extendToPositiveInfinity());
              } else {
                pushLhsState = CompoundState.bottom();
              }
              if (lhsState.hasLowerBound()) {
                pushRhsState = CompoundState.of(SimpleInterval.singleton(lhsState.getLowerBound().subtract(BigInteger.ONE)).extendToNegativeInfinity());
              } else {
                pushRhsState = CompoundState.bottom();
              }
            } else if (this.stateToPush.equals(CompoundState.logicalFalse())) {
              if (rhsState.hasUpperBound()) {
                pushLhsState = CompoundState.of(SimpleInterval.singleton(rhsState.getUpperBound()).extendToNegativeInfinity());
              } else {
                pushLhsState = rhsState;
              }
              if (lhsState.hasLowerBound()) {
                pushRhsState = CompoundState.of(SimpleInterval.singleton(lhsState.getLowerBound()).extendToPositiveInfinity());
              } else {
                pushRhsState = lhsState;
              }
            } else {
              return this.expressionVisitor.getBaseState();
            }
            break;
          case MINUS:
            pushLhsState = this.stateToPush.add(rhsState);
            pushRhsState = lhsState.add(this.stateToPush.negate());
            break;
          case MULTIPLY:
            pushLhsState = this.stateToPush.divide(rhsState);
            pushRhsState = this.stateToPush.divide(lhsState);
            break;
          case NOT_EQUALS:
            if (this.stateToPush.equals(CompoundState.logicalTrue())) {
              pushLhsState = rhsState.invert();
              pushRhsState = lhsState.invert();
            } else if (this.stateToPush.equals(CompoundState.logicalFalse())) {
              pushLhsState = pushRhsState = lhsState.intersectWith(rhsState);
            } else {
              return this.expressionVisitor.getBaseState();
            }
            break;
          case PLUS:
            pushLhsState = this.stateToPush.add(rhsState.negate());
            pushRhsState = this.stateToPush.add(lhsState.negate());
            break;
          case LESS_EQUAL:
            if (this.stateToPush.equals(CompoundState.logicalTrue())) {
              if (rhsState.hasUpperBound()) {
                pushLhsState = CompoundState.of(SimpleInterval.singleton(rhsState.getUpperBound()).extendToNegativeInfinity());
              } else {
                pushLhsState = rhsState;
              }
              if (lhsState.hasLowerBound()) {
                pushRhsState = CompoundState.of(SimpleInterval.singleton(lhsState.getLowerBound()).extendToPositiveInfinity());
              } else {
                pushRhsState = lhsState;
              }
            } else if (this.stateToPush.equals(CompoundState.logicalFalse())) {
              if (rhsState.hasUpperBound()) {
                pushLhsState = CompoundState.of(SimpleInterval.singleton(rhsState.getUpperBound().add(BigInteger.ONE)).extendToPositiveInfinity());
              } else {
                pushLhsState = CompoundState.bottom();
              }
              if (lhsState.hasLowerBound()) {
                pushRhsState = CompoundState.of(SimpleInterval.singleton(lhsState.getLowerBound().subtract(BigInteger.ONE)).extendToNegativeInfinity());
              } else {
                pushRhsState = CompoundState.bottom();
              }
            } else {
              return this.expressionVisitor.getBaseState();
            }
            break;
          case LESS_THAN:
            if (this.stateToPush.equals(CompoundState.logicalTrue())) {
              if (rhsState.hasLowerBound()) {
                pushLhsState = CompoundState.of(SimpleInterval.singleton(rhsState.getLowerBound().subtract(BigInteger.ONE)).extendToNegativeInfinity());
              } else {
                pushLhsState = CompoundState.bottom();
              }
              if (lhsState.hasUpperBound()) {
                pushRhsState = CompoundState.of(SimpleInterval.singleton(lhsState.getUpperBound().add(BigInteger.ONE)).extendToPositiveInfinity());
              } else {
                pushRhsState = CompoundState.bottom();
              }
            } else if (this.stateToPush.equals(CompoundState.logicalFalse())) {
              if (rhsState.hasLowerBound()) {
                pushLhsState = CompoundState.of(SimpleInterval.singleton(rhsState.getLowerBound()).extendToPositiveInfinity());
              } else {
                pushLhsState = rhsState;
              }
            if (lhsState.hasUpperBound()) {
              pushRhsState =
                  CompoundState.of(SimpleInterval.singleton(lhsState.getUpperBound()).extendToNegativeInfinity());
            } else {
              pushRhsState = lhsState;
            }
          } else {
            return this.expressionVisitor.getBaseState();
          }
          break;
        case MODULO:
          BigInteger lhsLowerBound = this.stateToPush.containsNegative() ? null : BigInteger.ZERO;
          BigInteger lhsUpperBound = this.stateToPush.containsPositive() ? null : BigInteger.ZERO;
          if (lhsLowerBound == null) {
            if (lhsUpperBound == null) {
              pushLhsState = CompoundState.top();
            } else {
              pushLhsState = CompoundState.of(SimpleInterval.singleton(lhsUpperBound).extendToNegativeInfinity());
            }
          } else {
            if (lhsUpperBound == null) {
              pushLhsState = CompoundState.of(SimpleInterval.singleton(lhsLowerBound).extendToPositiveInfinity());
            } else {
              pushLhsState = CompoundState.of(SimpleInterval.of(lhsLowerBound, lhsUpperBound));
            }
          }
          BigInteger modValue =
              this.stateToPush.hasLowerBound() ? this.stateToPush.getLowerBound().abs().add(BigInteger.ONE) : null;
          if (this.stateToPush.hasUpperBound()) {
            BigInteger modValueFromUpperBound = this.stateToPush.getUpperBound().abs().add(BigInteger.ONE);
            if (modValue == null) {
              modValue = modValueFromUpperBound;
            } else if (modValueFromUpperBound != null) {
              modValue = modValue.max(modValueFromUpperBound);
            }
          }
          if (modValue == null) {
            pushRhsState = CompoundState.top();
          } else {
            pushRhsState = CompoundState.of(SimpleInterval.of(modValue.negate(), BigInteger.ONE.negate()));
            pushLhsState = pushRhsState.unionWith(SimpleInterval.of(BigInteger.ONE, modValue));
          }
          break;
        default:
          return this.expressionVisitor.getBaseState();
        }
        pushLhsState = pushLhsState.intersectWith(lhsState);
        pushRhsState = pushRhsState.intersectWith(rhsState);
        this.expressionVisitor.setBaseState(
            pE.getOperand1().accept(new PushStateExpressionVisitor(this.expressionVisitor, lhsState)));
        this.expressionVisitor.setBaseState(
            pE.getOperand2().accept(new PushStateExpressionVisitor(this.expressionVisitor, rhsState)));
        prevLhsState = pE.getOperand1().accept(this.expressionVisitor);
        prevRhsState = pE.getOperand2().accept(this.expressionVisitor);
      }
      return this.expressionVisitor.getBaseState();
    }

  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pElement, List<AbstractState> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) {

    return null;
  }

  private ExpressionToFormulaVisitor getExpressionToFormulaVisitor(final CFAEdge pEdge) {
    return new ExpressionToFormulaVisitor(new VariableNameExtractor() {

      @Override
      public String extract(CIdExpression pCIdExpression) throws UnrecognizedCCodeException {
        return getVarName(pCIdExpression, pEdge);
      }
    });
  }
}