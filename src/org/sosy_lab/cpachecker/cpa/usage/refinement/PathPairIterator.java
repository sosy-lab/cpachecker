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
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.bam.BAMMultipleCEXSubgraphComputer;
import org.sosy_lab.cpachecker.cpa.bam.BAMSubgraphComputer.BackwardARGState;
import org.sosy_lab.cpachecker.cpa.bam.BAMSubgraphComputer.MissingBlockException;
import org.sosy_lab.cpachecker.cpa.bam.BAMTransferRelation;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.cpa.usage.refinement.RefinementBlockFactory.PathEquation;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;


public class PathPairIterator extends
    GenericIterator<Pair<UsageInfo, UsageInfo>, Pair<ExtendedARGPath, ExtendedARGPath>> {

  private final Set<List<Integer>> refinedStates = new HashSet<>();
  private final BAMTransferRelation transfer;
  private final Multimap<AbstractState, AbstractState> fromReducedToExpand;
  private BAMMultipleCEXSubgraphComputer subgraphComputer;

  //Statistics
  private StatTimer computingPath = new StatTimer("Time for path computing");
  private StatTimer additionTimerCheck = new StatTimer("Time for addition checks");
  private StatCounter numberOfPathCalculated = new StatCounter("Number of path calculated");
  private StatCounter numberOfPathFinished = new StatCounter("Number of new path calculated");
  private StatCounter numberOfRepeatedConstructedPaths = new StatCounter("Number of repeated path computed");
  //private int numberOfrepeatedPaths = 0;

  private Map<AbstractState, Iterator<ARGState>> toCallerStatesIterator = new HashMap<>();
  private Map<UsageInfo, BackwardARGState> previousForkForUsage = new IdentityHashMap<>();

  private Map<UsageInfo, List<ExtendedARGPath>> computedPathsForUsage = new IdentityHashMap<>();
  private Map<UsageInfo, Iterator<ExtendedARGPath>> currentIterators = new IdentityHashMap<>();

  private final Function<ARGState, Integer> idExtractor;

  //internal state
  private ExtendedARGPath firstPath = null;

  public PathPairIterator(ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>> pWrapper,
      BAMTransferRelation bamTransfer, PathEquation type) throws InvalidConfigurationException {
    super(pWrapper);
    transfer = bamTransfer;
    fromReducedToExpand = transfer.getMapFromReducedToExpand();

    switch (type) {
      case ARGStateId:
        idExtractor = ARGState::getStateId;
        break;

      case CFANodeId:
        idExtractor = s -> AbstractStates.extractLocation(s).getNodeNumber();
        break;

      default:
        throw new InvalidConfigurationException("Unexpexted type " + type);

    }
  }

  @Override
  protected void init(Pair<UsageInfo, UsageInfo> pInput) {
    firstPath = null;
    //subgraph computer need partitioning, which is not built at creation.
    //Thus, we move the creation of subgraphcomputer here
    subgraphComputer = transfer.createBAMMultipleSubgraphComputer(idExtractor);
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
  public void printDetailedStatistics(StatisticsWriter pOut) {
    pOut.spacer()
      .put(computingPath)
      .put(additionTimerCheck)
      .put(numberOfPathCalculated)
      .put(numberOfPathFinished)
      .put(numberOfRepeatedConstructedPaths);
  }

  @Override
  public void handleFinishSignal(Class<? extends RefinementInterface> callerClass) {
    if (callerClass.equals(IdentifierIterator.class)) {
      //Refinement iteration finishes
      refinedStates.clear();
    } else if (callerClass.equals(PointIterator.class)) {
      toCallerStatesIterator.clear();
      previousForkForUsage.clear();
      currentIterators.clear();
      computedPathsForUsage.clear();
    }
  }

  private void updateTheComputedSet(ExtendedARGPath path) {
    UsageInfo usage = path.getUsageInfo();

    boolean alreadyComputed = computedPathsForUsage.containsKey(usage);

    if (!path.isUnreachable()) {
      List<ExtendedARGPath> alreadyComputedPaths;
      if (!alreadyComputed) {
        alreadyComputedPaths = new LinkedList<>();
        computedPathsForUsage.put(usage, alreadyComputedPaths);
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

    if (iterator != null && iterator.hasNext()) {
      return iterator.next();
    }

    computingPath.start();
    //try to compute more paths
    if (!previousForkForUsage.containsKey(info)) {
      //The first time, we have no path to iterate
      BackwardARGState newTarget = new BackwardARGState((ARGState)info.getKeyState());
      currentPath = restorePathFrom(newTarget);
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
      List<Integer>changedStateNumbers = from(affectedStates).transform(idExtractor).toList();
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
    BackwardARGState childOfForkState = null;
    ARGPath newPath = null;

    List<BackwardARGState> potentialForkStates = findNextForksInTail(lastAffectedState);
    if (potentialForkStates.isEmpty()) {
      return null;
    }
    Iterator<BackwardARGState> forkIterator = potentialForkStates.iterator();

    do {
      //Determine next branching point
      nextParent = null;
      while (nextParent == null && forkIterator.hasNext()) {
        //This is a backward state, which displays the following state after real reduced state, which we want to found
        childOfForkState = forkIterator.next();

        nextParent = findNextBranchingParent(childOfForkState);
      }

      if (nextParent == null) {
        return null;
      }

      BackwardARGState clonedNextParent = new BackwardARGState(nextParent);
      //Because of cached paths, we cannot change the part of it
      ARGState rootOfTheClonedPart = cloneTheRestOfPath(childOfForkState);
      rootOfTheClonedPart.addParent(clonedNextParent);
      //Restore the new path from branching point
      newPath = restorePathFrom(clonedNextParent);

    } while (newPath != null);

    return newPath;
  }

  private ARGState cloneTheRestOfPath(BackwardARGState pChildOfForkState) {
    BackwardARGState currentState = pChildOfForkState;
    ARGState originState = currentState.getARGState();
    BackwardARGState previousState = new BackwardARGState(originState), clonedState;
    ARGState root = previousState;

    while (!currentState.getChildren().isEmpty()) {
      assert currentState.getChildren().size() == 1;
      currentState = getNextStateOnPath(currentState);
      originState = currentState.getARGState();
      clonedState = new BackwardARGState(originState);
      clonedState.addParent(previousState);
      previousState = clonedState;
    }
    return root;
  }

  /** Finds the parentState (in ARG), which corresponds to the child (in the path)
   *
   * @param forkChildInPath child, which has more than one parents, which are not yet explored
   * @return found parent state
   */

  private ARGState findNextBranchingParent(BackwardARGState forkChildInPath) {

    ARGState forkChildInARG = forkChildInPath.getARGState();
    assert forkChildInARG.getParents().size() == 1;
    ARGState forkState = forkChildInARG.getParents().iterator().next();
    //Clone is necessary, make tree set for determinism
    Set<ARGState> callerStates = Sets.newTreeSet();

    from(fromReducedToExpand.get(forkState))
      .transform(s -> (ARGState) s)
      .forEach(callerStates::add);

    Iterator<ARGState> iterator;
    //It is important to put a backward state in map, because we can find the same real state during exploration
    //but for it a new backward state will be created
    //Disagree, try to put a real state
    if (toCallerStatesIterator.containsKey(forkChildInARG)) {
      //Means we have already handled this state, just get the next one
      iterator = toCallerStatesIterator.get(forkChildInARG);
    } else {
      //We get this fork the second time (the first one was from path computer)
      //Found the caller, we have explored the first time
      ARGState previousCallerInARG = getPreviousStateOnPath(forkChildInPath).getARGState();
      assert callerStates.remove(previousCallerInARG);
      iterator = callerStates.iterator();
      toCallerStatesIterator.put(forkChildInARG, iterator);
    }

    if (iterator.hasNext()) {
      return iterator.next();
    } else {
      //We need to find the next fork
      //Do not change the fork state and start exploration from this one
      return null;
    }
  }

  /**
   * Due to special structure of ARGPath,
   * the real fork state (reduced entry) is not included into it.
   * We need to get it.
   *
   * @param parent a state after that we need to found a fork
   * @return a state of the nearest fork
   */
  private List<BackwardARGState> findNextForksInTail(BackwardARGState parent) {

    List<BackwardARGState> potentialForkStates = new LinkedList<>();
    Map<ARGState, ARGState> exitStateToEntryState = new TreeMap<>();
    BackwardARGState currentStateOnPath = parent;
    ARGState currentStateInARG, realParent;

    while (currentStateOnPath.getChildren().size() > 0) {

      assert currentStateOnPath.getChildren().size() == 1;
      currentStateOnPath = getNextStateOnPath(currentStateOnPath);
      currentStateInARG = currentStateOnPath.getARGState();

      //No matter which parent to take - interesting one is single anyway
      realParent = currentStateInARG.getParents().iterator().next();

      //Check if it is an exit state, we are waiting
      //Attention! Recursion is not supported here!
      if (exitStateToEntryState.containsKey(realParent)) {
        //Due to complicated structure of path we saved an expanded exit state and it isn't contained in the path,
        //so, we look for its parent
        ARGState expandedEntryState = exitStateToEntryState.get(realParent);
        //remove all child in cache
        for (ARGState expandedExitState : expandedEntryState.getChildren()) {
          exitStateToEntryState.remove(expandedExitState);
        }
        potentialForkStates.remove(expandedEntryState);
      }

      if (fromReducedToExpand.containsKey(realParent) &&
          fromReducedToExpand.get(realParent).size() > 1) {

        assert realParent.getParents().size() == 0;
        assert fromReducedToExpand.get(realParent).size() > 1;

        //Now we should check, that there is no corresponding exit state in the path
        //only in this case this is a real fork

        //This is expanded state on the path at function call
        ARGState expandedEntryState = getPreviousStateOnPath(currentStateOnPath).getARGState();
        //We may have several children, so add all of them
        for (ARGState expandedExitState : expandedEntryState.getChildren()) {
          exitStateToEntryState.put(expandedExitState, currentStateOnPath);
        }
        //Save child and if we meet it, we remove the parent as not a fork

        potentialForkStates.add(currentStateOnPath);
      }
    }

    return potentialForkStates;
  }


  ARGPath restorePathFrom(BackwardARGState pLastElement) {
    assert (pLastElement != null && !pLastElement.isDestroyed());
      //we delete this state from other unsafe

    try {
      ARGState rootOfSubgraph = subgraphComputer.findPath(pLastElement, refinedStates);
      assert (rootOfSubgraph != null);
      if (rootOfSubgraph == BAMMultipleCEXSubgraphComputer.DUMMY_STATE_FOR_REPEATED_STATE) {
        return null;
      }
      ARGPath result = ARGUtils.getRandomPath(rootOfSubgraph);
      numberOfPathCalculated.inc();
      if (result != null) {
        numberOfPathFinished.inc();
      }
      if (checkThePathHasRepeatedStates(result)) {
        return null;
      }
      return result;
    } catch (MissingBlockException e) {
      return null;
    } catch (InterruptedException e) {
      return null;
    }
  }


  /* Functions only to simplify the understanding:
   */

  private BackwardARGState getNextStateOnPath(BackwardARGState state) {
    return (BackwardARGState) state.getChildren().iterator().next();
  }

  private BackwardARGState getPreviousStateOnPath(BackwardARGState state) {
    return (BackwardARGState) state.getParents().iterator().next();
  }

  private boolean checkThePathHasRepeatedStates(ARGPath path) {
    additionTimerCheck.start();
    List<Integer> ids =
        from(path.asStatesList())
        .transform(idExtractor)
        .toList();

    boolean repeated =
        from(refinedStates)
        .anyMatch(l -> ids.containsAll(l));

    if (repeated) {
      numberOfRepeatedConstructedPaths.inc();
    }
    additionTimerCheck.stop();
    return repeated;
  }

}
