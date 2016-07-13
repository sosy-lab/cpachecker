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
import org.sosy_lab.cpachecker.cpa.ifcsecurity.flowpolicies.ConglomeratePolicy;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.flowpolicies.Edge;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.policies.PredefinedPolicies;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.policies.SecurityClasses;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * CPA-Abstract-State for enforcing a Security Policy
 */
public class PolicyEnforcementState<E extends Comparable<? super E>> implements AbstractState, Cloneable, Serializable,
LatticeAbstractState<PolicyEnforcementState<E>>, Graphable, AbstractQueryableState{


  private static final long serialVersionUID = 8288327797371384819L;
  /**
   * Internal Variable: Security Policy
   */
  private ConglomeratePolicy<E> policy;
  /**
   * Internal Variable: Default Security Level of new unspecified information
   */
  private E defaultlevel;
  /**
   * Internal Variable: Mapping between Variable and allowed Security Level
   */
  private Map<Variable,E> allowedsecurityclassmapping=new TreeMap<>();
  /**
   * Internal Variable: Mapping between Variable and the Security Levels actually contained in the information
   */
  private Map<Variable,SortedSet<E>> contentsecurityclasslevels= new TreeMap<> ();
  /**
   * Internal Variable: Mapping between Variable and whether it is a global Variable
   */
  private Map<Variable,Boolean> isglobal=new TreeMap<>();
  /**
   * Internal Variable: Set of Variables/Functions that should be checked at every state for a Security Violation
   */
  private Set<Variable> immediatecheck=new TreeSet<>();
  /**
   * Internal Variable: Signalize whether at this state all Variables/Functions should be checked for a Security Violation
   */
  private boolean checkthis=true;
  /**
   * Internal Variable: Signalize whether the Security Class "ANY" should be ignored for the Output-information
   */
  private boolean ignoreany=false;

  @Override
  public String getCPAName() {
    return "PolicyEnforcementCPA";
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    if(pProperty.equals("noninterference")){

      //Check Violation
      if(this.isCheckthis()){
        for(Entry<Variable, SortedSet<E>> entry: this.contentsecurityclasslevels.entrySet()){
            Variable var=entry.getKey();
//          if(this.isglobal.containsKey(var)){
//            if(this.isglobal.get(var).equals(true)){
              E sink=this.allowedsecurityclassmapping.get(var);
              SortedSet<E> source=this.contentsecurityclasslevels.get(var);
              Edge<E> edge=new Edge<>(sink,source);
              if(!(this.policy.getEdges().contains(edge))){
                return true;
              }
            }
//            }
//          }
      }
      //Check immediate outputs
      for(Variable var: this.immediatecheck){
        if(this.allowedsecurityclassmapping.containsKey(var) && this.contentsecurityclasslevels.containsKey(var)){
          E sink=this.allowedsecurityclassmapping.get(var);
          SortedSet<E> source=this.contentsecurityclasslevels.get(var);
          Edge<E> edge=new Edge<>(sink,source);
          if(!(this.policy.getEdges().contains(edge))){
            return true;
          }
        }
      }
      return false;
    }
    return false;
  }

  @Override
  public String toDOTLabel() {
    StringBuilder sb = new StringBuilder();

    sb.append("{");
    sb.append("\\n");
    sb.append("[Policy]="+policy.toString());
    sb.append("\\n");
//    sb.append("[Default Level]="+defaultlevel.toString());
//    sb.append("\\n");
    sb.append("[Initial-Variable Level]");
    for(Entry<Variable, E> entry:allowedsecurityclassmapping.entrySet()){
      Variable key=entry.getKey();
      SecurityClasses value=(SecurityClasses) entry.getValue();
      if(!(ignoreany && value.equals(PredefinedPolicies.UNIMPORTANT))){
        sb.append("("+key+","+value+"), ");
      }
    }
    //sb.append(initialmap.toString());
    sb.append("\\n");
    sb.append("[Content-Level]=");
    sb.append("\\n");
    for(Entry<Variable, SortedSet<E>> entry:contentsecurityclasslevels.entrySet()){
      Variable key=entry.getKey();
      @SuppressWarnings("unchecked")
      SortedSet<SecurityClasses> value2=(SortedSet<SecurityClasses>)  entry.getValue();
      SecurityClasses value=(SecurityClasses) allowedsecurityclassmapping.get(key);
      if(!(ignoreany && value.equals(PredefinedPolicies.UNIMPORTANT))){
        sb.append("("+key+","+value2+"), ");
      }
    }
    sb.append("\\n");
    sb.append("[ImmediateCheckSet]=");
    sb.append("\\n");
    sb.append(immediatecheck);
// //  sb.append(history.toString());
// //    sb.append("[Globals]="+this.isglobal);
// //    sb.append("\\n");
   sb.append("}");
   sb.append("\\n");

    return sb.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    //Highlight all potential Security Leak
    for(Entry<Variable, SortedSet<E>> entry: this.contentsecurityclasslevels.entrySet()){
      Variable var=entry.getKey();
//    if(this.isglobal.containsKey(var)){
//      if(this.isglobal.get(var).equals(true)){
        E sink=this.allowedsecurityclassmapping.get(var);
        SortedSet<E> source=entry.getValue();
        Edge<E> edge=new Edge<>(sink,source);
        if(!(this.policy.getEdges().contains(edge))){
          return true;
        }
      }
//    for(Variable entry: this.contentsecurityclasslevels.entrySet()){
//    Variable var=entry.getKey();
//      if(this.isglobal.containsKey(var)){
//        if(this.isglobal.get(var).equals(true)){
//          E sink=this.allowedsecurityclassmapping.get(var);
//          SortedSet<E> source=this.contentsecurityclasslevels.get(var);
//          Edge edge=new Edge(sink,source);
//          if(!(this.policy.getEdges().contains(edge))){
//            return true;
//          }
//        }
//      }
//    }
    return false;
  }

  @Override
  public boolean equals(Object pOther){
    if (this==pOther) {
       return true;
    }
    if (pOther==null) {
       return false;
    }
    if(!(pOther instanceof PolicyEnforcementState)){
      return false;
    }
    @SuppressWarnings("unchecked")
    PolicyEnforcementState<E> ostate=(PolicyEnforcementState<E>) pOther;
    if(!(this.policy.equals(ostate.policy))){
      return false;
    }
    if(!(this.defaultlevel.equals(ostate.defaultlevel))){
      return false;
    }
    if(!(this.allowedsecurityclassmapping.equals(ostate.allowedsecurityclassmapping))){
      return false;
    }
    if(!(this.contentsecurityclasslevels.equals(ostate.contentsecurityclasslevels))){
      return false;
    }
    return true;
}

  @Override
  public PolicyEnforcementState<E> join(PolicyEnforcementState<E> pOther) {
    if(this.equals(pOther)) {
      return pOther;
    }
    else{
      //Strongest Post Condition
      PolicyEnforcementState<E> merge=this;
      for(Entry<Variable, E> entry: pOther.allowedsecurityclassmapping.entrySet()){
        //(SortedSet<Variable>)
        Variable var=entry.getKey();
        SortedSet<E> deps = pOther.contentsecurityclasslevels.get(var);
        E initialmap=entry.getValue();
        SortedSet<E> ndeps =new TreeSet<>();
        if(this.allowedsecurityclassmapping.containsKey(var)){
          assert(merge.allowedsecurityclassmapping.containsKey(var));
          assert(merge.contentsecurityclasslevels.containsKey(var));
          ndeps=(merge.contentsecurityclasslevels.get(var));
          initialmap=(merge.allowedsecurityclassmapping.get(var));
        }
        for(E sc: deps){
          ndeps.add(sc);
        }
        merge.contentsecurityclasslevels.put(var,ndeps);
        merge.allowedsecurityclassmapping.put(var,initialmap);
      }
      return merge;
    }
  }

  @Override
  public boolean isLessOrEqual(PolicyEnforcementState<E> pOther) throws CPAException, InterruptedException {

    return this.equals(pOther);
  }


  @Override
  public PolicyEnforcementState<E> clone(){
    //Makes a deep copy of the state (except ignoreany, checkthis)
    try {
      super.clone();
    } catch (CloneNotSupportedException e) {
      //    logger.logUserException(Level.WARNING, e, "");
    }

    PolicyEnforcementState<E> result=new PolicyEnforcementState<>();

    result.policy=this.policy;

    result.defaultlevel=this.defaultlevel;

    result.allowedsecurityclassmapping=new TreeMap<>();
    for(Entry<Variable, E> entry :this.allowedsecurityclassmapping.entrySet()){
      Variable key=entry.getKey();
      E val=entry.getValue();
      result.allowedsecurityclassmapping.put(key, val);
    }

    result.contentsecurityclasslevels=new TreeMap<>();
    for(Entry<Variable, SortedSet<E>> entry :this.contentsecurityclasslevels.entrySet()){
      Variable key=entry.getKey();
      SortedSet<E> val=entry.getValue();
      result.contentsecurityclasslevels.put(key, val);
    }

    result.isglobal=this.isglobal;

    result.immediatecheck=this.immediatecheck;

    return result;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

//  protected boolean getcheckthis(){
//    return checkthis;
//  }
//
//  protected Map<Variable,SortedSet<E>> getcontentsecurityclasslevels(){
//    return contentsecurityclasslevels;
//  }


  protected ConglomeratePolicy<E> getPolicy() {
    return policy;
  }


  protected void setPolicy(ConglomeratePolicy<E> pPolicy) {
    policy = pPolicy;
  }


  protected E getDefaultlevel() {
    return defaultlevel;
  }


  protected void setDefaultlevel(E pDefaultlevel) {
    defaultlevel = pDefaultlevel;
  }


  public Map<Variable, E> getAllowedsecurityclassmapping() {
    return allowedsecurityclassmapping;
  }


  protected void setAllowedsecurityclassmapping(Map<Variable, E> pAllowedsecurityclassmapping) {
    allowedsecurityclassmapping = pAllowedsecurityclassmapping;
  }


  public Map<Variable, SortedSet<E>> getContentsecurityclasslevels() {
    return contentsecurityclasslevels;
  }


  protected void setContentsecurityclasslevels(Map<Variable, SortedSet<E>> pContentsecurityclasslevels) {
    contentsecurityclasslevels = pContentsecurityclasslevels;
  }


  protected Map<Variable, Boolean> getIsglobal() {
    return isglobal;
  }


  protected void setIsglobal(Map<Variable, Boolean> pIsglobal) {
    isglobal = pIsglobal;
  }


  protected Set<Variable> getImmediatecheck() {
    return immediatecheck;
  }


  protected void setImmediatecheck(SortedSet<Variable> pImmediatecheck) {
    immediatecheck = pImmediatecheck;
  }


  public boolean isCheckthis() {
    return checkthis;
  }


  protected void setCheckthis(boolean pCheckthis) {
    checkthis = pCheckthis;
  }


  protected boolean isIgnoreany() {
    return ignoreany;
  }


  protected void setIgnoreany(boolean pIgnoreany) {
    ignoreany = pIgnoreany;
  }

}
