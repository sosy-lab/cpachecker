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
package org.sosy_lab.cpachecker.core.algorithm.tiger.util;

import com.google.common.collect.Lists;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
import org.sosy_lab.cpachecker.util.predicates.AssignableTerm;
import org.sosy_lab.cpachecker.util.predicates.regions.NamedRegionManager;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;


public class TestCase {

  private int id;
  private Map<String, BigInteger> inputs;
  private Map<String, BigInteger> outputs;
  private List<CFAEdge> path;
  private List<CFAEdge> errorPath;
  private Region presenceCondition;
  private ARGPath argPath;
  NamedRegionManager bddCpaNamedRegionManager;

  public TestCase(int pI, Map<String, BigInteger> pInputs, Map<String, BigInteger> pOutputs,
      List<CFAEdge> pPath,
      List<CFAEdge> pShrinkedErrorPath,
      Region pPresenceCondition,
      NamedRegionManager pBddCpaNamedRegionManager) {
    inputs = pInputs;
    outputs = pOutputs;
    path = pPath;
    errorPath = pShrinkedErrorPath;
    presenceCondition = pPresenceCondition;
    id = pI;
    bddCpaNamedRegionManager = pBddCpaNamedRegionManager;
  }

  public TestCase(int pI, @SuppressWarnings("unused") List<TestStep> pTestSteps,
      ARGPath pTargetPath, List<CFAEdge> pList,
      Region pPresenceCondition,
      @SuppressWarnings("unused") NamedRegionManager pBddCpaNamedRegionManager,
      Map<String, BigInteger> pInputValues,
      @SuppressWarnings("unused") Pair<TreeSet<Entry<AssignableTerm, Object>>, TreeSet<Entry<AssignableTerm, Object>>> pInputsAndOutputs) {
    id = pI;
    argPath = pTargetPath;
    errorPath = pList;
    presenceCondition = pPresenceCondition;
    inputs = pInputValues;
    bddCpaNamedRegionManager = pBddCpaNamedRegionManager;
  }

  public int getId() {
    return id;
  }

  public List<CFAEdge> getPath() {
    return path;
  }

  public ARGPath getArgPath() {
    return argPath;
  }

  public List<CFAEdge> getErrorPath() {
    return errorPath;
  }

  public Map<String, BigInteger> getInputs() {
    return inputs;
  }

  public Map<String, BigInteger> getOutputs() {
    return outputs;
  }

  public Region getPresenceCondition() {
    return presenceCondition;
  }

  public String toCode() {
    String str = "int input() {\n  static int index = 0;\n  switch (index) {\n";

    int index = 0;
    for (BigInteger input : inputs.values()) {
      str += "  case " + index + ":\n    index++;\n    return " + input + ";\n";
      index++;
    }

    str += "  default:\n    return 0;\n  }\n}\n";

    return str;
  }

  public List<String> calculateCoveredLabels() {
    List<String> result = Lists.newLinkedList();
    for (CFAEdge edge : this.getPath()) {
      if (edge == null) {
        continue;
      }
      CFANode predecessor = edge.getPredecessor();
      if (predecessor instanceof CLabelNode
          && !((CLabelNode) predecessor).getLabel().isEmpty()) {
        result.add(((CLabelNode) predecessor).getLabel());
      }
    }
    return result;
  }

  public ThreeValuedAnswer coversGoal(Goal pGoal) {
    NondeterministicFiniteAutomaton<GuardedEdgeLabel> lAutomaton = pGoal.getAutomaton();
    Set<NondeterministicFiniteAutomaton.State> lCurrentStates = new HashSet<>();
    Set<NondeterministicFiniteAutomaton.State> lNextStates = new HashSet<>();

    lCurrentStates.add(lAutomaton.getInitialState());

    boolean lHasPredicates = false;

    for (CFAEdge lCFAEdge : this.getPath()) {
      for (NondeterministicFiniteAutomaton.State lCurrentState : lCurrentStates) {
        // Automaton accepts as soon as it sees a final state (implicit self-loop)
        if (lAutomaton.getFinalStates()
            .contains(lCurrentState)) { return ThreeValuedAnswer.ACCEPT; }

        for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lOutgoingEdge : lAutomaton
            .getOutgoingEdges(lCurrentState)) {
          GuardedEdgeLabel lLabel = lOutgoingEdge.getLabel();

          if (lLabel.hasGuards()) {
            lHasPredicates = true;
          } else {
            if (lLabel.contains(lCFAEdge)) {
              lNextStates.add(lOutgoingEdge.getTarget());
            }
          }
        }
      }

      lCurrentStates.clear();

      Set<NondeterministicFiniteAutomaton.State> lTmp = lCurrentStates;
      lCurrentStates = lNextStates;
      lNextStates = lTmp;
    }

    for (NondeterministicFiniteAutomaton.State lCurrentState : lCurrentStates) {
      // Automaton accepts as soon as it sees a final state (implicit self-loop)
      if (lAutomaton.getFinalStates().contains(lCurrentState)) { return ThreeValuedAnswer.ACCEPT; }
    }

    if (lHasPredicates) {
      return ThreeValuedAnswer.UNKNOWN;
    } else {
      return ThreeValuedAnswer.REJECT;
    }
  }


  public List<Goal> getCoveredGoals(List<Goal> pAllGoals) {
    List<Goal> coveredGoals = new ArrayList<>();
    for (Goal goal : pAllGoals) {
      ThreeValuedAnswer answer = coversGoal(goal);
      if (answer.equals(ThreeValuedAnswer.ACCEPT)) {
        coveredGoals.add(goal);
      }
    }
    return coveredGoals;
  }

  @Override
  public String toString() {
    //String returnStr = inputs.toString();


    String returnStr =
        "TestCase "
            + id;

    if (getPresenceCondition() != null) {
      returnStr +=
          "TestCase "
              + id
              + " with configurations "
              + bddCpaNamedRegionManager.dumpRegion(getPresenceCondition())
                  .toString()
                  .replace("__SELECTED_FEATURE_", "")
                  .replace(" & TRUE", "");
    }

    returnStr += ":\n\n";
    returnStr += "\tinputs and outputs {\n";
    for (String variable : inputs.keySet()) {
      returnStr += "\t\t-> " + variable + " = " + inputs.get(variable) + "\n";
    }
    for (String variable : outputs.keySet()) {
      returnStr += "\t\t<- " + variable + " = " + outputs.get(variable) + "\n";
    }
    returnStr += "\t}";
    /* if (presenceCondition != null) {
      returnStr += " with configurations " + bddCpaNamedRegionManager.dumpRegion(getPresenceCondition());
    }*/

    return returnStr;
  }

  private boolean
      sameValues(Map<String, BigInteger> thisValues, Map<String, BigInteger> otherValues) {
    // if both are null they are equal
    if (thisValues == null && otherValues == null) {
      return true;
    }
    // if only one is null they are not equal
    if (thisValues == null || otherValues == null) {
      return false;
    }

    for (Entry<String, BigInteger> input : thisValues.entrySet()) {
      if (!otherValues.containsKey(input.getKey())) {
        return false;
      }
      if (!otherValues.get(input.getKey()).equals(thisValues.get(input.getKey()))) {
        return false;
      }
    }
    return true;
  }

  private boolean samePresenceCondition(Region otherPresenceCondition) {
    if (this.getPresenceCondition() == null && otherPresenceCondition == null) {
      return true;
    }

    if (this.getPresenceCondition() == null || otherPresenceCondition == null) {
      return false;
    }

    if (!this.getPresenceCondition().equals(otherPresenceCondition)) {
      return false;
    }
    return true;

  }

  public boolean isEquivalent(TestCase other) {

    // equal input
    if (!sameValues(this.getInputs(), other.getInputs())) {
      return false;
    }

    // equal output
    if (!sameValues(this.getOutputs(), other.getOutputs())) {
      return false;
    }

    // equal presence condition
    if (!samePresenceCondition(other.getPresenceCondition())) {
      return false;
    }
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof TestCase) {
      TestCase other = (TestCase) o;
      return (id==other.id&&inputs.equals(other.inputs) && path.equals(other.path) && (presenceCondition != null
          ? presenceCondition.equals(other.getPresenceCondition()) : true));
    }

    return false;
  }

  @Override
  public int hashCode() {
    return 38495 + 33 * inputs.hashCode() + 13 * path.hashCode()
        + (presenceCondition != null ? 25 * presenceCondition.hashCode() : 0);
  }
}
