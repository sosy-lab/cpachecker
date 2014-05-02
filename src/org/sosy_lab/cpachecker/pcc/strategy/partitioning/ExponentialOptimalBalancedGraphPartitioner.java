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
package org.sosy_lab.cpachecker.pcc.strategy.partitioning;

import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.pcc.BalancedGraphPartitioner;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.PartialReachedSetDirectedGraph;


public class ExponentialOptimalBalancedGraphPartitioner implements BalancedGraphPartitioner{

  public ExponentialOptimalBalancedGraphPartitioner(ShutdownNotifier pShutdownNotifier) {
    // TODO Auto-generated constructor stub
  }

  @Override
  public List<Set<Integer>> computePartitioning(int pNumPartitions, PartialReachedSetDirectedGraph pGraph) {
    long currentNumCrossingEdges, minNumCrossingEdges = -1;
    List<Set<Integer>> currentPartitioning, bestPartitioning = null;

    while (hasNextPermutation()) {
      currentPartitioning = computeNextPermutation();
      currentNumCrossingEdges = getNumberOfPartitionCrossingEdges(pGraph, currentPartitioning);

      if (minNumCrossingEdges >= currentNumCrossingEdges || minNumCrossingEdges == -1) {
        bestPartitioning = currentPartitioning;
      }
    }

    return bestPartitioning;
  }

  private boolean hasNextPermutation(){
    // TODO implement
    return false;
  }

  private List<Set<Integer>> computeNextPermutation(){
    // TODO implement or replace
    return null;
  }

  private long getNumberOfPartitionCrossingEdges(final PartialReachedSetDirectedGraph pGraph,
      final List<Set<Integer>> partitioning) {
    long result = 0;
    for (Set<Integer> partition : partitioning) {
      result += pGraph.getNumAdjacentNodesOutsideSet(partition);
    }
    return result;
  }
}
