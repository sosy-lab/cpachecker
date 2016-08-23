/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.refinement;

import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Collections2.filter;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.cfa.ast.AAssignment;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
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
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath.PathIterator;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UseDefRelation {

  /**
   * the set of variables of boolean character
   */
  private Set<String> booleanVariables = new HashSet<>();

  /**
   * the use-def relation
   *
   * The key of the map has to be the {@link Pair} of {@link ARGState} and {@link CFAEdge}.
   * {@link ARGState} alone would not be precise enough because of multi edges,
   * and {@link CFAEdge}s alone would not be precise enough because one edge may occur
   * multiple times in a {@link ARGPath}.
   */
  private final Map<Pair<ARGState, CFAEdge>, Pair<Set<ASimpleDeclaration>, Set<ASimpleDeclaration>>> relation = new LinkedHashMap<>();

  /**
   * the cache for storing uses that are not yet resolved
   *
   * This information also contained in the relation, but would require iterating over it
   * to find such uses. Rather than iterating, a cache is used to store this information.
   */
  private final Set<ASimpleDeclaration> unresolvedUses = new HashSet<>();

  /**
   * the flag to determine, if the final (failing, contradicting) assume edge
   * has already been handled (mind that the traversal proceeds in reverse order)
   */
  private boolean hasContradictingAssumeEdgeBeenHandled = false;

  /**
   * the flag to determine if all assume operations should add to the use-def-relation
   * instead of only the final (failing, contradicting) one
   */
  private boolean addAllAssumes = false;

  public UseDefRelation(final ARGPath path,
      final Set<String> pBooleanVariables,
      final boolean pAddAllAssumes) {

    booleanVariables = pBooleanVariables;
    addAllAssumes    = pAddAllAssumes;

    buildRelation(path);
  }

  public Map<ARGState, Collection<ASimpleDeclaration>> getExpandedUses(ARGPath path) {

    Map<ARGState, Collection<ASimpleDeclaration>> expandedUses = new LinkedHashMap<>();
    Collection<ASimpleDeclaration> unresolvedUses = new HashSet<>();

    PathIterator it = path.reverseFullPathIterator();

    while (it.hasNext()) {
      it.advance();
      ARGState currentState;
      if (it.isPositionWithState()) {
        currentState = it.getAbstractState();
      } else {
        currentState = it.getPreviousAbstractState();
      }
      CFAEdge currentEdge = it.getOutgoingEdge();

      unresolvedUses.removeAll(getDef(currentState, currentEdge));
      unresolvedUses.addAll(getUses(currentState, currentEdge));
      expandedUses.put(currentState, new HashSet<>(unresolvedUses));
    }

    return expandedUses;
  }

  public Collection<String> getUsesAsQualifiedName() {
    Set<String> uses = new HashSet<>();
    for (Set<ASimpleDeclaration> useSet :
        FluentIterable.from(relation.values()).transform(Pair::getSecond).toSet()) {
      for (ASimpleDeclaration use : useSet) {
        uses.add(use.getQualifiedName());
      }
    }

    return uses;
  }

  public Set<ARGState> getUseDefStates() {
    return FluentIterable.from(relation.keySet()).transform(Pair::getFirst).toSet();
  }

  private void buildRelation(ARGPath path) {
    PathIterator iterator = path.reverseFullPathIterator();

    while (iterator.hasNext()) {
      iterator.advance();
      CFAEdge edge = iterator.getOutgoingEdge();
      ARGState state;
      if (iterator.isPositionWithState()) {
        state = iterator.getAbstractState();
      } else {
        state = iterator.getPreviousAbstractState();
      }

      updateUseDefRelation(state, edge);

      // stop the traversal once a fix-point is reached
      if(hasContradictingAssumeEdgeBeenHandled && unresolvedUses.isEmpty()) {
        break;
      }
    }
  }

  private boolean hasUnresolvedUse(ASimpleDeclaration use) {
    return unresolvedUses.contains(use);
  }

  private void addUseDef(ARGState state, CFAEdge edge, ASimpleDeclaration def, ASimpleDeclaration use) {
    updateRelation(state, edge, Sets.newHashSet(def), Sets.newHashSet(use));
  }

  private void addUseDef(ARGState state, CFAEdge edge, ASimpleDeclaration def, Set<ASimpleDeclaration> uses) {
    updateRelation(state, edge, Sets.newHashSet(def), uses);
  }

  private void addUseDef(ARGState state, CFAEdge edge, Set<ASimpleDeclaration> defs, Set<ASimpleDeclaration> uses) {
    updateRelation(state, edge, defs, uses);
  }

  private void addUseDef(ARGState state, CFAEdge edge, Set<ASimpleDeclaration> uses) {
    updateRelation(state, edge, Collections.<ASimpleDeclaration>emptySet(), uses);
  }

  private void updateRelation(ARGState state, CFAEdge edge, Set<ASimpleDeclaration> defs, Set<ASimpleDeclaration> uses) {
    assert(!relation.containsKey(Pair.of(state, edge))) : "There is already a use-def entry for this pair of state, edge";

    relation.put(Pair.of(state, edge), Pair.of(defs, uses));
    unresolvedUses.removeAll(defs);
    unresolvedUses.addAll(uses);
  }

  private Collection<ASimpleDeclaration> getDef(ARGState state, CFAEdge edge) {
    if(relation.containsKey(Pair.of(state, edge))) {
      return relation.get(Pair.of(state, edge)).getFirst();
    } else {
      return Collections.emptySet();
    }
  }

  private Collection<ASimpleDeclaration> getUses(ARGState state, CFAEdge edge) {
    if(relation.containsKey(Pair.of(state, edge))) {
      return relation.get(Pair.of(state, edge)).getSecond();
    } else {
      return Collections.emptySet();
    }
  }

  private void updateUseDefRelation(ARGState state, CFAEdge edge) {
    switch (edge.getEdgeType()) {

      case FunctionReturnEdge:
        AFunctionCall summaryExpr = ((FunctionReturnEdge)edge).getSummaryEdge().getExpression();

        if (summaryExpr instanceof AFunctionCallAssignmentStatement) {
          Set<ASimpleDeclaration> assignedVariables = acceptLeft(((CFunctionCallAssignmentStatement) summaryExpr).getLeftHandSide());

          if(assignedVariables.size() > 1) {
            break;
          }

          ASimpleDeclaration assignedVariable = Iterables.getOnlyElement(assignedVariables);
          if(hasUnresolvedUse(assignedVariable)) {
            addUseDef(state, edge, assignedVariable, ((FunctionReturnEdge)edge).getFunctionEntry().getReturnVariable().get());
          }
        }

        break;

      case DeclarationEdge:
        CDeclaration declaration = ((CDeclarationEdge)edge).getDeclaration();

        // only variable declarations are of interest
        if (declaration instanceof AVariableDeclaration && hasUnresolvedUse(declaration)) {
          addUseDef(state, edge, declaration, getVariablesUsedInDeclaration(declaration));
        }

        break;

      case ReturnStatementEdge:
        AReturnStatementEdge returnStatementEdge = (AReturnStatementEdge)edge;
        if (returnStatementEdge.asAssignment().isPresent()) {
          handleAssignments(returnStatementEdge.asAssignment().get(), edge, state);
        }

        break;

      case FunctionCallEdge:
        final FunctionCallEdge functionCallEdge = (FunctionCallEdge) edge;
        final FunctionEntryNode functionEntryNode = functionCallEdge.getSuccessor();

        ArrayList<ASimpleDeclaration> parameters = new ArrayList<>(functionEntryNode.getFunctionParameters().size());
        for (AParameterDeclaration parameterDeclaration : functionEntryNode.getFunctionParameters()) {
          parameters.add(parameterDeclaration);
        }

        Set<ASimpleDeclaration> defs = new HashSet<>();
        Set<ASimpleDeclaration> uses = new HashSet<>();
        for (int parameterIndex = 0; parameterIndex < parameters.size(); parameterIndex++) {
          if (hasUnresolvedUse(parameters.get(parameterIndex))) {
            defs.add(parameters.get(parameterIndex));
            uses.addAll(acceptAll(functionCallEdge.getArguments().get(parameterIndex)));
          }
        }
        addUseDef(state, edge, defs, uses);

        break;

      case AssumeEdge:
        if (hasContradictingAssumeEdgeBeenHandled) {
          handleFeasibleAssumption(state, (CAssumeEdge)edge);
        } else {
          hasContradictingAssumeEdgeBeenHandled = !addAllAssumes;
          addUseDef(state, edge, acceptAll(((CAssumeEdge)edge).getExpression()));
        }

        break;

      case StatementEdge:
        CStatement statement = ((CStatementEdge)edge).getStatement();

        if (statement instanceof AExpressionAssignmentStatement
            || statement instanceof AFunctionCallAssignmentStatement) {
          handleAssignments((AAssignment) statement, edge, state);
        }
        break;

      default:
        // nothing to do for any other types of edges
        break;
    }
  }

  private void handleFeasibleAssumption(ARGState state, CAssumeEdge assumeEdge) {

    CExpression expression = assumeEdge.getExpression();

    // One can treat [x == c] or [!(x != c)] as an assignment of the constant c
    // to the variable x, so that such an assume resolves an unresolved use.
    // If the variable "x" has boolean character, this also works for assumes
    // like [x != c] or [!(x == c)].
    CBinaryExpression binaryExpression = ((CBinaryExpression) expression);

    ASimpleDeclaration operand = null;
    if (binaryExpression.getOperand1() instanceof CIdExpression
        && binaryExpression.getOperand2() instanceof CLiteralExpression) {
      operand = ((CIdExpression)binaryExpression.getOperand1()).getDeclaration();
    }

    else if (binaryExpression.getOperand2() instanceof CIdExpression
        && binaryExpression.getOperand1() instanceof CLiteralExpression) {
      operand = ((CIdExpression)binaryExpression.getOperand2()).getDeclaration();
    }

    if (isEquality(assumeEdge, binaryExpression.getOperator()) && hasUnresolvedUse(operand)) {
      addUseDef(state, assumeEdge, operand, Collections.<ASimpleDeclaration>emptySet());
    }

    else {
      if(isInequality(assumeEdge, binaryExpression.getOperator())
          && hasUnresolvedUse(operand)
          && hasBooleanCharacter(operand)) {
        addUseDef(state, assumeEdge, operand, Collections.<ASimpleDeclaration>emptySet());
      }
    }
  }

  private boolean hasBooleanCharacter(ASimpleDeclaration operand) {
    return booleanVariables.contains(operand.getQualifiedName());
  }

  private boolean isEquality(CAssumeEdge assumeEdge, BinaryOperator operator) {
    return ((assumeEdge.getTruthAssumption() && operator == BinaryOperator.EQUALS)
        || (!assumeEdge.getTruthAssumption() && operator == BinaryOperator.NOT_EQUALS));
  }

  private boolean isInequality(CAssumeEdge assumeEdge, BinaryOperator operator) {
    return ((assumeEdge.getTruthAssumption() && operator== BinaryOperator.NOT_EQUALS)
        || (!assumeEdge.getTruthAssumption() && operator == BinaryOperator.EQUALS));
  }

  private static Set<ASimpleDeclaration> acceptLeft(ALeftHandSide exp) {
    return CFAUtils.traverseLeftHandSideRecursively(exp)
        .filter(AIdExpression.class)
        .transform(AIdExpression::getDeclaration)
        .toSet();
  }

  private static Set<ASimpleDeclaration> acceptAll(AExpression exp) {
    return CFAUtils.traverseRecursively(exp)
        .filter(AIdExpression.class)
        .transform(AIdExpression::getDeclaration)
        .toSet();
  }


  /**
   * This method computes the variables that are used in the declaration of a variable.
   */
  private Set<ASimpleDeclaration> getVariablesUsedInDeclaration(CDeclaration declaration) {
    AInitializer initializer = ((AVariableDeclaration) declaration).getInitializer();

    if (initializer == null) {
      return Collections.emptySet();
    }

    return getVariablesUsedForInitialization(initializer);
  }

  /**
   * This method computes the variables that are used for initializing another variable from a given initializer.
   */
  private Set<ASimpleDeclaration> getVariablesUsedForInitialization(AInitializer initializer) {
    // e.g. .x=b or .p.x.=1  as part of struct initialization
    if (initializer instanceof CDesignatedInitializer) {
      return getVariablesUsedForInitialization(((CDesignatedInitializer) initializer).getRightHandSide());
    }

    // e.g. {a, b, s->x} (array) , {.x=1, .y=0} (initialization of struct, array)
    else if (initializer instanceof CInitializerList) {
      Set<ASimpleDeclaration> readVars = new HashSet<>();

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

  private void handleAssignments(AAssignment assignment, CFAEdge edge, ARGState state) {
    final ALeftHandSide leftHandSide = assignment.getLeftHandSide();
    final Set<ASimpleDeclaration> assignedVariables = acceptLeft(leftHandSide);
    final Set<ASimpleDeclaration> allLeftHandSideVariables = acceptAll(leftHandSide);
    final Set<ASimpleDeclaration> leftHandSideUses = new HashSet<>(filter(allLeftHandSideVariables, not(in(assignedVariables))));

    if(assignedVariables.size() > 1) {
      return;
    }

/*
    // hack to handle assignments of structs, which keeps the whole struct in "use" all the time,
    // until is is reassigned, and not only a single field
    // if assigned variable is resolving a dependency
    if (dependencies.contains(Iterables.getOnlyElement(assignedVariables))) {
      // hack to handle assignments of structs (keeps the whole struct in use all the time)
      if(leftHandSide.toString().contains("->")) {
        //Syso("NO remove " + Iterables.getOnlyElement(assignedVariables) + " in " + leftHandSide.toString());
        addDependency(assignedVariables);
      }
      else {
        //Syso("DO remove " + Iterables.getOnlyElement(assignedVariables) + " in " + leftHandSide.toString());
        dependencies.remove(Iterables.getOnlyElement(assignedVariables));
      }
*/

    // if assigned variable is resolving a dependency
    if (hasUnresolvedUse(Iterables.getOnlyElement(assignedVariables))) {
      // all variables that occur in combination with the leftHandSide additionally
      // to the needed one (e.g. a[i] i is additionally) are added as dependency

      Set<ASimpleDeclaration> rightHandSideUses;
      // all variables of the right hand side are "used" afterwards
      if (assignment instanceof AExpressionAssignmentStatement) {
        rightHandSideUses = acceptAll((AExpression) assignment.getRightHandSide());
      } else if (assignment instanceof AFunctionCallAssignmentStatement){
        AFunctionCallAssignmentStatement funcStmt = (AFunctionCallAssignmentStatement) assignment;
        rightHandSideUses = getVariablesUsedAsParameters(funcStmt.getFunctionCallExpression().getParameterExpressions());
      } else {
        throw new AssertionError("Unhandled assignment type.");
      }

      addUseDef(state, edge, Iterables.getOnlyElement(assignedVariables), Sets.union(leftHandSideUses, rightHandSideUses));
    }
  }

  /**
   * This method returns the variables that are used in a given list of CExpressions.
   */
  private Set<ASimpleDeclaration> getVariablesUsedAsParameters(List<? extends AExpression> parameters) {
    Set<ASimpleDeclaration> usedParameters = new HashSet<>();
    for (AExpression expression : parameters) {
      usedParameters.addAll(acceptAll(expression));
    }
    return usedParameters;
  }
}
