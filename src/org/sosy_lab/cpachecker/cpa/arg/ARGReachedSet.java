/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.Precisions;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

/**
 * This class is a modifiable live view of a reached set, which shows the ARG
 * relations between the elements, and enforces a correct ARG when the set is
 * modified through this wrapper.
 */
public class ARGReachedSet {

  private final ReachedSet mReached;
//  private final UnmodifiableReachedSet mUnmodifiableReached;

  public ARGReachedSet(ReachedSet pReached) {
    mReached = checkNotNull(pReached);
//    mUnmodifiableReached = new UnmodifiableReachedSetWrapper(mReached);
  }

  public ReachedSet asReachedSet() {
    return mReached;
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
   * {@link #adaptPrecision(ARGState, Precision)}).
   * @param e The root of the removed subtree, may not be the initial element.
   * @param p The new precision.
   */
  public void removeSubtree(ARGState e, Precision p) {
    Set<ARGState> toWaitlist = removeSubtree0(e);

    for (ARGState ae : toWaitlist) {
      mReached.updatePrecision(ae, adaptPrecision(mReached.getPrecision(ae), p));
      mReached.reAddToWaitlist(ae);
    }
  }

  /**
   * Set a new precision for each single state in the reached set.
   * @param p The new precision, may be for a single CPA (c.f. {@link #adaptPrecision(ARGState, Precision)}).
   */
  public void updatePrecisionGlobally(Precision pNewPrecision) {
    Map<Precision, Precision> precisionUpdateCache = Maps.newIdentityHashMap();

    for (AbstractState s : mReached) {
      Precision oldPrecision = mReached.getPrecision(s);

      Precision newPrecision = precisionUpdateCache.get(oldPrecision);
      if (newPrecision == null) {
        newPrecision = adaptPrecision(oldPrecision, pNewPrecision);
        precisionUpdateCache.put(oldPrecision, newPrecision);
      }

      mReached.updatePrecision(s, newPrecision);
    }
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
  private Precision adaptPrecision(Precision pOldPrecision, Precision pNewPrecision) {
    return Precisions.replaceByType(pOldPrecision, pNewPrecision, pNewPrecision.getClass());
  }

  private Set<ARGState> removeSubtree0(ARGState e) {
    Preconditions.checkNotNull(e);
    Preconditions.checkArgument(!e.getParents().isEmpty(), "May not remove the initial element from the ARG/reached set");

    Set<ARGState> toUnreach = e.getSubgraph();

    // collect all elements covered by the subtree
    List<ARGState> newToUnreach = new ArrayList<ARGState>();

    for (ARGState ae : toUnreach) {
      newToUnreach.addAll(ae.getCoveredByThis());
    }
    toUnreach.addAll(newToUnreach);

    mReached.removeAll(toUnreach);

    Set<ARGState> toWaitlist = removeSet(toUnreach);

    return toWaitlist;
  }

  /**
   * Remove a set of elements from the ARG. There are no sanity checks.
   *
   * The result will be a set of elements that need to be added to the waitlist
   * to re-discover the removed elements. These are the parents of the removed
   * elements which are not removed themselves.
   *
   * @param elements the elements to remove
   * @return the elements to re-add to the waitlist
   */
  private static Set<ARGState> removeSet(Set<ARGState> elements) {
    Set<ARGState> toWaitlist = new LinkedHashSet<ARGState>();
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
  public void uncover(ARGState element) {
    element.uncover();

    // this is the subtree of elements which now become uncovered
    Set<ARGState> uncoveredSubTree = element.getSubgraph();

    for (ARGState e : uncoveredSubTree) {
      assert !e.isCovered();

      e.setCovering();

      if (!e.wasExpanded()) {
        // its a leaf
        mReached.reAddToWaitlist(e);
      }
    }
  }

  public static class ForwardingARTReachedSet extends ARGReachedSet {

    protected final ARGReachedSet delegate;

    public ForwardingARTReachedSet(ARGReachedSet pReached) {
      super(pReached.mReached);
      delegate = pReached;
    }

    @Override
    public ReachedSet asReachedSet() {
      return delegate.asReachedSet();
    }

    @Override
    public void removeSubtree(ARGState pE) {
      delegate.removeSubtree(pE);
    }

    @Override
    public void removeSubtree(ARGState pE, Precision pP) {
      delegate.removeSubtree(pE, pP);
    }
  }
}
