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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
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
    CompoundState expressionState = expressionFormula.accept(resolver, pElement.getEnvironment());
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

    final InvariantsFormula<CompoundState> value;
    if (decl.getInitializer() != null && decl.getInitializer() instanceof CInitializerExpression) {
      CExpression init = ((CInitializerExpression)decl.getInitializer()).getExpression();
      value = init.accept(getExpressionToFormulaVisitor(pEdge));
    } else {
      value = InvariantsFormulaManager.INSTANCE.asConstant(CompoundState.top());
    }

    return pElement.assign(varName, value, pEdge.toString());
  }

  private InvariantsState handleFunctionCall(InvariantsState pElement, CFunctionCallEdge pEdge) throws UnrecognizedCCodeException {

    InvariantsState newElement = pElement;
    List<String> formalParams = pEdge.getSuccessor().getFunctionParameterNames();
    List<CExpression> actualParams = pEdge.getArguments();

    for (Pair<String, CExpression> param : Pair.zipList(formalParams, actualParams)) {
      CExpression actualParam = param.getSecond();

      InvariantsFormula<CompoundState> value = actualParam.accept(getExpressionToFormulaVisitor(pEdge));

      String formalParam = scope(param.getFirst(), pEdge.getSuccessor().getFunctionName());
      newElement = newElement.assign(formalParam, value, pEdge.toString());
    }

    return newElement;
  }

  private InvariantsState handleStatement(InvariantsState pElement, CStatementEdge pEdge) throws UnrecognizedCCodeException {

    if (pEdge.getStatement() instanceof CAssignment) {
      CAssignment assignment = (CAssignment)pEdge.getStatement();
      ExpressionToFormulaVisitor etfv = getExpressionToFormulaVisitor(pEdge);
      InvariantsFormula<CompoundState> value = assignment.getRightHandSide().accept(etfv);
      CExpression leftHandSide = assignment.getLeftHandSide();
      if (leftHandSide instanceof CArraySubscriptExpression) {
        CArraySubscriptExpression arraySubscriptExpression = (CArraySubscriptExpression) leftHandSide;
        String array = getVarName(arraySubscriptExpression.getArrayExpression(), pEdge);
        InvariantsFormula<CompoundState> subscript = arraySubscriptExpression.getSubscriptExpression().accept(etfv);
        return pElement.assignArray(array, subscript, value, pEdge.toString());
      } else {
        String varName = getVarName(leftHandSide, pEdge);
        return pElement.assign(varName, value, pEdge.toString());
      }
    }

    return pElement;
  }

  private InvariantsState handleReturnStatement(InvariantsState pElement, CReturnStatementEdge pEdge) throws UnrecognizedCCodeException {
    String calledFunctionName = pEdge.getPredecessor().getFunctionName();
    CExpression returnedExpression = pEdge.getExpression();
    InvariantsFormula<CompoundState> returnedState = returnedExpression.accept(getExpressionToFormulaVisitor(pEdge));
    String returnValueName = scope(RETURN_VARIABLE_BASE_NAME, calledFunctionName);
    return pElement.assign(returnValueName, returnedState, pEdge.toString());
  }

  private InvariantsState handleFunctionReturn(InvariantsState pElement, CFunctionReturnEdge pFunctionReturnEdge)
      throws UnrecognizedCCodeException {
      CFunctionSummaryEdge summaryEdge = pFunctionReturnEdge.getSummaryEdge();

      CFunctionCall expression = summaryEdge.getExpression();

      String callerFunctionName = pFunctionReturnEdge.getSuccessor().getFunctionName();
      String calledFunctionName = pFunctionReturnEdge.getPredecessor().getFunctionName();

      String returnValueName = scope(RETURN_VARIABLE_BASE_NAME, calledFunctionName);

      InvariantsFormula<CompoundState> value = InvariantsFormulaManager.INSTANCE.asVariable(returnValueName);

      // expression is an assignment operation, e.g. a = g(b);
      if (expression instanceof CFunctionCallAssignmentStatement) {
        CFunctionCallAssignmentStatement funcExp = (CFunctionCallAssignmentStatement)expression;

        CExpression operand1 = funcExp.getLeftHandSide();

        // left hand side of the expression has to be a variable
        if (operand1 instanceof CIdExpression) {
          final String varName = scope(((CIdExpression) operand1).getName(), callerFunctionName);
          pElement = pElement.assign(varName, value, pFunctionReturnEdge.toString());
        }
      }
      return pElement;
  }

  static String getVarName(CExpression lhs, CFAEdge edge) throws UnrecognizedCCodeException {
    if (lhs instanceof CIdExpression) {
      CIdExpression var = (CIdExpression) lhs;
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
    } else if (lhs instanceof CFieldReference) {
      CFieldReference fieldRef = (CFieldReference) lhs;
      String varName = fieldRef.getFieldName();
      CExpression owner = fieldRef.getFieldOwner();
      if (owner != null) {
        varName = getVarName(owner, edge) + "." + varName;
      }
      return varName;
    } else if (lhs instanceof CArraySubscriptExpression) {
      CArraySubscriptExpression arraySubscript = (CArraySubscriptExpression) lhs;
      CExpression subscript = arraySubscript.getSubscriptExpression();
      CExpression owner = arraySubscript.getArrayExpression();
      if (subscript instanceof CIntegerLiteralExpression) {
        CIntegerLiteralExpression literal = (CIntegerLiteralExpression) subscript;
        return getVarName(owner, edge) + "[" + literal.asLong()  + "]";
      } else {
        return getVarName(owner, edge) + "[*]";
      }
    }
    throw new UnrecognizedCCodeException("unknown left hand side of assignment", edge, lhs);
  }

  private static String scope(String var, String function) {
    return function + "::" + var;
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
      public String extract(CExpression pCExpression) throws UnrecognizedCCodeException {
        return getVarName(pCExpression, pEdge);
      }
    });
  }
}