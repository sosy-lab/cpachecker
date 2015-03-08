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
package org.sosy_lab.cpachecker.cpa.value.refiner.utils;

import static com.google.common.base.Predicates.*;
import static com.google.common.collect.Collections2.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.AArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AInitializer;
import org.sosy_lab.cpachecker.cfa.ast.AInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.ALeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.livevar.DeclarationCollectingVisitor;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.MemoryLocation;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisInterpolant;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * This class allows to obtain interpolants statically from a given ARGPath.
 */
public class UseDefBasedInterpolator {

  /**
   * defines how to handle feasible assume edges
   */
  private String handleFeasibleAssumeEdges = "NONE";

  /**
   * the flag to determine, if the final (failing, contradicting) assume edge
   * has already been handled (mind that the traversal proceeds in reverse order)
   */
  private boolean hasContradictingAssumeEdgeBeenHandled = false;

  /**
   * the set of "uses" that are not yet resolved by a "definition"
   */
  private final Set<ASimpleDeclaration> dependencies = new HashSet<>();

  /**
   * the mapping from {@link ARGState}s to {@link ValueAnalysisInterpolant}s.
   */
  private Map<ARGState, ValueAnalysisInterpolant> interpolants = new LinkedHashMap<>();

  /**
   * the mapping from declarations to the number of times it was "used"
   */
  private Map<ASimpleDeclaration, Integer> updateCounter = new HashMap<>();

  public UseDefBasedInterpolator(final String pHandleFeasibleAssumeEdges) {
    handleFeasibleAssumeEdges = pHandleFeasibleAssumeEdges;
  }

  /**
   * This method obtains the mapping from {@link ARGState}s to {@link ValueAnalysisInterpolant}s.
   *
   * This method iterates over the path, building a use-def-relation that is seeded by the identifiers
   * that occur in the last (in iteration order, the first) assume edge of the path. Hence, this
   * interpolation approach only works if the given path is an (infeasible) sliced prefix, obtained
   * via {@link ErrorPathClassifier#obtainSlicedPrefix}.
   *
   * @param path the path (i.e., infeasible sliced prefix) for which to obtain the interpolants
   * @return the mapping mapping from {@link ARGState}s to {@link ValueAnalysisInterpolant}s
   */
  public Map<ARGState, ValueAnalysisInterpolant> obtainInterpolants(ARGPath path) {
    dependencies.clear();
    interpolants.clear();

    List<CFAEdge> edges = path.getInnerEdges();
    List<ARGState> states = path.asStatesList();

    for (int i = edges.size() - 1; i >= 0; i--) {
      addCurrentInterpolant(states.get(i + 1));

      CFAEdge edge = edges.get(i);
      if (edge.getEdgeType() == CFAEdgeType.MultiEdge) {
        for (CFAEdge singleEdge : Lists.reverse(((MultiEdge)edge).getEdges())) {
          updateDependencies(singleEdge);
        }
      }
      else {
        updateDependencies(edge);
      }
    }

    return orderInterpolants();
  }

  private Map<ARGState, ValueAnalysisInterpolant> orderInterpolants() {
    Map<ARGState, ValueAnalysisInterpolant> ordered = new LinkedHashMap<>(interpolants.size());
    ListIterator<ARGState> iter = new ArrayList<>(interpolants.keySet()).listIterator(interpolants.size());
    while (iter.hasPrevious()) {
      ARGState state = iter.previous();
      ordered.put(state, interpolants.get(state));
    }
    return ordered;
  }

  private void updateDependencies(CFAEdge edge) {
    switch (edge.getEdgeType()) {

    case FunctionReturnEdge:
      AFunctionCall summaryExpr = ((FunctionReturnEdge)edge).getSummaryEdge().getExpression();

      if (summaryExpr instanceof AFunctionCallAssignmentStatement) {
        ASimpleDeclaration assignedVariable = Iterables.getOnlyElement(acceptLeft(((CFunctionCallAssignmentStatement) summaryExpr).getLeftHandSide()));
        if (dependencies.remove(assignedVariable)) {
          addDependency(Collections.<ASimpleDeclaration>singleton(((FunctionReturnEdge)edge).getFunctionEntry().getReturnVariable().get()));
        }
      }

      break;

    case DeclarationEdge:
      CDeclaration declaration = ((CDeclarationEdge)edge).getDeclaration();

      // only variable declarations are of interest
      if (declaration instanceof AVariableDeclaration && dependencies.remove(declaration)) {
        AInitializer initializer = ((AVariableDeclaration) declaration).getInitializer();
        if (initializer != null) {
          addDependency(getVariablesUsedForInitialization(initializer));
        }
      }

      break;

    case ReturnStatementEdge:
      AReturnStatementEdge returnStatementEdge = (AReturnStatementEdge)edge;
      if (returnStatementEdge.asAssignment().isPresent()) {
        handleAssignments(returnStatementEdge.asAssignment().get());
      }

      break;

    case FunctionCallEdge:
      final FunctionCallEdge functionCallEdge = (FunctionCallEdge) edge;
      final FunctionEntryNode functionEntryNode = functionCallEdge.getSuccessor();

      ArrayList<ASimpleDeclaration> parameters = new ArrayList<>(functionEntryNode.getFunctionParameters().size());
      for (AParameterDeclaration parameterDeclaration : functionEntryNode.getFunctionParameters()) {
        parameters.add(parameterDeclaration);
      }

      for (int parameterIndex = 0; parameterIndex < parameters.size(); parameterIndex++) {
        if (dependencies.remove(parameters.get(parameterIndex))) {
          addDependency(acceptAll(functionCallEdge.getArguments().get(parameterIndex)));
        }
      }

      break;

    case AssumeEdge:
      if (hasContradictingAssumeEdgeBeenHandled) {
        handleFeasibleAssumption(edge);
      } else {
        addDependency(acceptAll(((CAssumeEdge)edge).getExpression()));
        hasContradictingAssumeEdgeBeenHandled = true;
      }

      break;

    case StatementEdge:
      CStatement statement = ((CStatementEdge)edge).getStatement();

      if (statement instanceof AExpressionAssignmentStatement
          || statement instanceof AFunctionCallAssignmentStatement) {
        handleAssignments((AAssignment) statement);
      }
      break;

    default:
      // nothing to do for any other types of edges
      break;
    }
  }

  private void addDependency(Collection<ASimpleDeclaration> declarations) {
    for (ASimpleDeclaration declaration : declarations) {
      dependencies.add(declaration);

      Integer counter = 0;
      if(updateCounter.containsKey(declaration)) {
        counter = updateCounter.get(declaration) + 1;
      }

      updateCounter.put(declaration, counter);
    }
  }

  private void addCurrentInterpolant(ARGState successor) {
    interpolants.put(successor, dependencies.isEmpty()
        ? createTrivialInterpolant()
        : createNonTrivialInterpolant());
  }

  private ValueAnalysisInterpolant createTrivialInterpolant() {
    return hasContradictingAssumeEdgeBeenHandled
        ? ValueAnalysisInterpolant.TRUE
        : ValueAnalysisInterpolant.FALSE;
  }

  private ValueAnalysisInterpolant createNonTrivialInterpolant() {

    HashMap<MemoryLocation, Value> values = new HashMap<>();
    for (ASimpleDeclaration declaration : dependencies) {

      String qualifiedName = declaration.getQualifiedName();
      Value value = new NumericValue(updateCounter.get(declaration));

      if(qualifiedName.contains("::")) {
        values.put(MemoryLocation.valueOf(qualifiedName.substring(0, qualifiedName.indexOf("::")),
            declaration.getName(), 0), value);
      } else {
        values.put(MemoryLocation.valueOf(qualifiedName, 0), value);
      }
    }

    return new ValueAnalysisInterpolant(values, Collections.<MemoryLocation, Type>emptyMap());
  }

  private void handleFeasibleAssumption(CFAEdge edge) {

    CAssumeEdge assumeEdge = (CAssumeEdge)edge;
    CExpression expr = assumeEdge.getExpression();

    // option 1)
    // returning here immediately  without doing anything would lead to
    // actual assignments ending up in the interpolant
    // this, however, would lead to failing refinements if for a variable
    // no assignment to a known value exists
    if(handleFeasibleAssumeEdges.equals("NONE")) {
      return;
    }


    // option 2)
    // treat [a == 1] or [!(a != 1)] like an assignment,
    // so that such an assume removes an open dependency
    // for an equality with a constant, we can remove the dependency
    // this still could fail if this assume is the "same" as the final, failing one
    if (handleFeasibleAssumeEdges.equals("EQUALITY") && isEquality(assumeEdge, expr)) {
      CBinaryExpression binExpr = ((CBinaryExpression) expr);
      if (binExpr.getOperand1() instanceof CIdExpression
          && binExpr.getOperand2() instanceof CLiteralExpression) {
        if (dependencies.contains(((CIdExpression)binExpr.getOperand1()).getDeclaration())) {
          dependencies.remove(((CIdExpression)binExpr.getOperand1()).getDeclaration());
        }
      }
      else if (binExpr.getOperand2() instanceof CIdExpression
          && binExpr.getOperand1() instanceof CLiteralExpression) {
        if (dependencies.contains(((CIdExpression)binExpr.getOperand2()).getDeclaration())) {
          dependencies.remove(((CIdExpression)binExpr.getOperand2()).getDeclaration());
        }
      }
    }

    // option 3)
    // in addition to option 2, we can add new dependencies
    // from all kinds of assumptions (e.g., [a < 1]).
    // These can never lead to a contradiction, as the given path
    // is sliced (infeasible) prefix, that must only fail at the
    // very last assume edge.
    // However, these extra assumptions might help the SMT solver.
    // For the value domain, this would only introduce overhead
    // for interpolation, because it can't deal with anything
    // but equality-assumptions. However, adding more than one
    // equality-assumption cannot have a positive effect in the
    // value domain, because it must be such that it "assigns"
    // a variable to a value it already is assigned to, because
    // otherwise, it would be contradicting, which can't be the case
    // because only the final assumption is contradicting.
    //
    // for all other binary operations, we keep/readd/update the dependency,
    // plus, we add the new dependencies for all variables that occur
    // in the "other" side of the binary relation
    else if (handleFeasibleAssumeEdges.equals("ALL")) {
      CBinaryExpression binExpr = ((CBinaryExpression) expr);

      Collection<ASimpleDeclaration> leftSide = acceptAll(binExpr.getOperand1());
      Collection<ASimpleDeclaration> rightSide = acceptAll(binExpr.getOperand2());

      for(ASimpleDeclaration leftDeclaration : leftSide) {
        if (dependencies.contains(leftDeclaration)) {
          addDependency(Collections.singleton(leftDeclaration));
          addDependency(rightSide);
        }
      }

      for(ASimpleDeclaration rightDeclaration : rightSide) {
        if (dependencies.contains(rightDeclaration)) {
          addDependency(Collections.singleton(rightDeclaration));
          addDependency(leftSide);
        }
      }
    }
  }

  private boolean isEquality(CAssumeEdge assumeEdge, CExpression expr) {
    return expr instanceof CBinaryExpression
        && ((assumeEdge.getTruthAssumption() && ((CBinaryExpression) expr).getOperator() == BinaryOperator.EQUALS)
            || (!assumeEdge.getTruthAssumption() && ((CBinaryExpression) expr).getOperator() == BinaryOperator.NOT_EQUALS));
  }


  /**
   * This is a more specific version of the CIdExpressionVisitor. For ArraySubscriptexpressions
   * we do only want the IdExpressions inside the ArrayExpression.
   */
  private static final class LeftHandSideIdExpressionVisitor extends DeclarationCollectingVisitor {
    @Override
    public Set<ASimpleDeclaration> visit(AArraySubscriptExpression pE) {
      return pE.getArrayExpression().<Set<ASimpleDeclaration>,
                                      Set<ASimpleDeclaration>,
                                      Set<ASimpleDeclaration>,
                                      RuntimeException,
                                      RuntimeException,
                                      LeftHandSideIdExpressionVisitor>accept_(this);
    }
  }

  private static Set<ASimpleDeclaration> acceptLeft(AExpression exp) {
    return exp.<Set<ASimpleDeclaration>,
                Set<ASimpleDeclaration>,
                Set<ASimpleDeclaration>,
                RuntimeException,
                RuntimeException,
                LeftHandSideIdExpressionVisitor>accept_(new LeftHandSideIdExpressionVisitor());
  }

  private static Set<ASimpleDeclaration> acceptAll(AExpression exp) {
    return exp.<Set<ASimpleDeclaration>,
                Set<ASimpleDeclaration>,
                Set<ASimpleDeclaration>,
                RuntimeException,
                RuntimeException,
                DeclarationCollectingVisitor>accept_(new DeclarationCollectingVisitor());
  }

  /**
   * This method computes the variables that are used for initializing an other variable from a given initializer.
   */
  private Collection<ASimpleDeclaration> getVariablesUsedForInitialization(AInitializer initializer) {
    // e.g. .x=b or .p.x.=1  as part of struct initialization
    if (initializer instanceof CDesignatedInitializer) {
      return getVariablesUsedForInitialization(((CDesignatedInitializer) initializer).getRightHandSide());
    }

    // e.g. {a, b, s->x} (array) , {.x=1, .y=0} (initialization of struct, array)
    else if (initializer instanceof CInitializerList) {
      Collection<ASimpleDeclaration> readVars = new ArrayList<>();

      for (CInitializer initializerList : ((CInitializerList) initializer).getInitializers()) {
        readVars.addAll(getVariablesUsedForInitialization(initializerList));
      }

      return readVars;
    }

    else if (initializer instanceof AInitializerExpression) {
      return acceptAll(((AInitializerExpression) initializer).getExpression());
    }

    else {
      throw new AssertionError("Missing case for if-then-else statement.");
    }
  }

  private void handleAssignments(AAssignment assignment) {
    final ALeftHandSide leftHandSide = assignment.getLeftHandSide();
    final Collection<ASimpleDeclaration> assignedVariables = acceptLeft(leftHandSide);
    final Collection<ASimpleDeclaration> allLeftHandSideVariables = acceptAll(leftHandSide);
    final Collection<ASimpleDeclaration> additionallyLeftHandSideVariables = filter(allLeftHandSideVariables, not(in(assignedVariables)));

    if(assignedVariables.size() > 1) {
      return;
    }

    // if assigned variable is resolving a dependency
    if (dependencies.remove(Iterables.getOnlyElement(assignedVariables))) {
      // all variables that occur in combination with the leftHandSide additionally
      // to the needed one (e.g. a[i] i is additionally) are added as dependency
      addDependency(additionallyLeftHandSideVariables);

      Collection<ASimpleDeclaration> newDependencies;
      // all variables of the right hand side are "used" afterwards
      if (assignment instanceof AExpressionAssignmentStatement) {
        newDependencies = acceptAll((AExpression) assignment.getRightHandSide());
      } else if (assignment instanceof AFunctionCallAssignmentStatement){
        AFunctionCallAssignmentStatement funcStmt = (AFunctionCallAssignmentStatement) assignment;
        newDependencies = getVariablesUsedAsParameters(funcStmt.getFunctionCallExpression().getParameterExpressions());
      } else {
        throw new AssertionError("Unhandled assignment type.");
      }

      addDependency(newDependencies);
    }
  }

  /**
   * This method returns the variables that are used in a given list of CExpressions.
   */
  private Collection<ASimpleDeclaration> getVariablesUsedAsParameters(List<? extends AExpression> parameters) {
    Collection<ASimpleDeclaration> usedParameters = new ArrayList<>();
    for (AExpression expression : parameters) {
      usedParameters.addAll(acceptAll(expression));
    }
    return usedParameters;
  }
}
