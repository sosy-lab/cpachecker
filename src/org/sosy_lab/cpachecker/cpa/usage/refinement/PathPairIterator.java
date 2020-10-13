// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage.refinement;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class PathPairIterator extends
    GenericIterator<Pair<UsageInfo, UsageInfo>, Pair<ExtendedARGPath, ExtendedARGPath>> {

  private final Set<List<Integer>> refinedStates = new HashSet<>();
  //private final BAMCPA bamCpa;
  private PathRestorator subgraphComputer;
  private final Map<UsageInfo, PathIterator> targetToPathIterator;
  private final Set<UsageInfo> skippedUsages;

  //Statistics
  private StatTimer computingPath = new StatTimer("Time for path computing");
  private StatTimer additionTimerCheck = new StatTimer("Time for addition checks");
  private StatCounter numberOfPathCalculated = new StatCounter("Number of path calculated");
  private StatCounter numberOfPathFinished = new StatCounter("Number of new path calculated");
  private StatCounter numberOfRepeatedConstructedPaths = new StatCounter("Number of repeated path computed");
  //private int numberOfrepeatedPaths = 0;

  private Map<UsageInfo, List<ExtendedARGPath>> computedPathsForUsage = new IdentityHashMap<>();
  private Map<UsageInfo, Iterator<ExtendedARGPath>> currentIterators = new IdentityHashMap<>();
  // Not set, hash is changed
  private List<ExtendedARGPath> missedPaths = new ArrayList<>();

  private final Function<ARGState, Integer> idExtractor;

  //internal state
  private ExtendedARGPath firstPath = null;
  private final int iterationLimit;

  private int[] pathCalculated = new int[2];

  public PathPairIterator(
      ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>> pWrapper,
      PathRestorator pComputer,
      Function<ARGState, Integer> pExtractor,
      ShutdownNotifier pNotifier,
      int pInterationLimit) {
    super(pWrapper, pNotifier);
    subgraphComputer = pComputer;
    targetToPathIterator = new IdentityHashMap<>();
    skippedUsages = new HashSet<>();
    idExtractor = pExtractor;
    iterationLimit = pInterationLimit;
  }

  @Override
  protected void init(Pair<UsageInfo, UsageInfo> pInput) {
    firstPath = null;
    pathCalculated[0] = 0;
    pathCalculated[1] = 0;
  }

  @Override
  protected Pair<ExtendedARGPath, ExtendedARGPath> getNext(Pair<UsageInfo, UsageInfo> pInput) {
    UsageInfo firstUsage, secondUsage;

    if (pathCalculated[0] >= iterationLimit) {
      return null;
    }

    firstUsage = pInput.getFirst();
    secondUsage = pInput.getSecond();

    if (skippedUsages.contains(firstUsage) || skippedUsages.contains(secondUsage)) {
      // We know, that it have no valuable paths
      // Note, there are some paths, but they are declined by 'refinedStates'
      return null;
    }

    if (firstPath == null) {
      //First time or it was unreachable last time
      firstPath = getNextPath(firstUsage, 0);
      if (firstPath == null) {
        return null;
      }
    }

    ExtendedARGPath secondPath = getNextPath(secondUsage, 1);
    if (secondPath == null) {
      //Reset the iterator
      currentIterators.remove(secondUsage);
      //And move shift the first one
      firstPath = getNextPath(firstUsage, 0);
      if (firstPath == null) {
        return null;
      }
      secondPath = getNextPath(secondUsage, 1);
      if (secondPath == null) {
        return null;
      }
    }

    return Pair.of(firstPath, secondPath);
  }

  private boolean checkIsUsageUnreachable(UsageInfo pInput) {
    return !computedPathsForUsage.containsKey(pInput)
        || computedPathsForUsage.get(pInput).isEmpty();
  }

  @SuppressWarnings("unchecked")
  @Override
  protected void finishIteration(Pair<ExtendedARGPath, ExtendedARGPath> pathPair, RefinementResult wrapperResult) {
    ExtendedARGPath firstExtendedPath, secondExtendedPath;

    firstExtendedPath = pathPair.getFirst();
    secondExtendedPath = pathPair.getSecond();

    List<ARGState> firstAffectedStates = null;
    List<ARGState> secondAffectedStates = null;

    Object predicateInfo = wrapperResult.getInfo(PredicateRefinerAdapter.class);
    if (predicateInfo instanceof List) {
      //affectedStates may be null, if the path was refined somewhen before

      //A feature of GenericSinglePathRefiner: if one path is false, the second one is not refined
      if (firstExtendedPath.isUnreachable()) {
        //This one is false
        firstAffectedStates = (List<ARGState>)predicateInfo;
      } else {
        //The second one must be
        Preconditions.checkArgument(secondExtendedPath.isUnreachable(), "Either the first path, or the second one must be unreachable here");
        secondAffectedStates = (List<ARGState>)predicateInfo;
      }
    }

    predicateInfo = wrapperResult.getInfo(LockRefiner.class);
    if (predicateInfo != null && predicateInfo instanceof Pair) {
      Pair<List<ARGState>, List<ARGState>> lockInfo =
          (Pair<List<ARGState>, List<ARGState>>) predicateInfo;

      firstAffectedStates = lockInfo.getFirst();
      secondAffectedStates = lockInfo.getSecond();
    }

    if (firstExtendedPath.isUnreachable()) {
      firstPath = null;
      handleAffectedStates(firstAffectedStates);
    }
    if (secondExtendedPath.isUnreachable()) {
      handleAffectedStates(secondAffectedStates);
    }
    updateTheComputedSet(firstExtendedPath);
    updateTheComputedSet(secondExtendedPath);
  }

  @Override
  protected void finish(Pair<UsageInfo, UsageInfo> pInput, RefinementResult pResult) {
    UsageInfo firstUsage = pInput.getFirst();
    UsageInfo secondUsage = pInput.getSecond();
    List<UsageInfo> unreacheableUsages = new ArrayList<>(2);

    if (!missedPaths.isEmpty()) {
      for (ExtendedARGPath path : new ArrayList<>(missedPaths)) {
        updateTheComputedSet(path);
      }
    }

    if (checkIsUsageUnreachable(firstUsage)) {
      unreacheableUsages.add(firstUsage);
    }
    if (checkIsUsageUnreachable(secondUsage)) {
      unreacheableUsages.add(secondUsage);
    }
    pResult.addInfo(this.getClass(), unreacheableUsages);
  }

  @Override
  protected void printDetailedStatistics(StatisticsWriter pOut) {
    pOut.spacer()
      .put(computingPath)
      .put(additionTimerCheck)
      .put(numberOfPathCalculated)
      .put(numberOfPathFinished)
      .put(numberOfRepeatedConstructedPaths);
  }

  @Override
  protected void handleFinishSignal(Class<? extends RefinementInterface> callerClass) {
    if (callerClass.equals(IdentifierIterator.class)) {
      //Refinement iteration finishes
      refinedStates.clear();
      targetToPathIterator.clear();
      firstPath = null;
    } else if (callerClass.equals(PointIterator.class)) {
      currentIterators.clear();
      computedPathsForUsage.clear();
      skippedUsages.clear();
      assert missedPaths.isEmpty();
    }
  }

  private void updateTheComputedSet(ExtendedARGPath path) {
    UsageInfo usage = path.getUsageInfo();

    boolean alreadyComputed = computedPathsForUsage.containsKey(usage);
    missedPaths.remove(path);

    if (!path.isUnreachable()) {
      List<ExtendedARGPath> alreadyComputedPaths;
      if (!alreadyComputed) {
        alreadyComputedPaths = new ArrayList<>();
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

  private ExtendedARGPath getNextPath(UsageInfo info, int usageNumber) {
    ARGPath currentPath;
    // Start from already computed set (it is partially refined)
    numberOfPathCalculated.inc();
    Iterator<ExtendedARGPath> iterator = currentIterators.get(info);
    if (iterator == null && computedPathsForUsage.containsKey(info)) {
      // first call
      // Clone the set to avoid concurrent modification
      iterator = new ArrayList<>(computedPathsForUsage.get(info)).iterator();
      currentIterators.put(info, iterator);
    }

    if (iterator != null && iterator.hasNext()) {
      return iterator.next();
    }

    computingPath.start();
    //try to compute more paths
    PathIterator pathIterator;
    if (targetToPathIterator.containsKey(info)) {
      pathIterator = targetToPathIterator.get(info);
    } else {
      ARGState target = (ARGState) info.getKeyState();
      pathIterator = subgraphComputer.iterator(target);
      targetToPathIterator.put(info, pathIterator);
    }
    if (pathCalculated[usageNumber] < iterationLimit) {
      currentPath = pathIterator.nextPath(refinedStates);
      pathCalculated[usageNumber]++;
    } else {
      currentPath = null;
    }
    computingPath.stop();

    if (currentPath == null) {
      // no path to iterate, finishing
      if (!computedPathsForUsage.containsKey(info) || computedPathsForUsage.get(info).size() == 0) {
        skippedUsages.add(info);
      }
      return null;
    }
    numberOfPathFinished.inc();
    ExtendedARGPath result = new ExtendedARGPath(currentPath, info);
    missedPaths.add(result);
    //Not add result now, only after refinement
    return result;
  }

  private void handleAffectedStates(List<ARGState> affectedStates) {
    // ARGState nextStart;
    // if (affectedStates != null) {
    List<Integer> changedStateNumbers = transformedImmutableListCopy(affectedStates, idExtractor);
    refinedStates.add(changedStateNumbers);

    /*
     * nextStart = affectedStates.get(affectedStates.size() - 1); } else { nextStart =
     * path.getFirstState().getChildren().iterator().next(); }
     */
  }
}
