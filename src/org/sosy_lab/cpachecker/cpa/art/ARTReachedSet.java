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
package org.sosy_lab.cpachecker.cpa.art;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Precisions;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * This class is a modifiable live view of a reached set, which shows the ART
 * relations between the elements, and enforces a correct ART when the set is
 * modified through this wrapper.
 */
public class ARTReachedSet {

  private final ReachedSet mReached;
//  private final UnmodifiableReachedSet mUnmodifiableReached;
  private final ARTCPA mCpa;

  public ARTReachedSet(ReachedSet pReached) {
    this(pReached, null);
  }

  public ARTReachedSet(ReachedSet pReached, ARTCPA pCpa) {
    mReached = checkNotNull(pReached);
//    mUnmodifiableReached = new UnmodifiableReachedSetWrapper(mReached);
    mCpa = pCpa;
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
  public void removeSubtree(ARTElement e) {
    Set<ARTElement> toWaitlist = removeSubtree0(e);

    for (ARTElement ae : toWaitlist) {
      mReached.reAddToWaitlist(ae);
    }
  }

  /**
   * Like {@link #removeSubtree(ARTElement)}, but when re-adding elements to the
   * waitlist adapts precisions with respect to the supplied precision p (see
   * {@link #adaptPrecision(ARTElement, Precision)}).
   * @param e The root of the removed subtree, may not be the initial element.
   * @param p The new precision.
   */
  public void removeSubtree(ARTElement e, Precision p) {
    Set<ARTElement> toWaitlist = removeSubtree0(e);

    for (ARTElement ae : toWaitlist) {
      mReached.updatePrecision(ae, adaptPrecision(ae, p));
      mReached.reAddToWaitlist(ae);
    }
  }
  public void removeSubtree(ARTElement e, Precision p1, Precision p2) {
    Set<ARTElement> toWaitlist = removeSubtree0(e);

    for (ARTElement ae : toWaitlist) {
      mReached.updatePrecision(ae, adaptPrecision(ae, p1));
      mReached.updatePrecision(ae, adaptPrecision(ae, p2));
      mReached.reAddToWaitlist(ae);
    }
  }

  /**
   * Adapts the precision stored in the reached set for lARTElement.
   * If the stored precision is a wrapper precision, pNewPrecision replaces the
   * component of the wrapper precision that corresponds to pNewPrecision.
   * Otherwise, pNewPrecision replaces the stored precision.
   * @param pARTElement Reached element for which the precision has to be adapted.
   * @param pNewPrecision New precision.
   * @return The adapted precision.
   */
  private Precision adaptPrecision(ARTElement pARTElement, Precision pNewPrecision) {
    Precision lOldPrecision = mReached.getPrecision(pARTElement);

    return Precisions.replaceByType(lOldPrecision, pNewPrecision, pNewPrecision.getClass());
  }

  private Set<ARTElement> removeSubtree0(ARTElement e) {
    Preconditions.checkNotNull(e);
    Preconditions.checkArgument(!e.getParents().isEmpty(), "May not remove the initial element from the ART/reached set");

    Set<ARTElement> toUnreach = e.getSubtree();

    // collect all elements covered by the subtree
    List<ARTElement> newToUnreach = new ArrayList<ARTElement>();

    for (ARTElement ae : toUnreach) {
      newToUnreach.addAll(ae.getCoveredByThis());
    }
    toUnreach.addAll(newToUnreach);

    mReached.removeAll(toUnreach);

    Set<ARTElement> toWaitlist = removeSet(toUnreach);

    return toWaitlist;
  }

  /**
   * Remove a set of elements from the ART. There are no sanity checks.
   *
   * The result will be a set of elements that need to be added to the waitlist
   * to re-discover the removed elements. These are the parents of the removed
   * elements which are not removed themselves.
   *
   * @param elements the elements to remove
   * @return the elements to re-add to the waitlist
   */
  private static Set<ARTElement> removeSet(Set<ARTElement> elements) {
    Set<ARTElement> toWaitlist = new LinkedHashSet<ARTElement>();
    for (ARTElement ae : elements) {

      for (ARTElement parent : ae.getParents()) {
        if (!elements.contains(parent)) {
          toWaitlist.add(parent);
        }
      }

      ae.removeFromART();
    }
    return toWaitlist;
  }

  /**
   * Remove an element and all elements below it from the tree, which are not
   * reachable via other paths through the ART (which in fact is a DAG, so there
   * may be such paths). Re-add all those elements to the waitlist which have
   * children which were covered by removed elements.
   *
   * The effect on the reached set is the same as if the transfer relation had
   * returned bottom (e.g. no successor) instead of this element. The only
   * exception is that the elements on such remaining paths may still be weaker
   * than if they had not been merged with the removed elements.
   *
   * @param e The root of the removed subtree, may not be the initial element.
   */
  public void replaceWithBottom(ARTElement e) {
    Preconditions.checkNotNull(e);
    Preconditions.checkArgument(!e.getParents().isEmpty(), "May not remove the initial element from the ART/reached set");

    Set<ARTElement> removedElements = new HashSet<ARTElement>();
    Deque<ARTElement> workList = new ArrayDeque<ARTElement>();

    workList.addAll(e.getChildren());
    removedElements.add(e);
    removeCoverage(e);
    e.removeFromART();

    while (!workList.isEmpty()) {
      ARTElement currentElement = workList.removeFirst();
      if (currentElement.getParents().isEmpty()) {
        // no other paths to this element

        if (removedElements.add(currentElement)) {
          // not yet handled
          workList.addAll(currentElement.getChildren());

          removedElements.addAll(currentElement.getCoveredByThis());
          removeCoverage(currentElement);

          currentElement.removeFromART();
        }
      }
    }

    mReached.removeAll(removedElements);
  }

  /**
   * Assume that one element does not cover any elements anymore.
   * This re-adds the parents of elements previously covered to the waitlist.
   *
   * You do not need to call this method if you removed elements through the
   * other methods provided by this class, they already ensure the same effect.
   *
   * Be careful when using this method! It's only necessary when either the
   * reached set is modified in some way that the ARTCPA doesn't notice,
   * or elements in the reached set were made stronger after they were added,
   * but elements should never be modified normally.
   *
   * @param element The element which does not cover anymore.
   */
  public void removeCoverage(ARTElement element) {
    for (ARTElement covered : ImmutableList.copyOf(element.getCoveredByThis())) {
      for (ARTElement parent : covered.getParents()) {
        mReached.reAddToWaitlist(parent);
      }
      covered.removeFromART(); // also removes from element.getCoveredByThis() set
    }
    assert element.getCoveredByThis().isEmpty();
  }

  /**
   * Check if an element is covered by another element in the reached set. The
   * element has to be in the reached set currently, so it was not covered when
   * it was added. Children of the given element are not considered for the
   * coverage check.
   *
   * If this method returns true, it already has done all the necessary
   * modifications to the reached set and the ART to reflect the new situation.
   * Both of them then look as if the element would have been covered at the time
   * it was produced by the transfer relation.
   *
   * @param element
   * @return
   * @throws CPAException
   */
  public boolean checkForCoveredBy(ARTElement element) throws CPAException {
    Preconditions.checkNotNull(element);
    Preconditions.checkArgument(mReached.contains(element));
    assert !element.isCovered() : "element in reached set but covered";

    if (mCpa == null) {
      throw new UnsupportedOperationException("Need CPA for coverage checks");
    }

    // get the reached set and remove the element itself and all its children
    Set<AbstractElement> localReached = new HashSet<AbstractElement>(mReached.getReached(element));
    Set<ARTElement> subtree = element.getSubtree();
    localReached.removeAll(subtree);

    StopOperator stopOp = mCpa.getStopOperator();
    boolean stop = stopOp.stop(element, localReached, mReached.getPrecision(element));

    if (stop) {
      // remove subtree from ART, but not the element itself
      Set<ARTElement> toUnreach = new HashSet<ARTElement>(subtree);
      toUnreach.remove(element);

      // collect all elements covered by the subtree
      List<ARTElement> newToUnreach = new ArrayList<ARTElement>();

      for (ARTElement ae : toUnreach) {
        newToUnreach.addAll(ae.getCoveredByThis());
      }
      toUnreach.addAll(newToUnreach);

      mReached.removeAll(toUnreach);
      mReached.remove(element);

      Set<ARTElement> toWaitlist = removeSet(toUnreach);
      // do not add the element itself, because it is covered
      toWaitlist.remove(element);

      for (ARTElement ae : toWaitlist) {
        mReached.reAddToWaitlist(ae);
      }
    }

    return stop;
  }

  public static class ForwardingARTReachedSet extends ARTReachedSet {

    protected final ARTReachedSet delegate;

    public ForwardingARTReachedSet(ARTReachedSet pReached) {
      super(pReached.mReached, pReached.mCpa);
      delegate = pReached;
    }

    @Override
    public ReachedSet asReachedSet() {
      return delegate.asReachedSet();
    }

    @Override
    public boolean checkForCoveredBy(ARTElement pElement) throws CPAException {
      return delegate.checkForCoveredBy(pElement);
    }

    @Override
    public void removeCoverage(ARTElement pElement) {
      delegate.removeCoverage(pElement);
    }

    @Override
    public void removeSubtree(ARTElement pE) {
      delegate.removeSubtree(pE);
    }

    @Override
    public void removeSubtree(ARTElement pE, Precision pP) {
      delegate.removeSubtree(pE, pP);
    }

    @Override
    public void replaceWithBottom(ARTElement pE) {
      delegate.replaceWithBottom(pE);
    }
  }
}
