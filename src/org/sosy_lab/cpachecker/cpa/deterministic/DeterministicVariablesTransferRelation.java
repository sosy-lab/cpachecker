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
package org.sosy_lab.cpachecker.cpa.deterministic;

import static org.sosy_lab.cpachecker.util.LiveVariables.LIVE_DECL_EQUIVALENCE;
import static org.sosy_lab.cpachecker.util.LiveVariables.TO_EQUIV_WRAPPER;

import com.google.common.base.Equivalence.Wrapper;
import com.google.common.collect.Iterables;

import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AInitializer;
import org.sosy_lab.cpachecker.cfa.ast.AInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatisticsUtils;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeterministicVariablesTransferRelation
  extends ForwardingTransferRelation<DeterministicVariablesState, DeterministicVariablesState, Precision> implements Statistics {

  private StatCounter numberOfAssumes = new StatCounter("Number of assume edges");
  private StatCounter numberOfNonDetAssumes = new StatCounter("Number of non-deterministic assume edges");

  private Set<CFANode> assumes = new HashSet<>();
  private Set<CFANode> nondetAssumes = new HashSet<>();

  @Override
  protected DeterministicVariablesState handleDeclarationEdge(final ADeclarationEdge pCfaEdge, final ADeclaration pDeclaration)
      throws CPATransferException {

    // we do only care about variable declarations
    if (!(pDeclaration instanceof AVariableDeclaration)) {
      return state;
    }

    Wrapper<ASimpleDeclaration> varDeclaration = LIVE_DECL_EQUIVALENCE.wrap((ASimpleDeclaration)pDeclaration);
    AInitializer initializer = ((AVariableDeclaration)varDeclaration.get()).getInitializer();

    // initializer is empty, return identity
    if (initializer == null) {
      return state;
    }

    if(initializer instanceof CInitializerExpression
        && areAllDeterministic(getVariablesUsedForInitialization(initializer))) {
      /* an initializer can either contains constants or variables or both, but no function calls
       * so the initialized variable is deterministic if all initializing identifiers are deterministic
       * this is trivially true in the case where an initializer consists of constants, only,
       * so this does not need to be handled in a special case, but all in the same
       */
      return state.addDeterministicVariable(varDeclaration);
    }

    return state;
  }

  @Override
  protected DeterministicVariablesState handleAssumption(final AssumeEdge cfaEdge,
      final AExpression expression,
      final boolean truthAssumption)
          throws CPATransferException {

    if (assumes.add(cfaEdge.getPredecessor())) {
      numberOfAssumes.inc();
    }

    if (!areAllDeterministic(handleExpression(expression)) && nondetAssumes.add(cfaEdge.getPredecessor())) {
      numberOfNonDetAssumes.inc();
    }

    return state;
  }

  @Override
  protected DeterministicVariablesState handleStatementEdge(final AStatementEdge cfaEdge, final AStatement statement)
      throws CPATransferException {
    // no assignments, thus return identity
    if (statement instanceof AExpressionStatement) {
      return state;
    }

    // no assignments, thus return identity
    else if (statement instanceof AFunctionCallStatement) {
      return state;
    }

    else if (statement instanceof AExpressionAssignmentStatement) {
      return handleAssignments((AAssignment) statement);
    }

    else if (statement instanceof AFunctionCallAssignmentStatement) {
      return handleAssignments((AAssignment) statement);
    }

    else {
      throw new CPATransferException("Missing case for if-then-else statement.");
    }
  }

  @Override
  protected DeterministicVariablesState handleReturnStatementEdge(final AReturnStatementEdge cfaEdge)
      throws CPATransferException {
    // for an empty return statement return the current state
    if (!cfaEdge.asAssignment().isPresent()) {
      return state;
    }

    return handleAssignments(cfaEdge.asAssignment().get());
  }

  @Override
  protected DeterministicVariablesState handleFunctionCallEdge(final FunctionCallEdge cfaEdge,
      final List<? extends AExpression> arguments,
      final List<? extends AParameterDeclaration> parameters,
      final String calledFunctionName) throws CPATransferException {

    assert (parameters.size() == arguments.size())
    || cfaEdge.getSuccessor().getFunctionDefinition().getType().takesVarArgs();

    Set<Wrapper<ASimpleDeclaration>> deterministicParameters = new HashSet<>(arguments.size());
    for (int i = 0; i < parameters.size(); i++) {
      if(areAllDeterministic(handleExpression(arguments.get(i)))) {
        deterministicParameters.add(LIVE_DECL_EQUIVALENCE.wrap((ASimpleDeclaration)parameters.get(i)));
      }
    }
    return state.addDeterministicVariables(deterministicParameters);
  }

  @Override
  protected DeterministicVariablesState handleFunctionReturnEdge(final FunctionReturnEdge cfaEdge,
      final FunctionSummaryEdge fnkCall,
      final AFunctionCall summaryExpr,
      final String callerFunctionName)
      throws CPATransferException {

    if (cfaEdge.getFunctionEntry().getReturnVariable().isPresent()) {
      ASimpleDeclaration returnVariable = cfaEdge.getFunctionEntry().getReturnVariable().get();

      // there are functions with returns that are called as FunctionCallStatement, i.e., without assigning
      if (summaryExpr instanceof AFunctionCallAssignmentStatement) {
        AFunctionCallAssignmentStatement assignExp = ((AFunctionCallAssignmentStatement)summaryExpr);

        final Collection<Wrapper<ASimpleDeclaration>> assignedVariables = handleLeftHandSide(assignExp.getLeftHandSide());

        // add or remove assigned variable, depending on state of return variable
        if(assignedVariables.size() == 1) {
          state = areAllDeterministic(Collections.singleton(LIVE_DECL_EQUIVALENCE.wrap(returnVariable)))
              ? state.addDeterministicVariable((Iterables.getOnlyElement(assignedVariables)))
              : state.removeDeterministicVariable((Iterables.getOnlyElement(assignedVariables)));
        }
      }

      state = state.removeDeterministicVariable(LIVE_DECL_EQUIVALENCE.wrap(returnVariable));
    }

    // cleanup by removing function parameter from state
    Set<Wrapper<ASimpleDeclaration>> parameters = new HashSet<>(fnkCall.getFunctionEntry().getFunctionDefinition().getParameters().size());
    for(AParameterDeclaration param : fnkCall.getFunctionEntry().getFunctionDefinition().getParameters()) {
      parameters.add(LIVE_DECL_EQUIVALENCE.wrap((ASimpleDeclaration)param));
    }
    return state.removeDeterministicVariables(parameters);
  }

  @Override
  protected DeterministicVariablesState handleFunctionSummaryEdge(final FunctionSummaryEdge cfaEdge) throws CPATransferException {
    AFunctionCall functionCall = cfaEdge.getExpression();
    if (functionCall instanceof AFunctionCallAssignmentStatement) {
      throw new CPATransferException("Woooooot? Didn't expect to see you here, AFunctionCallAssignmentStatement!");

    } else if (functionCall instanceof AFunctionCallStatement) {
      throw new CPATransferException("Woooooot? Didn't expect to see you here, AFunctionCallStatement!");

    } else {
      throw new CPATransferException("Missing case for if-then-else statement.");
    }
  }

  /**
   * This method returns a collection of all variables occurring in the given expression.
   */
  private Collection<Wrapper<ASimpleDeclaration>> handleExpression(final AExpression expression) {
    return CFAUtils.traverseRecursively(expression)
        .filter(AIdExpression.class)
        .transform(AIdExpression::getDeclaration)
        .transform(TO_EQUIV_WRAPPER)
        .toSet();
  }

  /**
   * This method returns a collection of the variables occurring in the given left-hand-side expression.
   */
  private Collection<Wrapper<ASimpleDeclaration>> handleLeftHandSide(
      final ALeftHandSide pLeftHandSide) {
    return CFAUtils.traverseLeftHandSideRecursively(pLeftHandSide)
        .filter(AIdExpression.class)
        .transform(AIdExpression::getDeclaration)
        .transform(TO_EQUIV_WRAPPER)
        .toSet();
  }

  /**
   * This method is a helper method for handling assignments.
   */
  private DeterministicVariablesState handleAssignments(final AAssignment pAssignment) {
    final Collection<Wrapper<ASimpleDeclaration>> assignedVariables = handleLeftHandSide(pAssignment.getLeftHandSide());

    if (assignedVariables.size() > 1) {
      return state;
    }

    if (pAssignment instanceof AExpressionAssignmentStatement) {
      Wrapper<ASimpleDeclaration> assignedVariable = Iterables.getOnlyElement(assignedVariables);

      return (areAllDeterministic(handleExpression((AExpression) pAssignment.getRightHandSide())))
          ? state.addDeterministicVariable(assignedVariable)
          : state.removeDeterministicVariable(assignedVariable);
    }

    // for function calls of functions without a body
    else if (pAssignment instanceof AFunctionCallAssignmentStatement) {
      return state.removeDeterministicVariable(Iterables.getOnlyElement(assignedVariables));
    }

    else {
      throw new AssertionError("Unhandled assignment type " + pAssignment.getClass());
    }
  }

  /**
   * This method checks if all variables contained in the collection are deterministic.
   */
  private boolean areAllDeterministic(final Collection<Wrapper<ASimpleDeclaration>> variables) {
    for (Wrapper<ASimpleDeclaration> variable : variables) {
      if(!state.isDeterministic(variable)) {
        return false;
      }
    }

    return true;
  }

  /**
   * This method obtains the collection of variables that are used for initializing another variable.
   */
  private Collection<Wrapper<ASimpleDeclaration>> getVariablesUsedForInitialization(final AInitializer init) throws CPATransferException {
    // e.g. .x=b or .p.x.=1 as part of struct initialization
    if (init instanceof CDesignatedInitializer) {
      return getVariablesUsedForInitialization(((CDesignatedInitializer) init).getRightHandSide());
    }

    // e.g. {a, b, s->x} (array) , {.x=1, .y=0} (initialization of struct, array)
    else if (init instanceof CInitializerList) {
      Collection<Wrapper<ASimpleDeclaration>> readVars = new ArrayList<>();

      for (CInitializer inList : ((CInitializerList) init).getInitializers()) {
        readVars.addAll(getVariablesUsedForInitialization(inList));
      }
      return readVars;
    }

    else if (init instanceof AInitializerExpression) {
      return handleExpression(((AInitializerExpression) init).getExpression());
    }

    else {
      throw new CPATransferException("Missing case for if-then-else statement.");
    }
  }

  @Override
  public String getName() {
    return getClass().getSimpleName();
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(pOut);

    writer.put(numberOfAssumes)
      .put(numberOfNonDetAssumes)
      .put("Level of non-determism",
          (numberOfAssumes.getValue() == 0)
          ? "0%"
          : StatisticsUtils.toPercent(numberOfNonDetAssumes.getValue(), numberOfAssumes.getValue()));
  }
}