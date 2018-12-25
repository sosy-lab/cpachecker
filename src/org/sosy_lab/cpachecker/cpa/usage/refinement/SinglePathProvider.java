/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.usage.refinement;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Function;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.bam.BAMMultipleCEXSubgraphComputer;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class SinglePathProvider extends
    WrappedConfigurableRefinementBlock<Pair<UsageInfo, UsageInfo>, Pair<ExtendedARGPath, ExtendedARGPath>> {

  private final Set<List<Integer>> refinedStates = new HashSet<>();
  private final Map<UsageInfo, ExtendedARGPath> cachedPaths = new HashMap<>();
  private final Set<UsageInfo> skippedUsages;
  private final BAMMultipleCEXSubgraphComputer subgraphComputer;

  private final Function<ARGState, Integer> idExtractor;

  // Statistics
  private StatTimer computingPath = new StatTimer("Time for path computing");
  private StatCounter numberOfSkippedPath = new StatCounter("Number of skipped paths");
  private StatCounter numberOfCachedPath = new StatCounter("Number of cache hits");
  private StatCounter numberOfPathCalculated = new StatCounter("Number of path calculated");

  public SinglePathProvider(
      ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>> pWrapper,
      BAMMultipleCEXSubgraphComputer pComputer,
      Function<ARGState, Integer> pExtractor) {

    super(pWrapper);
    subgraphComputer = pComputer;
    skippedUsages = new HashSet<>();
    idExtractor = pExtractor;
  }

  @SuppressWarnings("unchecked")
  @Override
  public RefinementResult performBlockRefinement(Pair<UsageInfo, UsageInfo> pInput)
      throws CPAException, InterruptedException {
    RefinementResult result = RefinementResult.createFalse();
    List<UsageInfo> unreacheableUsages = new ArrayList<>(2);
    UsageInfo firstUsage = pInput.getFirst();
    UsageInfo secondUsage = pInput.getSecond();

    ExtendedARGPath firstPath = createPathFor(firstUsage);
    if (firstPath == null) {
      unreacheableUsages.add(firstUsage);
      result.addInfo(this.getClass(), unreacheableUsages);
      return result;
    }
    ExtendedARGPath secondPath = createPathFor(secondUsage);
    if (secondPath == null) {
      unreacheableUsages.add(secondUsage);
      result.addInfo(this.getClass(), unreacheableUsages);
      return result;
    }
    result = wrappedRefiner.performBlockRefinement(Pair.of(firstPath, secondPath));
    Object predicateInfo = result.getInfo(PredicateRefinerAdapter.class);
    if (predicateInfo != null && predicateInfo instanceof List) {
      handleAffectedStates((List<ARGState>) predicateInfo);
    }
    predicateInfo = result.getInfo(LockRefiner.class);
    if (predicateInfo != null && predicateInfo instanceof Pair) {
      Pair<List<ARGState>, List<ARGState>> lockInfo =
          (Pair<List<ARGState>, List<ARGState>>) predicateInfo;

      handleAffectedStates(lockInfo.getFirst());
      handleAffectedStates(lockInfo.getSecond());
    }

    addIfReachable(firstPath, unreacheableUsages);
    addIfReachable(secondPath, unreacheableUsages);
    if (!unreacheableUsages.isEmpty()) {
      result.addInfo(this.getClass(), unreacheableUsages);
    }
    return result;
  }

  private void addIfReachable(ExtendedARGPath path, List<UsageInfo> usages) {
    if (path.isUnreachable()) {
      UsageInfo unreachableUsage = path.getUsageInfo();
      skippedUsages.add(unreachableUsage);
      usages.add(unreachableUsage);
    } else {
      cachedPaths.put(path.getUsageInfo(), path);
    }
  }

  private ExtendedARGPath createPathFor(UsageInfo usage) {
    ARGPath currentPath;
    // Start from already computed set (it is partially refined)
    if (skippedUsages.contains(usage)) {
      numberOfSkippedPath.inc();
      return null;
    } else if (cachedPaths.containsKey(usage)) {
      numberOfCachedPath.inc();
      return cachedPaths.get(usage);
    }

    computingPath.start();
    ARGState target = (ARGState) usage.getKeyState();
    currentPath = subgraphComputer.computePath(target, refinedStates);
    computingPath.stop();

    if (currentPath == null) {
      // no path to iterate, finishing
      skippedUsages.add(usage);
      return null;
    } else {
      numberOfPathCalculated.inc();
      return new ExtendedARGPath(currentPath, usage);
    }
  }

  @Override
  public void printStatistics(StatisticsWriter pOut) {
    StatisticsWriter writer =
        pOut.spacer()
        .put(computingPath)
        .put(numberOfSkippedPath)
        .put(numberOfCachedPath)
        .put(numberOfPathCalculated);
    wrappedRefiner.printStatistics(writer);
  }

  @Override
  protected void handleFinishSignal(Class<? extends RefinementInterface> callerClass) {
    if (callerClass.equals(IdentifierIterator.class)) {
      // Refinement iteration finishes
      refinedStates.clear();
    } else if (callerClass.equals(PointIterator.class)) {
      skippedUsages.clear();
      cachedPaths.clear();
    }
  }

  private void handleAffectedStates(List<ARGState> affectedStates) {
    if (affectedStates == null || affectedStates.isEmpty()) {
      return;
    }
    List<Integer> changedStateNumbers = from(affectedStates).transform(idExtractor).toList();
    assert !changedStateNumbers.isEmpty();
    refinedStates.add(changedStateNumbers);
  }

}
