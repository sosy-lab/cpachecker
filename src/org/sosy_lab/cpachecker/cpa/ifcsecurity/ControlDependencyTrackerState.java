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

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.combetoutputgenerator.lib.values.StringValue;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.Variable;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * CPA-Abstract-State for tracking the Active Control Dependencies
 */
public class ControlDependencyTrackerState
    implements AbstractState, Cloneable, Serializable,
        LatticeAbstractState<ControlDependencyTrackerState>, Graphable {


  private static final long serialVersionUID = -2622026109609951120L;
  /**
   * Active Control Dependencies
   */

  /**
   * Internal Variable: Context L -> V
   */
  private Map<CFANode, SortedSet<Variable>> contexts = new TreeMap<>();

  public Map<CFANode, SortedSet<Variable>> getContexts() {
    return contexts;
  }

  public void setContexts(Map<CFANode, SortedSet<Variable>> pContexts) {
    contexts = pContexts;
  }

  /**
   * Internal Variable: unrefined Context L -> V
   */
  private Map<CFANode, SortedSet<Variable>> uRcontexts = new TreeMap<>();


  public Map<CFANode, SortedSet<Variable>> getuRcontexts() {
    return uRcontexts;
  }


  public void setuRcontexts(Map<CFANode, SortedSet<Variable>> pURcontexts) {
    uRcontexts = pURcontexts;
  }

  @Override
  public String toDOTLabel() {
    StringBuilder sb = new StringBuilder();

    sb.append("{");
    sb.append("\\n");
    sb.append("[Context]="+contexts.toString());
    sb.append("\\n");
    sb.append("}");
    sb.append("\\n");

    return sb.toString();
  }

  @Override
  public String toString() {
    return toDOTLabel();
  }

  public StringValue toJson() {
    // StringBuilder sb = new StringBuilder();
    //
    // if(guards.getSize()==0){
    // return null;
    // }
    //
    // StringValue stateGuards=new StringValue(guards.toString());
    //
    // return stateGuards;
    return null;
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  public boolean isEqual(ControlDependencyTrackerState pOther) {
    if (this == pOther) { return true; }
    if (pOther == null) { return false; }
    for (Entry<CFANode, SortedSet<Variable>> entry : this.contexts.entrySet()) {
      CFANode cfaNode=entry.getKey();
      if (pOther.contexts.containsKey(cfaNode)) {
        if (!(this.contexts.get(cfaNode).containsAll(
            pOther.contexts.get(cfaNode)))) { return false; }
      } else {
        return false;
      }
    }
    for (Entry<CFANode, SortedSet<Variable>> entry : pOther.contexts.entrySet()) {
      CFANode cfaNode=entry.getKey();
      if (this.contexts.containsKey(cfaNode)) {
        if (!(pOther.contexts.get(cfaNode).containsAll(
            this.contexts.get(cfaNode)))) { return false; }
      } else {
        return false;
      }
    }
    return true;
  }




  @Override
  public ControlDependencyTrackerState join(ControlDependencyTrackerState pOther) {
    if (this.isEqual(pOther)) {
      return pOther;
    } else {
      //Strongest Post Condition
      ControlDependencyTrackerState merge = this;
      //implicit copy of this
      //explicit copy of pOther
      for (CFANode cfaNode : pOther.contexts.keySet()) {
        SortedSet<Variable> deps = pOther.contexts.get(cfaNode);
        SortedSet<Variable> ndeps = new TreeSet<>();
        if (this.contexts.containsKey(cfaNode)) {
          assert (merge.contexts.containsKey(cfaNode));
          ndeps = merge.contexts.get(cfaNode);
        }
        for (Variable var2 : deps) {
          ndeps.add(var2);
        }
        merge.contexts.put(cfaNode, ndeps);
      }
      return merge;
    }
  }

  @Override
  public boolean isLessOrEqual(ControlDependencyTrackerState pOther) throws CPAException, InterruptedException {
    if (this == pOther) { return true; }
    if (pOther == null) { return false; }
    for (Entry<CFANode, SortedSet<Variable>> entry : this.contexts.entrySet()) {
      CFANode var=entry.getKey();
      if (pOther.contexts.containsKey(var)) {
        if (!(pOther.contexts.get(var).containsAll(
            this.contexts.get(var)))) {
          return false;
          }
      } else {
        return false;
      }
    }
    return true;
  }


  @Override
  public ControlDependencyTrackerState clone(){

    try {
      super.clone();
    } catch (CloneNotSupportedException e) {
    }

    ControlDependencyTrackerState result = new ControlDependencyTrackerState();

    result.contexts = new TreeMap<>();
    for (Entry<CFANode, SortedSet<Variable>> entry : this.contexts.entrySet()) {
      CFANode key=entry.getKey();
      SortedSet<Variable> vars = entry.getValue();
      SortedSet<Variable> nvars = new TreeSet<>();
      for (Variable var : vars) {
        nvars.add(var);
      }
      result.contexts.put(key, nvars);
    }

    result.uRcontexts = new TreeMap<>();
    for (Entry<CFANode, SortedSet<Variable>> entry : this.uRcontexts.entrySet()) {
      CFANode key=entry.getKey();
      SortedSet<Variable> vars = entry.getValue();
      SortedSet<Variable> nvars = new TreeSet<>();
      for (Variable var : vars) {
        nvars.add(var);
      }
      result.uRcontexts.put(key, nvars);
    }

    return result;
  }
}
