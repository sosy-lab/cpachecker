// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.graph.Traverser;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.UniqueIdGenerator;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.EnvironmentActionEdge;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperState;
import org.sosy_lab.cpachecker.core.defaults.EmptyEdge;
import org.sosy_lab.cpachecker.core.defaults.WrapperCFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithDummyLocation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithLocations;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractEdge;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractEdge.FormulaDescription;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateProjectedState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;

public class ARGState extends AbstractSingleWrapperState
    implements Comparable<ARGState>, Graphable, Splitable, AbstractStateWithEdge {

  private static final long serialVersionUID = 2608287648397165040L;

  // We use a List here although we would like to have a Set
  // because ArrayList is much more memory efficient than e.g. LinkedHashSet.
  // Also these collections are small and so a slow contains() method won't hurt.
  // To enforce set semantics, do not add elements except through addparent()!
  private final Collection<ARGState> children = new ArrayList<>(1);
  private final Collection<ARGState> parents = new ArrayList<>(1);

  private ARGState mCoveredBy = null;
  private Collection<ARGState> projectedFrom = null;
  private Collection<ARGState> projectedTo = null;
  private Pair<ARGState, ARGState> appliedFrom = null;
  private Collection<ARGState> appliedTo = null;
  private Set<ARGState> mCoveredByThis = null; // lazy initialization because rarely needed

  // boolean which keeps track of which elements have already had their successors computed
  private boolean wasExpanded = false;
  private boolean mayCover = true;
  private boolean destroyed = false;
  private boolean hasCoveredParent = false;

  private ARGState mergedWith = null;

  private final int stateId;

  // If this is a target state, we may store additional information here.
  private transient CounterexampleInfo counterexample;

  private static UniqueIdGenerator idGenerator = new UniqueIdGenerator();

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

  public Collection<ARGState> getSuccessors() {
    List<ARGState> result = new ArrayList<>(getChildren());
    if (projectedTo != null) {
      result.addAll(projectedTo);
    }
    if (appliedTo != null) {
      result.addAll(appliedTo);
    }
    return result;
  }

  /**
   * Returns the edge from current state to child or Null, if there is no edge.
   * Both forward and backward analysis must be considered!
   *
   * If there are several edges between the states,
   * only one of them will be returned, non-deterministically.
   */
  @Nullable
  public CFAEdge getEdgeToChild(ARGState pChild) {
    // Disabled the following check:
    //    checkArgument(children.contains(pChild));
    // In some cases we want to iterate all traces that have been explored
    // by an analysis. Possible traces might be 'interrupted' by covered states.
    // Covered states do not have children, so we expect the return value null in this case.

    final AbstractStateWithLocations currentLocs =
        extractStateByType(this, AbstractStateWithLocations.class);
    final AbstractStateWithLocations childLocs =
        extractStateByType(pChild, AbstractStateWithLocations.class);

    if (currentLocs != null && childLocs != null) {
      // first try to get a normal edge
      // consider only the actual analysis direction
      Collection<CFAEdge> ingoingEdgesOfChild = Sets.newHashSet(childLocs.getIngoingEdges());
      for (CFAEdge edge : currentLocs.getOutgoingEdges()) {
        if (ingoingEdgesOfChild.contains(edge)) { return edge; }
      }

      // then try to get a special edge, just to have some edge.
      for (CFANode currentLoc : currentLocs.getLocationNodes()) {
        for (CFANode childLoc : childLocs.getLocationNodes()) {
          if (currentLoc.getLeavingSummaryEdge() != null
              && currentLoc.getLeavingSummaryEdge().getSuccessor().equals(childLoc)) { // Forwards
            return currentLoc.getLeavingSummaryEdge();
          }
        }
      }
    }

    // check for dummy location
    AbstractStateWithDummyLocation stateWithDummyLocation =
        AbstractStates.extractStateByType(pChild, AbstractStateWithDummyLocation.class);
    if (currentLocs != null
        && stateWithDummyLocation != null
        && stateWithDummyLocation.isDummyLocation()) {
      for (CFAEdge enteringEdge : stateWithDummyLocation.getEnteringEdges()) {
        for (CFANode currentLocation : currentLocs.getLocationNodes()) {
          if (enteringEdge.getPredecessor().equals(currentLocation)) {
            return enteringEdge;
          }
        }
      }
    }

    // there is no edge
    return null;
  }

  /**
   * Returns the edges from the current state to the child state, or an empty list
   * if there is no path between both states.
   */
  public List<CFAEdge> getEdgesToChild(ARGState pChild) {
    CFAEdge singleEdge = getEdgeToChild(pChild);

    // no direct connection, this is only possible for ARG holes during dynamic
    // multiedges, it is guaranteed that there is exactly one path and no other
    // leaving edges from the parent to the child
    if (singleEdge == null) {
      ImmutableList.Builder<CFAEdge> allEdges = ImmutableList.builder();
      CFANode currentLoc = AbstractStates.extractLocation(this);
      CFANode childLoc = AbstractStates.extractLocation(pChild);

      if (currentLoc != null && childLoc != null) {
        while (!currentLoc.equals(childLoc)) {
          // we didn't find a proper connection to the child so we return an empty list
          if (currentLoc.getNumLeavingEdges() != 1) {
            if (CFAUtils.allSuccessorsOf(currentLoc).contains(childLoc)) {
              allEdges.add(currentLoc.getEdgeTo(childLoc));
              return allEdges.build();
            }
            return ImmutableList.of();
          }

          final CFAEdge leavingEdge = currentLoc.getLeavingEdge(0);
          allEdges.add(leavingEdge);
          currentLoc = leavingEdge.getSuccessor();
        }

        // Can not check transition in environment by apply, because it can be removed due to
        // coverage

        if (this.appliedTo != null && this.appliedTo.contains(pChild)) {
          return Collections.singletonList(
              new BlankEdge(
                  "application",
                  FileLocation.DUMMY,
                  currentLoc,
                  childLoc,
                  "application"));
        } else if (this.appliedFrom != null) {
          // Origin edge may be not enough as it may be merged with something else
          ARGState projection = this.appliedFrom.getSecond();
          return predicateWay(projection, currentLoc, childLoc);
        }
      }
      return allEdges.build();
    } else {
      return Collections.singletonList(singleEdge);
    }
  }

  private List<CFAEdge> predicateWay(ARGState projection, CFANode currentLoc, CFANode childLoc) {
    assert projection.getProjectedFrom() != null;
    PredicateProjectedState predicateState =
        AbstractStates.extractStateByType(projection, PredicateProjectedState.class);

    if (predicateState != null) {
      AbstractEdge edge = predicateState.getAbstractEdge();
      List<CFAEdge> result = new ArrayList<>();

      if (edge instanceof PredicateAbstractEdge) {
        Collection<CAssignment> statements =
            transformedImmutableListCopy(
                ((PredicateAbstractEdge) edge).getFormulas(),
                FormulaDescription::getAssignment);
        for (CAssignment s : statements) {
          result.add(
              new EnvironmentActionEdge(
                  s.toASTString(),
                  s,
                  s.getFileLocation(),
                  currentLoc,
                  childLoc));
        }

      } else if (edge == EmptyEdge.getInstance()) {
        result.add(
            new BlankEdge(
                "empty predicate edge",
                FileLocation.DUMMY,
                currentLoc,
                childLoc,
                "empty predicate edge"));
      }
      return result;
    } else {
      return generalWay(projection, currentLoc, childLoc);
    }
  }

  @SuppressWarnings("unused")
  private final List<CFAEdge>
      generalWay(ARGState projection, CFANode currentLoc, CFANode childLoc) {

    assert projection.getProjectedFrom() != null;
    Collection<ARGState> edgeParts = projection.getProjectedFrom();
    List<CFAEdge> result = new ArrayList<>();

    for (ARGState edgePart : edgeParts) {
      AbstractEdge edge =
          ((AbstractStateWithEdge) AbstractStates
              .extractStateByType(edgePart, AbstractStateWithLocations.class)).getAbstractEdge();
      assert edge instanceof WrapperCFAEdge;
      CFAEdge realEdge = ((WrapperCFAEdge) edge).getCFAEdge();
      // Need to replace locations for correct path
      CFAEdge newEdge;
      if (realEdge instanceof CStatementEdge) {
        CStatementEdge oldStatement = (CStatementEdge) realEdge;
        newEdge =
            new EnvironmentActionEdge(
                realEdge.getRawStatement(),
                oldStatement.getStatement(),
                realEdge.getFileLocation(),
                currentLoc,
                childLoc);
      } else if (realEdge instanceof CFunctionReturnEdge) {
        CFunctionReturnEdge oldReturn = (CFunctionReturnEdge) realEdge;
        newEdge =
            new EnvironmentActionEdge(
                realEdge.getRawStatement(),
                oldReturn.getSummaryEdge().getExpression(),
                realEdge.getFileLocation(),
                currentLoc,
                childLoc);
      } else if (realEdge instanceof CAssumeEdge) {
        newEdge = realEdge;
      } else {
        throw new UnsupportedOperationException("Edge is not supported: " + realEdge.getClass());
      }
      result.add(newEdge);
    }
    return result;
  }

  /**
   * Return a duplicate-free iterable over all states that are transitively reachable from this
   * state via {@link #getChildren()}. The current state is included. The iterable always reflects
   * the current state of the ARG. The behavior is undefined if the ARG is changed during iteration
   * over it.
   */
  public FluentIterable<ARGState> getSubgraph() {
    assert !destroyed : "Don't use destroyed ARGState " + this;
    // return Sets.newHashSet(Traverser.forGraph(ARGState::getSuccessors).breadthFirst(this));
    return from(Traverser.forGraph(ARGState::getSuccessors).breadthFirst(this));
  }

  // coverage

  public void setCovered(@NonNull ARGState pCoveredBy) {
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
      return ImmutableSet.of();
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

  void addProjection(ARGState pProjection) {
    assert !destroyed : "Don't use destroyed ARGState " + this;
    assert !pProjection.destroyed : "Don't use destroyed ARGState " + this;

    if (projectedTo == null) {
      projectedTo = new ArrayList<>();
    }
    projectedTo.add(pProjection);

    if (pProjection.projectedFrom == null) {
      pProjection.projectedFrom = new ArrayList<>();
    }

    pProjection.projectedFrom.add(this);
  }

  public Collection<ARGState> getProjectedFrom() {
    return projectedFrom;
  }

  public Collection<ARGState> getProjectedTo() {
    return projectedTo == null ? ImmutableList.of() : projectedTo;
  }

  void setAsAppliedFrom(ARGState pLeftState, ARGState pRightState) {
    assert !destroyed : "Don't use destroyed ARGState " + this;
    assert appliedFrom == null : "Second applied of element " + this;

    appliedFrom = Pair.of(pLeftState, pRightState);
    pLeftState.setAppliedTo(this);
    pRightState.setAppliedTo(this);
  }

  public Pair<ARGState, ARGState> getAppliedFrom() {
    return appliedFrom;
  }

  private void setAppliedTo(ARGState pState) {
    assert !destroyed : "Don't use destroyed ARGState " + this;

    if (appliedTo == null) {
      appliedTo = new ArrayList<>();
    }
    appliedTo.add(pState);
  }

  public Collection<ARGState> getAppliedTo() {
    return appliedTo == null ? ImmutableList.of() : appliedTo;
  }

  // was-expanded marker so we can identify open leafs

  public boolean wasExpanded() {
    return wasExpanded;
  }

  public void markExpanded() {
    wasExpanded = true;
  }

  void deleteChild(ARGState child) {
    assert (children.contains(child));
    assert (child.parents.contains(this));
    children.remove(child);
    child.parents.remove(this);
  }

  // counterexample

  /**
   * Store additional information about the counterexample that leads to this target state.
   */
  public void addCounterexampleInformation(CounterexampleInfo pCounterexample) {
    checkState(counterexample == null);
    checkArgument(isTarget());
    checkArgument(!pCounterexample.isSpurious());
    // With BAM, the targetState and the last state of the path
    // may actually be not identical.
    checkArgument(pCounterexample.getTargetState().isTarget());
    counterexample = pCounterexample;
  }

  public void replaceCounterexampleInformation(CounterexampleInfo pCounterexample) {
    checkArgument(isTarget());
    checkArgument(!pCounterexample.isSpurious());
    checkArgument(pCounterexample.getTargetState().isTarget());
    counterexample = pCounterexample;
  }

  /**
   * Get additional information about the counterexample that is associated with this target state,
   * if present.
   */
  public Optional<CounterexampleInfo> getCounterexampleInformation() {
    checkState(isTarget());
    return Optional.ofNullable(counterexample);
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
    return from(elements).transform(ARGState::getStateId);
  }

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

    if (appliedTo != null) {
      for (ARGState applied : appliedTo) {
        Pair<ARGState, ARGState> oldAppliedFrom = applied.appliedFrom;
        if (oldAppliedFrom.getFirst() == this) {
          applied.appliedFrom = Pair.of(null, oldAppliedFrom.getSecond());
        } else if (oldAppliedFrom.getSecond() == this) {
          applied.appliedFrom = Pair.of(oldAppliedFrom.getFirst(), null);
        } else {
          throw new UnsupportedOperationException("Missed state: " + applied);
        }
      }
      appliedTo = null;
    }

    if (appliedFrom != null) {
      ARGState first = appliedFrom.getFirst();
      ARGState second = appliedFrom.getSecond();
      if (first != null) {
        first.appliedTo.remove(this);
      }
      if (second != null) {
        second.appliedTo.remove(this);
      }
      appliedFrom = null;
    }

    if (projectedFrom != null && !projectedFrom.isEmpty()) {
      for (ARGState projection : projectedFrom) {
        projection.projectedTo.remove(this);
      }
      projectedFrom = null;
    }

    if (projectedTo != null && !projectedTo.isEmpty()) {
      for (ARGState projection : projectedTo) {
        projection.projectedFrom.remove(this);
      }
      projectedTo = null;
    }
  }

  /**
   * This method does basically the same as removeFromARG for this element, but
   * before destroying it, it will copy all relationships to other elements to
   * a new state. I.e., the replacement element will receive all parents and
   * children of this element, and it will also cover all elements which are
   * currently covered by this element.
   *
   * @param replacement the replacement for this state
   */
  public void replaceInARGWith(ARGState replacement) {
    assert !destroyed : "Don't use destroyed ARGState " + this;
    assert !replacement.destroyed : "Don't use destroyed ARGState " + replacement;
    assert !isCovered() : "Not implemented: Replacement of covered element " + this;
    assert !replacement.isCovered() : "Cannot replace with covered element " + replacement;
    assert !this.equals(replacement) : "Don't replace ARGState " + this + " with itself";

    // copy children
    for (ARGState child : children) {
      assert child.parents.contains(this) : "Inconsistent ARG at " + this;
      child.parents.remove(this);
      child.addParent(replacement);
    }
    children.clear();

    for (ARGState parent : parents) {
      assert parent.children.contains(this) : "Inconsistent ARG at " + this;
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
        assert this.equals(covered.mCoveredBy) : "Inconsistent coverage relation at " + this;
        covered.mCoveredBy = replacement;
        replacement.mCoveredByThis.add(covered);
      }

      mCoveredByThis.clear();
      mCoveredByThis = null;
    }

    if (appliedTo != null) {
      replacement.appliedTo = new ArrayList<>();
      for (ARGState applied : appliedTo) {
        Pair<ARGState, ARGState> oldAppliedFrom = applied.appliedFrom;
        if (oldAppliedFrom.getFirst() == this) {
          applied.appliedFrom = Pair.of(replacement, oldAppliedFrom.getSecond());
        } else if (oldAppliedFrom.getSecond() == this) {
          applied.appliedFrom = Pair.of(oldAppliedFrom.getFirst(), replacement);
        } else {
          throw new UnsupportedOperationException("Missed state: " + applied);
        }
        replacement.appliedTo.add(applied);
      }
      appliedTo = null;
    }

    if (appliedFrom != null) {
      ARGState first = appliedFrom.getFirst();
      ARGState second = appliedFrom.getSecond();
      first.appliedTo.remove(this);
      first.appliedTo.add(replacement);
      second.appliedTo.remove(this);
      second.appliedTo.add(replacement);
      replacement.appliedFrom = Pair.of(first, second);
      appliedFrom = null;
    }

    if (projectedFrom != null) {
      for (ARGState projection : this.projectedFrom) {
        assert !projection.destroyed;
        projection.projectedTo.remove(this);
        projection.projectedTo.add(replacement);
      }
      replacement.projectedFrom = this.projectedFrom;
      this.projectedFrom = null;
    }

    if (projectedTo != null && !projectedTo.isEmpty()) {
      for (ARGState projection : projectedTo) {
        assert !projection.destroyed;
        projection.projectedFrom.remove(this);
        projection.projectedFrom.add(replacement);
      }
      replacement.projectedTo = this.projectedTo;
      this.projectedTo = null;
    }

    destroyed = true;
  }

  @Override
  public ARGState forkWithReplacements(Collection<AbstractState> pReplacementStates){
    AbstractState wrappedState = this.getWrappedState();
    AbstractState newWrappedState = null;
    if (wrappedState instanceof Splitable) {
        newWrappedState = ((Splitable)wrappedState).forkWithReplacements(pReplacementStates);
    } else {
        newWrappedState = wrappedState;
    }

    ARGState newState = new ARGState(newWrappedState,null);
    newState.makeTwinOf(this);

    return newState;
  }

  public void makeTwinOf(ARGState pTemplateState) {

    checkState(this.stateId != pTemplateState.stateId);
    checkState(!pTemplateState.destroyed);
    checkState(pTemplateState.counterexample == null);

    this.wasExpanded = pTemplateState.wasExpanded;
    this.mayCover = pTemplateState.mayCover;
    this.hasCoveredParent = pTemplateState.hasCoveredParent;

  }

  public void removeParent(ARGState pOtherParent) {
    checkNotNull(pOtherParent);
    assert !destroyed : "Don't use destroyed ARGState " + this;

    // Manually enforce set semantics.
    if (parents.contains(pOtherParent)) {
      assert pOtherParent.children.contains(this);
      parents.remove(pOtherParent);
      pOtherParent.children.remove(this);
    } else {
      assert !pOtherParent.children.contains(this) : "Problem detected!";
    }
  }

  public static void clearIdGenerator() {
    idGenerator = new UniqueIdGenerator();
  }

  @Override
  public AbstractEdge getAbstractEdge() {
    return ((AbstractStateWithEdge) getWrappedState()).getAbstractEdge();
  }

  @Override
  public boolean hasEmptyEffect() {
    return ((AbstractStateWithEdge) getWrappedState()).hasEmptyEffect();
  }

  @Override
  public boolean isProjection() {
    AbstractState wrapped = getWrappedState();
    if (wrapped instanceof AbstractStateWithEdge) {
      return projectedFrom != null;
    } else {
      return false;
    }
  }
}
