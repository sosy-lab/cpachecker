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
package org.sosy_lab.cpachecker.cpa.arg;

import static com.google.common.base.Preconditions.*;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.util.UniqueIdGenerator;

import com.google.common.base.Function;
import com.google.common.collect.Sets;

public class ARGState extends AbstractSingleWrapperState implements Comparable<ARGState>, Graphable {

  private static final long serialVersionUID = 2608287648397165040L;

  // We use a List here although we would like to have a Set
  // because ArrayList is much more memory efficient than e.g. LinkedHashSet.
  // Also these collections are small and so a slow contains() method won't hurt.
  // To enforce set semantics, do not add elements except through addparent()!
  private final Collection<ARGState> children = new ArrayList<>(1);
  private final Collection<ARGState> parents = new ArrayList<>(1);

  private ARGState mCoveredBy = null;
  private Set<ARGState> mCoveredByThis = null; // lazy initialization because rarely needed

  // boolean which keeps track of which elements have already had their successors computed
  private boolean wasExpanded = false;
  private boolean mayCover = true;
  private boolean destroyed = false;
  private boolean hasCoveredParent = false;

  private ARGState mergedWith = null;

  private final int stateId;

  private static final UniqueIdGenerator idGenerator = new UniqueIdGenerator();

  public ARGState(@Nullable AbstractState pWrappedState, @Nullable ARGState pParentElement) {
    super(pWrappedState);
    stateId = idGenerator.getFreshId();
    if (pParentElement != null) {
      addParent(pParentElement);
    }
  }

  // parent & child relations

  /**
   * Get the parent elements of this state.
   * @return A unmodifiable collection of ARGStates without duplicates.
   */
  public Collection<ARGState> getParents() {
    return Collections.unmodifiableCollection(parents);
  }

  public void addParent(ARGState pOtherParent) {
    checkNotNull(pOtherParent);
    assert !destroyed : "Don't use destroyed ARGState " + this;

    // Manually enforce set semantics.
    if (!parents.contains(pOtherParent)) {
      assert !pOtherParent.children.contains(this);
      parents.add(pOtherParent);
      pOtherParent.children.add(this);
    } else {
      assert pOtherParent.children.contains(this);
    }
  }

  /**
   * Get the child elements of this state.
   * @return An unmodifiable collection of ARGStates without duplicates.
   */
  public Collection<ARGState> getChildren() {
    assert !destroyed : "Don't use destroyed ARGState " + this;
    return Collections.unmodifiableCollection(children);
  }

  /**
   * Returns the edge from current state to child or Null, if there is no edge.
   * Both forward and backward analysis must be considered!
   */
  @Nullable
  public CFAEdge getEdgeToChild(ARGState pChild) {
    // Disabled the following check:
    //    checkArgument(children.contains(pChild));
    // In some cases we want to iterate all traces that have been explored
    // by an analysis. Possible traces might be 'interrupted' by covered states.
    // Covered states do not have children, so we expect the return value null in this case.

    CFANode currentLoc = extractLocation(this);
    CFANode childLoc = extractLocation(pChild);

    if (currentLoc.hasEdgeTo(childLoc)) { // Forwards
      return currentLoc.getEdgeTo(childLoc);

    } else if (currentLoc.getLeavingSummaryEdge() != null
            && currentLoc.getLeavingSummaryEdge().getSuccessor().equals(childLoc)) { // Forwards
      return currentLoc.getLeavingSummaryEdge();

    } else if (childLoc.hasEdgeTo(currentLoc)) { // Backwards
      return childLoc.getEdgeTo(currentLoc);

    } else if (currentLoc.getEnteringSummaryEdge() != null
          && currentLoc.getEnteringSummaryEdge().getSuccessor().equals(childLoc)) { // Backwards
      return currentLoc.getEnteringSummaryEdge();

    } else {
      return null;
    }
  }

  public Set<ARGState> getSubgraph() {
    assert !destroyed : "Don't use destroyed ARGState " + this;
    Set<ARGState> result = new HashSet<>();
    Deque<ARGState> workList = new ArrayDeque<>();

    workList.add(this);

    while (!workList.isEmpty()) {
      ARGState currentElement = workList.removeFirst();
      if (result.add(currentElement)) {
        // currentElement was not in result
        workList.addAll(currentElement.children);
      }
    }
    return result;
  }

  // coverage

  public void setCovered(@Nonnull ARGState pCoveredBy) {
    checkState(!isCovered(), "Cannot cover already covered element %s", this);
    checkNotNull(pCoveredBy);
    checkArgument(pCoveredBy.mayCover, "Trying to cover with non-covering element %s", pCoveredBy);

    mCoveredBy = pCoveredBy;
    if (pCoveredBy.mCoveredByThis == null) {
      // lazy initialization because rarely needed
      pCoveredBy.mCoveredByThis = new LinkedHashSet<>(2);
    }
    pCoveredBy.mCoveredByThis.add(this);
  }

  public void uncover() {
    assert isCovered();
    assert mCoveredBy.mCoveredByThis.contains(this);

    mCoveredBy.mCoveredByThis.remove(this);
    mCoveredBy = null;
  }

  public boolean isCovered() {
    assert !destroyed : "Don't use destroyed ARGState " + this;
    return mCoveredBy != null;
  }

  public ARGState getCoveringState() {
    checkState(isCovered());
    return mCoveredBy;
  }

  public Set<ARGState> getCoveredByThis() {
    assert !destroyed : "Don't use destroyed ARGState " + this;
    if (mCoveredByThis == null) {
      return Collections.emptySet();
    } else {
      return Collections.unmodifiableSet(mCoveredByThis);
    }
  }

  public boolean mayCover() {
    return mayCover && !hasCoveredParent && !isCovered();
  }

  public void setNotCovering() {
    assert !destroyed : "Don't use destroyed ARGState " + this;
    mayCover = false;
  }

  void setHasCoveredParent(boolean pHasCoveredParent) {
    assert !destroyed : "Don't use destroyed ARGState " + this;
    hasCoveredParent = pHasCoveredParent;
  }

  // merged-with marker so that stop can return true for merged elements

  void setMergedWith(ARGState pMergedWith) {
    assert !destroyed : "Don't use destroyed ARGState " + this;
    assert mergedWith == null : "Second merging of element " + this;

    mergedWith = pMergedWith;
  }

  public ARGState getMergedWith() {
    return mergedWith;
  }

  // was-expanded marker so we can identify open leafs

  boolean wasExpanded() {
    return wasExpanded;
  }

  void markExpanded() {
    wasExpanded = true;
  }

  void deleteChild(ARGState child) {
    assert (children.contains(child));
    children.remove(child);
    child.parents.remove(this);
  }

  // small and less important stuff

  public int getStateId() {
    return stateId;
  }

  public boolean isDestroyed() {
    return destroyed;
  }

  /**
   * The ordering of this class is the chronological creation order.
   */
  @Override
  public final int compareTo(ARGState pO) {
    return Integer.compare(this.stateId, pO.stateId);
  }

  @Override
  public final boolean equals(Object pObj) {
    // Object.equals() is consistent with our compareTo()
    // because stateId is a unique identifier.
    return super.equals(pObj);
  }

  @Override
  public final int hashCode() {
    // Object.hashCode() is consistent with our compareTo()
    // because stateId is a unique identifier.
    return super.hashCode();
  }

  public boolean isOlderThan(ARGState other) {
    return (stateId < other.stateId);
  }

  @Override
  public boolean isTarget() {
    return !hasCoveredParent && !isCovered() && super.isTarget();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (destroyed) {
      sb.append("Destroyed ");
    }
    if (mCoveredBy != null) {
      sb.append("Covered ");
    }
    sb.append("ARG State (Id: ");
    sb.append(stateId);
    if (!destroyed) {
      sb.append(", Parents: ");
      sb.append(stateIdsOf(parents));
      sb.append(", Children: ");
      sb.append(stateIdsOf(children));

      if (mCoveredBy != null) {
        sb.append(", Covered by: ");
        sb.append(mCoveredBy.stateId);
      } else {
        sb.append(", Covering: ");
        sb.append(stateIdsOf(getCoveredByThis()));
      }
    }
    sb.append(") ");
    sb.append(getWrappedState());
    return sb.toString();
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
    if (getWrappedState() instanceof Graphable) {
      return ((Graphable)getWrappedState()).shouldBeHighlighted();
    }
    return false;
  }

  private Iterable<Integer> stateIdsOf(Iterable<ARGState> elements) {
    return from(elements).transform(TO_STATE_ID);
  }

  private static final Function<ARGState, Integer> TO_STATE_ID = new Function<ARGState, Integer>() {
    @Override
    public Integer apply(ARGState pInput) {
      return pInput.stateId;
    }
  };

  // removal from ARG

  /**
   * This method removes this element from the ARG and  also removes the element
   * from the covered set of the other element covering this element, if it is
   * covered.
   *
   * This means, if its children do not have any other parents, they will be not
   * reachable any more, i.e. they do not belong to the ARG any more. But those
   * elements will not be removed from the covered set.
   */
  public void removeFromARG() {
    assert !destroyed : "Don't use destroyed ARGState " + this;

    detachFromARG();

    clearCoverageRelation();

    destroyed = true;
  }

  /**
   * This method removes the element from the covered set of the other
   * element covering this element, if it is covered.
   */
  private void clearCoverageRelation() {
    if (isCovered()) {
      assert mCoveredBy.mCoveredByThis.contains(this);

      mCoveredBy.mCoveredByThis.remove(this);
      mCoveredBy = null;
    }

    if (mCoveredByThis != null) {
      for (ARGState covered : mCoveredByThis) {
        covered.mCoveredBy = null;
      }
      mCoveredByThis.clear();
      mCoveredByThis = null;
    }
  }

  /**
   * This method removes this element from the ARG by removing it from its
   * parents' children list and from its children's parents list.
   */
  void detachFromARG() {
    assert !destroyed : "Don't use destroyed ARGState " + this;

    // clear children
    for (ARGState child : children) {
      assert (child.parents.contains(this));
      child.parents.remove(this);
    }
    children.clear();

    // clear parents
    for (ARGState parent : parents) {
      assert (parent.children.contains(this));
      parent.children.remove(this);
    }
    parents.clear();
  }

  /**
   * This method does basically the same as removeFromARG for this element, but
   * before destroying it, it will copy all relationships to other elements to
   * a new state. I.e., the replacement element will receive all parents and
   * children of this element, and it will also cover all elements which are
   * currently covered by this element.
   *
   * @param replacement
   */
  public void replaceInARGWith(ARGState replacement) {
    assert !destroyed : "Don't use destroyed ARGState " + this;
    assert !replacement.destroyed : "Don't use destroyed ARGState " + replacement;
    assert !isCovered() : "Not implemented: Replacement of covered element " + this;
    assert !replacement.isCovered() : "Cannot replace with covered element " + replacement;

    // copy children
    for (ARGState child : children) {
      assert (child.parents.contains(this)) : "Inconsistent ARG at " + this;
      child.parents.remove(this);
      child.addParent(replacement);
    }
    children.clear();

    for (ARGState parent : parents) {
      assert (parent.children.contains(this)) : "Inconsistent ARG at " + this;
      parent.children.remove(this);
      replacement.addParent(parent);
    }
    parents.clear();

    if (mCoveredByThis != null) {
      if (replacement.mCoveredByThis == null) {
        // lazy initialization because rarely needed
        replacement.mCoveredByThis = Sets.newHashSetWithExpectedSize(mCoveredByThis.size());
      }

      for (ARGState covered : mCoveredByThis) {
        assert covered.mCoveredBy == this : "Inconsistent coverage relation at " + this;
        covered.mCoveredBy = replacement;
        replacement.mCoveredByThis.add(covered);
      }

      mCoveredByThis.clear();
      mCoveredByThis = null;
    }

    destroyed = true;
  }
}
