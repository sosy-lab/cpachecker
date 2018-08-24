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

import java.util.HashSet;
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

  private final Set<UsageInfo> skippedUsages;
  private final BAMMultipleCEXSubgraphComputer subgraphComputer;

  // Statistics
  private StatTimer computingPath = new StatTimer("Time for path computing");
  private StatCounter numberOfPathCalculated = new StatCounter("Number of path calculated");

  public SinglePathProvider(
      ConfigurableRefinementBlock<Pair<ExtendedARGPath, ExtendedARGPath>> pWrapper,
      BAMMultipleCEXSubgraphComputer pComputer) {

    super(pWrapper);
    subgraphComputer = pComputer;
    skippedUsages = new HashSet<>();
  }

  @Override
  public RefinementResult performBlockRefinement(Pair<UsageInfo, UsageInfo> pInput)
      throws CPAException, InterruptedException {
    RefinementResult result = RefinementResult.createFalse();

    ExtendedARGPath firstPath = createPathFor(pInput.getFirst());
    if (firstPath == null) {
      return result;
    }
    ExtendedARGPath secondPath = createPathFor(pInput.getSecond());
    if (secondPath == null) {
      return result;
    }
    return wrappedRefiner.performBlockRefinement(Pair.of(firstPath, secondPath));
  }

  private ExtendedARGPath createPathFor(UsageInfo usage) {
    ARGPath currentPath;
    // Start from already computed set (it is partially refined)
    if (skippedUsages.contains(usage)) {
      return null;
    }

    numberOfPathCalculated.inc();

    computingPath.start();
    //try to compute more paths
    currentPath = subgraphComputer.computePath((ARGState) usage.getKeyState());
    computingPath.stop();

    if (currentPath == null) {
      // no path to iterate, finishing
      skippedUsages.add(usage);
      return null;
    }
    //Not add result now, only after refinement
    return new ExtendedARGPath(currentPath, usage);
  }

  @Override
  public void printStatistics(StatisticsWriter pOut) {
    pOut.spacer()
        .put(computingPath)
        .put(numberOfPathCalculated);
  }

  @Override
  protected void handleFinishSignal(Class<? extends RefinementInterface> callerClass) {
    if (callerClass.equals(PointIterator.class)) {
      skippedUsages.clear();
    }
  }

}
