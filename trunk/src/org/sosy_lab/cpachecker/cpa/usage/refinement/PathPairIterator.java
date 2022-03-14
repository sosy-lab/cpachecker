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
import java.util.Set;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.bam.BAMCPA;
import org.sosy_lab.cpachecker.cpa.bam.BAMMultipleCEXSubgraphComputer;
import org.sosy_lab.cpachecker.cpa.bam.BAMSubgraphIterator;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.cpa.usage.refinement.RefinementBlockFactory.PathEquation;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class PathPairIterator
    extends GenericIterator<Pair<UsageInfo, UsageInfo>, Pair<ExtendedARGPath, ExtendedARGPath>> {

  private final Set<List<Integer>> refinedStates = new HashSet<>();
  private final BAMCPA bamCpa;
  private BAMMultipleCEXSubgraphComputer subgraphComputer;
  private final IdentityHashMap<UsageInfo, BAMSubgraphIterator> targetToPathIterator;

  // Statistics
  private StatTimer computingPath = new StatTimer("Time for path computing");
  private StatTimer additionTimerCheck = new StatTimer("Time for addition checks");
  private StatCounter numberOfPathCalculated = new StatCounter("Number of path calculated");
  private StatCounter numberOfPathFinished = new StatCounter("Number of new path calculated");
  private StatCounter numberOfRepeatedConstructedPaths =
      new StatCounter("Number of repeated path computed");
  // private int numberOfrepeatedPaths = 0;

  private IdentityHashMap<UsageInfo, List<ExtendedARGPath>> computedPathsForUsage =
      new IdentityHashMap<>();
  private IdentityHashMap<UsageInfo, Iterator<ExtendedARGPath>> currentIterators =
      new IdentityHashMap<>();

  private final Function<ARGState, Integer> idExtractor;

  // internal state
  private ExtendedARGPath firstPath = null;

  public PathPairIterator(
      ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>> pWrapper,
      BAMCPA pBamCpa,
      PathEquation type)
      throws InvalidConfigurationException {
    super(pWrapper);
    bamCpa = pBamCpa;

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
    targetToPathIterator = new IdentityHashMap<>();
  }

  @Override
  protected void init(Pair<UsageInfo, UsageInfo> pInput) {
    firstPath = null;
    // subgraph computer need partitioning, which is not built at creation.
    // Thus, we move the creation of subgraphcomputer here
    subgraphComputer = bamCpa.createBAMMultipleSubgraphComputer(idExtractor);
  }

  @Override
  protected Pair<ExtendedARGPath, ExtendedARGPath> getNext(Pair<UsageInfo, UsageInfo> pInput) {
    UsageInfo firstUsage, secondUsage;
    firstUsage = pInput.getFirst();
    secondUsage = pInput.getSecond();

    if (firstPath == null) {
      // First time or it was unreachable last time
      firstPath = getNextPath(firstUsage);
      if (firstPath == null) {
        return null;
      }
    }

    ExtendedARGPath secondPath = getNextPath(secondUsage);
    if (secondPath == null) {
      // Reset the iterator
      currentIterators.remove(secondUsage);
      // And move shift the first one
      firstPath = getNextPath(firstUsage);
      if (firstPath == null) {
        return null;
      }
      secondPath = getNextPath(secondUsage);
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

  @Override
  protected void finishIteration(
      Pair<ExtendedARGPath, ExtendedARGPath> pathPair, RefinementResult wrapperResult) {
    ExtendedARGPath firstExtendedPath, secondExtendedPath;

    firstExtendedPath = pathPair.getFirst();
    secondExtendedPath = pathPair.getSecond();

    Object predicateInfo = wrapperResult.getInfo(PredicateRefinerAdapter.class);
    if (predicateInfo instanceof List) {
      @SuppressWarnings("unchecked")
      List<ARGState> affectedStates = (List<ARGState>) predicateInfo;
      // affectedStates may be null, if the path was refined somewhen before

      // A feature of GenericSinglePathRefiner: if one path is false, the second one is not refined
      if (firstExtendedPath.isUnreachable()) {
        // This one is false
        handleAffectedStates(affectedStates);
        // Need to clean first path
        firstPath = null;
      } else {
        // The second one must be
        Preconditions.checkArgument(
            secondExtendedPath.isUnreachable(),
            "Either the first path, or the second one must be unreachable here");
        handleAffectedStates(affectedStates);
      }
    } else {
      if (firstPath.isUnreachable()) {
        firstPath = null;
      }
    }
    updateTheComputedSet(firstExtendedPath);
    updateTheComputedSet(secondExtendedPath);
  }

  @Override
  protected void finish(Pair<UsageInfo, UsageInfo> pInput, RefinementResult pResult) {
    UsageInfo firstUsage = pInput.getFirst();
    UsageInfo secondUsage = pInput.getSecond();
    List<UsageInfo> unreacheableUsages = new ArrayList<>(2);

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
      // Refinement iteration finishes
      refinedStates.clear();
      targetToPathIterator.clear();
      firstPath = null;
      subgraphComputer = null;
    } else if (callerClass.equals(PointIterator.class)) {
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
        // We should reset iterator to avoid ConcurrentModificationException
        alreadyComputedPaths.remove(path);
      }
    }
  }

  private ExtendedARGPath getNextPath(UsageInfo info) {
    ARGPath currentPath;
    // Start from already computed set (it is partially refined)
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
    // try to compute more paths
    BAMSubgraphIterator pathIterator;
    if (targetToPathIterator.containsKey(info)) {
      pathIterator = targetToPathIterator.get(info);
    } else {
      ARGState target = (ARGState) info.getKeyState();
      pathIterator = subgraphComputer.iterator(target);
      targetToPathIterator.put(info, pathIterator);
    }
    currentPath = pathIterator.nextPath(refinedStates);
    computingPath.stop();

    if (currentPath == null) {
      // no path to iterate, finishing
      return null;
    }
    // Not add result now, only after refinement
    return new ExtendedARGPath(currentPath, info);
  }

  private void handleAffectedStates(List<ARGState> affectedStates) {
    // ARGState nextStart;
    // if (affectedStates != null) {
    List<Integer> changedStateNumbers = transformedImmutableListCopy(affectedStates, idExtractor);
    refinedStates.add(changedStateNumbers);

    /*  nextStart = affectedStates.get(affectedStates.size() - 1);
    } else {
      nextStart = path.getFirstState().getChildren().iterator().next();
    }
    previousForkForUsage.put(path.getUsageInfo(), (BackwardARGState)nextStart);*/
  }
}
