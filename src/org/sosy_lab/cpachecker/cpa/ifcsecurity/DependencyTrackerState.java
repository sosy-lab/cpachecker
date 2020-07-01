// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.ifcsecurity;

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.Variable;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * CPA-Abstract-State for tracking which variables/functions are depends on which other variables/functions
 */
public class DependencyTrackerState
    implements AbstractState, Cloneable, Serializable, LatticeAbstractState<DependencyTrackerState>,
        Graphable {


  private static final long serialVersionUID = -9169677539829708995L;
  /** Internal Variable: Dependencies */
  private Map<Variable, NavigableSet<Variable>> dependencies = new TreeMap<>();

  public Map<Variable, NavigableSet<Variable>> getDependencies() {
    return dependencies;
  }

  public void setDependencies(Map<Variable, NavigableSet<Variable>> pDependencies) {
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
    for (Entry<Variable, NavigableSet<Variable>> entry : this.dependencies.entrySet()) {
      Variable var=entry.getKey();
      if (pOther.dependencies.containsKey(var)) {
        if (!entry.getValue().containsAll(pOther.dependencies.get(var))) {
          return false;
        }
      } else {
        return false;
      }
    }
    for (Entry<Variable, NavigableSet<Variable>> entry : pOther.dependencies.entrySet()) {
      Variable var=entry.getKey();
      if (this.dependencies.containsKey(var)) {
        if (!entry.getValue().containsAll(this.dependencies.get(var))) {
          return false;
        }
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
      // implicit copy of this
      // explicit copy of pOther
      for (Entry<Variable, NavigableSet<Variable>> entry : pOther.dependencies.entrySet()) {
        Variable var = entry.getKey();
        NavigableSet<Variable> deps = entry.getValue();
        NavigableSet<Variable> ndeps = new TreeSet<>();
        if (this.dependencies.containsKey(var)) {
          assert (merge.dependencies.containsKey(var));
          ndeps = merge.dependencies.get(var);
        }
        ndeps.addAll(deps);
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
    // [l]>[l,h]
    for (Entry<Variable, NavigableSet<Variable>> entry : this.dependencies.entrySet()) {
      Variable var=entry.getKey();
      if (pOther.dependencies.containsKey(var)) {
        if (!entry.getValue().containsAll(pOther.dependencies.get(var))) {
          return false;
        }
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
    for (Entry<Variable, NavigableSet<Variable>> entry : this.dependencies.entrySet()) {
      Variable key=entry.getKey();
      NavigableSet<Variable> vars = entry.getValue();
      NavigableSet<Variable> nvars = new TreeSet<>();
      nvars.addAll(vars);
      result.dependencies.put(key, nvars);
    }

    return result;
  }

}
