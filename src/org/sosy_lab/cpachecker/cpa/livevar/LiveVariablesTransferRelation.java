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
package org.sosy_lab.cpachecker.cpa.livevar;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpressionCollectingVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/**
 * This transferrelation computes the live variables for each location.
 *
 * Note that alias information is currently not used, thus, the analysis may be
 * imprecise e.g. if a pointer pointing to a variable is dereferenced and a new
 * value is assigned.
 */
public class LiveVariablesTransferRelation extends ForwardingTransferRelation<LiveVariablesState, LiveVariablesState, Precision> {

  private final Multimap<CFANode, String> liveVariables = HashMultimap.<CFANode, String>create();

  @Override
  protected Collection<LiveVariablesState> postProcessing(@Nullable LiveVariablesState successor) {
    if (successor == null) {
      return Collections.emptySet();
    } else {
      liveVariables.putAll(edge.getPredecessor(), successor.getLiveVariables());
      return Collections.singleton(successor);
    }
  }

  @Override
  protected LiveVariablesState handleMultiEdge(MultiEdge cfaEdge) throws CPATransferException {
    // as we are using the backwards analysis, we also have to iterate over
    // multiedges in reverse
    for (final CFAEdge innerEdge : Lists.reverse(cfaEdge.getEdges())) {
      edge = innerEdge;
      final LiveVariablesState intermediateResult = handleSimpleEdge(innerEdge);
      state = intermediateResult;
    }
    edge = cfaEdge; // reset edge
    return state;
  }

  /**
   * Returns a collection of all variable names which occur in expression
   */
  private Collection<String> handleExpression(CExpression expression) {
    Set<CIdExpression> result = expression.accept(new CIdExpressionCollectingVisitor());

    return FluentIterable.from(result).transform(new Function<CIdExpression, String>() {
      @Override
      public String apply(CIdExpression exp) {
        return exp.getDeclaration().getQualifiedName();
      }}).toSet();
  }

  /**
   * Returns a collection of the variable name in the leftHandSide, or if
   * it is a CFieldReference we return an empty set.
   */
  private Collection<String> handleLeftHandSide(CLeftHandSide exp) {
    if (exp instanceof CFieldReference) {
      return Collections.emptySet();
    } else {
      return handleExpression(exp);
    }
  }

  @Override
  protected  LiveVariablesState handleAssumption(CAssumeEdge cfaEdge, CExpression expression, boolean truthAssumption)
      throws CPATransferException {

    // all variables in assumption become live
    return state.addLiveVariables(handleExpression(expression));
  }

  @Override
  protected LiveVariablesState handleDeclarationEdge(CDeclarationEdge cfaEdge, CDeclaration decl)
      throws CPATransferException {

    // we do only care about variable declarations
    if (!(decl instanceof CVariableDeclaration)) {
      return state;
    }

    CVariableDeclaration varDecl = (CVariableDeclaration) decl;
    String deadVarName = varDecl.getQualifiedName();
    Collection<String> deadVar = Collections.singleton(deadVarName);
    CInitializer init = varDecl.getInitializer();

    // there is no initializer thus we only have to remove the initialized variable
    // from the live variables
    if (init == null) {
      return state.removeLiveVariables(deadVar);

      // don't do anything if declarated variable is not live
    } else if (!state.contains(deadVarName)) {
      return state;
    }

    return state.removeAndAddLiveVariables(deadVar, getVariablesUsedForInitialization(init));
  }

  /**
   * This method computes the variables that are used for initializing an other
   * variable from a given initializer.
   */
  private Collection<String> getVariablesUsedForInitialization(CInitializer init) throws CPATransferException {
    // e.g. .x=b or .p.x.=1  as part of struct initialization
    if (init instanceof CDesignatedInitializer) {
      return getVariablesUsedForInitialization(((CDesignatedInitializer) init).getRightHandSide());


    // e.g. {a, b, s->x} (array) , {.x=1, .y=0} (initialization of struct, array)
    } else if (init instanceof CInitializerList) {
      Collection<String> readVars = new ArrayList<>();

      for (CInitializer inList : ((CInitializerList) init).getInitializers()) {
        readVars.addAll(getVariablesUsedForInitialization(inList));
      }
      return readVars;


    } else if (init instanceof CInitializerExpression) {
      return handleExpression(((CInitializerExpression) init).getExpression());

    } else {
      throw new CPATransferException("Missing case for if-then-else statement.");
    }
  }

  @Override
  protected LiveVariablesState handleStatementEdge(CStatementEdge cfaEdge, CStatement statement)
      throws CPATransferException {
    if (statement instanceof CExpressionAssignmentStatement) {
      return handleAssignments((CAssignment) statement);

      // no changes as there is no assignment, thus we can return the last state
    } else if (statement instanceof CExpressionStatement) {
      return state;

    } else if (statement instanceof CFunctionCallAssignmentStatement) {
      return handleAssignments((CAssignment) statement);

    } else if (statement instanceof CFunctionCallStatement) {

      CFunctionCallStatement funcStmt = (CFunctionCallStatement) statement;
      return state.addLiveVariables(getVariablesUsedAsParameters(funcStmt
                                                                  .getFunctionCallExpression()
                                                                  .getParameterExpressions()));

    } else {
      throw new CPATransferException("Missing case for if-then-else statement.");
    }
  }

  private LiveVariablesState handleAssignments(CAssignment assignment) {
    Collection<String> assignedVariable = handleLeftHandSide(assignment.getLeftHandSide());
    Collection<String> rightHandSideVariables;

    if (assignment instanceof CExpressionAssignmentStatement) {
      rightHandSideVariables = handleExpression((CExpression) assignment.getRightHandSide());

    } else if (assignment instanceof CFunctionCallAssignmentStatement){
      CFunctionCallAssignmentStatement funcStmt = (CFunctionCallAssignmentStatement) assignment;
      rightHandSideVariables = getVariablesUsedAsParameters(funcStmt.getFunctionCallExpression().getParameterExpressions());

    } else {
      throw new AssertionError("Unhandled assignment type.");
    }

    // this maybe a field reference which is assigned, therefore we have to
    // leave the variable live
    if (assignedVariable.isEmpty()) {
      return state.addLiveVariables(rightHandSideVariables);

      // parameters of function calls always have to get live, because the
      // function needs those for assigning their variables
    } else if (state.contains(assignedVariable.iterator().next())
               || assignment instanceof CFunctionCallAssignmentStatement) {
      return state.removeAndAddLiveVariables(assignedVariable, rightHandSideVariables);

      // assigned variable is not live, so we do not need to make the
      // rightHandSideVariables live
    } else {
      return state;
    }
  }

  /**
   * This method returns the variables that are used in a given list of CExpressions.
   */
  private Collection<String> getVariablesUsedAsParameters(List<CExpression> parameters) {
    Collection<String> newLiveVars = new ArrayList<>();
    for (CExpression expression : parameters) {
      newLiveVars.addAll(handleExpression(expression));
    }
    return newLiveVars;
  }

  @Override
  protected LiveVariablesState handleReturnStatementEdge(CReturnStatementEdge cfaEdge)
      throws CPATransferException {
    // this is an empty return statement (return;)
    if (!cfaEdge.asAssignment().isPresent()) {
      return state;
    }

    return handleAssignments(cfaEdge.asAssignment().get());
  }

  @Override
  protected LiveVariablesState handleFunctionCallEdge(CFunctionCallEdge cfaEdge,
      List<CExpression> arguments, List<CParameterDeclaration> parameters,
      String calledFunctionName) throws CPATransferException {
    /* This analysis is (mostly) used during cfa creation, when no edges between
     * different functions exist, thus this function is mainly unused. However
     * for the purpose of having a complete CPA which works on the graph with
     * all functions connected, this method is implemented.
     */

    Collection<String> variablesInArguments = new ArrayList<>();
    for (CExpression arg : arguments) {
      variablesInArguments.addAll(handleExpression(arg));
    }

    // we can safely remove the parameters from the live variables as the function
    // starts at this edge.
    Collection<String> parameterVars = new ArrayList<>(parameters.size());
    for (CParameterDeclaration decl : parameters) {
      parameterVars.add(decl.getQualifiedName());
    }

    return state.removeAndAddLiveVariables(parameterVars, variablesInArguments);
  }

  @Override
  protected LiveVariablesState handleFunctionReturnEdge(CFunctionReturnEdge cfaEdge,
      CFunctionSummaryEdge fnkCall, CFunctionCall summaryExpr, String callerFunctionName)
      throws CPATransferException {
    /* This analysis is (mostly) used during cfa creation, when no edges between
     * different functions exist, thus this function is mainly unused. However
     * for the purpose of having a complete CPA which works on the graph with
     * all functions connected, this method is implemented.
     */

    // we can remove the assigned variable from the live variables
    if (summaryExpr instanceof CFunctionCallAssignmentStatement) {
      return handleAssignments((CAssignment) summaryExpr);

    // no assigned variable -> nothing to change
    } else {
      return state;
    }
  }

  public void putInitialLiveVariables(CFANode node, Collection<String> liveVars) {
    liveVariables.putAll(node, liveVars);
  }

  /**
   * Returns the liveVariables that are currently computed. Calling this method
   * makes only sense if the analysis was completed
   * @return a Multimap containing the variables that are live at each location
   */
  public Multimap<CFANode, String> getLiveVariables() {
    return liveVariables;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {
    return null;
  }
}
