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
package org.sosy_lab.cpachecker.cpa.bam;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.bam.BAMCEXSubgraphComputer.BackwardARGState;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Precisions;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

public class ARGSubtreeRemover {

  private final BlockPartitioning partitioning;
  private final BAMDataManager data;
  private final Reducer wrappedReducer;
  private final BAMCache bamCache;
  private final LogManager logger;
  private final Timer removeCachedSubtreeTimer;
  private final boolean doPrecisionRefinementForAllStates;

  public ARGSubtreeRemover(BAMCPA bamCpa, Timer pRemoveCachedSubtreeTimer) {
    this.partitioning = bamCpa.getBlockPartitioning();
    this.data = bamCpa.getData();
    this.wrappedReducer = bamCpa.getReducer();
    this.bamCache = bamCpa.getData().bamCache;
    this.logger = bamCpa.getData().logger;
    this.removeCachedSubtreeTimer = pRemoveCachedSubtreeTimer;
    doPrecisionRefinementForAllStates = bamCpa.doPrecisionRefinementForAllStates();
  }

  void removeSubtree(ARGReachedSet mainReachedSet, ARGPath pPath,
                     ARGState element, List<Precision> pNewPrecisions,
                     List<Predicate<? super Precision>> pNewPrecisionTypes) {

    final ARGState firstState = (ARGState)mainReachedSet.asReachedSet().getFirstState();
    final ARGState lastState = (ARGState)mainReachedSet.asReachedSet().getLastState();

    assert pPath.asStatesList().get(0).getWrappedState() == firstState : "path should start with root state";
    assert Iterables.getLast(pPath.asStatesList()).getWrappedState() == lastState : "path should end with target state";
    assert lastState.isTarget();

    final List<ARGState> relevantCallStates = getRelevantCallStates(pPath.asStatesList(), element);
    assert relevantCallStates.get(0).getWrappedState() == firstState : "root should be relevant";
    assert relevantCallStates.size() >= 1 : "at least the main-function should be open at the target-state";

    Multimap<ARGState, ARGState> neededRemoveCachedSubtreeCalls = LinkedHashMultimap.create();

    //iterate from root to element and remove all subtrees for subgraph calls
    for (int i = 0; i < relevantCallStates.size() - 1; i++) { // ignore root and the last element
      final ARGState pathElement = relevantCallStates.get(i);
      final ARGState nextElement = relevantCallStates.get(i+1);
      neededRemoveCachedSubtreeCalls.put(
              getReachedState(pathElement),
              getReachedState(nextElement));
    }

    if (bamCache.doesAggressiveCaching()) {
      ensureExactCacheHitsOnPath(mainReachedSet, pPath, element, pNewPrecisions, neededRemoveCachedSubtreeCalls);
    }

    final ARGState lastRelevantNode = getReachedState(Iterables.getLast(relevantCallStates));
    final ARGState target = getReachedState(element);
    for (final Entry<ARGState, ARGState> removeCachedSubtreeArguments : neededRemoveCachedSubtreeCalls.entries()) {
      ReachedSet nextReachedSet = data.initialStateToReachedSet.get(removeCachedSubtreeArguments.getValue());
      final List<Precision> newPrecisions;
      final List<Predicate<? super Precision>> newPrecisionTypes;
      if (doPrecisionRefinementForAllStates
          // special option (mostly for testing)
          || (removeCachedSubtreeArguments.getValue() == lastRelevantNode)
          // last iteration, most inner block for refinement
          || (nextReachedSet != null && target.getParents().contains(nextReachedSet.getFirstState()))) {
          // if second state of reached-set is removed, update parent-precision
        newPrecisions = pNewPrecisions;
        newPrecisionTypes = pNewPrecisionTypes;
      } else {
        newPrecisions = Collections.emptyList(); // no update of precision needed
        newPrecisionTypes = Collections.emptyList();
      }
      removeCachedSubtree(removeCachedSubtreeArguments.getKey(), removeCachedSubtreeArguments.getValue(), newPrecisions, newPrecisionTypes);
    }

    removeCachedSubtree(getReachedState(Iterables.getLast(relevantCallStates)),
            getReachedState(element), pNewPrecisions, pNewPrecisionTypes);

    // the main-reachedset contains only the root, exit-states and targets.
    // we assume, that the current refinement was caused by a target-state.
    mainReachedSet.removeSubtree(lastState);
  }

  private ARGState getReachedState(ARGState state) {
    return data.getMostInnerState(((BackwardARGState) state).getARGState());
  }

  /** just remove a state and its subtree from the given reachedSet. */
  static void removeSubtree(ARGReachedSet reachedSet, ARGState argElement) {
    if (BAMTransferRelation.isHeadOfMainFunction(extractLocation(argElement))) {
      reachedSet.removeSubtree((ARGState)reachedSet.asReachedSet().getLastState());
    } else {
      reachedSet.removeSubtree(argElement);
    }
  }

  private void removeCachedSubtree(ARGState rootState, ARGState removeElement,
                                   List<Precision> pNewPrecisions,
                                   List<Predicate<? super Precision>> pPrecisionTypes) {
    assert pNewPrecisions.size() == pPrecisionTypes.size();
    removeCachedSubtreeTimer.start();

    try {

      logger.log(Level.FINER, "Remove cached subtree for", removeElement, " issued with precision", pNewPrecisions);

      if (removeElement.isDestroyed()) {
        logger.log(Level.FINER, "state was destroyed before");
        //apparently, removeElement was removed due to prior deletions
        return;
      }

      CFANode rootNode = extractLocation(rootState);
      Block rootSubtree = partitioning.getBlockForCallNode(rootNode);
      ReachedSet reachedSet = data.initialStateToReachedSet.get(rootState);
      assert reachedSet.contains(removeElement) : "removing state from wrong reachedSet: " + removeElement;
      assert !removeElement.getParents().isEmpty();

      Precision newPrecision = null;
      Predicate<? super Precision> newPrecisionType = null;
      if (!pNewPrecisions.isEmpty()) {
        final Pair<Precision, Predicate<? super Precision>> p = getUpdatedPrecision(
            reachedSet.getPrecision(removeElement), rootSubtree, pNewPrecisions, pPrecisionTypes);
        newPrecision = p.getFirst();
        newPrecisionType = p.getSecond();
      }

      AbstractState reducedRootState = wrappedReducer.getVariableReducedState(rootState, rootSubtree, rootNode);
      Precision reducedRootPrecision = reachedSet.getPrecision(reachedSet.getFirstState());
      bamCache.removeReturnEntry(reducedRootState, reducedRootPrecision, rootSubtree);
      bamCache.removeBlockEntry(reducedRootState, reducedRootPrecision, rootSubtree);

      logger.log(Level.FINEST, "Removing subtree, adding a new cached entry, and removing the former cached entries");

      boolean updateCacheNeeded = removeElement.getParents().contains(reachedSet.getFirstState());

      ARGReachedSet argReachedSet = new ARGReachedSet(reachedSet);
      if (newPrecision == null) {
        removeSubtree(argReachedSet, removeElement);
      } else {
        argReachedSet.removeSubtree(removeElement, newPrecision, newPrecisionType);
      }

      if (updateCacheNeeded && newPrecision != null) {
        logger.log(Level.FINER, "updating cache");
        bamCache.updatePrecisionForEntry(reducedRootState, reducedRootPrecision, rootSubtree, newPrecision);
      }

    } finally {
      removeCachedSubtreeTimer.stop();
    }
  }

  /**
   * This method builds the updated precision for the refinement.
   * Normally, a list of sub-precisions would be used and the reached-set updates it in its own.
   * For BAM we build the correct 'complete' precision, because we have to reduce it for the current block.
   * Thus instead of a list, we only have one top-level precision-object that wraps other updated precisions.
   */
  private Pair<Precision, Predicate<? super Precision>> getUpdatedPrecision(
      Precision removePrecision, final Block context,
      final List<Precision> precisions, final List<Predicate<? super Precision>> precisionTypes) {
    assert precisions.size() == precisionTypes.size() && !precisions.isEmpty();

    for (int i = 0; i < precisions.size(); i++) {
      removePrecision = Precisions.replaceByType(removePrecision, precisions.get(i), precisionTypes.get(i));
    }

    final Precision reducedPrecision = wrappedReducer.getVariableReducedPrecision(removePrecision, context);

    return Pair.<Precision, Predicate<? super Precision>>of(
        reducedPrecision, Predicates.instanceOf(reducedPrecision.getClass()));
  }

  /** returns only those states, where a block starts that is 'open' at the cutState. */
  private List<ARGState> getRelevantCallStates(List<ARGState> path, ARGState bamCutState) {
    final Deque<ARGState> openCallStates = new ArrayDeque<>();
    for (final ARGState bamState : path) {

      final ARGState state = ((BackwardARGState) bamState).getARGState();

      // ASSUMPTION: there can be several block-exits at once per location, but only one block-entry per location.

      // we use a loop here, because a return-node can be the exit of several blocks at once.
      ARGState tmp = state;
      while (data.expandedStateToReducedState.containsKey(tmp) && bamCutState != bamState) {
        assert partitioning.isReturnNode(extractLocation(tmp)) : "the mapping of expanded to reduced state should only exist for block-return-locations";
        // we are leaving a block, remove the start-state from the stack.
        tmp = (ARGState) data.expandedStateToReducedState.get(tmp);
        openCallStates.removeLast();
        // INFO:
        // if we leave several blocks at once, we leave the blocks in reverse order,
        // because the call-state of the most outer block is checked first.
        // We ignore this here, because we just need the 'number' of block-exits.
      }

      if (data.initialStateToReachedSet.containsKey(state)) {
        assert partitioning.isCallNode(extractLocation(state)) : "the mapping of initial state to reached-set should only exist for block-start-locations";
        // we start a new sub-reached-set, add state as start-state of a (possibly) open block.
        // if we are at lastState, we do not want to enter the block
        openCallStates.addLast(bamState);
      }

      if (bamCutState == bamState) {
        // TODO:
        // current solution: when we found the cutState, we only enter new blocks, but never leave one.
        // maybe better solution: do not enter or leave a block, when we found the cutState.
        break;
      }
    }

    return new ArrayList<>(openCallStates);
  }

  /** there might be some "imprecise" cache entries used along the path.
   * We remove all of them and create the "precise" entry for re-exploration.
   * We only update those blocks, where a nested block is imprecise. */
  private void ensureExactCacheHitsOnPath(ARGReachedSet mainReachedSet, ARGPath pPath, final ARGState pElement,
                                          List<Precision> pNewPrecisions, Multimap<ARGState, ARGState> neededRemoveCachedSubtreeCalls) {
    Map<ARGState, UnmodifiableReachedSet> pathElementToOuterReachedSet = getReachedSetMapping(
        pPath, mainReachedSet.asReachedSet());

    // we start at the cutState, because until then the precision seems to be sufficient.
    Iterator<ARGState> remainingPathElements = pPath.asStatesList().listIterator(pPath.asStatesList().indexOf(pElement));
    assert remainingPathElements.hasNext() : "cutState should not be last state";

    // we start with the cutState and iterate over the rest of the path
    while (remainingPathElements.hasNext()) {
        ARGState currentElement = remainingPathElements.next();
        if (data.initialStateToReachedSet.containsKey(currentElement.getWrappedState())) {
          ARGState currentReachedState = getReachedState(currentElement);
          CFANode node = extractLocation(currentReachedState);
          Block currentBlock = partitioning.getBlockForCallNode(node);
          AbstractState reducedState = wrappedReducer.getVariableReducedState(currentReachedState, currentBlock, node);
          removeUnpreciseCacheEntriesOnPath(currentElement, reducedState, pNewPrecisions, currentBlock,
                  remainingPathElements, pathElementToOuterReachedSet,
                  neededRemoveCachedSubtreeCalls);
        }
    }
  }

  /** get a mapping from states to their reached-set,
   * such that "reached.contains(state)" is satisfied.
   * We limit the mapping to block-call-states only,
   * because this is sufficient for further processing. */
  private Map<ARGState, UnmodifiableReachedSet> getReachedSetMapping(ARGPath path,
                                                                   UnmodifiableReachedSet mainReachedSet) {

    Map<ARGState, UnmodifiableReachedSet> pathElementToOuterReachedSet = new HashMap<>();
    Deque<UnmodifiableReachedSet> openReachedSets = new ArrayDeque<>();
    openReachedSets.addLast(mainReachedSet);

    for (ARGState pathState : path.asStatesList()) {
      ARGState state = ((BackwardARGState) pathState).getARGState();

      // we use a loop here, because a return-node can be the exit of several blocks at once.
      // we have to handle returnNodes before entryNodes, because some nodes can be both,
      // and the transferRelation also handles entryNodes as first case.
      ARGState tmp = state;
      while (data.expandedStateToReducedState.containsKey(tmp)) {
        tmp = (ARGState)data.expandedStateToReducedState.get(tmp);
        openReachedSets.removeLast();
        // INFO:
        // if we leave several blocks at once, we leave the blocks in reverse order,
        // because the call-state of the most outer block is checked first.
        // We ignore this here, because we just need the 'number' of block-exits.
      }

      // this part comes after handling returnStates --> returnStates from path are part of the outer-block-reachedSet
      if (data.initialStateToReachedSet.containsKey(state)) {
        // limit mapping to call-states only -> less memory-overhead
        pathElementToOuterReachedSet.put(pathState, openReachedSets.getLast());
        // the block can be equal, if this is a loop-block.
        openReachedSets.addLast(data.initialStateToReachedSet.get(state));
      }
    }

    return pathElementToOuterReachedSet;
  }

  private boolean removeUnpreciseCacheEntriesOnPath(ARGState rootState, AbstractState reducedRootState,
                                                    List<Precision> pNewPrecisions, Block rootBlock,
                                                    Iterator<ARGState> remainingPathElements,
                                                    Map<ARGState, UnmodifiableReachedSet> pathElementToOuterReachedSet,
                                                    Multimap<ARGState, ARGState> neededRemoveCachedSubtreeCalls) {
    UnmodifiableReachedSet outerReachedSet = Preconditions.checkNotNull(pathElementToOuterReachedSet.get(rootState));

    boolean isNewPrecisionEntry = createNewPreciseEntry(
        (ARGState) rootState.getWrappedState(),
        reducedRootState, pNewPrecisions, rootBlock, outerReachedSet);

    //fine, this block will not lead to any problems anymore, but maybe inner blocks will?
    //-> check other (inner) blocks on path
    boolean foundInnerUnpreciseEntries = false;
    while (remainingPathElements.hasNext()) {
      ARGState currentElement = remainingPathElements.next();

      if (data.initialStateToReachedSet.containsKey(currentElement.getWrappedState())) {
        ARGState currentReachedState = getReachedState(currentElement);
        CFANode node = extractLocation(currentReachedState);
        Block currentBlock = partitioning.getBlockForCallNode(node);
        AbstractState reducedState = wrappedReducer.getVariableReducedState(currentReachedState, currentBlock, node);

        boolean removedUnpreciseInnerBlock =
                removeUnpreciseCacheEntriesOnPath(currentElement, reducedState, pNewPrecisions, currentBlock,
                        remainingPathElements, pathElementToOuterReachedSet, neededRemoveCachedSubtreeCalls);
        if (removedUnpreciseInnerBlock) {
          //ok we indeed found an inner block that was unprecise
          if (isNewPrecisionEntry && !foundInnerUnpreciseEntries) {
            //if we are in a reached set that already uses the new precision and this is the first such entry we have to remove the subtree starting from currentElement in the rootReachedSet
            neededRemoveCachedSubtreeCalls.put(getReachedState(rootState), currentReachedState);
            foundInnerUnpreciseEntries = true;
          }
        }
      }

      if (data.expandedStateToReducedState.containsKey(currentElement.getWrappedState())) {
        //our block ended. Leave..
        return foundInnerUnpreciseEntries || !isNewPrecisionEntry;
      }
    }

    return foundInnerUnpreciseEntries || !isNewPrecisionEntry;
  }

  /** This method creates a new precise entry if necessary, and returns whether the used entry needs a new precision. */
  private boolean createNewPreciseEntry(final ARGState initialState, final AbstractState reducedRootState,
      final List<Precision> pNewPrecisions, final Block context, final UnmodifiableReachedSet outerReachedSet) {

    // create updated precision
    Precision rootPrecision = outerReachedSet.getPrecision(initialState);
    for (Precision pNewPrecision : pNewPrecisions) {
      rootPrecision = Precisions.replaceByType(rootPrecision, pNewPrecision, Predicates.instanceOf(pNewPrecision.getClass()));
    }

    // reduce the new precision and add a precise key for the new precision if needed
    Precision reducedNewPrecision = wrappedReducer.getVariableReducedPrecision(rootPrecision, context);
    if (!bamCache.containsPreciseKey(reducedRootState, reducedNewPrecision, context)) {
      ReachedSet reachedSet = data.createInitialReachedSet(reducedRootState, reducedNewPrecision);
      bamCache.put(reducedRootState, reducedNewPrecision, context, reachedSet);
    }

    // check if the used precision is equal to the new precision
    UnmodifiableReachedSet innerReachedSet = data.initialStateToReachedSet.get(initialState);
    Precision usedPrecision = innerReachedSet.getPrecision(innerReachedSet.getFirstState());
    boolean isNewPrecisionEntry = usedPrecision.equals(reducedNewPrecision);
    return isNewPrecisionEntry;
  }
}
