// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.bam.BAMSubgraphComputer.BackwardARGState;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMCache.BAMCacheEntry;
import org.sosy_lab.cpachecker.cpa.bam.cache.BAMCacheAggressiveImpl;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.statistics.ThreadSafeTimerContainer.TimerWrapper;

public class ARGInPlaceSubtreeRemover extends ARGSubtreeRemover {

  public ARGInPlaceSubtreeRemover(AbstractBAMCPA bamCpa, TimerWrapper pRemoveCachedSubtreeTimer) {
    super(bamCpa, pRemoveCachedSubtreeTimer);
  }

  @Override
  void removeSubtree(
      ARGReachedSet mainReachedSet,
      ARGPath pPath,
      ARGState element,
      List<Precision> pNewPrecisions,
      List<Predicate<? super Precision>> pNewPrecisionTypes)
      throws InterruptedException {

    final BackwardARGState cutPoint = (BackwardARGState) element;
    final ARGState cutPointAsArgState = getReachedState(cutPoint);
    final ARGState firstState = (ARGState) mainReachedSet.asReachedSet().getFirstState();
    final ARGState lastState = (ARGState) mainReachedSet.asReachedSet().getLastState();

    assert pPath.asStatesList().get(0).getWrappedState() == firstState
        : "path should start with root state";
    assert Iterables.getLast(pPath.asStatesList()).getWrappedState() == lastState
        : "path should end with target state";
    assert lastState.isTarget();

    final Map<BackwardARGState, ARGState> blockInitAndExitStates =
        getBlockInitAndExitStates(pPath.asStatesList());
    final List<BackwardARGState> relevantCallStates =
        getRelevantCallStates(pPath.asStatesList(), cutPoint);
    // assert relevantCallStates.get(0).getWrappedState() == firstState : "root should be relevant";
    // assert relevantCallStates.size() >= 1 : "at least the main-function should be open at the
    // target-state";

    Multimap<BackwardARGState, ARGState> neededRemoveCachedSubtreeCalls =
        LinkedHashMultimap.create();

    // iterate from root to element and remove all subtrees for subgraph calls
    for (int i = 0; i < relevantCallStates.size() - 1; i++) { // ignore root and the last element
      final BackwardARGState pathElement = relevantCallStates.get(i);
      final BackwardARGState nextElement = relevantCallStates.get(i + 1);
      neededRemoveCachedSubtreeCalls.put(pathElement, getReachedState(nextElement));
    }

    if (bamCache instanceof BAMCacheAggressiveImpl) {
      ensureExactCacheHitsOnPath(
          pPath,
          cutPoint,
          pNewPrecisions,
          neededRemoveCachedSubtreeCalls,
          mainReachedSet.asReachedSet(),
          blockInitAndExitStates);
    }

    for (final Entry<BackwardARGState, ARGState> removeCachedSubtreeArguments :
        neededRemoveCachedSubtreeCalls.entries()) {
      BackwardARGState bamState = removeCachedSubtreeArguments.getKey();
      assert data.hasInitialState(bamState.getARGState());
      assert data.getReachedSetForInitialState(
              bamState.getARGState(), blockInitAndExitStates.get(bamState))
          .contains(removeCachedSubtreeArguments.getValue());
    }

    if (mainReachedSet.asReachedSet().contains(cutPointAsArgState)) {
      assert relevantCallStates.isEmpty();
      mainReachedSet.removeSubtree(cutPointAsArgState, pNewPrecisions, pNewPrecisionTypes);
      // nothing else needed, because cutPoint is not at entry- or exit-location of a block.

    } else {
      assert !relevantCallStates.isEmpty();
      // first remove the cut-state directly
      BackwardARGState lastRelevantState = Iterables.getLast(relevantCallStates);
      removeCachedSubtree(
          getReachedState(lastRelevantState),
          blockInitAndExitStates.get(lastRelevantState),
          getReachedState(cutPoint),
          pNewPrecisions,
          pNewPrecisionTypes);

      // then remove some important states along the path, sufficient for re-exploration
      final ARGState lastRelevantNode = getReachedState(Iterables.getLast(relevantCallStates));
      final Function<ARGState, Pair<List<Precision>, List<Predicate<? super Precision>>>>
          precUpdate =
              stateToRemove -> {
                assert !(stateToRemove instanceof BackwardARGState);
                if (mustUpdatePrecision(
                    lastRelevantNode, getReachedState(element), stateToRemove)) {
                  return Pair.of(pNewPrecisions, pNewPrecisionTypes);
                } else {
                  // no update of precision needed
                  return Pair.of(ImmutableList.of(), ImmutableList.of());
                }
              };

      for (final Entry<BackwardARGState, ARGState> removeCachedSubtreeArguments :
          neededRemoveCachedSubtreeCalls.entries()) {
        ARGState stateToRemove = removeCachedSubtreeArguments.getValue();
        BackwardARGState rootState = removeCachedSubtreeArguments.getKey();
        Pair<List<Precision>, List<Predicate<? super Precision>>> p =
            precUpdate.apply(stateToRemove);
        removeCachedSubtreeIfPossible(
            getReachedState(rootState),
            blockInitAndExitStates.get(rootState),
            stateToRemove,
            p.getFirst(),
            p.getSecond());
      }

      if (Objects.equals(firstState, relevantCallStates.get(0).getARGState())
          && AbstractStates.isTargetState(lastState)) {
        // old code:
        // the main-reachedset contains only the root, exit-states and targets.
        // we assume, that the current refinement was caused by a target-state.
        assert firstState.getChildren().contains(lastState);
        mainReachedSet.removeSubtree(lastState);
      } else {
        BackwardARGState stateToRemove = relevantCallStates.get(0);
        Pair<List<Precision>, List<Predicate<? super Precision>>> p =
            precUpdate.apply(stateToRemove.getARGState());
        mainReachedSet.removeSubtree(stateToRemove.getARGState(), p.getFirst(), p.getSecond());
      }
    }
  }

  private boolean mustUpdatePrecision(
      final ARGState lastRelevantNode, final ARGState target, final ARGState stateToRemove) {
    return doPrecisionRefinementForAllStates
        // special option (mostly for testing)
        || Objects.equals(stateToRemove, lastRelevantNode)
        // last iteration, most inner block for refinement
        || (data.hasInitialState(stateToRemove)
            && !target.getParents().isEmpty()
            && Iterables.all(target.getParents(), p -> p.getParents().isEmpty()));
    // if there are parents, but no grandparents
    // --> if second state of reached-set is removed, update parent-precision
  }

  /** just remove a state and its subtree from the given reachedSet. */
  static void removeSubtree(ARGReachedSet reachedSet, ARGState argElement)
      throws InterruptedException {
    if (AbstractBAMTransferRelation.isHeadOfMainFunction(extractLocation(argElement))) {
      assert ((ARGState) reachedSet.asReachedSet().getFirstState())
          .getChildren()
          .contains(reachedSet.asReachedSet().getLastState());
      reachedSet.removeSubtree((ARGState) reachedSet.asReachedSet().getLastState());
    } else {
      reachedSet.removeSubtree(argElement);
    }
  }

  private void removeCachedSubtreeIfPossible(
      ARGState rootState,
      ARGState exitState,
      ARGState removeElement,
      List<Precision> pNewPrecisions,
      List<Predicate<? super Precision>> pPrecisionTypes)
      throws InterruptedException {
    if (removeElement.isDestroyed()) {
      logger.log(Level.FINER, "state was destroyed before");
      // apparently, removeElement was removed due to prior deletions
    } else {
      removeCachedSubtree(rootState, exitState, removeElement, pNewPrecisions, pPrecisionTypes);
    }
  }

  /**
   * This method removes a state from the corresponding reached-set. This is basically the same as
   * {@link ARGReachedSet#removeSubtree(ARGState)}, but we also update the BAM-cache.
   */
  private void removeCachedSubtree(
      ARGState rootState,
      ARGState exitState,
      ARGState removeElement,
      List<Precision> pNewPrecisions,
      List<Predicate<? super Precision>> pPrecisionTypes)
      throws InterruptedException {
    assert pNewPrecisions.size() == pPrecisionTypes.size();
    removeCachedSubtreeTimer.start();
    logger.log(
        Level.FINER,
        "Remove cached subtree for",
        removeElement,
        " issued with precision",
        pNewPrecisions);

    CFANode rootNode = extractLocation(rootState);
    Block rootSubtree = partitioning.getBlockForCallNode(rootNode);
    ReachedSet reachedSet = data.getReachedSetForInitialState(rootState, exitState);
    assert reachedSet.contains(removeElement)
        : "removing state from wrong reachedSet: " + removeElement;
    assert !removeElement.getParents().isEmpty();

    AbstractState reducedRootState =
        wrappedReducer.getVariableReducedState(rootState, rootSubtree, rootNode);
    Precision reducedRootPrecision = reachedSet.getPrecision(reachedSet.getFirstState());
    BAMCacheEntry entry = bamCache.get(reducedRootState, reducedRootPrecision, rootSubtree);
    if (entry != null) {
      // TODO do we need this check? Maybe there is a bug, if the entry is not available?
      entry.deleteInfo();
    }

    ARGReachedSet argReachedSet = new ARGReachedSet(reachedSet);
    if (pNewPrecisions.isEmpty()) {
      // no new precision needed, simply remove the subtree
      removeSubtree(argReachedSet, removeElement);

    } else {
      final Pair<Precision, Predicate<? super Precision>> newPrecision =
          getUpdatedPrecision(
              reachedSet.getPrecision(removeElement), pNewPrecisions, pPrecisionTypes);
      if (removeElement.getParents().contains(reachedSet.getFirstState())) {
        // after removing the state, only the root-state (and maybe other branches
        // starting at root) would remain, with a new precision for root.
        // instead of modifying the existing reached-set,
        // we create a new reached-set with a new root with the new precision.
        logger.log(Level.FINER, "creating reached-set with new precision");
        Precision reducedPrecision =
            wrappedReducer.getVariableReducedPrecision(newPrecision.getFirst(), rootSubtree);
        data.createAndRegisterNewReachedSet(reducedRootState, reducedPrecision, rootSubtree);
      } else {
        argReachedSet.removeSubtree(
            removeElement, newPrecision.getFirst(), newPrecision.getSecond());
      }
    }

    removeCachedSubtreeTimer.stop();
  }

  /** This method builds the updated precision for the refinement. */
  private Pair<Precision, Predicate<? super Precision>> getUpdatedPrecision(
      Precision removePrecision,
      final List<Precision> precisions,
      final List<Predicate<? super Precision>> precisionTypes) {
    assert precisions.size() == precisionTypes.size() && !precisions.isEmpty();
    for (int i = 0; i < precisions.size(); i++) {
      removePrecision =
          Precisions.replaceByType(removePrecision, precisions.get(i), precisionTypes.get(i));
    }
    return Pair.of(removePrecision, Predicates.instanceOf(removePrecision.getClass()));
  }

  /**
   * there might be some "imprecise" cache entries used along the path. We remove all of them and
   * create the "precise" entry for re-exploration. We only update those blocks, where a nested
   * block is imprecise.
   */
  private void ensureExactCacheHitsOnPath(
      ARGPath pPath,
      final BackwardARGState pElement,
      List<Precision> pNewPrecisions,
      Multimap<BackwardARGState, ARGState> neededRemoveCachedSubtreeCalls,
      UnmodifiableReachedSet pMainReachedSet,
      Map<BackwardARGState, ARGState> blockInitAndExitStates)
      throws InterruptedException {
    boolean cutStateFound = false;
    final Deque<Boolean> needsNewPrecisionEntries = new ArrayDeque<>();
    final Deque<Boolean> foundInnerUnpreciseEntries = new ArrayDeque<>();
    final Deque<BackwardARGState> rootStates = new ArrayDeque<>();

    // add root from main-reached-set
    needsNewPrecisionEntries.add(false);
    foundInnerUnpreciseEntries.add(false);
    rootStates.add((BackwardARGState) pPath.getFirstState());

    for (final ARGState pathState : pPath.asStatesList()) {
      final BackwardARGState bamState = (BackwardARGState) pathState;
      assert needsNewPrecisionEntries.size() == foundInnerUnpreciseEntries.size();
      assert needsNewPrecisionEntries.size() == rootStates.size();

      if (bamState.equals(pElement)) {
        cutStateFound = true;
      }

      for (AbstractState tmp : data.getExpandedStatesList(bamState.getARGState())) {
        AbstractState reducedExitState = data.getReducedStateForExpandedState(tmp);
        boolean isNewPrecisionEntry = needsNewPrecisionEntries.removeLast();
        boolean isNewPrecisionEntryForOuterBlock = needsNewPrecisionEntries.getLast();
        boolean removedUnpreciseInnerBlock = foundInnerUnpreciseEntries.removeLast();
        boolean foundInnerUnpreciseEntry = foundInnerUnpreciseEntries.getLast();

        BackwardARGState rootState = rootStates.removeLast();
        if ((removedUnpreciseInnerBlock || isNewPrecisionEntry)
            && !isNewPrecisionEntryForOuterBlock
            && !foundInnerUnpreciseEntry) {

          if (cutStateFound) {
            // we indeed found an inner block that was imprecise,
            // if we are in a reached set that already uses the new precision and this is the first
            // such entry
            // we have to remove the subtree starting from currentElement in the rootReachedSet
            neededRemoveCachedSubtreeCalls.put(rootState, (ARGState) reducedExitState);
          }

          assert data.getReachedSetForInitialState(
                      rootState.getARGState(), blockInitAndExitStates.get(rootState))
                  .contains(reducedExitState)
              : "reachedset for initial state "
                  + rootState.getARGState()
                  + " does not contain state "
                  + reducedExitState;

          // replace last
          foundInnerUnpreciseEntries.removeLast();
          foundInnerUnpreciseEntries.addLast(true);
        }
      }

      if (data.hasInitialState(bamState.getARGState())) {
        // before reaching the cutstate, we assume that all cache-entries are sufficient
        final UnmodifiableReachedSet openReachedSet;
        if (rootStates.getLast().getWrappedState() == pMainReachedSet.getFirstState()) {
          openReachedSet = pMainReachedSet;
        } else {
          ARGState initialState = rootStates.getLast().getARGState();
          openReachedSet =
              data.getReachedSetForInitialState(
                  initialState, blockInitAndExitStates.get(rootStates.getLast()));
        }
        boolean preciseEntry =
            !cutStateFound
                || createNewPreciseEntry(
                    bamState, pNewPrecisions, openReachedSet, blockInitAndExitStates);
        needsNewPrecisionEntries.addLast(preciseEntry);
        foundInnerUnpreciseEntries.addLast(false);
        rootStates.addLast(bamState);
      }
    }

    // now only the initial elements should be on the stacks
    assert !Iterables.getOnlyElement(needsNewPrecisionEntries);
    assert Objects.equals(rootStates.getLast(), pPath.getFirstState());
  }

  /**
   * This method creates a new precise entry if necessary, and returns whether the used entry needs
   * a new precision.
   */
  private boolean createNewPreciseEntry(
      BackwardARGState rootState,
      List<Precision> pNewPrecisions,
      UnmodifiableReachedSet outerReachedSet,
      Map<BackwardARGState, ARGState> blockInitAndExitStates)
      throws InterruptedException {

    // create updated precision
    ARGState initialState = rootState.getARGState();
    Precision rootPrecision = outerReachedSet.getPrecision(initialState);
    for (Precision pNewPrecision : pNewPrecisions) {
      Precision tmp =
          Precisions.replaceByType(
              rootPrecision, pNewPrecision, Predicates.instanceOf(pNewPrecision.getClass()));
      if (tmp != null) { // precision changed
        rootPrecision = tmp;
      }
    }
    assert rootPrecision != null;

    // reduce the new precision and add a precise key for the new precision if needed
    CFANode node = extractLocation(rootState);
    Block context = partitioning.getBlockForCallNode(node);
    AbstractState reducedRootState =
        wrappedReducer.getVariableReducedState(getReachedState(rootState), context, node);
    Precision reducedNewPrecision =
        wrappedReducer.getVariableReducedPrecision(rootPrecision, context);
    if (!bamCache.containsPreciseKey(reducedRootState, reducedNewPrecision, context)) {
      data.createAndRegisterNewReachedSet(reducedRootState, reducedNewPrecision, context);
    }

    // check if the used precision is equal to the new precision
    UnmodifiableReachedSet innerReachedSet =
        data.getReachedSetForInitialState(initialState, blockInitAndExitStates.get(rootState));
    Precision usedPrecision = innerReachedSet.getPrecision(innerReachedSet.getFirstState());
    boolean isNewPrecisionEntry = !usedPrecision.equals(reducedNewPrecision);
    return isNewPrecisionEntry;
  }
}
