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
package org.sosy_lab.cpachecker.cpa.usage.refinement;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.bam.BAMMultipleCEXSubgraphComputer;
import org.sosy_lab.cpachecker.cpa.bam.BAMSubgraphComputer.BackwardARGState;
import org.sosy_lab.cpachecker.cpa.bam.BAMSubgraphComputer.MissingBlockException;
import org.sosy_lab.cpachecker.cpa.bam.BAMTransferRelation;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.util.Pair;


public class PathPairIterator extends
    GenericIterator<Pair<UsageInfo, UsageInfo>, Pair<ExtendedARGPath, ExtendedARGPath>> {

  private final Set<List<Integer>> refinedStates = new HashSet<>();
  private final BAMTransferRelation transfer;
  private final Multimap<AbstractState, AbstractState> fromReducedToExpand;
  private BAMMultipleCEXSubgraphComputer subgraphComputer;

  //Statistics
  private Timer computingPath = new Timer();
  private Timer additionTimer = new Timer();
  private int numberOfPathCalculated = 0;
  private int successfulAdditionChecks = 0;
  //private int numberOfrepeatedPaths = 0;

  private Map<AbstractState, Iterator<ARGState>> toCallerStatesIterator = new HashMap<>();
  private Map<UsageInfo, BackwardARGState> previousForkForUsage = new IdentityHashMap<>();

  private Map<UsageInfo, List<ExtendedARGPath>> computedPathsForUsage = new IdentityHashMap<>();
  private Map<UsageInfo, Iterator<ExtendedARGPath>> currentIterators = new IdentityHashMap<>();

  private final Function<ARGState, Integer> GET_ORIGIN_STATE_NUMBERS = s -> ((BackwardARGState)s).getARGState().getStateId();

  private final static Iterator<ExtendedARGPath> DUMMY_INTERATOR = new HashSet<ExtendedARGPath>().iterator();

  //internal state
  private ExtendedARGPath firstPath = null;

  public PathPairIterator(ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>> pWrapper,
      BAMTransferRelation bamTransfer) {
    super(pWrapper);
    transfer = bamTransfer;
    fromReducedToExpand = transfer.getMapFromReducedToExpand();

  }

  @Override
  protected void init(Pair<UsageInfo, UsageInfo> pInput) {
    firstPath = null;
    //subgraph computer need partitioning, which is not built at creation.
    //Thus, we move the creation of subgraphcomputer here
    subgraphComputer = transfer.createBAMMultipleSubgraphComputer();
  }

  @Override
  protected Pair<ExtendedARGPath, ExtendedARGPath> getNext(Pair<UsageInfo, UsageInfo> pInput) {
    UsageInfo firstUsage, secondUsage;
    firstUsage = pInput.getFirst();
    secondUsage = pInput.getSecond();

    if (firstPath == null) {
      //First time or it was unreachable last time
      firstPath = getNextPath(firstUsage);
      if (firstPath == null) {
        checkAreUsagesUnreachable(pInput);
        return null;
      }
    }

    ExtendedARGPath secondPath = getNextPath(secondUsage);
    if (secondPath == null) {
      //Reset the iterator
      currentIterators.remove(secondUsage);
      //And move shift the first one
      firstPath = getNextPath(firstUsage);
      if (firstPath == null) {
        checkAreUsagesUnreachable(pInput);
        return null;
      }
      secondPath = getNextPath(secondUsage);
      if (secondPath == null) {
        checkAreUsagesUnreachable(pInput);
        return null;
      }
    }
    return Pair.of(firstPath, secondPath);
  }

  private void checkAreUsagesUnreachable(Pair<UsageInfo, UsageInfo> pInput) {
    UsageInfo firstUsage = pInput.getFirst();
    UsageInfo secondUsage = pInput.getSecond();

    checkIsUsageUnreachable(firstUsage);
    checkIsUsageUnreachable(secondUsage);
  }

  private void checkIsUsageUnreachable(UsageInfo pInput) {
    if (!computedPathsForUsage.containsKey(pInput) || computedPathsForUsage.get(pInput).size() == 0) {
      pInput.setAsUnreachable();
    }
  }

  @Override
  protected void finalize(Pair<ExtendedARGPath, ExtendedARGPath> pathPair, RefinementResult wrapperResult) {
    ExtendedARGPath firstExtendedPath, secondExtendedPath;

    firstExtendedPath = pathPair.getFirst();
    secondExtendedPath = pathPair.getSecond();

    Object predicateInfo = wrapperResult.getInfo(PredicateRefinerAdapter.class);
    if (predicateInfo != null && predicateInfo instanceof List) {
      @SuppressWarnings("unchecked")
      List<ARGState> affectedStates = (List<ARGState>)predicateInfo;
      //affectedStates may be null, if the path was refined somewhen before

      //A feature of GenericSinglePathRefiner: if one path is false, the second one is not refined
      if (firstExtendedPath.isUnreachable()) {
        //This one is false
        handleAffectedStates(affectedStates, firstExtendedPath);
        handleAffectedStates(null, secondExtendedPath);
        //Need to clean first path
        firstPath = null;
      } else {
        //The second one must be
        Preconditions.checkArgument(secondExtendedPath.isUnreachable(), "Either the first path, or the second one must be unreachable here");
        handleAffectedStates(null, firstExtendedPath);
        handleAffectedStates(affectedStates, secondExtendedPath);
      }
    } else {
      handleAffectedStates(null, firstExtendedPath);
      handleAffectedStates(null, secondExtendedPath);
      if (firstPath.isUnreachable()){
        firstPath = null;
      }
    }
    updateTheComputedSet(firstExtendedPath);
    updateTheComputedSet(secondExtendedPath);
  }

  @Override
  public void printDetailedStatistics(PrintStream pOut) {
    pOut.println("--PathPairIterator--");
    pOut.println("--Timer for path computing:          " + computingPath);
    pOut.println("--Timer for addition checks:         " + additionTimer);
    pOut.println("Number of path calculated:           " + numberOfPathCalculated);
    pOut.println("Number of successful Addition Checks:" + successfulAdditionChecks);
  }

  @Override
  public Object handleFinishSignal(Class<? extends RefinementInterface> callerClass) {
    if (callerClass.equals(IdentifierIterator.class)) {
      //Refinement iteration finishes
      refinedStates.clear();
    } else if (callerClass.equals(PointIterator.class)) {
      toCallerStatesIterator.clear();
      previousForkForUsage.clear();
      currentIterators.clear();
      computedPathsForUsage.clear();
    }
    return null;
  }

  private void updateTheComputedSet(ExtendedARGPath path) {
    UsageInfo usage = path.getUsageInfo();

    boolean alreadyComputed = computedPathsForUsage.containsKey(usage);

    if (!path.isUnreachable()) {
      List<ExtendedARGPath> alreadyComputedPaths;
      if (!alreadyComputed) {
        alreadyComputedPaths = new LinkedList<>();
      } else {
        alreadyComputedPaths = computedPathsForUsage.get(usage);
      }
      if (!alreadyComputedPaths.contains(path)) {
        alreadyComputedPaths.add(path);
      }
    } else if (path.isUnreachable() && alreadyComputed) {
      List<ExtendedARGPath> alreadyComputedPaths = computedPathsForUsage.get(usage);
      if (alreadyComputedPaths.contains(path)) {
        //We should reset iterator to avoid ConcurrentModificationException
        alreadyComputedPaths.remove(path);
      }
    }
  }

  private ExtendedARGPath getNextPath(UsageInfo info) {
    ARGPath currentPath;

    //Start from already computed set (it is partially refined)
    Iterator<ExtendedARGPath> iterator = currentIterators.get(info);
    if (iterator == null && computedPathsForUsage.containsKey(info)) {
      //first call
      iterator = computedPathsForUsage.get(info).iterator();
      currentIterators.put(info, iterator);
    }
    if (iterator != null && iterator != DUMMY_INTERATOR && iterator.hasNext()) {
      return iterator.next();
    } else if (iterator != DUMMY_INTERATOR) {
      //We handle all previously found paths, disable the iterator
      currentIterators.put(info, DUMMY_INTERATOR);
    }

    computingPath.start();
    //try to compute more paths
    if (!previousForkForUsage.containsKey(info)) {
      //The first time, we have no path to iterate
      BackwardARGState newTarget = new BackwardARGState((ARGState)info.getKeyState());
      currentPath = computePath(newTarget);
      //currentPath may become null if it goes through repeated (refined) states
    } else {
      currentPath = computeNextPath(previousForkForUsage.get(info));
    }
    computingPath.stop();

    if (currentPath == null) {
      //no path to iterate, finishing
      return null;
    }
    //Not add result now, only after refinement
    return new ExtendedARGPath(currentPath, info);
  }

  private void handleAffectedStates(List<ARGState> affectedStates, ExtendedARGPath path) {
    ARGState nextStart;
    if (affectedStates != null) {
      List<Integer>changedStateNumbers = from(affectedStates).transform(GET_ORIGIN_STATE_NUMBERS).toList();
      refinedStates.add(changedStateNumbers);

      nextStart = affectedStates.get(affectedStates.size() - 1);
    } else {
      nextStart = path.getFirstState().getChildren().iterator().next();
    }
    previousForkForUsage.put(path.getUsageInfo(), (BackwardARGState)nextStart);
  }

  //lastAffectedState is Backward!
  private ARGPath computeNextPath(BackwardARGState lastAffectedState) {
    assert lastAffectedState != null;

    ARGState nextParent = null;
    BackwardARGState previousCaller, childOfForkState = lastAffectedState;
    do {
      //This is a backward state, which displays the following state after real reduced state, which we want to found
      childOfForkState = findPreviousFork(childOfForkState);

      if (childOfForkState == null) {
        return null;
      }

      //parent = forkState.getParents().iterator().next();
     // assert parent != null;
      ARGState realChildOfForkState = childOfForkState.getARGState();
      assert realChildOfForkState.getParents().size() == 1;
      ARGState forkState = realChildOfForkState.getParents().iterator().next();
      //Clone is necessary, make tree set for determinism
      Set<ARGState> callerStates = Sets.newTreeSet();
      for (AbstractState state : fromReducedToExpand.get(forkState)) {
        callerStates.add((ARGState)state);
      }
      previousCaller = (BackwardARGState) childOfForkState.getParents().iterator().next();

      assert callerStates != null;

      Iterator<ARGState> iterator;
      //It is important to put a backward state in map, because we can find the same real state during exploration
      //but for it a new backward state will be created
      //Disagree, try to put a real state
      if (toCallerStatesIterator.containsKey(realChildOfForkState)) {
        //Means we have already handled this state, just get the next one
        iterator = toCallerStatesIterator.get(realChildOfForkState);
      } else {
        //We get this fork the second time (the first one was from path computer)
        //Found the caller, we have explored the first time
        ARGState realPreviousCaller = previousCaller.getARGState();
        assert callerStates.remove(realPreviousCaller);
        iterator = callerStates.iterator();
        toCallerStatesIterator.put(realChildOfForkState, iterator);
      }

      if (iterator.hasNext()) {
        nextParent = iterator.next();
      } else {
        //We need to find the next fork
        //Do not change the fork state and start exploration from this one
      }
    } while (nextParent == null);

    BackwardARGState newNextParent = new BackwardARGState(nextParent);
    //Because of cached paths, we cannot change the part of it
    ARGState clonedChildOfForkState = cloneTheRestOfPath(childOfForkState);
    clonedChildOfForkState.addParent(newNextParent);
    return computePath(newNextParent);
  }

  private ARGState cloneTheRestOfPath(BackwardARGState pChildOfForkState) {
    BackwardARGState currentState = pChildOfForkState;
    ARGState originState = currentState.getARGState();
    BackwardARGState previousState = new BackwardARGState(originState), clonedState;
    ARGState result = previousState;

    while (!currentState.getChildren().isEmpty()) {
      assert currentState.getChildren().size() == 1;
      currentState = (BackwardARGState) currentState.getChildren().iterator().next();
      originState = currentState.getARGState();
      clonedState = new BackwardARGState(originState);
      clonedState.addParent(previousState);
      previousState = clonedState;
    }
    return result;
  }

  /**
   * Due to special structure of ARGPath,
   * the real fork state (reduced entry) is not included into it.
   * We need to get it.
   *
   * @param parent
   * @return
   */
  private BackwardARGState findPreviousFork(BackwardARGState parent) {
    List<BackwardARGState> potentialForkStates = new LinkedList<>();
    Map<ARGState, ARGState> exitStateToEntryState = new TreeMap<>();
    BackwardARGState currentState = parent;
    ARGState realState = currentState.getARGState();
    assert parent.getParents().size() == 1;
    BackwardARGState currentParent = (BackwardARGState)parent.getParents().iterator().next();
    ARGState realParent = currentParent.getARGState();
    while (currentState.getChildren().size() > 0) {

      assert currentState.getChildren().size() == 1;
      currentState = (BackwardARGState) currentState.getChildren().iterator().next();
      realState = currentState.getARGState();

      //No matter which parent to take - interesting one is single anyway
      realParent = realState.getParents().iterator().next();

      //Check if it is an exit state, we are waiting
      //Attention! Recursion is not supported here!
      if (exitStateToEntryState.containsKey(realParent)) {
        //Due to complicated structure of path we saved an expanded exit state and it isn't contained in the path,
        //so, we look for its parent
        ARGState entryState = exitStateToEntryState.get(realParent);
        //remove all child in cache
        for (ARGState displayedChild : entryState.getChildren()) {
          exitStateToEntryState.remove(displayedChild);
        }
        potentialForkStates.remove(entryState);
      }

      if (fromReducedToExpand.containsKey(realParent) &&
          fromReducedToExpand.get(realParent).size() > 1) {

        assert realParent.getParents().size() == 0;
        assert fromReducedToExpand.get(realParent).size() > 1;

        //Now we should check, that there is no corresponding exit state in the path only in this case this is a real fork
        parent = (BackwardARGState) currentState.getParents().iterator().next();
        ARGState displayedParent = parent.getARGState();
        //We may have several children, so add all of them
        for (ARGState displayedChild : displayedParent.getChildren()) {
          exitStateToEntryState.put(displayedChild, currentState);
        }
        //Save child and if we meet it, we remove the parent as not a fork

        potentialForkStates.add(currentState);
      }
    }

    if (potentialForkStates.isEmpty()) {
      return null;
    } else {
      return potentialForkStates.get(0);
    }
  }

  ARGPath computePath(BackwardARGState pLastElement) {
    assert (pLastElement != null && !pLastElement.isDestroyed());
      //we delete this state from other unsafe

    try {
      ARGState rootOfSubgraph = subgraphComputer.findPath(pLastElement, refinedStates);
      assert (rootOfSubgraph != null);
      if (rootOfSubgraph == BAMMultipleCEXSubgraphComputer.DUMMY_STATE_FOR_REPEATED_STATE) {
        return null;
      }
      ARGPath result = ARGUtils.getRandomPath(rootOfSubgraph);
      additionTimer.start();
      List<Integer> stateNumbers = from(result.asStatesList()).transform(GET_ORIGIN_STATE_NUMBERS).toList();
      for (List<Integer> previousStates : refinedStates) {
        if (stateNumbers.containsAll(previousStates)) {
          successfulAdditionChecks++;
        }
      }
      additionTimer.stop();
      numberOfPathCalculated++;
      return result;
    } catch (MissingBlockException e) {
      return null;
    } catch (InterruptedException e) {
      return null;
    }

  }
}
