/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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

import com.google.common.base.Predicate;
import com.google.common.collect.Multimap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;


public class MultipleARGSubtreeRemover extends ARGSubtreeRemover {

  /* These states are linked with other sets,
   * so these states should be removed with breaking these relations
   */
  private Set<ARGState> setsTotallyIntegratedInCache = new HashSet<>();

  /* These states are saved only in the main BAM cache,
   * so these states should be removed only from this one
   */
  private Set<ReachedSet> setsOnlyLocatedInCache = new HashSet<>();
  private Multimap<AbstractState, AbstractState> reducedToExpand;
  private Map<ARGState, Set<ARGState>> cachedSubtreesToRemove = new HashMap<>();
  private Multimap<String, ReachedSet> functionToRootState;
  private BAMTransferRelation transfer;

  public MultipleARGSubtreeRemover(BAMCPA bamcpa,
      StatTimer removeCachedSubtreeTimer, Multimap<String, ReachedSet> map,
      Multimap<AbstractState, AbstractState> map2,
      BAMTransferRelation pTransfer) {
    super(bamcpa, removeCachedSubtreeTimer);
    functionToRootState = map;
    reducedToExpand = map2;
    transfer = pTransfer;
  }

  //This method is a preparation for cleaning the reached sets without removing
  @Override
  protected void removeCachedSubtree(ARGState rootState, ARGState removeElement,
      List<Precision> pNewPrecisions,
      List<Predicate<? super Precision>> pPrecisionTypes) {
    removeCachedSubtreeTimer.start();

    Set<ARGState> set;
    Set<ARGState> toDelete = new HashSet<>();
    if (cachedSubtreesToRemove.containsKey(rootState)) {
      set = cachedSubtreesToRemove.get(rootState);
      for (ARGState state : set) {
        if (state.getSubgraph().contains(removeElement)) {
          return;
        } else if (removeElement.getSubgraph().contains(state)){
          toDelete.add(state);
        }
      }
      for (ARGState state : toDelete) {
        set.remove(state);
      }
    } else {
      set = new HashSet<>();
      cachedSubtreesToRemove.put(rootState, set);
    }
    set.add(removeElement);
    removeCachedSubtreeTimer.stop();
  }

  @Override
  protected void handleEndOfThePath(ARGPath pPath, ARGState affectedState,
      Map<ARGState, ARGState> pSubgraphStatesToReachedState) {
    List<ARGState> tail = trimPath(pPath, affectedState);

    List<ARGState> callNodes = getCallNodes(tail);
    setsTotallyIntegratedInCache.addAll(callNodes);
  }

  /** remove all states before pState from path */
  private static List<ARGState> trimPath(final ARGPath pPath, final ARGState pState) {
    boolean meet = false;
    final List<ARGState> result = new ArrayList<>();
    for (ARGState state : pPath.asStatesList()) {
      if (state.equals(pState)) { meet = true; }
      if (meet) {
        result.add(state);
      }
    }
    if (meet) {
      return result;
    } else {
      throw new IllegalArgumentException("State " + pState + " could not be found in path " + pPath + ".");
    }
  }

  public void addStateForRemoving(ARGState state) {
    //This not the state to remove. Now we should find all such states using the map
    LinkedList<String> toProcess = new LinkedList<>();

    String functionName = AbstractStates.extractLocation(state).getFunctionName();
    Collection<ReachedSet> reachedSets;
    Collection<AbstractState> callers;
    toProcess.add(functionName);
    while (!toProcess.isEmpty()) {
      functionName = toProcess.pollFirst();
      reachedSets = functionToRootState.get(functionName);
      if (reachedSets != null) {
        for (ReachedSet set : reachedSets) {
          AbstractState reducedState = set.getFirstState();
          if (reducedToExpand.containsKey(reducedState)) {
            callers = reducedToExpand.get(reducedState);
            for (AbstractState caller : callers) {
              setsTotallyIntegratedInCache.add((ARGState)caller);
              CallstackState previousState = AbstractStates.extractStateByType(caller, CallstackState.class).getPreviousState();
              if (previousState == null) {
                //main function
                continue;
              }
              toProcess.add(previousState.getCurrentFunction());
            }
            reducedToExpand.removeAll(reducedState);
          } else {
            setsOnlyLocatedInCache.add(set);
          }
        }
        functionToRootState.removeAll(functionName);
      }
    }
  }

  private List<ARGState> getCallNodes(List<ARGState> path) {
    Deque<ARGState> openCallElements = new ArrayDeque<>();

    for (final ARGState pathState : path) {
      CFANode node = extractLocation(pathState);

      if (partitioning.isCallNode(node)) {
        // we have a callnode, but current block is wrong, add new currentBlock and state as relevant.
        // the block can be equal, if this is a loop-block.
        openCallElements.addLast(pathState);
      }
    }

    return new ArrayList<>(openCallElements);
  }

  public void cleanCaches() {
    removeCachedSubtreeTimer.start();

    for (ARGState rootState : setsTotallyIntegratedInCache) {
      cleanReachedSet(data.getReachedSetForInitialState(rootState));
    }
    for (ReachedSet reachedSet : setsOnlyLocatedInCache) {
      cleanReachedSet(reachedSet);
    }
    transfer.removeStateFromAuxiliaryCaches(setsTotallyIntegratedInCache);
    setsTotallyIntegratedInCache.clear();
    removeCachedSubtreeTimer.stop();
  }

  private void cleanReachedSet(ReachedSet reachedSet) {
    AbstractState reducedRootState = reachedSet.getFirstState();
    CFANode rootNode = extractLocation(reducedRootState);
    Block rootSubtree = partitioning.getBlockForCallNode(rootNode);
    Precision reducedRootPrecision = reachedSet.getPrecision(reducedRootState);
    bamCache.remove(reducedRootState, reducedRootPrecision, rootSubtree);
  }
}
