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
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.BlockGuard;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;

import java.io.Serializable;

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
  private BlockGuard guards=new BlockGuard();

  public BlockGuard getGuards() {
    return guards;
  }


  public void setGuards(BlockGuard pGuards) {
    guards = pGuards;
  }

  @Override
  public String toDOTLabel() {
    StringBuilder sb = new StringBuilder();

    sb.append("{");
    sb.append("\\n");
    sb.append("[Guards]="+guards.toString());
    sb.append("\\n");
    sb.append("}");
    sb.append("\\n");

    return sb.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  public boolean isEqual(ControlDependencyTrackerState pOther) {
      if (this==pOther) {
         return true;
      }
      if (pOther==null) {
         return false;
      }
      return this.guards.equals(pOther.guards);
  }




  @Override
  public ControlDependencyTrackerState join(ControlDependencyTrackerState pOther) {
      if(this.isEqual(pOther)) {
        return pOther;
      }
      else{
        //Strongest Post Condition
        ControlDependencyTrackerState merge=this;
        try {
          merge.guards=this.guards.meet(pOther.guards);
        } catch (UnsupportedCCodeException e) {
          //logger.log(Level.WARNING,e.toString());
        }
        return merge;
      }
  }

  @Override
  public boolean isLessOrEqual(ControlDependencyTrackerState pOther) throws CPAException, InterruptedException {
    return this.isEqual(pOther);
  }


  @Override
  public ControlDependencyTrackerState clone(){
    try {
      super.clone();
    } catch (CloneNotSupportedException e) {
  //    logger.logUserException(Level.WARNING, e, "");
    }
    ControlDependencyTrackerState result=new ControlDependencyTrackerState();
    result.guards=this.guards.clone();
    return result;
  }
}
