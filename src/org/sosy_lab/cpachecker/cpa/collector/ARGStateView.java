/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
import java.util.Collection;
import java.util.Collections;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

public class ARGStateView implements Graphable {
  private static final UniqueIdGenerator idGenerator = new UniqueIdGenerator();
  private final int currentID;
  private final AbstractState wrappedelement;
  private final boolean toMergeID;
  private final ARGState element;
  private final int stateId;
  private ImmutableList<ARGState> childrenlist;
  private ImmutableList<ARGState> parentslist;


  public ARGStateView(
      ARGState cElement,
      @Nullable ARGState pParentElement,
      @Nullable Collection<ARGState> cParents,
      @Nullable Collection<ARGState> cChildren,
      boolean toMerge,
      LogManager clogger) {
    stateId = idGenerator.getFreshId();
    element = cElement;
    toMergeID = toMerge;
    wrappedelement = element.getWrappedState();
    currentID = element.getStateId();
    if (cChildren != null) {
      childrenlist = ImmutableList.copyOf(cChildren);
    }
    if (cParents != null) {
      parentslist = ImmutableList.copyOf(cParents);
    }
  }

  @Override
  public String toDOTLabel() {
    StringBuilder sb = new StringBuilder();

    sb.append("myARG State: (myId: ");
    sb.append(stateId);
    sb.append(", ARGId: ");
    sb.append(currentID);
    sb.append(", toMerge: ");
    sb.append(toMergeID);
    if (parentslist != null) {
      sb.append(", myParentslist: ");
      sb.append(stateIdsOf(parentslist));
    }
    if (childrenlist != null) {
      sb.append(", myChildrenlist: ");
      sb.append(stateIdsOf(childrenlist));
    }
    sb.append(wrappedelement);
    return sb.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  private Iterable<Integer> stateIdsOf(Iterable<ARGState> elements) {
    return from(elements).transform(ARGState::getStateId);
  }

  public Collection<ARGState> getChildrenOfToMerge() {
    return Collections.unmodifiableCollection(childrenlist);
  }

  public ARGState getARGState() {
    return element;
  }

  public int getStateId() {
    return currentID;
  }

  public int getMyStateId() {
    return stateId;
  }

  public ImmutableList<ARGState> getParentslist() {
    return parentslist;
  }
}
