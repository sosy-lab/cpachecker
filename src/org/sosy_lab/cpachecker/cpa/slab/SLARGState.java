/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.slab;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.NamedProperty;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocations;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.Splitable;

/** ARGState for Symbolic Locations */
public class SLARGState extends ARGState
    implements AbstractState, Targetable, AbstractStateWithLocations {

  private static final long serialVersionUID = -1008999926741613988L;
  private Map<SLARGState, EdgeSet> parentsToEdgeSets;
  private Map<SLARGState, EdgeSet> childrenToEdgeSets;

  private boolean isInit;
  private boolean isError;
  private boolean isAbstractionState = true;

  public SLARGState(
      SLARGState parent,
      EdgeSet edgeFromParent,
      boolean isInit,
      boolean isError,
      AbstractState wrappedState) {
    super(wrappedState, null); // second parameter is null so that we can add the parent here *
    this.parentsToEdgeSets = new HashMap<>();
    this.childrenToEdgeSets = new HashMap<>();
    if (parent != null) { // *
      assert edgeFromParent != null;
      addParent(parent, edgeFromParent);
    }
    this.isInit = isInit;
    this.isError = isError;
  }

  /**
   * This copy constructor does not make a deep copy of the wrapped state! It will also have no
   * parents or children.
   * @param pState the state to be copied
   */
  public SLARGState(SLARGState pState) {
    this(null, null, pState.isInit, pState.isError, pState.getWrappedState());
  }

  /**
   * In the (SLAB)ARG, replace pState2 by "this" and copy all parents from pState1. This is designed
   * to be used in a merge where "this" corresponds to e_{new}, pState1 corresponds to e' and
   * pState2 corresponds to e".
   */
  public void useAsReplacement(SLARGState pState1, SLARGState pState2) {
    // pState2 gets replaced by this:
    for (ARGState argParent : pState2.getParents()) {
      EdgeSet edgeToCopy = pState2.parentsToEdgeSets.get(argParent);
      assert edgeToCopy != null;
      EdgeSet newEdge = new EdgeSet(edgeToCopy);
      if (argParent != pState2) {
        this.addParent((SLARGState) argParent, newEdge);
      } else {
        this.addParent(this, newEdge);
      }
    }
    for (ARGState argChild : pState2.getChildren()) {
      EdgeSet edgeToCopy = pState2.getEdgeSetToChild(argChild);
      assert edgeToCopy != null;
      EdgeSet newEdge = new EdgeSet(edgeToCopy);
      if (argChild != pState2) {
        /* replaced this line by the line below. should be fine. TODO: remove this note if it is fine
        this.addChild((SLARGState) argChild, newEdge);*/
        ((SLARGState) argChild).addParent(this, newEdge);
      } else {
        // we already added this in the parents-loop above!
      }
    }
    pState2.removeFromARG();

    // all parents from pState1 are just cloned:
    for (ARGState argParent : pState1.getParents()) {
      SLARGState parent = (SLARGState) argParent;
      addParent(parent, new EdgeSet(pState1.parentsToEdgeSets.get(parent)));
    }
  }

  @Override
  public CFAEdge getEdgeToChild(ARGState argChild) {
    EdgeSet edgeSet = getEdgeSetToChild(argChild);
    return edgeSet != null ? getEdgeSetToChild(argChild).choose() : null;
  }

  @Override
  public List<CFAEdge> getEdgesToChild(ARGState argChild) {
    CFAEdge edge = getEdgeToChild(argChild);
    if (edge!=null) {
      return ImmutableList.of(edge);
    } else {
      return ImmutableList.of();
    }
  }

  public @Nullable EdgeSet getEdgeSetToChild(ARGState child) {
    return childrenToEdgeSets.get(child);
  }

  @Override
  public boolean isTarget() {
    return isError;
  }

  public boolean isInit() {
    return isInit;
  }

  @Override
  public @NonNull Set<Property> getViolatedProperties() throws IllegalStateException {
    return NamedProperty.singleton("Error state reached");
  }

  /*@Override
  public void markExpanded() {
    expanded = true;
  }

  @Override
  public boolean wasExpanded() {
    return expanded;
  }*/

  public boolean isAbstractionState() {
    return isAbstractionState;
  }

  @Override
  public String toString() {
    String result = super.toString();
    return result.replaceAll("ARG State", "SLARG State");
  }

  public void addParent(SLARGState pParent, EdgeSet pEdgeSet) {
    super.addParent(pParent);
    assert pEdgeSet != null;
    assert !this.parentsToEdgeSets.containsKey(pParent);
    assert !this.parentsToEdgeSets.containsValue(pEdgeSet);
    this.parentsToEdgeSets.put(pParent, pEdgeSet);
    pParent.childrenToEdgeSets.put(this, pEdgeSet);
  }

  @Override
  public void addParent(ARGState s) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeParent(ARGState argParent) {
    SLARGState parent = (SLARGState) argParent;

    super.removeParent(parent);

    assert this.parentsToEdgeSets.containsKey(parent);
    this.parentsToEdgeSets.remove(parent);
    parent.childrenToEdgeSets.remove(this);
  }

  @Override
  public void removeFromARG() {
    assert !this.isDestroyed() : "Don't use destroyed ARGState " + this;
    for (ARGState argParent : new ArrayList<>(getParents())) { // prevent concurrent modification
      if (argParent != this) {
        SLARGState parent = (SLARGState) argParent;
        removeParent(parent);
      }
    }
    for (ARGState argChild : new ArrayList<>(getChildren())) { // prevent concurrent modification
      if (argChild != this) {
        SLARGState child = (SLARGState) argChild;
        child.removeParent(this);
      }
    }
    super.removeFromARG();
  }

  @Override
  public ARGState forkWithReplacements(Collection<AbstractState> pReplacementStates) {
    AbstractState wrappedState = this.getWrappedState();
    AbstractState newWrappedState = null;
    if (wrappedState instanceof Splitable) {
      newWrappedState = ((Splitable) wrappedState).forkWithReplacements(pReplacementStates);
    } else {
      newWrappedState = wrappedState;
    }

    ARGState newState = new SLARGState(null, null, this.isInit(), this.isTarget(), newWrappedState);
    newState.makeTwinOf(this);

    return newState;
  }

  /**
   * This method does basically the same as removeFromARG for this element, but before destroying
   * it, it will copy all relationships to other elements to a new state. I.e., the replacement
   * element will receive all parents and children of this element, and it will also cover all
   * elements which are currently covered by this element.
   *
   * @param replacement the replacement for this state
   */
  @Override
  public void replaceInARGWith(ARGState replacement) {
    assert !this.isDestroyed() : "Don't use destroyed ARGState " + this;
    assert !replacement.isDestroyed() : "Don't use destroyed ARGState " + replacement;
    assert !isCovered() : "Not implemented: Replacement of covered element " + this;
    assert !replacement.isCovered() : "Cannot replace with covered element " + replacement;
    assert !(this == replacement) : "Don't replace ARGState " + this + " with itself";

    // copy children
    for (ARGState child : new ArrayList<>(getChildren())) {
      assert (child.getParents().contains(this)) : "Inconsistent ARG at " + this;
      ((SLARGState) child)
          .addParent((SLARGState) replacement, new EdgeSet(this.getEdgeSetToChild(child)));
      child.removeParent(this);
    }

    for (ARGState parent : new ArrayList<>(getParents())) {
      assert (parent.getChildren().contains(this)) : "Inconsistent ARG at " + this;
      ((SLARGState) replacement)
          .addParent(
              (SLARGState) parent, new EdgeSet(((SLARGState) parent).getEdgeSetToChild(this)));
      this.removeParent(parent);
    }

    super.replaceInARGWith(replacement);
  }

  @Override
  public Iterable<CFANode> getLocationNodes() {
    // We need to implement this method of the AbstractStateWithLocations interface, otherwise the
    // counterexample check will not work correctly (the advance()-method in DefaultFullPathIterator
    // expects there to be some location information for determining the right CFAEdge)
    ImmutableSet.Builder<CFANode> locations = ImmutableSet.builder();
    for (Entry<SLARGState, EdgeSet> entry : parentsToEdgeSets.entrySet()) {
      for (Iterator<CFAEdge> it = entry.getValue().iterator(); it.hasNext(); ) {
        locations.add(it.next().getSuccessor());
      }
    }
    for (Entry<SLARGState, EdgeSet> entry : childrenToEdgeSets.entrySet()) {
      for (Iterator<CFAEdge> it = entry.getValue().iterator(); it.hasNext(); ) {
        locations.add(it.next().getPredecessor());
      }
    }
    return locations.build();
  }

  @Override
  public Iterable<CFAEdge> getOutgoingEdges() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterable<CFAEdge> getIngoingEdges() {
    throw new UnsupportedOperationException();
  }
}
