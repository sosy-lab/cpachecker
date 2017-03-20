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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSetWrapper;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Precisions;

/**
 * This class is a modifiable live view of a reached set, which shows the ARG
 * relations between the elements, and enforces a correct ARG when the set is
 * modified through this wrapper.
 */
public class ARGReachedSet {

  private final int refinementNumber;
  private final ARGCPA cpa;

  private final ReachedSet mReached;
  private final UnmodifiableReachedSet mUnmodifiableReached;

  /**
   * Constructor for ARGReachedSet as a simple wrapper around ReachedSet.
   * If possible, do not use this constructor but the other one that takes
   * an ARGCPA instance as parameter.
   * This class notifies the ARGCPA of removed counterexamples if possible
   * to reduce memory usage.
   */
  public ARGReachedSet(ReachedSet pReached) {
    this(pReached, null);
  }

  public ARGReachedSet(ReachedSet pReached, ARGCPA pCpa) {
    this(pReached, pCpa, -1);
  }

  /**
   * This constructor may be used only during an refinement
   * which should be added to the refinement graph .dot file.
   */
  public ARGReachedSet(ReachedSet pReached, ARGCPA pCpa, int pRefinementNumber) {
    mReached = checkNotNull(pReached);
    mUnmodifiableReached = new UnmodifiableReachedSetWrapper(mReached);

    cpa = pCpa;
    refinementNumber = pRefinementNumber;
  }

  public UnmodifiableReachedSet asReachedSet() {
    return mUnmodifiableReached;
  }

  /**
   * Remove an element and all elements below it from the tree. Re-add all those
   * elements to the waitlist which have children which are either removed or were
   * covered by removed elements.
   *
   * @param e The root of the removed subtree, may not be the initial element.
   */
  public void removeSubtree(ARGState e) {
    Set<ARGState> toWaitlist = removeSubtree0(e);

    for (ARGState ae : toWaitlist) {
      mReached.reAddToWaitlist(ae);
    }
  }

  /**
   * Like {@link #removeSubtree(ARGState)}, but when re-adding elements to the
   * waitlist adapts precisions with respect to the supplied precision p (see
   * {@link #adaptPrecision(Precision, Precision, Predicate)}).
   * @param e The root of the removed subtree, may not be the initial element.
   * @param p The new precision.
   * @throws InterruptedException if operation is interrupted
   */
  public void removeSubtree(ARGState e, Precision p, Predicate<? super Precision> pPrecisionType)
      throws InterruptedException {
    for (ARGState ae : removeSubtree0(e)) {
      mReached.updatePrecision(ae, adaptPrecision(mReached.getPrecision(ae), p, pPrecisionType));
      mReached.reAddToWaitlist(ae);
    }
  }

  /**
   * Like {@link #removeSubtree(ARGState)}, but when re-adding elements to the
   * waitlist adapts precisions with respect to the supplied precision p (see
   * {@link #adaptPrecision(Precision, Precision, Predicate)}).
   * If multiple precisions are given,
   * adapt all matching sub-precisions of a WrappedPrecision.
   *
   * @param pState The root of the removed subtree, may not be the initial element.
   * @param pPrecisions The new precisions.
   * @param pPrecTypes the types of the precisions.
   * @throws InterruptedException if operation is interrupted
   */
  public void removeSubtree(
      ARGState pState, List<Precision> pPrecisions, List<Predicate<? super Precision>> pPrecTypes)
      throws InterruptedException {

    Preconditions.checkNotNull(pState);
    Preconditions.checkNotNull(pPrecisions);
    Preconditions.checkNotNull(pPrecTypes);

    Preconditions.checkArgument(pPrecisions.size() == pPrecTypes.size());

    Set<ARGState> toWaitlist = removeSubtree0(pState);

    for (ARGState waitingState : toWaitlist) {
      Precision waitingStatePrec = mReached.getPrecision(waitingState);
      Preconditions.checkState(waitingStatePrec != null);

      for (int i = 0; i < pPrecisions.size(); i++) {
        Precision adaptedPrec = adaptPrecision(waitingStatePrec, pPrecisions.get(i), pPrecTypes.get(i));

        // adaptedPrec == null, if the precision component was not changed
        if (adaptedPrec != null ) {
          waitingStatePrec = adaptedPrec;
        }
        Preconditions.checkState(waitingStatePrec != null);
      }

      mReached.updatePrecision(waitingState, waitingStatePrec);
      mReached.reAddToWaitlist(waitingState);
    }
  }

  /**
   * Safely remove a port of the ARG which has been proved as completely
   * unreachable. This method takes care of the coverage relationships of the
   * removed nodes, re-adding covered nodes to the waitlist if necessary.
   * @param rootOfInfeasiblePart The root of the subtree to remove.
   */
  public void removeInfeasiblePartofARG(ARGState rootOfInfeasiblePart) {
    dumpSubgraph(rootOfInfeasiblePart);

    Set<ARGState> infeasibleSubtree = rootOfInfeasiblePart.getSubgraph();

    for (ARGState removedNode : infeasibleSubtree) {
      removeCoverageOf(removedNode);
    }

    Set<ARGState> parentsOfRoot = ImmutableSet.copyOf(rootOfInfeasiblePart.getParents());
    Set<ARGState> parentsOfRemovedStates = removeSet(infeasibleSubtree);

    assert parentsOfRoot.equals(parentsOfRemovedStates);
  }

  /**
   * This method cuts of the subtree in the ARG, starting with the given state.
   *
   * Other than {@link #removeSubtree(ARGState)} and its variants, this method
   * does not care about keeping the coverage-relation consistent, so using this
   * method might, and very likely will, introduce unsoundness that has to be
   * handled appropriately by other means (e.g., a later full re-exploration).
   *
   * @param argState the state to be removed including its subtree
   */
  public void cutOffSubtree(ARGState argState) {
    Set<ARGState> subgraph = argState.getSubgraph();
    mReached.removeAll(subgraph);

    for (ARGState ae : subgraph) {
      ae.detachFromARG();
    }
  }

  /**
   * This method (re)adds the given state to the waitlist and changes the
   * precision of the state to the supplied precision.
   *
   * @param state the state to (re)add to the waitlist
   * @param precision the new precision to apply at this state
   * @param pPrecisionType the type of the precision
   */
  public void readdToWaitlist(ARGState state, Precision precision, Predicate<? super Precision> pPrecisionType) {
    mReached.updatePrecision(state, adaptPrecision(mReached.getPrecision(state), precision, pPrecisionType));
    mReached.reAddToWaitlist(state);
  }

  public void updatePrecisionForState(ARGState state, Precision precision, Predicate<? super Precision> pPrecisionType) {
    mReached.updatePrecision(state, adaptPrecision(mReached.getPrecision(state), precision, pPrecisionType));
  }

  /**
   * Set a new precision for each single state in the reached set.
   * @param pNewPrecision The new precision, may be for a single CPA
   *                      (c.f. {@link #adaptPrecision(Precision, Precision, Predicate)}).
   * @param pPrecisionType the type of the precision
   */
  public void updatePrecisionGlobally(Precision pNewPrecision,
      Predicate<? super Precision> pPrecisionType) {
    Map<Precision, Precision> precisionUpdateCache = Maps.newIdentityHashMap();

    mReached.forEach(
        (s, oldPrecision) -> {
          Precision newPrecision =
              precisionUpdateCache.computeIfAbsent(
                  oldPrecision, oldPrec -> adaptPrecision(oldPrec, pNewPrecision, pPrecisionType));

          mReached.updatePrecision(s, newPrecision);
        });
  }

  public void updateFirstStatePrecision(Precision pNewPrecision,
      Predicate<? super Precision> pPrecisionType) {

    ARGState s = (ARGState) mReached.getFirstState();
    Precision oldPrecision = mReached.getPrecision(s);
    PredicatePrecision old = Precisions.extractPrecisionByType(oldPrecision, PredicatePrecision.class);
    if (old != null) {
      PredicatePrecision newP = Precisions.extractPrecisionByType(pNewPrecision, PredicatePrecision.class);
      PredicatePrecision merged = newP.mergeWith(old);
      pNewPrecision = Precisions.replaceByType(pNewPrecision, merged, pPrecisionType);
    }
    Precision newPrecision = adaptPrecision(oldPrecision, pNewPrecision, pPrecisionType);
    mReached.updatePrecision(s, newPrecision);
  }

  /**
   * Adapts a precision with a new precision.
   * If the old precision is a wrapper precision, pNewPrecision replaces the
   * component of the wrapper precision that corresponds to pNewPrecision.
   * Otherwise, pNewPrecision is returned.
   * @param pOldPrecision The old precision.
   * @param pNewPrecision New precision.
   * @return The adapted precision.
   */
  private Precision adaptPrecision(Precision pOldPrecision, Precision pNewPrecision,
    Predicate<? super Precision> pPrecisionType) {
    return Precisions.replaceByType(pOldPrecision, pNewPrecision, pPrecisionType);
  }

  private Set<ARGState> removeSubtree0(ARGState e) {
    Preconditions.checkNotNull(e);
    Preconditions.checkArgument(!e.getParents().isEmpty(), "May not remove the initial element from the ARG/reached set");

    dumpSubgraph(e);

    Set<ARGState> toUnreach = e.getSubgraph();

    // collect all elements covered by the subtree
    List<ARGState> newToUnreach = new ArrayList<>();

    for (ARGState ae : toUnreach) {
      newToUnreach.addAll(ae.getCoveredByThis());
    }
    // we remove the covered states completely,
    // maybe we re-explore them later and find coverage again.
    // caution: siblings of the covered state might be re-explored, too,
    // they should be covered by the existing/previous siblings
    // (if sibling not removed and precision is not weaker)
    toUnreach.addAll(newToUnreach);

    Set<ARGState> toWaitlist = removeSet(toUnreach);

    return toWaitlist;
  }

  private void dumpSubgraph(ARGState e) {
    if (cpa == null) {
      return;
    }

    ARGToDotWriter refinementGraph = cpa.getARGExporter().getRefinementGraphWriter();
    if (refinementGraph == null) {
      return;
    }

    SetMultimap<ARGState, ARGState> successors =
        ARGUtils.projectARG(e, ARGState::getChildren, ARGUtils.RELEVANT_STATE);

    SetMultimap<ARGState, ARGState> predecessors =
        ARGUtils.projectARG(e, ARGState::getParents, ARGUtils.RELEVANT_STATE);

    try {
      refinementGraph.enterSubgraph("cluster_" + refinementNumber,
                                    "Refinement " + refinementNumber);

      refinementGraph.writeSubgraph(e,
          Functions.forMap(successors.asMap(), ImmutableSet.<ARGState>of()),
          Predicates.alwaysTrue(),
          Predicates.alwaysFalse());

      refinementGraph.leaveSubgraph();

      for (ARGState predecessor : predecessors.get(e)) {
        // insert edge from predecessor to e in global graph
        refinementGraph.writeEdge(predecessor, e);
      }

    } catch (IOException ex) {
      cpa.getLogger().logUserException(Level.WARNING, ex, "Could not write refinement graph to file");
    }

  }

  /**
   * Remove a set of elements from the ARG and reached set. There are no sanity checks.
   *
   * The result will be a set of elements that need to be added to the waitlist
   * to re-discover the removed elements. These are the parents of the removed
   * elements which are not removed themselves. The set is sorted based on the
   * relation defined by {@link ARGState#compareTo(ARGState)}), i.e., oldest-first.
   *
   * @param elements the elements to remove
   * @return the elements to re-add to the waitlist
   */
  private SortedSet<ARGState> removeSet(Set<ARGState> elements) {
    mReached.removeAll(elements);

    SortedSet<ARGState> toWaitlist = new TreeSet<>();
    for (ARGState ae : elements) {
      for (ARGState parent : ae.getParents()) {
        if (!elements.contains(parent)) {
          toWaitlist.add(parent);
        }
      }

      ae.removeFromARG();
    }
    return toWaitlist;
  }

  /**
   * Remove all covering relations from a node so that this node does not cover
   * any other node anymore.
   * Also adds any now uncovered leaf nodes to the waitlist.
   *
   * Call this method when you have changed (strengthened) an abstract state.
   */
  public void removeCoverageOf(ARGState v) {
    for (ARGState coveredByChildOfV : ImmutableList.copyOf(v.getCoveredByThis())) {
      uncover(coveredByChildOfV);
    }
    assert v.getCoveredByThis().isEmpty();
  }

  /**
   * Mark a covered element as non-covered.
   * This method also re-adds all leaves in that part of the ARG to the waitlist.
   *
   * @param element The covered ARGState to uncover.
   */
  private void uncover(ARGState element) {
    element.uncover();

    // this is the subtree of elements which now become uncovered
    Set<ARGState> uncoveredSubTree = element.getSubgraph();

    for (ARGState e : uncoveredSubTree) {
      assert !e.isCovered();

      e.setHasCoveredParent(false);

      if (!e.wasExpanded()) {
        // its a leaf
        mReached.reAddToWaitlist(e);
      }
    }
  }

  /**
   * Try covering an ARG state by other states in the reached set.
   * If successful, also mark the subtree below this state as covered,
   * which means that all states in this subtree do not cover any states anymore.
   * @param v The state which should be covered if possible.
   * @return whether the covering was successful
   */
  public boolean tryToCover(ARGState v) throws CPAException, InterruptedException {
    assert v.mayCover();

    cpa.getStopOperator().stop(v, mReached.getReached(v), mReached.getPrecision(v));
    // ignore return value of stop, because it will always be false

    if (v.isCovered()) {
      Set<ARGState> subtree = v.getSubgraph();
      subtree.remove(v);

      removeCoverageOf(v);
      for (ARGState childOfV : subtree) {
        // all states in the subtree (including v) may not cover anymore
        removeCoverageOf(childOfV);
      }

      for (ARGState childOfV : subtree) {
        // all states in the subtree (excluding v)
        // are removed from the waitlist,
        // are not covered anymore directly

        if (childOfV.isCovered()) {
          childOfV.uncover();
        }
      }

      for (ARGState childOfV : subtree) {
        mReached.removeOnlyFromWaitlist(childOfV);

        childOfV.setHasCoveredParent(true);

        // each child of v now doesn't cover anything anymore
        assert childOfV.getCoveredByThis().isEmpty();
        assert !childOfV.mayCover();
      }

      mReached.removeOnlyFromWaitlist(v);

      return true;
    }
    return false;
  }

  /**
   * Try covering an ARG state by other states in the reached set. This method
   * deliberately allows for unsoundness, by not caring about keeping the
   * coverage relation in the ARG consistent. When asked to be sound, this
   * method delegates to {@link ARGReachedSet#tryToCover(ARGState)}
   *
   * @param v the state which should be covered if possible
   * @param beUnsound whether or not the be unsound
   * @return whether the covering was successful
   */
  public boolean tryToCover(ARGState v, boolean beUnsound) throws CPAException, InterruptedException {
    assert v.mayCover();

    if (beUnsound) {
      cpa.getStopOperator().stop(v, mReached.getReached(v), mReached.getPrecision(v));
      return v.isCovered();
    }

    return tryToCover(v);
  }

  public static class ForwardingARGReachedSet extends ARGReachedSet {

    protected final ARGReachedSet delegate;

    public ForwardingARGReachedSet(ARGReachedSet pReached) {
      super(pReached.mReached);
      delegate = pReached;
    }

    @Override
    public UnmodifiableReachedSet asReachedSet() {
      return delegate.asReachedSet();
    }

    @Override
    public void removeSubtree(ARGState pE) {
      delegate.removeSubtree(pE);
    }

    @Override
    public void removeSubtree(
        ARGState pE, Precision pP, Predicate<? super Precision> pPrecisionType)
        throws InterruptedException {
      delegate.removeSubtree(pE, pP, pPrecisionType);
    }
  }

  /**
   * This method should only be used with great caution! It removes all pending
   * states from the waitlist, and therefore effectively prevents the analysis
   * from continuing.
   *
   * Depending on the states contained in the reached set this can lead to unsound
   * behaviour (e.g. no state in waitlist anymore, but an existing error was not found)
   */
  public void clearWaitlist() {
    while (mReached.hasWaitingState()) {
      mReached.popFromWaitlist();
    }
  }
}
