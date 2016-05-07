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
package org.sosy_lab.cpachecker.cpa.ifcsecurity;

import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.Variable;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.util.SetUtil;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

import java.io.Serializable;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * CPA-Abstract-State for tracking which variables/funtions are dependendent on which other variables/funtions
 */
public class DependencyTrackerState implements AbstractState, Serializable,
    LatticeAbstractState<DependencyTrackerState>, Graphable, AbstractQueryableState {

  private static final long serialVersionUID = -7164706513665824978L;

  /**
   * Internal Variable: Dependencies
   */
  private Map<Variable, SortedSet<Variable>> dependencies = new TreeMap<>();

  public Map<Variable, SortedSet<Variable>> getDependencies() {
    return dependencies;
  }


  public void setDependencies(Map<Variable, SortedSet<Variable>> pDependencies) {
    dependencies = pDependencies;
  }


  /**
   * Utility for computation of Set-Operations over Variables.
   */
  private static SetUtil<Variable> setutil = new SetUtil<>();

  @Override
  public String getCPAName() {
    return "DependencyTrackerCPA";
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    return false;
  }

  @Override
  public Object evaluateProperty(String pProperty) throws InvalidQueryException {
    return null;
  }

  @Override
  public void modifyProperty(String pModification) throws InvalidQueryException {

  }

  @Override
  public String toDOTLabel() {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\\n");
    sb.append("[Dependencies]=");
    sb.append(dependencies.toString());
    sb.append("\\n");
    sb.append("}");
    sb.append("\\n");
    return sb.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  public boolean isEqual(DependencyTrackerState pOther) {
    if (this == pOther) { return true; }
    if (pOther == null) { return false; }
    for (Variable var : this.dependencies.keySet()) {
      if (pOther.dependencies.containsKey(var)) {
        if (!(setutil.isSubset(this.dependencies.get(var),
            pOther.dependencies.get(var)))) { return false; }
      } else {
        return false;
      }
    }
    for (Variable var : pOther.dependencies.keySet()) {
      if (this.dependencies.containsKey(var)) {
        if (!(setutil.isSubset(pOther.dependencies.get(var),
            this.dependencies.get(var)))) { return false; }
      } else {
        return false;
      }
    }
    return true;
  }

  @Override
  public DependencyTrackerState join(DependencyTrackerState pOther) {
    if (this.isEqual(pOther)) {
      return pOther;
    } else {
      //Strongest Post Condition
      DependencyTrackerState merge = this;
      //implicit copy of this
      //explicit copy of pOther
      for (Variable var : pOther.dependencies.keySet()) {
        SortedSet<Variable> deps = pOther.dependencies.get(var);
        SortedSet<Variable> ndeps = new TreeSet<>();
        if (this.dependencies.containsKey(var)) {
          assert (merge.dependencies.containsKey(var));
          ndeps = merge.dependencies.get(var);
        }
        for (Variable var2 : deps) {
          ndeps.add(var2);
        }
        merge.dependencies.put(var, ndeps);
      }
      return merge;
    }
  }

  @Override
  public boolean isLessOrEqual(DependencyTrackerState pOther)
      throws CPAException, InterruptedException {
    if (this == pOther) { return true; }
    if (pOther == null) { return false; }
    //[l]>[l,h]
    for (Variable var : this.dependencies.keySet()) {
      if (pOther.dependencies.containsKey(var)) {
        if (!(setutil.isSubset(this.dependencies.get(var),
            pOther.dependencies.get(var)))) { return false; }
      } else {
        return false;
      }
    }
    return true;
  }


  @Override
  public DependencyTrackerState clone() {
    DependencyTrackerState result = new DependencyTrackerState();

    result.dependencies = new TreeMap<>();
    for (Variable key : this.dependencies.keySet()) {
      SortedSet<Variable> vars = this.dependencies.get(key);
      SortedSet<Variable> nvars = new TreeSet<>();
      for (Variable var : vars) {
        nvars.add(var);
      }
      result.dependencies.put(key, nvars);
    }

    return result;
  }

}
