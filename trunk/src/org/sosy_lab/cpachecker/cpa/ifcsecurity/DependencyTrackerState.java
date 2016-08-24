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
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.Variable;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * CPA-Abstract-State for tracking which variables/functions are depends on which other variables/functions
 */
public class DependencyTrackerState
    implements AbstractState, Cloneable, Serializable, LatticeAbstractState<DependencyTrackerState>,
        Graphable {


  private static final long serialVersionUID = -9169677539829708995L;
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
    for (Entry<Variable, SortedSet<Variable>> entry : this.dependencies.entrySet()) {
      Variable var=entry.getKey();
      if (pOther.dependencies.containsKey(var)) {
        if (!(this.dependencies.get(var).containsAll(
            pOther.dependencies.get(var)))) { return false; }
      } else {
        return false;
      }
    }
    for (Entry<Variable, SortedSet<Variable>> entry : pOther.dependencies.entrySet()) {
      Variable var=entry.getKey();
      if (this.dependencies.containsKey(var)) {
        if (!(pOther.dependencies.get(var).containsAll(
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
    for (Entry<Variable, SortedSet<Variable>> entry : this.dependencies.entrySet()) {
      Variable var=entry.getKey();
      if (pOther.dependencies.containsKey(var)) {
        if (!(this.dependencies.get(var).containsAll(
            pOther.dependencies.get(var)))) { return false; }
      } else {
        return false;
      }
    }
    return true;
  }


  @Override
  public DependencyTrackerState clone() {
    try {
      super.clone();
    } catch (CloneNotSupportedException e) {
      //    logger.logUserException(Level.WARNING, e, "");
    }

    DependencyTrackerState result = new DependencyTrackerState();

    result.dependencies = new TreeMap<>();
    for (Entry<Variable, SortedSet<Variable>> entry : this.dependencies.entrySet()) {
      Variable key=entry.getKey();
      SortedSet<Variable> vars = entry.getValue();
      SortedSet<Variable> nvars = new TreeSet<>();
      for (Variable var : vars) {
        nvars.add(var);
      }
      result.dependencies.put(key, nvars);
    }

    return result;
  }

}
