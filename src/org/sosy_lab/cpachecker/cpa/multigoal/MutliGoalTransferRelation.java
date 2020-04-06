/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.multigoal;

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.location.WeavingType;
import org.sosy_lab.cpachecker.cpa.location.WeavingVariable;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;

public class MutliGoalTransferRelation extends SingleEdgeTransferRelation {
  private Set<CFAEdgesGoal> goals;
  private Set<CFAEdgesGoal> coveredGoals;
  private Map<Object, WeavingVariable> weavingVarStore;

  MutliGoalTransferRelation() {
    coveredGoals = new HashSet<>();
    weavingVarStore = new HashMap<>();
  }

  private void createweavingVariableForObj(Object obj, int assumeNumber) {
    // already created
    if (weavingVarStore != null
        && weavingVarStore.containsKey(obj)
        && weavingVarStore.get(obj).getAssumptionValue() == assumeNumber) {
      return;
    }
    String name = "weaved_" + UUID.randomUUID().toString().replaceAll("-", "");
    CVariableDeclaration varDecl =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            true,
            CStorageClass.AUTO,
            CNumericTypes.INT,
            name,
            name,
            name,
            new CInitializerExpression(FileLocation.DUMMY, CIntegerLiteralExpression.ZERO));

    CIdExpression variable =
        new CIdExpression(FileLocation.DUMMY, CNumericTypes.INT, name, varDecl);

    CExpressionAssignmentStatement increment =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY,
            variable,
            new CBinaryExpression(
                FileLocation.DUMMY,
                CNumericTypes.INT,
                CNumericTypes.INT,
                variable,
                CIntegerLiteralExpression.ONE,
                CBinaryExpression.BinaryOperator.PLUS));

    CExpression assumption =
        new CBinaryExpression(
            FileLocation.DUMMY,
            CNumericTypes.INT,
            CNumericTypes.INT,
            variable,
            CIntegerLiteralExpression.createDummyLiteral(assumeNumber, CNumericTypes.INT),
            CBinaryExpression.BinaryOperator.EQUALS);

    weavingVarStore.put(obj, new WeavingVariable(varDecl, increment, assumption, assumeNumber));

  }

  LinkedHashSet<Pair<WeavingVariable, WeavingType>> getPredWeavingEdges(MultiGoalState predState) {
    if (predState.isInitialState()) {
      return initializeWeavingForAllGoals();
    } else {
      return new LinkedHashSet<>();
    }
  }

  Map<CFAEdgesGoal, Map<PartialPath, Integer>> getPredNegatedPathStates(MultiGoalState predState) {
    Map<CFAEdgesGoal, Map<PartialPath, Integer>> negatedPaths = new HashMap<>();
    if (predState.isInitialState()) {
      for (CFAEdgesGoal goal : goals) {
        if (goal.getNegatedPaths().size() > 0) {
        Map<PartialPath, Integer> map = new HashMap<>();
        for (PartialPath path : goal.getNegatedPaths()) {
          map.put(path, 0);
        }
        negatedPaths.put(goal, map);
        }
      }
    } else {
      for (Entry<CFAEdgesGoal, ImmutableMap<PartialPath, Integer>> entry : predState
          .getNegatedPathsPerGoal()
          .entrySet()) {
        Map<PartialPath, Integer> pathStates = new HashMap<>();
        for (Entry<PartialPath, Integer> pathState : entry.getValue().entrySet()) {
          pathStates.put(pathState.getKey(), pathState.getValue());
        }
        negatedPaths.put(entry.getKey(), pathStates);
      }
    }
    return negatedPaths;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      final AbstractState pState,
      final Precision pPrecision,
      final CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {

    MultiGoalState predState = (MultiGoalState) pState;
    LinkedHashSet<Pair<WeavingVariable, WeavingType>> weavingEdges = getPredWeavingEdges(predState);
    Map<CFAEdgesGoal, Map<PartialPath, Integer>> negatedPaths = getPredNegatedPathStates(predState);

    // if edge is weaved, it needs to be removed from weaved edges instead of processing the edge
    Set<CFAEdge> weavedEdges = new HashSet<>(predState.getWeavedEdges());
    if (weavedEdges.contains(pCfaEdge)) {
      weavedEdges.remove(pCfaEdge);
      return Collections
          .singleton(
              new MultiGoalState(
                  predState.getGoals(),
                  weavingEdges,
                  weavedEdges,
                  predState.getNegatedPathsPerGoal()));
    } else {
      return processEdge(predState, pCfaEdge, weavingEdges, negatedPaths);
    }
  }


  private Collection<MultiGoalState>
      processEdge(
          MultiGoalState predState,
          final CFAEdge pCfaEdge,
          LinkedHashSet<Pair<WeavingVariable, WeavingType>> edgesToWeave,
          Map<CFAEdgesGoal, Map<PartialPath, Integer>> pNegatedPredPathStates) {

    Map<CFAEdgesGoal, Integer> succGoals = new HashMap<>(predState.getGoals());

    boolean needsWeaving = false;
    for (CFAEdgesGoal goal : goals) {
      needsWeaving = processGoal(goal, succGoals, pCfaEdge, pNegatedPredPathStates);
      if (pCfaEdge instanceof AssumeEdge) {
        if (pNegatedPredPathStates.containsKey(goal)) {
          Map<PartialPath, Integer> pathStates = pNegatedPredPathStates.get(goal);
          for (Entry<PartialPath, Integer> entry : pathStates.entrySet()) {
            // needs weaving for goal edges as soon as negated paths are present
            if (entry.getKey().acceptsEdge(pCfaEdge, entry.getValue())) {
              edgesToWeave.add(Pair.of(getWeavedVariable(entry.getKey()), WeavingType.INCREMENT));
              pathStates.put(entry.getKey(), (entry.getValue() + 1));
            } else {
              pathStates.put(entry.getKey(), -1);
            }
          }
        }
      }
    }
    if (needsWeaving) {
      edgesToWeave.add(Pair.of(getWeavedVariable(pCfaEdge), WeavingType.INCREMENT));
    }

    Set<CFAEdgesGoal> finishedGoals =
        getFinishedGoals(succGoals, pNegatedPredPathStates, predState);

    if (finishedGoals.size() == 0) {
      return Collections
          .singleton(
              new MultiGoalState(
                  succGoals,
                  edgesToWeave,
                  predState.getWeavedEdges(),
                  pNegatedPredPathStates));
    }
    // make sure each successor only covers 1 goal
    // otherwise weaving will break
    // use successor to create the new states, but do not include successor in returned states
    return generateSuccessorPerFinishedGoal(
        finishedGoals,
        succGoals,
        edgesToWeave,
        predState);
  }




  private MultiGoalState generateSuccessorForGoal(
      CFAEdgesGoal goal,
      Set<CFAEdgesGoal> finishedGoals,
      Map<CFAEdgesGoal, Integer> succGoals,
      LinkedHashSet<Pair<WeavingVariable, WeavingType>> edgesToWeave,
      MultiGoalState predState) {

    // HashSet<MultiGoalState> succs = new HashSet<>();
    Map<CFAEdgesGoal, Integer> successorGoals = new HashMap<>(succGoals);
    successorGoals.keySet().removeAll(finishedGoals);
    successorGoals.put(goal, goal.getPath().size());
    LinkedHashSet<Pair<WeavingVariable, WeavingType>> newEdgesToWeave =
        new LinkedHashSet<>(edgesToWeave);
    if (goal.getPath().size() > 1 || goal.getNegatedPaths().size() > 0) {
      for (CFAEdge cfaEdge : goal.getPath().getEdges()) {
        newEdgesToWeave.add(Pair.of(getWeavedVariable(cfaEdge), WeavingType.ASSUMPTION));
      }
    }

    if (goal.getNegatedPaths().size() > 0) {
      for(PartialPath path: goal.getNegatedPaths()) {
        newEdgesToWeave.add(Pair.of(getWeavedVariable(path), WeavingType.NEGATEDASSUMPTION));
      }
    }
    return new MultiGoalState(
      successorGoals,
      newEdgesToWeave,
    predState.getWeavedEdges(),
        predState.getNegatedPathsPerGoal());
  }

  private WeavingVariable getWeavedVariable(Object obj) {
    return weavingVarStore.get(obj);
  }

  private Collection<MultiGoalState> generateSuccessorPerFinishedGoal(
      Set<CFAEdgesGoal> finishedGoals,
      Map<CFAEdgesGoal, Integer> succGoals,
      LinkedHashSet<Pair<WeavingVariable, WeavingType>> edgesToWeave,
      MultiGoalState predState) {

    // TODO might need change concerning paths
    HashSet<MultiGoalState> succs = new HashSet<>();
    Iterator<CFAEdgesGoal> iter = finishedGoals.iterator();
    while (iter.hasNext()) {
      CFAEdgesGoal goal = iter.next();
      succs.add(
          generateSuccessorForGoal(goal, finishedGoals, succGoals, edgesToWeave, predState));
    }
    return succs;
  }


  private Set<CFAEdgesGoal> getFinishedGoals(
      Map<CFAEdgesGoal, Integer> succGoals,
      Map<CFAEdgesGoal, Map<PartialPath, Integer>> negatedPathStates,
      MultiGoalState predState) {


    HashMap<CFAEdgesGoal, ImmutableMap<PartialPath, Integer>> immutableCopy = new HashMap<>();
    for (Entry<CFAEdgesGoal, Map<PartialPath, Integer>> entry : negatedPathStates.entrySet()) {
      immutableCopy.put(entry.getKey(), ImmutableMap.copyOf(entry.getValue()));
    }
    return MultiGoalState
        .calculateCoveredGoals(
            ImmutableMap.copyOf(succGoals),
            ImmutableMap.copyOf(immutableCopy),
            predState.getWeavedEdges(),
            predState.getEdgesToWeave());
  }



  private boolean
      processGoal(
          CFAEdgesGoal goal,
          Map<CFAEdgesGoal, Integer> pSuccGoals,
          CFAEdge pCfaEdge,
          Map<CFAEdgesGoal, Map<PartialPath, Integer>> pNegatedPredPathStates) {
    int index = 0;
    if (pSuccGoals.containsKey(goal)) {
      index = pSuccGoals.get(goal);
    }
    if (goal.acceptsEdge(pCfaEdge, index)) {
      index++;
      pSuccGoals.put(goal, index);
      return goal.getPath().size() > 1
          || (pNegatedPredPathStates.containsKey(goal)
              && pNegatedPredPathStates.get(goal).size() > 0);
    }
    return false;
  }

  public LinkedHashSet<Pair<WeavingVariable, WeavingType>> initializeWeavingForAllGoals() {
    LinkedHashSet<Pair<WeavingVariable, WeavingType>> weavingEdges = new LinkedHashSet<>();
    for (CFAEdgesGoal goal : goals) {
      // no need to weave goal consists of only 1 edge and no negated paths are present
      if (goal.getPath().size() > 1
          || (goal.getNegatedPaths() != null && goal.getNegatedPaths().size() > 0)) {
        for (CFAEdge edge : goal.getPath().getEdges()) {
          createweavingVariableForObj(edge, 1);
          weavingEdges.add(Pair.of(getWeavedVariable(edge), WeavingType.DECLARATION));
        }
      }
      if (goal.getNegatedPaths() != null) {
        for (PartialPath negatedPath : goal.getNegatedPaths()) {
          createweavingVariableForObj(negatedPath, negatedPath.size());
          weavingEdges.add(Pair.of(getWeavedVariable(negatedPath), WeavingType.DECLARATION));
        }
      }
    }
    return weavingEdges;
  }

  public Set<CFAEdgesGoal> getGoals() {
    return goals;
  }


  public void setGoals(Set<CFAEdgesGoal> pGoals) {
    goals = pGoals;
  }

  public void addCoveredGoal(CFAEdgesGoal pGoal) {
    coveredGoals.add(pGoal);
    goals.remove(pGoal);
  }

  public void addGoal(CFAEdgesGoal pGoal) {
    goals.add(pGoal);
  }

}
