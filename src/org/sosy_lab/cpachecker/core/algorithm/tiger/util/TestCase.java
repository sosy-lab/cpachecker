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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;


public class TestCase {

  private int id;
  private List<TestCaseVariable> inputs;
  private List<TestCaseVariable> outputs;
  private List<CFAEdge> path;
  private List<Pair<CFAEdgeWithAssumptions, Boolean>> errorPath;
  private Region presenceCondition;
  private ARGPath argPath;
  private BDDUtils bddUtils;

  public TestCase(
      int pID,
      List<TestCaseVariable> pInputs,
      List<TestCaseVariable> pOutputs,
      List<CFAEdge> pPath,
      List<Pair<CFAEdgeWithAssumptions, Boolean>> pShrinkedErrorPath,
      Region pPresenceCondition,
      BDDUtils pBddUtils) {
    inputs = pInputs;
    outputs = pOutputs;
    path = pPath;
    errorPath = pShrinkedErrorPath;
    presenceCondition = pPresenceCondition;
    id = pID;
    bddUtils = pBddUtils;
  }

  public String dumpPresenceCondition() {
    String pc = bddUtils.dumpRegion(presenceCondition);
    return pc.replaceAll("@[0-9]+", "").replace(" & TRUE", "");
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

  public List<Pair<CFAEdgeWithAssumptions, Boolean>> getErrorPath() {
    return errorPath;
  }

  public List<TestCaseVariable> getInputs() {
    return inputs;
  }

  public List<TestCaseVariable> getOutputs() {
    return outputs;
  }

  public Region getPresenceCondition() {
    return presenceCondition;
  }

  public String toCode() {
    String str = "int input() {\n  static int index = 0;\n  switch (index) {\n";

    int index = 0;
    for (TestCaseVariable var : inputs) {
      str += "  case " + index + ":\n    index++;\n    return " + var.getValue() + ";\n";
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
    return pGoal.getsCoveredByPath(this.getPath());
  }

  public <T extends Goal> Set<T> getCoveredGoals(Collection<T> pAllGoals)
  {
    Set<T> coveredGoals = new HashSet<>();
    for (T goal : pAllGoals) {
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
              + bddUtils.dumpRegion(getPresenceCondition())
                  .replace("__SELECTED_FEATURE_", "")
                  .replace(" & TRUE", "");
    }

    returnStr += ":\n\n";
    returnStr += "\tinputs and outputs {\n";
    for (TestCaseVariable variable : inputs) {
      returnStr += "\t\t-> " + variable.getName() + " = " + variable.getValue() + "\n";
    }
    for (TestCaseVariable variable : outputs) {
      returnStr += "\t\t<- " + variable.getName() + " = " + variable.getValue() + "\n";
    }
    returnStr += "\t}";
    /* if (presenceCondition != null) {
      returnStr += " with configurations " + bddCpaNamedRegionManager.dumpRegion(getPresenceCondition());
    }*/

    return returnStr;
  }

  private boolean
      sameValues(List<TestCaseVariable> thisValues, List<TestCaseVariable> otherValues) {
    // if both are null they are equal
    if (thisValues == null && otherValues == null) {
      return true;
    }
    // if only one is null they are not equal
    if (thisValues == null || otherValues == null) {
      return false;
    }

    if (thisValues.size() != otherValues.size()) {
      return false;
    }

    for (int i = 0; i < thisValues.size(); i++) {
      if (!thisValues.get(i).getName().equals(otherValues.get(i).getName())) {
        return false;
      }
      if (!thisValues.get(i).getValue().equals(otherValues.get(i).getValue())) {
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
