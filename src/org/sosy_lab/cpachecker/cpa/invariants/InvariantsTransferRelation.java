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
package org.sosy_lab.cpachecker.cpa.invariants;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CollectVarsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CompoundIntervalFormulaManager;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ExpressionToFormulaVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ExpressionToFormulaVisitor.VariableNameExtractor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaCompoundStateEvaluationVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Variable;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerState;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerTransferRelation;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;

public enum InvariantsTransferRelation implements TransferRelation {

  INSTANCE;

  /**
   * Base name of the variable that is introduced to pass results from
   * returning function calls.
   */
  static final String RETURN_VARIABLE_BASE_NAME = "___cpa_temp_result_var_";

  private static final CollectVarsVisitor<CompoundInterval> COLLECT_VARS_VISITOR = new CollectVarsVisitor<>();

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pElement, Precision pPrecision, CFAEdge pEdge)
      throws CPATransferException {

    InvariantsState element = (InvariantsState) pElement;
    InvariantsPrecision precision = (InvariantsPrecision) pPrecision;

    element = getSuccessor(pEdge, precision, element);

    if (element == null) {
      return Collections.emptySet();
    }
    return Collections.singleton(element);
  }

  private InvariantsState getSuccessor(CFAEdge pEdge, InvariantsPrecision pPrecision, InvariantsState pState) throws UnrecognizedCFAEdgeException, UnrecognizedCCodeException {
    InvariantsState element = pState;
    switch (pEdge.getEdgeType()) {
    case BlankEdge:
      break;
    case FunctionReturnEdge:
      element = handleFunctionReturn(element, (CFunctionReturnEdge) pEdge, pPrecision);
      break;
    case ReturnStatementEdge:
      element = handleReturnStatement(element, (CReturnStatementEdge) pEdge, pPrecision);
      break;
    case AssumeEdge:
      element = handleAssume(element, (CAssumeEdge) pEdge, pPrecision);
      break;
    case DeclarationEdge:
      element = handleDeclaration(element, (CDeclarationEdge) pEdge, pPrecision);
      break;
    case FunctionCallEdge:
      element = handleFunctionCall(element, (CFunctionCallEdge) pEdge, pPrecision);
      break;
    case StatementEdge:
      element = handleStatement(element, (CStatementEdge) pEdge, pPrecision);
      break;
    case MultiEdge:
      Iterator<CFAEdge> edgeIterator = ((MultiEdge) pEdge).iterator();
      while (element != null && edgeIterator.hasNext()) {
        element = getSuccessor(edgeIterator.next(), pPrecision, element);
      }
      break;
    default:
      throw new UnrecognizedCFAEdgeException(pEdge);
    }
    if (element != null && pPrecision != null && !pPrecision.isRelevant(pEdge)) {
      element = element.clear();
    }
    return element;
  }

  private InvariantsState handleAssume(InvariantsState pElement, CAssumeEdge pEdge, InvariantsPrecision pPrecision) throws UnrecognizedCCodeException {
    CExpression expression = pEdge.getExpression();
    // Create a formula representing the edge expression
    InvariantsFormula<CompoundInterval> expressionFormula = expression.accept(getExpressionToFormulaVisitor(pEdge, pElement));

    /*
     * If the expression definitely evaluates to false when truth is assumed or
     * the expression definitely evaluates to true when falsehood is assumed,
     * the state is unreachable.
     */
    if (pEdge.getTruthAssumption() && pElement.definitelyImplies(CompoundIntervalFormulaManager.INSTANCE.logicalNot(expressionFormula))
        || !pEdge.getTruthAssumption() && pElement.definitelyImplies(expressionFormula)) {
      return null;
    }

    /*
     * Assume the state of the expression:
     * If truth is assumed, any non-zero value, otherwise zero.
     */
    if (!pEdge.getTruthAssumption()) {
      expressionFormula = CompoundIntervalFormulaManager.INSTANCE.logicalNot(expressionFormula);
    }
    InvariantsState result = pElement.assume(expressionFormula, pEdge);
    return result;
  }

  private InvariantsState handleDeclaration(InvariantsState pElement, CDeclarationEdge pEdge, InvariantsPrecision pPrecision) throws UnrecognizedCCodeException {
    if (!(pEdge.getDeclaration() instanceof CVariableDeclaration)) {
      return pElement;
    }

    CVariableDeclaration decl = (CVariableDeclaration) pEdge.getDeclaration();

    String varName = decl.getName();
    if (!decl.isGlobal()) {
      varName = scope(varName, pEdge.getSuccessor().getFunctionName());
    }

    InvariantsFormula<CompoundInterval> value;
    if (decl.getInitializer() != null && decl.getInitializer() instanceof CInitializerExpression) {
      CExpression init = ((CInitializerExpression)decl.getInitializer()).getExpression();
      value = init.accept(getExpressionToFormulaVisitor(pEdge, pElement));
      if (containsArrayWildcard(value)) {
        value = toConstant(value, pElement);
      }

    } else {
      value = CompoundIntervalFormulaManager.INSTANCE.asConstant(CompoundInterval.top());
    }

    value = topIfProblematicType(pElement, value, decl.getType());
    return pElement.assign(varName, value, pEdge);
  }

  private InvariantsState handleFunctionCall(final InvariantsState pElement, final CFunctionCallEdge pEdge, InvariantsPrecision pPrecision) throws UnrecognizedCCodeException {

    InvariantsState newElement = pElement;
    List<String> formalParams = pEdge.getSuccessor().getFunctionParameterNames();
    List<CParameterDeclaration> declarations = pEdge.getSuccessor().getFunctionParameters();
    List<CExpression> actualParams = pEdge.getArguments();
    int limit = Math.min(formalParams.size(), actualParams.size());
    formalParams = FluentIterable.from(formalParams).limit(limit).toList();
    actualParams = FluentIterable.from(actualParams).limit(limit).toList();

    Iterator<CParameterDeclaration> declarationIterator = declarations.iterator();
    for (Pair<String, CExpression> param : Pair.zipList(formalParams, actualParams)) {
      CExpression actualParam = param.getSecond();
      CParameterDeclaration declaration = declarationIterator.next();

      InvariantsFormula<CompoundInterval> value = actualParam.accept(getExpressionToFormulaVisitor(new VariableNameExtractor() {

        @Override
        public String extract(CExpression pCExpression) throws UnrecognizedCCodeException {
          return getVarName(pCExpression, pEdge, pEdge.getPredecessor().getFunctionName(), pElement);
        }
      }, pElement));
      if (containsArrayWildcard(value)) {
        value = toConstant(value, pElement);
      }
      String formalParam = scope(param.getFirst(), pEdge.getSuccessor().getFunctionName());

      value = topIfProblematicType(pElement, value, declaration.getType());
      newElement = newElement.assign(formalParam, value, pEdge);
    }

    return newElement;
  }

  private static CompoundInterval evaluate(InvariantsFormula<CompoundInterval> pFormula, InvariantsState pState) {
    return pFormula.accept(new FormulaCompoundStateEvaluationVisitor(), pState.getEnvironment());
  }

  private static InvariantsFormula<CompoundInterval> toConstant(InvariantsFormula<CompoundInterval> pFormula, InvariantsState pState) {
    return CompoundIntervalFormulaManager.INSTANCE.asConstant(evaluate(pFormula, pState));
  }

  private InvariantsState handleStatement(InvariantsState pElement, CStatementEdge pEdge, InvariantsPrecision pPrecision) throws UnrecognizedCCodeException {

    if (pEdge.getStatement() instanceof CAssignment) {
      CAssignment assignment = (CAssignment)pEdge.getStatement();
      ExpressionToFormulaVisitor etfv = getExpressionToFormulaVisitor(pEdge, pElement);
      CExpression leftHandSide = assignment.getLeftHandSide();
      CRightHandSide rightHandSide = assignment.getRightHandSide();
      InvariantsFormula<CompoundInterval> value = assignment.getRightHandSide().accept(etfv);
      if (CompoundIntervalFormulaManager.isDefinitelyTop(value) && rightHandSide instanceof CFunctionCallExpression) {
        CFunctionCallExpression cFunctionCallExpression = (CFunctionCallExpression) rightHandSide;
        CExpression functionNameExpression = cFunctionCallExpression.getFunctionNameExpression();
        if (functionNameExpression instanceof CIdExpression) {
          CIdExpression idExpression = (CIdExpression) functionNameExpression;
          if (idExpression.getName().equals("__VERIFIER_nondet_uint")) {
            value = CompoundIntervalFormulaManager.INSTANCE.asConstant(CompoundInterval.zero().extendToPositiveInfinity());
          }
        }
      }
      value = topIfProblematicType(pElement, value, leftHandSide.getExpressionType());
      return handleAssignment(pElement, pEdge.getPredecessor().getFunctionName(), pEdge, leftHandSide, value, pPrecision);
    }

    return pElement;
  }

  private InvariantsState handleAssignment(InvariantsState pElement, String pFunctionName, CFAEdge pEdge, CExpression pLeftHandSide, InvariantsFormula<CompoundInterval> pValue, InvariantsPrecision pPrecision) throws UnrecognizedCCodeException {
    ExpressionToFormulaVisitor etfv = getExpressionToFormulaVisitor(pEdge, pElement);
    if (pLeftHandSide instanceof CArraySubscriptExpression) {
      CArraySubscriptExpression arraySubscriptExpression = (CArraySubscriptExpression) pLeftHandSide;
      String array = getVarName(arraySubscriptExpression.getArrayExpression(), pEdge, pFunctionName);
      InvariantsFormula<CompoundInterval> subscript = arraySubscriptExpression.getSubscriptExpression().accept(etfv);
      return pElement.assignArray(array, subscript, pValue, pEdge);
    } else {
      String varName = getVarName(pLeftHandSide, pEdge, pFunctionName);
      return pElement.assign(varName, pValue, pEdge);
    }
  }

  private InvariantsFormula<CompoundInterval> topIfProblematicType(InvariantsState pElement, InvariantsFormula<CompoundInterval> pFormula, CType pType) {
    if (pType instanceof CSimpleType && ((CSimpleType) pType).isUnsigned()) {
      CompoundInterval value = evaluate(pFormula, pElement);
      if (value.containsNegative()) {
        return CompoundIntervalFormulaManager.INSTANCE.asConstant(CompoundInterval.top());
      }
    }
    return pFormula;
  }

  private InvariantsState handleReturnStatement(InvariantsState pElement, CReturnStatementEdge pEdge, InvariantsPrecision pPrecision) throws UnrecognizedCCodeException {
    String calledFunctionName = pEdge.getPredecessor().getFunctionName();
    CExpression returnedExpression = pEdge.getExpression();
    // If the return edge has no statement, no return value is passed: "return;"
    if (returnedExpression == null) {
      return pElement;
    }
    ExpressionToFormulaVisitor etfv = getExpressionToFormulaVisitor(pEdge, pElement);
    InvariantsFormula<CompoundInterval> returnedState = returnedExpression.accept(etfv);
    String returnValueName = scope(RETURN_VARIABLE_BASE_NAME, calledFunctionName);
    return pElement.assign(returnValueName, returnedState, pEdge);
  }

  private InvariantsState handleFunctionReturn(InvariantsState pElement, CFunctionReturnEdge pFunctionReturnEdge, InvariantsPrecision pPrecision)
      throws UnrecognizedCCodeException {
      CFunctionSummaryEdge summaryEdge = pFunctionReturnEdge.getSummaryEdge();

      CFunctionCall expression = summaryEdge.getExpression();

      String calledFunctionName = pFunctionReturnEdge.getPredecessor().getFunctionName();

      String returnValueName = scope(RETURN_VARIABLE_BASE_NAME, calledFunctionName);

      InvariantsFormula<CompoundInterval> value = CompoundIntervalFormulaManager.INSTANCE.asVariable(returnValueName);

      // expression is an assignment operation, e.g. a = g(b);
      if (expression instanceof CFunctionCallAssignmentStatement) {
        CFunctionCallAssignmentStatement funcExp = (CFunctionCallAssignmentStatement) expression;

        return handleAssignment(pElement, pFunctionReturnEdge.getSuccessor().getFunctionName(), pFunctionReturnEdge, funcExp.getLeftHandSide(), value, pPrecision);
      }

      Iterator<CExpression> actualParamIterator = summaryEdge.getExpression().getFunctionCallExpression().getParameterExpressions().iterator();
      InvariantsState result = pElement;
      for (String formalParamName : pFunctionReturnEdge.getPredecessor().getEntryNode().getFunctionParameterNames()) {
        if (!actualParamIterator.hasNext()) {
          break;
        }
        CExpression actualParam = actualParamIterator.next();
        InvariantsFormula<CompoundInterval> actualParamFormula = actualParam.accept(getExpressionToFormulaVisitor(summaryEdge, pElement));
        if (actualParamFormula instanceof Variable) {
          String actualParamName = ((Variable<?>) actualParamFormula).getName();
          String formalParamPrefixDeref = calledFunctionName + "::" + formalParamName + "->";
          String formalParamPrefixAccess = calledFunctionName + "::" + formalParamName + ".";
          for (Entry<? extends String, ? extends InvariantsFormula<CompoundInterval>> entry : pElement.getEnvironment().entrySet()) {
            String varName = entry.getKey();
            if (varName.startsWith(formalParamPrefixDeref)) {
              String formalParamSuffix = varName.substring(formalParamPrefixDeref.length());
              result = result.assign(actualParamName + "->" + formalParamSuffix, entry.getValue(), summaryEdge);
            } else if (varName.startsWith(formalParamPrefixAccess)) {
              String formalParamSuffix = varName.substring(formalParamPrefixAccess.length());
              result = result.assign(actualParamName + "." + formalParamSuffix, entry.getValue(), summaryEdge);
            }
          }
        }
      }

      return result;
  }

  public static String getVarName(CExpression pLhs, CFAEdge pEdge) throws UnrecognizedCCodeException {
    return getVarName(pLhs, pEdge, pEdge.getSuccessor().getFunctionName());
  }
  public static String getVarName(CExpression pLhs, CFAEdge pEdge, String pFunctionName) throws UnrecognizedCCodeException {
    return getVarName(pLhs, pEdge, pFunctionName, null);
  }

  public static String getVarName(CExpression pLhs, CFAEdge pEdge, String pFunctionName, InvariantsState pState) throws UnrecognizedCCodeException {
    if (pLhs instanceof CIdExpression) {
      CIdExpression var = (CIdExpression) pLhs;
      String varName = var.getName();
      if (var.getDeclaration() != null) {
        CSimpleDeclaration decl = var.getDeclaration();

        if (decl instanceof CDeclaration && ((CDeclaration)decl).isGlobal() || decl instanceof CEnumerator) {
        } else {
          varName = scope(varName, pFunctionName);
        }
    }
    return varName;
    } else if (pLhs instanceof CFieldReference) {
      CFieldReference fieldRef = (CFieldReference) pLhs;
      String varName = fieldRef.getFieldName();
      CExpression owner = fieldRef.getFieldOwner();
      if (owner != null) {
        varName = getVarName(owner, pEdge, pFunctionName) + (fieldRef.isPointerDereference() ? "->" : ".") + varName;
      }
      return varName;
    } else if (pLhs instanceof CArraySubscriptExpression) {
      CArraySubscriptExpression arraySubscript = (CArraySubscriptExpression) pLhs;
      CExpression subscript = arraySubscript.getSubscriptExpression();
      CExpression owner = arraySubscript.getArrayExpression();
      if (subscript instanceof CIntegerLiteralExpression) {
        CIntegerLiteralExpression literal = (CIntegerLiteralExpression) subscript;
        return String.format("%s[%d]", getVarName(owner, pEdge, pFunctionName), literal.asLong()).toString();
      } else if (pState != null) {
        CompoundInterval subscriptValue = evaluate(subscript.accept(InvariantsTransferRelation.INSTANCE.getExpressionToFormulaVisitor(pEdge, pState)), pState);
        if (subscriptValue.isSingleton()) {
          return String.format("%s[%d]", getVarName(owner, pEdge, pFunctionName), subscriptValue.getValue()).toString();
        }
      }
      return String.format("%s[*]", getVarName(owner, pEdge, pFunctionName)).toString();
    } else if (pLhs instanceof CPointerExpression) {
      CPointerExpression pe = (CPointerExpression) pLhs;
      if (pe.getOperand() instanceof CLeftHandSide) {
        return String.format("*(%s)", getVarName(pe.getOperand(), pEdge));
      }
      return pLhs.toString();
    } else if (pLhs instanceof CCastExpression) {
      CCastExpression cast = (CCastExpression) pLhs;
      return getVarName(cast.getOperand(), pEdge);
    } else {
      return pLhs.toString(); // This actually seems wrong but is currently the only way to deal with some cases of pointer arithmetics
    }
  }

  static String scope(String pVar, String pFunction) {
    return pFunction + "::" + pVar;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pElement, List<AbstractState> pOtherElements,
      CFAEdge pCfaEdge, Precision pPrecision) throws UnrecognizedCCodeException {

    InvariantsState state = (InvariantsState) pElement;
    CFAEdge edge = pCfaEdge;
    if (edge instanceof MultiEdge) {
      for (CFAEdge subEdge : ((MultiEdge) edge)) {
        InvariantsState current = state;
        Collection<? extends AbstractState> next = strengthen(state, pOtherElements, subEdge, pPrecision);
        if (next == null) {
          state = current;
        } else if (next.isEmpty()) {
          return next;
        } else {
          state = (InvariantsState) Iterables.getOnlyElement(next);
        }
      }
      return Collections.singleton(state);
    }
    CLeftHandSide leftHandSide = getLeftHandSide(edge);
    if (leftHandSide instanceof CPointerExpression || leftHandSide instanceof CFieldReference && ((CFieldReference) leftHandSide).isPointerDereference()) {
      FluentIterable<PointerState> pointerStates = FluentIterable.from(pOtherElements).filter(PointerState.class);
      if (pointerStates.isEmpty()) {
        return Collections.singleton(state.clear());
      }
      InvariantsState result = state;
      for (PointerState pointerState : pointerStates) {
        LocationSet locationSet = PointerTransferRelation.asLocations(leftHandSide, pointerState);
        if (locationSet.isTop()) {
          return Collections.singleton(state.clear());
        }
        InvariantsFormula<CompoundInterval> top = CompoundIntervalFormulaManager.INSTANCE.asConstant(CompoundInterval.top());
        for (String location : PointerTransferRelation.toNormalSet(pointerState, locationSet)) {
          if (location.contains("->") || location.contains(".")) {
            int lastIndexOfDot = location.lastIndexOf('.');
            int lastIndexOfArrow = location.lastIndexOf("->");
            if (lastIndexOfArrow >= 0) {
              ++lastIndexOfArrow;
            }
            int lastIndexOfSep = Math.max(lastIndexOfDot, lastIndexOfArrow);
            assert lastIndexOfSep >= 0;
            String end = location.substring(lastIndexOfSep + 1);
            for (String variableName : result.getEnvironment().keySet()) {
              if (variableName.endsWith("->" + end)
                  || variableName.endsWith("." + end)) {
                result = result.assign(variableName, top, edge);
              }
            }
          }
          result = result.assign(location, top, edge);
        }
      }
      return Collections.singleton(result);
    }
    return null;
  }

  public ExpressionToFormulaVisitor getExpressionToFormulaVisitor(final CFAEdge pEdge) {
    return getExpressionToFormulaVisitor(pEdge, null);
  }

  public ExpressionToFormulaVisitor getExpressionToFormulaVisitor(final VariableNameExtractor pVariableNameExtractor) {
    return getExpressionToFormulaVisitor(pVariableNameExtractor, null);
  }

  private ExpressionToFormulaVisitor getExpressionToFormulaVisitor(final CFAEdge pEdge, final @Nullable InvariantsState pState) {
    return getExpressionToFormulaVisitor(new VariableNameExtractor() {

      @Override
      public String extract(CExpression pCExpression) throws UnrecognizedCCodeException {
        return getVarName(pCExpression, pEdge);
      }
    }, pState);
  }

  private ExpressionToFormulaVisitor getExpressionToFormulaVisitor(final VariableNameExtractor pVariableNameExtractor, final @Nullable InvariantsState pState) {
    final Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> environment;
    environment = pState == null ? Collections.<String, InvariantsFormula<CompoundInterval>>emptyMap() : pState.getEnvironment();
    return new ExpressionToFormulaVisitor(pVariableNameExtractor, environment);
  }

  private boolean containsArrayWildcard(InvariantsFormula<CompoundInterval> pFormula) {
    for (String pVarName : pFormula.accept(COLLECT_VARS_VISITOR)) {
      if (pVarName.contains("[*]")) {
        return true;
      }
    }
    return false;
  }

  private CLeftHandSide getLeftHandSide(CFAEdge pEdge) {
    if (pEdge instanceof CStatementEdge) {
      CStatementEdge statementEdge = (CStatementEdge) pEdge;
      if (statementEdge.getStatement() instanceof CAssignment) {
        CAssignment assignment = (CAssignment)statementEdge.getStatement();
        return assignment.getLeftHandSide();
      }
    } else if (pEdge instanceof CFunctionCallEdge) {
      CFunctionCallEdge functionCallEdge = (CFunctionCallEdge) pEdge;
      CFunctionCall functionCall = functionCallEdge.getSummaryEdge().getExpression();
      if (functionCall instanceof CFunctionCallAssignmentStatement) {
        CFunctionCallAssignmentStatement assignment = (CFunctionCallAssignmentStatement) functionCall;
        return assignment.getLeftHandSide();
      }
    }
    return null;
  }
}