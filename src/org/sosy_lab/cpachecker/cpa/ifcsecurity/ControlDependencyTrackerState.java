// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.ifcsecurity;

import java.io.Serializable;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.BlockGuard;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

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
      } catch (UnsupportedCodeException e) {
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
