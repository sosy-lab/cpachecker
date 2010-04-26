/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.fllesh.fql.fllesh.reachability;

import java.util.Set;

import org.sosy_lab.cpachecker.cpa.composite.CompositeElement;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecision;

public class Waypoint {
  private Query mQuery;
  
  private CompositeElement mCompositeElement;
  /* if precision is null it has to be an target waypoint */
  private CompositePrecision mCompositePrecision;
  
  private Set<Integer> mStatesOfFirstAutomaton;
  private Set<Integer> mStatesOfSecondAutomaton;
  
  public Waypoint(Query pQuery, CompositeElement pCompositeElement, CompositePrecision pCompositePrecision, Set<Integer> pStatesOfFirstAutomaton, Set<Integer> pStatesOfSecondAutomaton) {
    assert(pQuery != null);
    // TODO: add assertion
    
    mQuery = pQuery;
    mCompositeElement = pCompositeElement;
    // precision can be null in case the waypoint is used as a target
    mCompositePrecision = pCompositePrecision;
    mStatesOfFirstAutomaton = pStatesOfFirstAutomaton;
    mStatesOfSecondAutomaton = pStatesOfSecondAutomaton;
  }
  
  public Query getQuery() {
    return mQuery;
  }
  
  public CompositeElement getElement() {
    return mCompositeElement;
  }
  
  public CompositePrecision getPrecision() {
    return mCompositePrecision;
  }
  
  public Set<Integer> getStatesOfFirstAutomaton() {
    return mStatesOfFirstAutomaton;
  }
  
  public Set<Integer> getStatesOfSecondAutomaton() {
    return mStatesOfSecondAutomaton;
  }
  
  @Override
  public String toString() {
    return "( <" + mCompositeElement.toString() + ", " + ((mCompositePrecision == null)?"NO PRECISION":mCompositePrecision.toString()) + ">, " + mStatesOfFirstAutomaton.toString() + ", " + mStatesOfSecondAutomaton.toString() + ")";
  }
  
  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    
    if (pOther == null) {
      return false;
    }
    
    if (pOther.getClass() == getClass()) {
      Waypoint lWaypoint = (Waypoint)pOther;
      
      return lWaypoint.mQuery.equals(mQuery) &&
              lWaypoint.mCompositeElement.equals(mCompositeElement) &&
              (lWaypoint.mCompositePrecision == null)?(mCompositePrecision == null):
              (lWaypoint.mCompositePrecision.equals(mCompositePrecision)) &&
              lWaypoint.mStatesOfFirstAutomaton.equals(mStatesOfFirstAutomaton) &&
              lWaypoint.mStatesOfSecondAutomaton.equals(mStatesOfSecondAutomaton);
    }
    
    return false;
  }
  
  @Override
  public int hashCode() {
    return 234223 + mQuery.hashCode() + mCompositeElement.hashCode() + ((mCompositePrecision == null)?0:mCompositePrecision.hashCode()) + mStatesOfFirstAutomaton.hashCode() + mStatesOfSecondAutomaton.hashCode();
  }
  
}
