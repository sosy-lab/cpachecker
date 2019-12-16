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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;


public class CollectorState extends AbstractSingleWrapperState implements Graphable, Serializable {
  private final LogManager logger;
  private static final UniqueIdGenerator idGenerator = new UniqueIdGenerator();
  private final int stateId;
  private final Collection<ARGState> wrappedParent;
  private final Collection<ARGState> wrappedChildren;
  private final int currentARGid;
  private final Iterable<Integer> StateID_wrappedParent;
  private final Iterable<Integer> StateID_wrappedChildren;
  private final ImmutableList List_wrappedParent;
  private final ImmutableList List_wrappedChildren;
  private final AbstractState current;
  private final AbstractState wrappedWrapped;
  private final Boolean ismerged;
  private final boolean toMerge=false;
  private final int count2;
  private Object secondparent;
  private Object firstparent;
  private ImmutableList<AbstractState> states;
  private final ARGState argstate;
  private AbstractState first;
  private int id;
  private AbstractState second;
  private Object firstParent;
  private Object firstChildren;
  private ArrayList ancestors = new ArrayList<ARGState>();
  private myARGState myARGTransferRelation;
  private myARGState myARG1;
  private myARGState myARG2;
  private myARGState myARGmerged;
  //private boolean merged = false;
  //private boolean toMerge = false;
  private static int count;

  public CollectorState(AbstractState pWrappedState,
                        @Nullable Collection<AbstractState> pCollectorState,
                        @Nullable myARGState myARGtransfer,
                        Boolean merged,
                        @Nullable myARGState pMyARG1,
                        @Nullable myARGState pMyARG2,
                        @Nullable myARGState pMyARGmerged,
                        LogManager cLogger) {
    super(pWrappedState);
    logger = cLogger;
    ismerged = merged;
    if (myARGtransfer != null) {
      this.myARGTransferRelation = myARGtransfer;
    }
    if (pMyARG1 != null) {
      this.myARG1 = pMyARG1;
    }
    if (pMyARG2 != null) {
      this.myARG2 = pMyARG2;
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
    StateID_wrappedParent = stateIdsOf(wrappedParent);
    wrappedChildren = wrapped.getChildren();
    StateID_wrappedChildren = stateIdsOf(wrappedChildren);
    current =  getWrappedState();
    List_wrappedParent = ImmutableList.copyOf(StateID_wrappedParent);
    List_wrappedChildren = ImmutableList.copyOf(StateID_wrappedChildren);
    currentARGid = ((ARGState) pWrappedState).getStateId();
    stateId = idGenerator.getFreshId();
    wrappedWrapped = argstate.getWrappedState();
    count2= this.getCount();
    count ++;

  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("\nCollectorCount:  ");
    sb.append(count2 + "\n");
    sb.append("Current:  ");
    sb.append("Sucessors: ");
    sb.append(List_wrappedChildren);
    sb.append(" Ancestors: ");
    sb.append(List_wrappedParent);
    sb.append (" StateID: " + currentARGid);

      sb.append("\n Current myARGID:  ");
      if (myARGmerged != null){
        sb.append("\n" + myARGmerged.toDOTLabel());
      }
      sb.append("\n" + "myARG 1:  ");
      if (myARG1 != null){
      sb.append("\n" + myARG1.toDOTLabel());
      }
      sb.append("\n" + "myARG 2:  ");
      if (myARG2 != null){
        sb.append("\n" + myARG2.toDOTLabel());
      }
      if (myARGTransferRelation != null){
        sb.append("\n" +"MyARGTransfer:");
        sb.append("\n" + myARGTransferRelation.toDOTLabel());}

    sb.append ("\n");
    return sb.toString();
  }

  private Iterable<Integer> stateIdsOf(Iterable<ARGState> elements) {
    return from(elements).transform(ARGState::getStateId);
  }
  private Iterable<Boolean> destroyedID(Iterable<ARGState> elements) {
    return from(elements).transform(ARGState::isDestroyed);
  }

  @Override
  public String toDOTLabel() {
    if (getWrappedState() instanceof Graphable) {
      return ((Graphable)getWrappedState()).toDOTLabel();
    }
    return "";
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }
  public ARGState getARGState() {
    return argstate ;
  }
  public myARGState getMyARGTransferRelation() {
    return myARGTransferRelation;
  }
  public myARGState getmyARG1() {
    return myARG1;
  }
  public myARGState getMyARG2() {
    return myARG2;
  }
  public myARGState getMyARGmerged() {
    return myARGmerged;
  }
  public int getStateId() { return currentARGid; }
  public int getCount(){return count;}

  /**
   * Get the child elements of this state.
   * @return An unmodifiable collection of ARGStates without duplicates.
   */
  public Collection<ARGState> getChildren() {
    return Collections.unmodifiableCollection(wrappedChildren);
  }

  public ImmutableList<AbstractState> getStorage() { return states; }


  public AbstractState getWrappedWrappedState() {
    return wrappedWrapped;
  }
  public ImmutableList getAncestor(){return List_wrappedParent; }
  public boolean ismerged() {
    return ismerged;
  }

}
