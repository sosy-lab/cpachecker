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
package org.sosy_lab.cpachecker.cpa.chc;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public class CHCState implements AbstractState {

  /*
   * store the node number: needed to build an ancestor tree
   */
  int nodeNumber;

  /*
   *  symbolic representation of the set of concrete
   *  states approximated by this abstract state
   */
  Constraint constraint = null;

  /*
   * abstract state from which this state has been
   * derived by using the transfer relation
   */
  CHCState ancestor = null;

  /*
   * the state associated with the program point of
   * the function call statement
   */
  CHCState caller = null;


  public CHCState() {
    nodeNumber = 0;
    constraint = new Constraint();
    caller = this;
  }

  public CHCState(int nodeId, Constraint constraint) {
    this.nodeNumber = nodeId;
    this.constraint = constraint;
    caller = this;
  }

  /*
   * Copy constructor
   */
  public CHCState(CHCState crState) {
    nodeNumber = crState.nodeNumber;
    constraint = new Constraint(crState.getConstraint());
    ancestor = crState;
    caller = crState.getCaller();
  }

  public void setNodeNumber(int nodeId) {
    this.nodeNumber = nodeId;
  }

  public void setConstraint(Constraint constraint) {
    this.constraint = constraint;
  }

  public void setAncestror(CHCState ancestor) {
    this.ancestor = ancestor;
  }

  public void setCaller(CHCState caller) {
    this.caller = caller;
  }

  public int getNodeId() {
    return nodeNumber;
  }

  public Constraint getConstraint() {
    return constraint;
  }

  public CHCState getAncestor() {
    return ancestor;
  }

  public CHCState getCaller() {
    return caller;
  }

  public void updateConstraint(Constraint cns) {
    constraint = ConstraintManager.and(constraint, cns);
  }

  public void addConstraint(Constraint cns) {
    constraint.and(cns);
  }

  public void join(CHCState state1) {
    constraint = ConstraintManager.convexHull(this.constraint,state1.getConstraint());
  }

  public boolean isBottom() {
    if (constraint.isFalse()) {
      return true;
    }
    return false;
  }

  @Override
  public String toString () {
    return constraint.toString();
  }
}
