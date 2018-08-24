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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.bam.BAMCPA;
import org.sosy_lab.cpachecker.cpa.bam.BAMMultipleCEXSubgraphComputer;
import org.sosy_lab.cpachecker.cpa.usage.UsageInfo;
import org.sosy_lab.cpachecker.cpa.usage.refinement.RefinementBlockFactory.PathEquation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.statistics.StatCounter;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class SinglePathProvider extends
    WrappedConfigurableRefinementBlock<Pair<UsageInfo, UsageInfo>, Pair<ExtendedARGPath, ExtendedARGPath>> {

  private final Set<List<Integer>> refinedStates = new HashSet<>();
  private final BAMCPA bamCpa;
  private final Function<ARGState, Integer> idExtractor;
  private final Set<UsageInfo> skippedUsages;

  // Statistics
  private StatTimer computingPath = new StatTimer("Time for path computing");
  private StatTimer additionTimerCheck = new StatTimer("Time for addition checks");
  private StatCounter numberOfPathCalculated = new StatCounter("Number of path calculated");
  private StatCounter numberOfPathFinished = new StatCounter("Number of new path calculated");
  private StatCounter numberOfRepeatedConstructedPaths =
      new StatCounter("Number of repeated path computed");

  public SinglePathProvider(
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
    skippedUsages = new HashSet<>();
  }

  @Override
  public RefinementResult performBlockRefinement(Pair<UsageInfo, UsageInfo> pInput)
      throws CPAException, InterruptedException {
    RefinementResult result = RefinementResult.createFalse();
    BAMMultipleCEXSubgraphComputer subgraphComputer =
        bamCpa.createBAMMultipleSubgraphComputer(idExtractor);

    ExtendedARGPath firstPath = createPathFor(subgraphComputer, pInput.getFirst());
    if (firstPath == null) {
      return result;
    }
    ExtendedARGPath secondPath = createPathFor(subgraphComputer, pInput.getFirst());
    if (secondPath == null) {
      return result;
    }
    result = wrappedRefiner.performBlockRefinement(Pair.of(firstPath, secondPath));
    Object predicateInfo = result.getInfo(PredicateRefinerAdapter.class);
    if (predicateInfo != null && predicateInfo instanceof List) {
      @SuppressWarnings("unchecked")
      List<ARGState> affectedStates = (List<ARGState>) predicateInfo;
      // affectedStates may be null, if the path was refined somewhen before
      refinedStates.add(from(affectedStates).transform(idExtractor).toList());
    }
    return result;
  }

  private ExtendedARGPath createPathFor(BAMMultipleCEXSubgraphComputer computer, UsageInfo usage) {
    ARGPath currentPath;
    // Start from already computed set (it is partially refined)
    numberOfPathCalculated.inc();

    computingPath.start();
    //try to compute more paths
    currentPath = computer.computePath((ARGState) usage.getKeyState());
    computingPath.stop();

    if (currentPath == null) {
      // no path to iterate, finishing
      skippedUsages.add(usage);
      return null;
    }
    numberOfPathFinished.inc();
    //Not add result now, only after refinement
    return new ExtendedARGPath(currentPath, usage);
  }

  @Override
  public void printStatistics(StatisticsWriter pOut) {
    pOut.spacer()
        .put(computingPath)
        .put(additionTimerCheck)
        .put(numberOfPathCalculated)
        .put(numberOfPathFinished)
        .put(numberOfRepeatedConstructedPaths);

  }

}
