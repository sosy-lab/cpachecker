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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CollectVarsVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.CompoundIntervalFormulaManager;
import org.sosy_lab.cpachecker.cpa.invariants.formula.ExpressionToFormulaVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.FormulaCompoundStateEvaluationVisitor;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.Variable;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerState;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerTransferRelation;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

class InvariantsTransferRelation extends SingleEdgeTransferRelation {

  //set of functions that may not appear in the source code
 // the value of the map entry is the explanation for the user
 private static final Map<String, String> UNSUPPORTED_FUNCTIONS
     = ImmutableMap.of("pthread_create", "threads");

  static final InvariantsTransferRelation INSTANCE = new InvariantsTransferRelation();

  /**
   * Base name of the variable that is introduced to pass results from
   * returning function calls.
   */
  static final String RETURN_VARIABLE_BASE_NAME = "___cpa_temp_result_var_";

  private static final CollectVarsVisitor<CompoundInterval> COLLECT_VARS_VISITOR = new CollectVarsVisitor<>();

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pEdge)
      throws CPATransferException {

    InvariantsState state = (InvariantsState) pState;
    InvariantsPrecision precision = (InvariantsPrecision) pPrecision;

    state = getSuccessor(pEdge, precision, state);

    if (state == null) {
      return Collections.emptySet();
    }

    state = state.updateAbstractionState(precision, pEdge);

    return Collections.singleton(state);
  }

  private InvariantsState getSuccessor(CFAEdge pEdge, InvariantsPrecision pPrecision, InvariantsState pState) throws UnrecognizedCFAEdgeException, UnrecognizedCodeException {
    InvariantsState state = pState.setTypes(getInvolvedVariables(pEdge));
    switch (pEdge.getEdgeType()) {
    case BlankEdge:
      break;
    case FunctionReturnEdge:
      state = handleFunctionReturn(state, (CFunctionReturnEdge) pEdge, pPrecision);
      break;
    case ReturnStatementEdge:
      state = handleReturnStatement(state, (CReturnStatementEdge) pEdge, pPrecision);
      break;
    case AssumeEdge:
      state = handleAssume(state, (CAssumeEdge) pEdge, pPrecision);
      break;
    case DeclarationEdge:
      state = handleDeclaration(state, (CDeclarationEdge) pEdge, pPrecision);
      break;
    case FunctionCallEdge:
      state = handleFunctionCall(state, (CFunctionCallEdge) pEdge, pPrecision);
      break;
    case StatementEdge:
      state = handleStatement(state, (CStatementEdge) pEdge, pPrecision);
      break;
    case MultiEdge:
      Iterator<CFAEdge> edgeIterator = ((MultiEdge) pEdge).iterator();
      while (state != null && edgeIterator.hasNext()) {
        state = getSuccessor(edgeIterator.next(), pPrecision, state);
      }
      break;
    default:
      throw new UnrecognizedCFAEdgeException(pEdge);
    }
    if (state != null && pPrecision != null && !pPrecision.isRelevant(pEdge)) {
      state = state.clear();
    }
    return state;
  }

  private InvariantsState handleAssume(InvariantsState pElement, CAssumeEdge pEdge, InvariantsPrecision pPrecision) throws UnrecognizedCodeException {
    CExpression expression = pEdge.getExpression();

    // Create a formula representing the edge expression
    InvariantsFormula<CompoundInterval> expressionFormula = expression.accept(getExpressionToFormulaVisitor(pEdge, pElement));
    if (!pEdge.getTruthAssumption()) {
      expressionFormula = CompoundIntervalFormulaManager.INSTANCE.logicalNot(expressionFormula);
    }

    return handleAssumption(pElement, pEdge, expressionFormula, pPrecision);
  }

  private InvariantsState handleAssumption(InvariantsState pState, CFAEdge pEdge, InvariantsFormula<CompoundInterval> pAssumption, InvariantsPrecision pPrecision) {
    /*
     * If the expression definitely evaluates to false, the state is unreachable.
     */
    if (pState.definitelyImplies(CompoundIntervalFormulaManager.INSTANCE.logicalNot(pAssumption))) {
      return null;
    }

    /*
     * Assume the state of the expression:
     */
    InvariantsState result = pState.assume(pAssumption);
    return result;
  }

  private InvariantsState handleDeclaration(InvariantsState pElement, CDeclarationEdge pEdge, InvariantsPrecision pPrecision) throws UnrecognizedCodeException {
    if (!(pEdge.getDeclaration() instanceof CVariableDeclaration)) {
      return pElement;
    }

    CVariableDeclaration decl = (CVariableDeclaration) pEdge.getDeclaration();

    String varName = decl.getName();
    if (!decl.isGlobal()) {
      varName = VariableNameExtractor.scope(varName, pEdge.getSuccessor().getFunctionName());
    }

    InvariantsFormula<CompoundInterval> value;
    if (decl.getInitializer() != null && decl.getInitializer() instanceof CInitializerExpression) {
      CExpression init = ((CInitializerExpression)decl.getInitializer()).getExpression();
      value = init.accept(getExpressionToFormulaVisitor(pEdge, pElement));
      if (containsArrayWildcard(value)) {
        value = toConstant(value, pElement.getEnvironment());
      }

    } else {
      value = CompoundIntervalFormulaManager.INSTANCE.asConstant(CompoundInterval.top());
    }

    value = handlePotentialOverflow(pElement, value, decl.getType());
    return pElement.assign(varName, value);
  }

  private InvariantsState handleFunctionCall(final InvariantsState pElement, final CFunctionCallEdge pEdge, InvariantsPrecision pPrecision) throws UnrecognizedCodeException {

    InvariantsState newElement = pElement;
    List<String> formalParams = pEdge.getSuccessor().getFunctionParameterNames();
    List<CParameterDeclaration> declarations = pEdge.getSuccessor().getFunctionParameters();
    List<CExpression> actualParams = pEdge.getArguments();
    int limit = Math.min(formalParams.size(), actualParams.size());

    ExpressionToFormulaVisitor actualParamExpressionToFormulaVisitor =
        getExpressionToFormulaVisitor(new VariableNameExtractor(pEdge, true, pElement.getEnvironment()), pElement);

    if (limit == 1 && "__VERIFIER_assume".equals(pEdge.getSuccessor().getFunctionName())) {
      return handleAssumption(pElement, pEdge, actualParams.get(0).accept(
          actualParamExpressionToFormulaVisitor), pPrecision);
    }

    formalParams = FluentIterable.from(formalParams).limit(limit).toList();
    actualParams = FluentIterable.from(actualParams).limit(limit).toList();

    Iterator<CParameterDeclaration> declarationIterator = declarations.iterator();
    for (Pair<String, CExpression> param : Pair.zipList(formalParams, actualParams)) {
      CExpression actualParam = param.getSecond();
      CParameterDeclaration declaration = declarationIterator.next();

      InvariantsFormula<CompoundInterval> value = actualParam.accept(actualParamExpressionToFormulaVisitor);
      if (containsArrayWildcard(value)) {
        value = toConstant(value, pElement.getEnvironment());
      }
      String formalParam = VariableNameExtractor.scope(param.getFirst(), pEdge.getSuccessor().getFunctionName());

      value = handlePotentialOverflow(pElement, value, declaration.getType());
      newElement = newElement.assign(formalParam, value);
    }

    return newElement;
  }

  private static CompoundInterval evaluate(
      InvariantsFormula<CompoundInterval> pFormula,
      Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return pFormula.accept(new FormulaCompoundStateEvaluationVisitor(), pEnvironment);
  }

  private static InvariantsFormula<CompoundInterval> toConstant(
      InvariantsFormula<CompoundInterval> pFormula,
      Map<? extends String, ? extends InvariantsFormula<CompoundInterval>> pEnvironment) {
    return CompoundIntervalFormulaManager.INSTANCE.asConstant(evaluate(pFormula, pEnvironment));
  }

  private InvariantsState handleStatement(InvariantsState pElement, CStatementEdge pEdge, InvariantsPrecision pPrecision) throws UnrecognizedCodeException {

    if (pEdge.getStatement() instanceof CFunctionCall) {
      CExpression fn = ((CFunctionCall) pEdge.getStatement()).getFunctionCallExpression().getFunctionNameExpression();
      if (fn instanceof CIdExpression) {
        String func = ((CIdExpression)fn).getName();
        if (UNSUPPORTED_FUNCTIONS.containsKey(func)) {
          throw new UnsupportedCCodeException(UNSUPPORTED_FUNCTIONS.get(func), pEdge, fn);
        }
      }
    }

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
      value = handlePotentialOverflow(pElement, value, leftHandSide.getExpressionType());
      return handleAssignment(pElement, pEdge.getPredecessor().getFunctionName(), pEdge, leftHandSide, value, pPrecision);
    }

    return pElement;
  }

  private InvariantsState handleAssignment(
      InvariantsState pElement,
      String pFunctionName,
      CFAEdge pEdge,
      CExpression pLeftHandSide,
      InvariantsFormula<CompoundInterval> pValue,
      InvariantsPrecision pPrecision) throws UnrecognizedCodeException {
    ExpressionToFormulaVisitor etfv = getExpressionToFormulaVisitor(pEdge, pElement);
    VariableNameExtractor variableNameExtractor = new VariableNameExtractor(pEdge, pElement.getEnvironment());
    if (pLeftHandSide instanceof CArraySubscriptExpression) {
      CArraySubscriptExpression arraySubscriptExpression = (CArraySubscriptExpression) pLeftHandSide;
      String array = variableNameExtractor.getVarName(arraySubscriptExpression.getArrayExpression());
      InvariantsFormula<CompoundInterval> subscript = arraySubscriptExpression.getSubscriptExpression().accept(etfv);
      return pElement.assignArray(array, subscript, pValue);
    } else {
      String varName = variableNameExtractor.getVarName(pLeftHandSide);
      return pElement.assign(varName, pValue);
    }
  }

  private InvariantsFormula<CompoundInterval> handlePotentialOverflow(
      InvariantsState pState,
      InvariantsFormula<CompoundInterval> pFormula,
      CType pType) {
    return ExpressionToFormulaVisitor.handlePotentialOverflow(pFormula, pState.getMachineModel(), pType, pState.getEnvironment());
  }

  private InvariantsState handleReturnStatement(InvariantsState pElement, CReturnStatementEdge pEdge, InvariantsPrecision pPrecision) throws UnrecognizedCodeException {
    String calledFunctionName = pEdge.getPredecessor().getFunctionName();
    // If the return edge has no statement, no return value is passed: "return;"
    if (!pEdge.getExpression().isPresent()) {
      return pElement;
    }
    ExpressionToFormulaVisitor etfv = getExpressionToFormulaVisitor(pEdge, pElement);
    InvariantsFormula<CompoundInterval> returnedState = pEdge.getExpression().get().accept(etfv);
    String returnValueName = VariableNameExtractor.scope(RETURN_VARIABLE_BASE_NAME, calledFunctionName);
    return pElement.assign(returnValueName, returnedState);
  }

  private InvariantsState handleFunctionReturn(InvariantsState pElement, CFunctionReturnEdge pFunctionReturnEdge, InvariantsPrecision pPrecision)
      throws UnrecognizedCodeException {
      CFunctionSummaryEdge summaryEdge = pFunctionReturnEdge.getSummaryEdge();

      CFunctionCall expression = summaryEdge.getExpression();

      String calledFunctionName = pFunctionReturnEdge.getPredecessor().getFunctionName();

      String returnValueName = VariableNameExtractor.scope(RETURN_VARIABLE_BASE_NAME, calledFunctionName);

      InvariantsFormula<CompoundInterval> value = CompoundIntervalFormulaManager.INSTANCE.asVariable(returnValueName);

      InvariantsState result = pElement;

      // expression is an assignment operation, e.g. a = g(b);
      if (expression instanceof CFunctionCallAssignmentStatement) {
        CFunctionCallAssignmentStatement funcExp = (CFunctionCallAssignmentStatement) expression;

        result = handleAssignment(pElement, pFunctionReturnEdge.getSuccessor().getFunctionName(), pFunctionReturnEdge, funcExp.getLeftHandSide(), value, pPrecision);
      } else {
        Iterator<CExpression> actualParamIterator = summaryEdge.getExpression().getFunctionCallExpression().getParameterExpressions().iterator();
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
                result = result.assign(actualParamName + "->" + formalParamSuffix, entry.getValue());
              } else if (varName.startsWith(formalParamPrefixAccess)) {
                String formalParamSuffix = varName.substring(formalParamPrefixAccess.length());
                result = result.assign(actualParamName + "." + formalParamSuffix, entry.getValue());
              }
            }
          }
        }
      }

      // Remove all variables that are in the scope of the returning function
      for (String variableName : result.getEnvironment().keySet()) {
        if (VariableNameExtractor.isFunctionScoped(variableName, calledFunctionName)) {
          result = result.clear(variableName);
        }
      }

      return result;
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
        Iterable<String> locations = PointerTransferRelation.toNormalSet(pointerState, locationSet);
        boolean moreThanOneLocation = hasMoreThanNElements(locations, 1);
        for (String location : locations) {
          int lastIndexOfDot = location.lastIndexOf('.');
          int lastIndexOfArrow = location.lastIndexOf("->");
          final boolean hasDot = lastIndexOfDot >= 0;
          final boolean hasArrow = lastIndexOfArrow >= 0;
          if (hasArrow || hasDot) {
            if (hasArrow) {
              ++lastIndexOfArrow;
            }
            int lastIndexOfSep = Math.max(lastIndexOfDot, lastIndexOfArrow);
            final String end = location.substring(lastIndexOfSep + 1);
            Iterable<? extends String> targets = FluentIterable.from(result.getEnvironment().keySet()).filter(new Predicate<String>() {

              @Override
              public boolean apply(String pVar) {
                return pVar != null && (pVar.endsWith("." + end) || pVar.endsWith("->" + end));
              }

            });
            if (moreThanOneLocation || hasMoreThanNElements(targets, 1)) {
              for (String variableName : targets) {
                result = result.assign(variableName, top);
              }
            }
          } else if (moreThanOneLocation) {
            result = result.assign(location, top);
          }
        }
      }
      return Collections.singleton(result);
    }
    return null;
  }

  private static ExpressionToFormulaVisitor getExpressionToFormulaVisitor(final CFAEdge pEdge, final InvariantsState pState) {
    return getExpressionToFormulaVisitor(new VariableNameExtractor(pEdge, pState.getEnvironment()), pState);
  }

  private static ExpressionToFormulaVisitor getExpressionToFormulaVisitor(final VariableNameExtractor pVariableNameExtractor, final InvariantsState pState) {
    return new ExpressionToFormulaVisitor(pVariableNameExtractor, pState.getEnvironment());
  }

  private static boolean containsArrayWildcard(InvariantsFormula<CompoundInterval> pFormula) {
    for (String pVarName : pFormula.accept(COLLECT_VARS_VISITOR)) {
      if (pVarName.contains("[*]")) {
        return true;
      }
    }
    return false;
  }

  private static CLeftHandSide getLeftHandSide(CFAEdge pEdge) {
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

  private static boolean hasMoreThanNElements(Iterable<?> pIterable, int pN) {
    return !Iterables.isEmpty(Iterables.skip(pIterable, pN));
  }

  /**
   * Gets the variables involved in the given edge.
   *
   * @param pCfaEdge the edge to be analyzed.
   * @param pVariableClassification the variable classification.
   *
   * @return the variables involved in the given edge.
   */
  public Map<String, CType> getInvolvedVariables(CFAEdge pCfaEdge) {
    switch (pCfaEdge.getEdgeType()) {
    case AssumeEdge: {
      AssumeEdge assumeEdge = (AssumeEdge) pCfaEdge;
      AExpression expression = assumeEdge.getExpression();
      return getInvolvedVariables(expression, pCfaEdge);
    }
    case MultiEdge: {
      MultiEdge multiEdge = (MultiEdge) pCfaEdge;
      Map<String, CType> result = new HashMap<>();
      for (CFAEdge edge : multiEdge) {
        result.putAll(getInvolvedVariables(edge));
      }
      return result;
    }
    case DeclarationEdge: {
      ADeclarationEdge declarationEdge = (ADeclarationEdge) pCfaEdge;
      ADeclaration declaration = declarationEdge.getDeclaration();
      if (declaration instanceof CVariableDeclaration) {
        CVariableDeclaration variableDeclaration = (CVariableDeclaration) declaration;
        String declaredVariable = variableDeclaration.getQualifiedName();
        CType type = variableDeclaration.getType();
        CInitializer initializer = variableDeclaration.getInitializer();
        if (initializer == null) {
          return Collections.singletonMap(declaredVariable, type);
        }
        Map<String, CType> result = new HashMap<>();
        result.put(declaredVariable, type);
        result.putAll(getInvolvedVariables(initializer, pCfaEdge));
        return result;
      } else if (declaration instanceof AVariableDeclaration) {
        throw new UnsupportedOperationException("Only C expressions are supported");
      } else {
        return Collections.emptyMap();
      }
    }
    case FunctionCallEdge: {
      FunctionCallEdge functionCallEdge = (FunctionCallEdge) pCfaEdge;
      Map<String, CType> result = new HashMap<>();
      for (AExpression argument : functionCallEdge.getArguments()) {
        result.putAll(getInvolvedVariables(argument, pCfaEdge));
      }
      for (AExpression parameter : functionCallEdge.getSummaryEdge().getExpression().getFunctionCallExpression().getParameterExpressions()) {
        result.putAll(getInvolvedVariables(parameter, pCfaEdge));
      }
      return result;
    }
    case ReturnStatementEdge: {
      AReturnStatementEdge returnStatementEdge = (AReturnStatementEdge) pCfaEdge;
      if (returnStatementEdge.getExpression().isPresent()) {
        AExpression returnExpression = returnStatementEdge.getExpression().get();
        Map<String, CType> result = new HashMap<>();
        result.put(VariableNameExtractor.scope(RETURN_VARIABLE_BASE_NAME, pCfaEdge.getSuccessor().getFunctionName()),
            (CType) returnExpression.getExpressionType());
        result.putAll(getInvolvedVariables(returnExpression, pCfaEdge));
        return result;
      }
      return Collections.emptyMap();
    }
    case StatementEdge: {
      AStatementEdge statementEdge = (AStatementEdge) pCfaEdge;
      AStatement statement = statementEdge.getStatement();
      if (statement instanceof AExpressionAssignmentStatement) {
        AExpressionAssignmentStatement expressionAssignmentStatement = (AExpressionAssignmentStatement) statement;
        Map<String, CType> result = new HashMap<>();
        result.putAll(getInvolvedVariables(expressionAssignmentStatement.getLeftHandSide(), pCfaEdge));
        result.putAll(getInvolvedVariables(expressionAssignmentStatement.getRightHandSide(), pCfaEdge));
        return result;
      } else if (statement instanceof AExpressionStatement) {
        return getInvolvedVariables(((AExpressionStatement) statement).getExpression(), pCfaEdge);
      } else if (statement instanceof AFunctionCallAssignmentStatement) {
        AFunctionCallAssignmentStatement functionCallAssignmentStatement = (AFunctionCallAssignmentStatement) statement;
        Map<String, CType> result = new HashMap<>();
        result.putAll(getInvolvedVariables(functionCallAssignmentStatement.getLeftHandSide(), pCfaEdge));
        AFunctionCallExpression functionCallExpression = functionCallAssignmentStatement.getFunctionCallExpression();
        for (AExpression expression : functionCallExpression.getParameterExpressions()) {
          result.putAll(getInvolvedVariables(expression, pCfaEdge));
        }
        return result;
      } else if (statement instanceof AFunctionCallStatement) {
        AFunctionCallStatement functionCallStatement = (AFunctionCallStatement) statement;
        Map<String, CType> result = new HashMap<>();
        for (AExpression expression : functionCallStatement.getFunctionCallExpression().getParameterExpressions()) {
          result.putAll(getInvolvedVariables(expression, pCfaEdge));
        }
        return result;
      } else {
        return Collections.emptyMap();
      }
    }
    case FunctionReturnEdge:
      FunctionReturnEdge functionReturnEdge = (FunctionReturnEdge) pCfaEdge;
      AFunctionCall functionCall = functionReturnEdge.getSummaryEdge().getExpression();
      if (functionCall instanceof AFunctionCallAssignmentStatement) {
        AFunctionCallAssignmentStatement functionCallAssignmentStatement = (AFunctionCallAssignmentStatement) functionCall;
        AFunctionCallExpression functionCallExpression = functionCall.getFunctionCallExpression();
        if (functionCallExpression != null) {
          Map<String, CType> result = new HashMap<>();
          result.put(VariableNameExtractor.scope(RETURN_VARIABLE_BASE_NAME, pCfaEdge.getPredecessor().getFunctionName()),
              (CType) functionCallExpression.getExpressionType());
          result.putAll(getInvolvedVariables(functionCallAssignmentStatement.getLeftHandSide(), pCfaEdge));
          return result;
        }
      }
      return Collections.emptyMap();
    case BlankEdge:
    case CallToReturnEdge:
    default:
      return Collections.emptyMap();
    }
  }

  /**
   * Gets the variables involved in the given CInitializer.
   *
   * @param pCInitializer the CInitializer to be analyzed.
   * @param pVariableClassification the variable classification.
   *
   * @return the variables involved in the given CInitializer.
   */
  private Map<String, CType> getInvolvedVariables(CInitializer pCInitializer, CFAEdge pCfaEdge) {
    if (pCInitializer instanceof CDesignatedInitializer) {
      return getInvolvedVariables(((CDesignatedInitializer) pCInitializer).getRightHandSide(), pCfaEdge);
    } else if (pCInitializer instanceof CInitializerExpression) {
      return getInvolvedVariables(((CInitializerExpression) pCInitializer).getExpression(), pCfaEdge);
    } else if (pCInitializer instanceof CInitializerList) {
      CInitializerList initializerList = (CInitializerList) pCInitializer;
      Map<String, CType> result = new HashMap<>();
      for (CInitializer initializer : initializerList.getInitializers()) {
        result.putAll(getInvolvedVariables(initializer, pCfaEdge));
      }
      return result;
    }
    return Collections.emptyMap();
  }

  /**
   * Gets the variables involved in the given expression.
   *
   * @param pExpression the expression to be analyzed.
   * @param pVariableClassification the variable classification.
   *
   * @return the variables involved in the given expression.
   */
  public static Map<String, CType> getInvolvedVariables(AExpression pExpression, CFAEdge pCfaEdge) {
    if (pExpression == null) {
      return Collections.emptyMap();
    } if (pExpression instanceof CExpression) {
      Map<String, CType> result = new HashMap<>();

      for (ALeftHandSide leftHandSide : ((CExpression) pExpression).accept(new DefaultCExpressionVisitor<Iterable<ALeftHandSide>, RuntimeException>() {

        @Override
        protected Iterable<ALeftHandSide> visitDefault(CExpression pExp) {
          return Collections.emptySet();
        }

        @Override
        public Iterable<ALeftHandSide> visit(CArraySubscriptExpression pIastArraySubscriptExpression) {
          return Collections.<ALeftHandSide>singleton(pIastArraySubscriptExpression);
        }

        @Override
        public Iterable<ALeftHandSide> visit(CFieldReference pIastFieldReference) {
          return Collections.<ALeftHandSide>singleton(pIastFieldReference);
        }

        @Override
        public Iterable<ALeftHandSide> visit(CIdExpression pIastIdExpression) {
          return Collections.<ALeftHandSide>singleton(pIastIdExpression);
        }

        @Override
        public Iterable<ALeftHandSide> visit(CPointerExpression pPointerExpression) {
          return Collections.<ALeftHandSide>singleton(pPointerExpression);
        }

        @Override
        public Iterable<ALeftHandSide> visit(CComplexCastExpression pComplexCastExpression) {
          return pComplexCastExpression.getOperand().accept(this);
        }

        @Override
        public Iterable<ALeftHandSide> visit(CBinaryExpression pIastBinaryExpression) {
          return Iterables.concat(pIastBinaryExpression.getOperand1().accept(this), pIastBinaryExpression.getOperand2().accept(this));
        }

        @Override
        public Iterable<ALeftHandSide> visit(CCastExpression pIastCastExpression) {
          return pIastCastExpression.getOperand().accept(this);
        }

        @Override
        public Iterable<ALeftHandSide> visit(CUnaryExpression pIastUnaryExpression) {
          return pIastUnaryExpression.getOperand().accept(this);
        }
      }
        )) {
        InvariantsFormula<CompoundInterval> formula;
        try {
          formula = ((CExpression) leftHandSide).accept(new ExpressionToFormulaVisitor(
              new VariableNameExtractor(
                  pCfaEdge,
                  Collections.<String, InvariantsFormula<CompoundInterval>>emptyMap())));

          for (String variableName : formula.accept(COLLECT_VARS_VISITOR)) {
            result.put(variableName, (CType) leftHandSide.getExpressionType());
          }
        } catch (UnrecognizedCodeException e) {
          // Don't record the variable name then
        }
      }

      return result;
    } else {
      throw new UnsupportedOperationException("Only C expressions are supported");
    }
  }
}
