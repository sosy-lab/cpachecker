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
  private final Collection<AbstractState> test = new ArrayList<>(1);
  private final Collection<AbstractState> test2;
  private ImmutableList<AbstractState> states;
  private final ARGState argstate;

  private final ImmutableList<ARGState> dieliste;

  public CollectorState(AbstractState pWrappedState,
                        @Nullable Collection<AbstractState> pCollectorState,
      LogManager clogger) {
    super(pWrappedState);
    if (pCollectorState != null) {
      this.states = ImmutableList.copyOf(pCollectorState);
    }
    logger = clogger;
    //logger.log(Level.INFO, "sonja collectorstate pwrappedState:\n" + pWrappedState);
    //AbstractState wrappedState = pWrappedState;
    //ARGState wrapped = (ARGState) ((CollectorState) wrappedState).getWrappedState();
    //logger.log(Level.INFO, "sonja collectorstate wrapped:\n" + wrapped);
    ARGState wrapped = (ARGState) pWrappedState;
    argstate = wrapped;
    wrappedParent = wrapped.getParents();
    StateID_wrappedParent = stateIdsOf(wrappedParent);
    wrappedChildren = wrapped.getChildren();
    StateID_wrappedChildren = stateIdsOf(wrappedChildren);
    current =  getWrappedState();
    //logger.log(Level.INFO, "sonja currentTEST:\n" + current);
    test.add(current);
    test2 = Collections.unmodifiableCollection(test);
    //logger.log(Level.INFO, "sonja currentTEST2:\n" + current);
    dieliste = ImmutableList.copyOf(new ARGState[]{argstate});
    List_wrappedParent = ImmutableList.copyOf(StateID_wrappedParent);
    List_wrappedChildren = ImmutableList.copyOf(StateID_wrappedChildren);
    currentARGid = ((ARGState) pWrappedState).getStateId();
    stateId = idGenerator.getFreshId();

 /**ARGState test1 = (ARGState) getWrappedState();
    ArrayList<ARGState> list = new ArrayList<>();
    list.add(test1);
// Create an ImmutableList from the ArrayList.
    //ImmutableList<ARGState> immutable = ImmutableList.copyOf(list);
    dieliste = ImmutableList.copyOf(list);
      //logger.log(Level.INFO, "sonjas immutableList:\n" + dieliste);**/
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    //sb.append("CollectorStateID:  ");
    //sb.append(stateId );
    sb.append("Current:  ");
    sb.append("Sucessors: ");
    sb.append(List_wrappedChildren);
    sb.append(" Ancestors: ");
    sb.append(List_wrappedParent);
    sb.append (" StateID: " + currentARGid);
    //sb.append("\n"+ "ARGState: " + getWrappedState()+ "\n");
    sb.append("\n"+ "ARGState: " + dieliste + "\n");
    //sb.append("\n"+ "ARGState: " + test2 + "\n");
   if (states != null) {
     sb.append("\n" + "Storage:  ");
     sb.append(states);
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
}
