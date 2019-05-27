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
import java.util.List;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;

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
  private Object secondparent;
  private Object firstparent;
  private Collection<ARGState> testmalparent;
  private AbstractState eA;
  private ARGState entryAnew;
  private Collection<ARGState> entryParent;
  private ARGState entryA;
  private ImmutableList<AbstractState> statestest;
  private ARGState argentry;
  private AbstractState entrywrapped;
  private ImmutableList<AbstractState> states;
  private final ARGState argstate;
  private final CFANode node;
  private List <CFAEdge> edgelist;
  private AbstractState first;
  private int id;
  private AbstractState second;
  private Object firstParent;
  private Object firstChildren;
  private ArrayList ancestors = new ArrayList<ARGState>();
  private Collection<ARGState> collectionARG = new ArrayList<ARGState>();
  private ArrayList parents = new ArrayList<ARGState>();
  private myARGState testmyARGtransfer;
  private myARGState testmyARG;
  private myARGState testmyARG2;

  public CollectorState(AbstractState pWrappedState,
                        @Nullable Collection<AbstractState> pCollectorState,
                        @Nullable myARGState myARGtransfer,
                        Boolean merged,
                        @Nullable myARGState myARG1,
                        @Nullable myARGState myARG2,
                        LogManager clogger) {
    super(pWrappedState);
    logger = clogger;
    ismerged = merged;
    if (myARGtransfer != null) {
      testmyARGtransfer = myARGtransfer;
    }
    if (myARG1 != null) {
      testmyARG = myARG1;
      //logger.log(Level.INFO, "sonja testmyARG:\n" + testmyARG.toDOTLabel());
    }
    if (myARG2 != null) {
      testmyARG2 = myARG2;
      //logger.log(Level.INFO, "sonja testmyARG2:\n" + testmyARG2.toDOTLabel());
    }

    if (pCollectorState != null) {
      this.states = ImmutableList.copyOf(pCollectorState);
      statestest = ImmutableList.copyOf(pCollectorState);
    }

    ARGState wrapped = (ARGState) pWrappedState;
    argstate = wrapped;
    wrappedParent = wrapped.getParents();
    StateID_wrappedParent = stateIdsOf(wrappedParent);
    wrappedChildren = wrapped.getChildren();
    StateID_wrappedChildren = stateIdsOf(wrappedChildren);
    current =  getWrappedState();
    //logger.log(Level.INFO, "sonja currentTEST:\n" + current);
    List_wrappedParent = ImmutableList.copyOf(StateID_wrappedParent);
    List_wrappedChildren = ImmutableList.copyOf(StateID_wrappedChildren);
    currentARGid = ((ARGState) pWrappedState).getStateId();
    stateId = idGenerator.getFreshId();
    wrappedWrapped = argstate.getWrappedState();


    //probably needed:
    CFANode currentLoc = AbstractStates.extractLocation(this);
    //logger.log(Level.INFO, "sonja CFANODE:\n" + currentLoc);
    //CFANode childLoc = AbstractStates.extractLocation(pChild);
    for (ARGState child: wrapped.getChildren()) {
      List<CFAEdge> edges = wrapped.getEdgesToChild(child);
      edgelist = edges;
     //logger.log(Level.INFO, "sonja CFAEDGE:\n" + edges);
    }
    node = currentLoc;

  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    //sb.append("CollectorStateID:  ");
    //sb.append(stateId );
    if (states != null) {
     sb.append("Current:  ");
     sb.append("Sucessors: ");
     sb.append(List_wrappedChildren);
     sb.append(" Ancestors: ");
     sb.append(List_wrappedParent);
     sb.append (" StateID: " + currentARGid);
      sb.append("\n" + "myARG 1:  ");
      if (testmyARG != null){
      sb.append("\n" + testmyARG.toDOTLabel());}
      sb.append("\n" + "myARG 2:  ");
      if (testmyARG2 != null){
        sb.append("\n" + testmyARG2.toDOTLabel());}

     sb.append("\n" + "Storage:  ");
     sb.append(states);
   }
   else{
     /**sb.append("NODE&EDGE ");
     sb.append(node);
     sb.append(edgelist);**/
     sb.append("Current:  ");
     sb.append("Sucessors: ");
     sb.append(List_wrappedChildren);
     sb.append(" Ancestors: ");
     sb.append(List_wrappedParent);
     sb.append (" StateID: " + currentARGid);
      if (testmyARGtransfer != null){
        sb.append("\n" +"MyARGTransfer:");
        sb.append("\n" + testmyARGtransfer.toDOTLabel());}
   }
    sb.append ("\n");
    return sb.toString();
  }

  private Iterable<Integer> stateIdsOf(Iterable<ARGState> elements) {
    return from(elements).transform(ARGState::getStateId);
  }
  private Iterable<Boolean> destroyedID(Iterable<ARGState> elements) {
    return from(elements).transform(ARGState::isDestroyed);
  }
  private Iterable<ARGState> mergedID(Iterable<ARGState> elements) {
    return from(elements).transform(ARGState::getMergedWith);
  }
  private Iterable<Collection<ARGState>> children(Iterable<ARGState> elements) {
    return from(elements).transform(ARGState::getChildren);
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
  public myARGState getMyARGTransfer() {
    return testmyARGtransfer;
  }
  public myARGState getTestmyARG() {
    return testmyARG;
  }
  public myARGState getTestmyARG2() {
    return testmyARG2;
  }
  public int getStateId() { return currentARGid; }
  /**
   * Get the child elements of this state.
   * @return An unmodifiable collection of ARGStates without duplicates.
   */
  public Collection<ARGState> getChildren() {
    return Collections.unmodifiableCollection(wrappedChildren);
  }

  public ImmutableList<AbstractState> getStorage() { return states; }

  public AbstractState getfirstStorage(){return states.get(0);}
  public AbstractState getsecondStorage(){return states.get(1);}

  public int getfirstID() {
    first =states.get(0);
    id = ((CollectorState) first).getStateId();
    return id;
  }
  public int getsecondID() {
    second =states.get(1);
    id = ((CollectorState) second).getStateId();
    return id;
  }

  public Object getParentsID() {
    firstParent = List_wrappedParent.get(0);
    return firstParent;
  }

  public Object getChildrenID() {
    if (List_wrappedChildren.size() >= 1) {
      firstChildren = List_wrappedChildren.get(0);
      return firstChildren;
    } else {
      return "";
    }
  }

  public AbstractState getWrappedWrappedState() {
    return wrappedWrapped;
  }
  public AbstractState getEntrywrapped() {
    return entrywrapped;
  }
  public ARGState getARGEntry() {
    return argentry;
  }
  public ImmutableList getAncestor(){return List_wrappedParent; }
  public Boolean ismerged() {
    return ismerged;
  }

}
