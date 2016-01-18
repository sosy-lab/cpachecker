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
package org.sosy_lab.cpachecker.cpa.loopstats;

import java.io.PrintStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatistics;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

class LoopstatsStatistics extends AbstractStatistics implements Statistics, LoopStatisticsReceiver {

  private final Map<ImmutableLoopStack, Integer> maxFinishedUnrollingsPerLoopStack = Maps.newHashMap();
  private final Map<ImmutableLoopStack, Integer> maxUnrollingsPerLoopStack = Maps.newHashMap();

  public LoopstatsStatistics() {
  }

  private <E> void putMax (Map<E, Integer> pTo, E pKey, int pValue ) {
    Integer currentMax = pTo.get(pKey);
    if (currentMax == null) {
      currentMax = pValue;
    } else {
      currentMax = Math.max(currentMax, pValue);
    }
    pTo.put(pKey, currentMax);
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {
    super.printStatistics(pOut, pResult, pReached);

    Set<Loop> loops = Sets.newHashSet();
    Map<Loop, Integer> maxUnrollingsPerLoop = Maps.newHashMap();
    Map<Loop, Integer> maxNesting = Maps.newHashMap();

    int maxFinishedLoopUnrollings = 0;
    int maxLoopUnrollings = 0;
    int maxNestingLevel = 0;
    Loop loopWithMaxUnrollings = null;

    for (Entry<ImmutableLoopStack, Integer> e: maxFinishedUnrollingsPerLoopStack.entrySet()) {
      Loop l = e.getKey().peekHead();

      loops.add(l);

      maxFinishedLoopUnrollings = Math.max(e.getValue(), maxFinishedLoopUnrollings);
      putMax(maxUnrollingsPerLoop, l, e.getValue());
      putMax(maxNesting, l, e.getKey().size());
    }

    for (Entry<ImmutableLoopStack, Integer> e: maxUnrollingsPerLoopStack.entrySet()) {
      Loop l = e.getKey().peekHead();

      loops.add(l);

      maxLoopUnrollings = Math.max(e.getValue(), maxLoopUnrollings);
      maxNestingLevel = Math.max(e.getKey().size()-1, maxNestingLevel);

      if (maxLoopUnrollings == e.getValue()) {
        loopWithMaxUnrollings = l;
      }
    }

    put(pOut, 0, "Number of loops entered", loops.size());
    put(pOut, 0, "Max. completed unrollings of a loop", maxFinishedLoopUnrollings);
    put(pOut, 0, "Max. unrollings of a loop", maxLoopUnrollings);
    if (loopWithMaxUnrollings != null) {
      put(pOut, 0, "Loop with max. unrollings", String.format("%s in line %d",
          loopWithMaxUnrollings.getLoopHeads().toString(),
          loopWithMaxUnrollings.getIncomingEdges().iterator().next().getLineNumber()));
    }
    put(pOut, 0, "Max. nesting of loops", maxNesting.size() - 1);

  }

  @Override
  public void signalLoopLeftAfter(ImmutableLoopStack pActiveLoops, int pNumberOfIterations) {

    putMax(maxFinishedUnrollingsPerLoopStack, pActiveLoops, pNumberOfIterations);
  }

  @Override
  public void signalNewLoopIteration(ImmutableLoopStack pActiveLoops, int pNthIteration) {

    putMax(maxUnrollingsPerLoopStack, pActiveLoops, pNthIteration);
  }

  @Override
  public String getName() {
    return "Loop";
  }


}
