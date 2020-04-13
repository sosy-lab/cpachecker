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
package org.sosy_lab.cpachecker.cpa.collector;


import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.ImmutableList;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nullable;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;


public class CollectorState extends AbstractSingleWrapperState implements Graphable, Serializable {
  private static final UniqueIdGenerator idGenerator = new UniqueIdGenerator();
  private static int count;
  private final Collection<ARGState> wrappedParent;
  private final int currentARGid;
  private final ImmutableList<Integer> List_wrappedParent;
  private final ImmutableList<Integer> List_wrappedChildren;
  private final Boolean ismerged;
  private final int count2;
  private final ARGState argstate;
  private Collection<ARGState> childrenTomerge2;
  private Collection<ARGState> childrenTomerge1;
  private ImmutableList<AbstractState> states;
  private ARGStateView myARGTransferRelation;
  private ARGStateView myARG1;
  private ARGStateView myARG2;
  private ARGStateView myARGmerged;
  private boolean isStopped = false;

  public CollectorState(
      AbstractState pWrappedState,
      @Nullable Collection<AbstractState> pCollectorState,
      @Nullable ARGStateView myARGtransfer,
      Boolean merged,
      @Nullable ARGStateView pMyARG1,
      @Nullable ARGStateView pMyARG2,
      @Nullable ARGStateView pMyARGmerged,
      LogManager cLogger) {
    super(pWrappedState);
    ismerged = merged;
    if (myARGtransfer != null) {
      this.myARGTransferRelation = myARGtransfer;
    }
    if (pMyARG1 != null) {
      this.myARG1 = pMyARG1;
      childrenTomerge1 = this.myARG1.getChildrenOfToMerge();
    }
    if (pMyARG2 != null) {
      this.myARG2 = pMyARG2;
      childrenTomerge2 = this.myARG2.getChildrenOfToMerge();
    }
    if (pMyARGmerged != null) {
      this.myARGmerged = pMyARGmerged;
    }

    if (pCollectorState != null) {
      this.states = ImmutableList.copyOf(pCollectorState);
    }

    ARGState wrapped = (ARGState) pWrappedState;
    argstate = wrapped;
    wrappedParent = wrapped.getParents();
    Iterable<Integer> stateID_wrappedParent = stateIdsOf(wrappedParent);
    Collection<ARGState> wrappedChildren = wrapped.getChildren();
    Iterable<Integer> stateID_wrappedChildren = stateIdsOf(wrappedChildren);
    List_wrappedParent = ImmutableList.copyOf(stateID_wrappedParent);
    List_wrappedChildren = ImmutableList.copyOf(stateID_wrappedChildren);
    currentARGid = ((ARGState) pWrappedState).getStateId();
    count2 = this.getCount();
    count++;

  }

  public boolean isStopped() {
    return isStopped;
  }

  public void setStopped() {
    isStopped = true;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("\nCollectorCount:  ");
    sb.append(count2).append("\n");
    sb.append("Current:  ");
    sb.append("Sucessors: ");
    sb.append(List_wrappedChildren);
    sb.append(" Ancestors: ");
    sb.append(List_wrappedParent);
    sb.append(" StateID: ").append(currentARGid);

    sb.append("\n Current myARGID Infos:  ");
    if (myARGmerged != null) {
      sb.append("\n" + "merged myARG:  ");
      sb.append("\n").append(myARGmerged.toDOTLabel());
    }
    if (myARG1 != null) {
      sb.append("\n" + "myARG 1:  ");
      sb.append("\n").append(myARG1.toDOTLabel());
      sb.append("\n" + "childrenOfMergePartner 1:  ");
      if (myARG1 != null) {
        sb.append("\n").append(childrenTomerge1);
      }
    }
    if (myARG2 != null) {
      sb.append("\n" + "myARG 2:  ");
      sb.append("\n").append(myARG2.toDOTLabel());
      sb.append("\n" + "childrenOfMergePartner 2:  ");
      if (myARG2 != null) {
        sb.append("\n").append(childrenTomerge2);
      }
    }
    if (myARGTransferRelation != null) {
      sb.append("\n" + "MyARGTransferRelation:");
      sb.append("\n").append(myARGTransferRelation.toDOTLabel());
    }

    sb.append("\n");
    return sb.toString();
  }

  private Iterable<Integer> stateIdsOf(Iterable<ARGState> elements) {
    return from(elements).transform(ARGState::getStateId);
  }

  @Override
  public String toDOTLabel() {
    if (getWrappedState() instanceof Graphable) {
      return ((Graphable) getWrappedState()).toDOTLabel();
    }
    return "";
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  public ARGState getARGState() {
    return argstate;
  }

  public ARGStateView getMyARGTransferRelation() {
    return myARGTransferRelation;
  }

  public ARGStateView getmyARG1() {
    return myARG1;
  }

  public ARGStateView getMyARG2() {
    return myARG2;
  }

  public ARGStateView getMyARGmerged() {
    return myARGmerged;
  }

  public int getStateId() {
    return currentARGid;
  }

  public int getCount() {
    return count;
  }

  public Collection<ARGState> getChildrenTomerge1() {
    return childrenTomerge1;
  }

  public Collection<ARGState> getChildrenTomerge2() {
    return childrenTomerge2;
  }

  public Collection<ARGState> getParents() {
    return Collections.unmodifiableCollection(wrappedParent);
  }

  public ImmutableList<AbstractState> getStorage() {
    return states;
  }

  public boolean ismerged() {
    return ismerged;
  }


  @Override
  public final boolean equals(Object pObj) {

    if (pObj instanceof CollectorState) {
      ARGState theArg = this.getARGState();
      CollectorState obj = (CollectorState) pObj;
      ARGState objARG = obj.getARGState();

      //compare ARGStates
      if (theArg == objARG) {
        return true;
      } else {
        return false;
      }
    }
    return false;
  }

  @Override
  public final int hashCode() {
    // Object.hashCode() is consistent with our compareTo()
    // because stateId is a unique identifier.
    return super.hashCode();
  }

}
