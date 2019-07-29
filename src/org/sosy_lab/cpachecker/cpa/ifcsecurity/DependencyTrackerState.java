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
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.combetoutputgenerator.lib.values.JsonArray;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.combetoutputgenerator.lib.values.JsonObj;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.combetoutputgenerator.lib.values.StringValue;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.Variable;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.flowpolicies.Edge;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.policies.SecurityClasses;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.precision.DepPrecision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

/**
 * CPA-Abstract-State for tracking which variables/functions are depends on which other variables/functions
 */
public class DependencyTrackerState
    implements AbstractState, Cloneable, Serializable, LatticeAbstractState<DependencyTrackerState>,
        Graphable,AbstractQueryableState {


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

  /**
   * Internal Variable: Precision;
   */
  private DepPrecision prec;

  public DepPrecision getPrec() {
    return prec;
  }

  public void setPrec(DepPrecision pPrec) {
    prec = pPrec;
  }

  @Override
  public String toDOTLabel() {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\\n");
    sb.append("[Dependencies-SC]={");
    DepPrecision iPrec = prec;
    for (Entry<Variable, SortedSet<Variable>> entry : this.dependencies.entrySet()) {
      Variable var = entry.getKey();
        SecurityClasses aSC = iPrec.getSC(var);
        SortedSet<Variable> dep= (entry.getValue());
        SortedSet<SecurityClasses> cSC = new TreeSet<>();
        for(Variable varD: dep){
          cSC.add(iPrec.getSC(varD));
        }
        Edge<SecurityClasses> edge = new Edge<>(aSC, cSC);
        sb.append("("+var+"="+"["+ dep +"]"+","+edge+")");
    }
    sb.append("}");
    sb.append("\\n");
    sb.append("}");
    sb.append("\\n");
    return sb.toString();
  }

  @Override
  public String toString() {
    return toDOTLabel();
  }

  public JsonObj toJson(String stateID) {
    JsonObj state=new JsonObj();
    JsonObj stateDeps=new JsonObj();
    state.add(new StringValue(stateID), stateDeps);
    for( Entry<Variable, SortedSet<Variable>> vardep:dependencies.entrySet()){
      JsonArray stateDepsR = new JsonArray();
      stateDeps.add(new StringValue(vardep.getKey().toString()), stateDepsR);
      for(Variable dep:vardep.getValue()){
        stateDepsR.add(new StringValue(dep.toString()));
      }
    }
    return state;
  }

  public JsonObj toJson() {
    if(dependencies.size()==0){
      return null;
    }

    JsonObj stateDeps=new JsonObj();
    for( Entry<Variable, SortedSet<Variable>> vardep:dependencies.entrySet()){
      JsonArray stateDepsR = new JsonArray();
      stateDeps.add(new StringValue(vardep.getKey().toString()), stateDepsR);
      for(Variable dep:vardep.getValue()){
        stateDepsR.add(new StringValue(dep.toString()));
      }
    }
    return stateDeps;
  }




  @Override
  public boolean shouldBeHighlighted() {
    DepPrecision iPrec = prec;
    for (Entry<Variable, SortedSet<Variable>> entry : this.dependencies.entrySet()) {
      Variable var = entry.getKey();
      // Immediate
      if (iPrec.isViolable(var)) {
        SecurityClasses aSC = iPrec.getSC(var);
        SortedSet<Variable> dep = (entry.getValue());
        SortedSet<SecurityClasses> cSC = new TreeSet<>();
        for (Variable varD : dep) {
          cSC.add(iPrec.getSC(varD));
        }
        Edge<SecurityClasses> edge = new Edge<>(aSC, cSC);
        SortedSet<Edge<SecurityClasses>> violationList = iPrec.getNset();
        if (violationList.contains(edge)) {
          return true;
        }
      }
    }
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
    for (Entry<Variable, SortedSet<Variable>> entry : this.dependencies.entrySet()) {
      Variable var=entry.getKey();
      if (pOther.dependencies.containsKey(var)) {
        if (!(pOther.dependencies.get(var).containsAll(
            this.dependencies.get(var)))) {
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


  @Override
  public String getCPAName() {
    return "DependencyTrackerCPA";
  }


  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    //EXIT + Property
    if (pProperty.equals("noninterference_All")) {
      DepPrecision iPrec = prec;
      for (Entry<Variable, SortedSet<Variable>> entry : this.dependencies.entrySet()) {
        Variable var = entry.getKey();
        if (iPrec.isViolable(var)) {
          SecurityClasses aSC = iPrec.getSC(var);
          SortedSet<Variable> dep= (entry.getValue());
          SortedSet<SecurityClasses> cSC = new TreeSet<>();
          for(Variable varD: dep){
            cSC.add(iPrec.getSC(varD));
          }
          Edge<SecurityClasses> edge = new Edge<>(aSC, cSC);
          SortedSet<Edge<SecurityClasses>> violationList = iPrec.getNset();
          if (violationList.contains(edge)) {
            return true;
          }
        }
      }
    }
    //Property
    if (pProperty.equals("noninterference_All_Violables")) {
      DepPrecision iPrec = prec;
      SortedSet<Variable> av = iPrec.getAlwaysViolation();
      for (Entry<Variable, SortedSet<Variable>> entry : this.dependencies.entrySet()) {
        Variable var = entry.getKey();
        //Immediate
        if (av.contains(var) && iPrec.isViolable(var)) {
          SecurityClasses aSC = iPrec.getSC(var);
          SortedSet<Variable> dep= (entry.getValue());
          SortedSet<SecurityClasses> cSC = new TreeSet<>();
          for(Variable varD: dep){
            cSC.add(iPrec.getSC(varD));
          }
          Edge<SecurityClasses> edge = new Edge<>(aSC, cSC);
          SortedSet<Edge<SecurityClasses>> violationList = iPrec.getNset();
          if (violationList.contains(edge)) {
            return true;
          }
        }
      }
    }
    return false;
  }

}
