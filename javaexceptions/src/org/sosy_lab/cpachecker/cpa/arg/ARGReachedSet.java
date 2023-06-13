// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.logging.Level;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSetWrapper;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.BiPredicates;
import org.sosy_lab.cpachecker.util.Precisions;

/**
 * This class is a modifiable live view of a reached set, which shows the ARG relations between the
 * elements, and enforces a correct ARG when the set is modified through this wrapper.
 */
public class ARGReachedSet {

  private final int refinementNumber;
  private final ConfigurableProgramAnalysis cpa;

  private final ReachedSet mReached;
  private final UnmodifiableReachedSet mUnmodifiableReached;

  /**
   * Constructor for ARGReachedSet as a simple wrapper around ReachedSet. If possible, do not use
   * this constructor but the other one that takes an ARGCPA instance as parameter. This class
   * notifies the ARGCPA of removed counterexamples if possible to reduce memory usage.
   */
  public ARGReachedSet(ReachedSet pReached) {
    this(pReached, null);
  }

  public ARGReachedSet(ReachedSet pReached, ConfigurableProgramAnalysis pCpa) {
    this(pReached, pCpa, -1);
  }

  /**
   * This constructor may be used only during an refinement which should be added to the refinement
   * graph .dot file.
   */
  public ARGReachedSet(
      ReachedSet pReached, ConfigurableProgramAnalysis pCpa, int pRefinementNumber) {
    mReached = checkNotNull(pReached);
    mUnmodifiableReached = new UnmodifiableReachedSetWrapper(mReached);

    cpa = pCpa;
    refinementNumber = pRefinementNumber;
  }

  public UnmodifiableReachedSet asReachedSet() {
    return mUnmodifiableReached;
  }

  /**
   * Remove an element and all elements below it from the tree. Re-add all those elements to the
   * waitlist which have children which are either removed or were covered by removed elements.
   *
   * @param e The root of the removed subtree, may not be the initial element.
   * @throws InterruptedException can be thrown in subclass
   */
  public void removeSubtree(ARGState e) throws InterruptedException {
    Set<ARGState> toWaitlist = removeSubtree0(e);

    for (ARGState ae : toWaitlist) {
      mReached.reAddToWaitlist(ae);
    }
  }

  /**
   * For the case that the ARG is not a tree, recalculate the ReachedSet by calculating the states
   * reachable from the rootState. States which have become unreachable get properly detached
   */
  public void recalculateReachedSet(ARGState rootState) {
    removeUnReachableFrom(Collections.singleton(rootState), ARGState::getChildren, x -> true);
  }

  /**
   * If at least one error state is present,remove the parts of the ARG from which no error state is
   * reachable. Warning: This might remove states that could cover other states.
   */
  public void removeSafeRegions() {
    Collection<AbstractState> targetStates =
        from(mReached).filter(AbstractStates::isTargetState).toList();
    if (!targetStates.isEmpty()) {
      removeUnReachableFrom(targetStates, ARGState::getParents, x -> x.wasExpanded());
    }
  }

  private void removeUnReachableFrom(
      Collection<AbstractState> startStates,
      Function<? super ARGState, ? extends Iterable<ARGState>> successorFunction,
      Predicate<ARGState> allowedToRemove) {
    Deque<AbstractState> toVisit = new ArrayDeque<>(startStates);
    Set<ARGState> reached = new HashSet<>();
    while (!toVisit.isEmpty()) {
      ARGState currentElement = (ARGState) toVisit.removeFirst();
      if (reached.add(currentElement)) {
        List<ARGState> notYetReached =
            from(successorFunction.apply(currentElement))
                .filter(x -> !reached.contains(x))
                .toList();
        toVisit.addAll(notYetReached);
      }
    }
    List<ARGState> toRemove = new ArrayList<>(2);
    for (AbstractState inOldReached : mReached) {
      if (!reached.contains(inOldReached) && allowedToRemove.apply((ARGState) inOldReached)) {
        toRemove.add((ARGState) inOldReached);
      }
    }
    for (ARGState state : toRemove) {
      if (!state.isDestroyed()) {
        removeCoverageOf(state);
        state.removeFromARG();
      }
    }
    mReached.removeAll(toRemove);
  }

  /**
   * Like {@link #removeSubtree(ARGState)}, but when re-adding elements to the waitlist adapts
   * precisions with respect to the supplied precision p (see {@link #adaptPrecision(Precision,
   * Precision, Predicate)}).
   *
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
   * Like {@link #removeSubtree(ARGState)}, but when re-adding elements to the waitlist adapts
   * precisions with respect to the supplied precision p (see {@link #adaptPrecision(Precision,
   * Precision, Predicate)}). If multiple precisions are given, adapt all matching sub-precisions of
   * a WrappedPrecision.
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
        Precision adaptedPrec =
            adaptPrecision(waitingStatePrec, pPrecisions.get(i), pPrecTypes.get(i));

        // adaptedPrec == null, if the precision component was not changed
        if (adaptedPrec != null) {
          waitingStatePrec = adaptedPrec;
        }
        Preconditions.checkState(waitingStatePrec != null);
      }

      mReached.updatePrecision(waitingState, waitingStatePrec);
      mReached.reAddToWaitlist(waitingState);
    }
  }

  /**
   * Safely remove a part of the ARG which has been proved as completely unreachable. This method
   * takes care of the coverage relationships of the removed nodes, re-adding covered nodes to the
   * waitlist if necessary.
   *
   * @param rootOfInfeasiblePart The root of the subtree to remove.
   */
  public void removeInfeasiblePartofARG(ARGState rootOfInfeasiblePart) {
    dumpSubgraph(rootOfInfeasiblePart);

    Set<ARGState> infeasibleSubtree = rootOfInfeasiblePart.getSubgraph().toSet();

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
   * <p>Other than {@link #removeSubtree(ARGState)} and its variants, this method does not care
   * about keeping the coverage-relation consistent, so using this method might, and very likely
   * will, introduce unsoundness that has to be handled appropriately by other means (e.g., a later
   * full re-exploration).
   *
   * @param argState the state to be removed including its subtree
   */
  public void cutOffSubtree(ARGState argState) {
    // copy whole subgraph before modifying ARG!
    ImmutableList<ARGState> subgraph = argState.getSubgraph().toList();
    mReached.removeAll(subgraph);

    for (ARGState ae : subgraph) {
      ae.detachFromARG();
    }
  }

  /**
   * This method (re)adds the given state to the waitlist and changes the precision of the state to
   * the supplied precision.
   *
   * @param state the state to (re)add to the waitlist
   * @param precision the new precision to apply at this state
   * @param pPrecisionType the type of the precision
   */
  public void readdToWaitlist(
      ARGState state, Precision precision, Predicate<? super Precision> pPrecisionType) {
    mReached.updatePrecision(
        state, adaptPrecision(mReached.getPrecision(state), precision, pPrecisionType));
    mReached.reAddToWaitlist(state);
  }

  public void updatePrecisionForState(
      ARGState state, Precision precision, Predicate<? super Precision> pPrecisionType) {
    mReached.updatePrecision(
        state, adaptPrecision(mReached.getPrecision(state), precision, pPrecisionType));
  }

  /**
   * Set a new precision for each single state in the reached set.
   *
   * @param pNewPrecision The new precision, may be for a single CPA (c.f. {@link
   *     #adaptPrecision(Precision, Precision, Predicate)}).
   * @param pPrecisionType the type of the precision
   */
  public void updatePrecisionGlobally(
      Precision pNewPrecision, Predicate<? super Precision> pPrecisionType) {
    IdentityHashMap<Precision, Precision> precisionUpdateCache = new IdentityHashMap<>();

    mReached.forEach(
        (s, oldPrecision) -> {
          Precision newPrecision =
              precisionUpdateCache.computeIfAbsent(
                  oldPrecision, oldPrec -> adaptPrecision(oldPrec, pNewPrecision, pPrecisionType));

          mReached.updatePrecision(s, newPrecision);
        });
  }

  /**
   * Adapts a precision with a new precision. If the old precision is a wrapper precision,
   * pNewPrecision replaces the component of the wrapper precision that corresponds to
   * pNewPrecision. Otherwise, pNewPrecision is returned.
   *
   * @param pOldPrecision The old precision.
   * @param pNewPrecision New precision.
   * @return The adapted precision.
   */
  private Precision adaptPrecision(
      Precision pOldPrecision,
      Precision pNewPrecision,
      Predicate<? super Precision> pPrecisionType) {
    return Precisions.replaceByType(pOldPrecision, pNewPrecision, pPrecisionType);
  }

  private Set<ARGState> removeSubtree0(ARGState e) {
    Preconditions.checkNotNull(e);
    Preconditions.checkArgument(
        !e.getParents().isEmpty(),
        "May not remove the initial state from the ARG/reached set.\nTrying to remove state '%s'.",
        e);

    dumpSubgraph(e);

    // collect all elements covered by the subtree
    ImmutableList<ARGState> subtree = e.getSubgraph().toList();
    ImmutableSet<ARGState> toUnreach =
        from(subtree).transformAndConcat(ARGState::getCoveredByThis).append(subtree).toSet();

    // we remove the covered states completely,
    // maybe we re-explore them later and find coverage again.
    // caution: siblings of the covered state might be re-explored, too,
    // they should be covered by the existing/previous siblings
    // (if sibling not removed and precision is not weaker)
    Set<ARGState> toWaitlist = removeSet(toUnreach);

    return toWaitlist;
  }

  private void dumpSubgraph(ARGState e) {
    if (!(cpa instanceof ARGCPA)) {
      return;
    }

    ARGCPA argCpa = (ARGCPA) cpa;

    ARGToDotWriter refinementGraph = argCpa.getARGExporter().getRefinementGraphWriter();
    if (refinementGraph == null) {
      return;
    }

    SetMultimap<ARGState, ARGState> successors =
        ARGUtils.projectARG(e, ARGState::getChildren, ARGUtils::isRelevantState);

    SetMultimap<ARGState, ARGState> predecessors =
        ARGUtils.projectARG(e, ARGState::getParents, ARGUtils::isRelevantState);

    try {
      refinementGraph.enterSubgraph(
          "cluster_" + refinementNumber, "Refinement " + refinementNumber);

      refinementGraph.writeSubgraph(
          e,
          Functions.forMap(successors.asMap(), ImmutableSet.of()),
          Predicates.alwaysTrue(),
          BiPredicates.alwaysFalse());

      refinementGraph.leaveSubgraph();

      for (ARGState predecessor : predecessors.get(e)) {
        // insert edge from predecessor to e in global graph
        refinementGraph.writeEdge(predecessor, e);
      }

    } catch (IOException ex) {
      argCpa
          .getLogger()
          .logUserException(Level.WARNING, ex, "Could not write refinement graph to file");
    }
  }

  /**
   * Remove a set of elements from the ARG and reached set. There are no sanity checks.
   *
   * <p>The result will be a set of elements that need to be added to the waitlist to re-discover
   * the removed elements. These are the parents of the removed elements which are not removed
   * themselves. The set is sorted based on the relation defined by {@link
   * ARGState#compareTo(ARGState)}), i.e., oldest-first.
   *
   * @param elements the elements to remove
   * @return the elements to re-add to the waitlist
   */
  private NavigableSet<ARGState> removeSet(Set<ARGState> elements) {
    mReached.removeAll(elements);

    NavigableSet<ARGState> toWaitlist = new TreeSet<>();
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
   * Remove all covering relations from a node so that this node does not cover any other node
   * anymore. Also adds any now uncovered leaf nodes to the waitlist.
   *
   * <p>Call this method when you have changed (strengthened) an abstract state.
   */
  public void removeCoverageOf(ARGState v) {
    for (ARGState coveredByChildOfV : ImmutableList.copyOf(v.getCoveredByThis())) {
      uncover(coveredByChildOfV);
    }
    assert v.getCoveredByThis().isEmpty();
  }

  /**
   * Mark a covered element as non-covered. This method also re-adds all leaves in that part of the
   * ARG to the waitlist.
   *
   * @param element The covered ARGState to uncover.
   */
  private void uncover(ARGState element) {
    element.uncover();

    // this is the subtree of elements which now become uncovered
    for (ARGState e : element.getSubgraph()) {
      assert !e.isCovered();

      e.setHasCoveredParent(false);

      if (!e.wasExpanded()) {
        // its a leaf
        mReached.reAddToWaitlist(e);
      }
    }
  }

  /**
   * Try covering an ARG state by other states in the reached set. If successful, also mark the
   * subtree below this state as covered, which means that all states in this subtree do not cover
   * any states anymore.
   *
   * @param v The state which should be covered if possible.
   * @return whether the covering was successful
   */
  public boolean tryToCover(ARGState v) throws CPAException, InterruptedException {
    assert v.mayCover();

    // sideeffect: coverage and cleanup of ARG is done in ARGStopSep#stop
    boolean stop = cpa.getStopOperator().stop(v, mReached.getReached(v), mReached.getPrecision(v));
    Preconditions.checkState(!stop);
    // ignore return value of stop, because it will always be false

    if (v.isCovered()) {
      ImmutableSet<ARGState> subtree = v.getSubgraph().filter(s -> !s.equals(v)).toSet();

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
   * Try covering an ARG state by other states in the reached set. This method deliberately allows
   * for unsoundness, by not caring about keeping the coverage relation in the ARG consistent. When
   * asked to be sound, this method delegates to {@link ARGReachedSet#tryToCover(ARGState)}
   *
   * @param v the state which should be covered if possible
   * @param beUnsound whether or not the be unsound
   * @return whether the covering was successful
   */
  public boolean tryToCover(ARGState v, boolean beUnsound)
      throws CPAException, InterruptedException {
    assert v.mayCover();

    if (beUnsound) {
      // sideeffect: coverage and cleanup of ARG is done in ARGStopSep#stop
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
  }

  /**
   * This method adds a state to the reached after splitting, but removes it from the waitlist. The
   * precision is taken from the original state. Only call this method if you are sure that the
   * state does not represent unreached concrete states, otherwise it will be unsound.
   */
  public void addForkedState(ARGState forkedState, ARGState originalState) {
    mReached.addNoWaitlist(forkedState, mReached.getPrecision(originalState));
  }
}
