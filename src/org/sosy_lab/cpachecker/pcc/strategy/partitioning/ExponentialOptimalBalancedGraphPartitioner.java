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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.interfaces.pcc.BalancedGraphPartitioner;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.PartialReachedSetDirectedGraph;


public class ExponentialOptimalBalancedGraphPartitioner implements BalancedGraphPartitioner {

  private final ShutdownNotifier shutdown;

  public ExponentialOptimalBalancedGraphPartitioner(ShutdownNotifier pShutdownNotifier) {
    shutdown = pShutdownNotifier;
  }

  @Override
  public List<Set<Integer>> computePartitioning(final int pNumPartitions, final PartialReachedSetDirectedGraph pGraph)
      throws InterruptedException {
    List<Integer> permutationIndices = initPermutationIndices(pGraph.getNumNodes());

    // compute partition for current order of nodes
    List<Set<Integer>> currentPartitioning, bestPartitioning =
        computePermutation(permutationIndices, pNumPartitions);

    long currentNumCrossingEdges, minNumCrossingEdges = getNumberOfPartitionCrossingEdges(pGraph, bestPartitioning);

    while (hasNextPermutation(permutationIndices)) {
      shutdown.shutdownIfNecessary();
      currentPartitioning = computeNextPermutation(permutationIndices, pNumPartitions);
      currentNumCrossingEdges = getNumberOfPartitionCrossingEdges(pGraph, currentPartitioning);

      if (minNumCrossingEdges >= currentNumCrossingEdges || minNumCrossingEdges == -1) {
        bestPartitioning = currentPartitioning;
      }
    }

    return bestPartitioning;
  }

  private List<Integer> initPermutationIndices(final int pNumIndices) {
    List<Integer> permutationIndices = new ArrayList<>(pNumIndices);
    for (int i = 0; i < pNumIndices; i++) {
      permutationIndices.add(0);
    }
    return permutationIndices;
  }

  private List<Set<Integer>> initPartition(final int pNumPartitions, final int maxSize) {
    List<Set<Integer>> result = new ArrayList<>(pNumPartitions);
    for (int i = 0; i < pNumPartitions; i++) {
      result.add(i, new HashSet<Integer>(maxSize));
    }
    return result;
  }

  private List<Set<Integer>> computePermutation(final List<Integer> pPermutationIndices, final int pNumPartitions) {

    List<Integer> orderedNodes = new LinkedList<>();
    for (int j = pPermutationIndices.size() - 1; j >= 0; j--) {
      orderedNodes.add(pPermutationIndices.get(j), j);
    }

    int maxSize = (int) Math.ceil(pPermutationIndices.size() / ((double) pNumPartitions));
    int numSmaller = maxSize * pNumPartitions - pPermutationIndices.size();

    List<Set<Integer>> result = initPartition(pNumPartitions, maxSize);

    Set<Integer> currentPartition;

    for (int i = 0; i < numSmaller; i++) {
      currentPartition = result.get(i);
      for (int j = i * (maxSize - 1); j < (i + 1) * (maxSize - 1); j++) {
        currentPartition.add(orderedNodes.get(j));
      }
    }

    for (int i = 0; i < pNumPartitions - numSmaller; i++) {
      currentPartition = result.get(i);
      for (int j = i * maxSize + numSmaller * (maxSize - 1); j < (i + 1) * maxSize + numSmaller * (maxSize - 1); j++) {
        currentPartition.add(orderedNodes.get(j));
      }
    }

    return result;
  }

  private boolean hasNextPermutation(final List<Integer> pPermutationIndices) {
    for (int i = 0, j = pPermutationIndices.size() - 1; i < pPermutationIndices.size(); i++, j--) {
      if (pPermutationIndices.get(i) < j) { return true; }
    }
    return false;
  }

  private List<Set<Integer>> computeNextPermutation(final List<Integer> pPermutationIndices, final int pNumPartitions) {
    for (int j = pPermutationIndices.size() - 1, i = 0; j >= 0; j--, i++) {
      if (pPermutationIndices.get(j) < i) {
        pPermutationIndices.set(j, pPermutationIndices.get(j) + 1);
        break;
      } else {
        pPermutationIndices.set(j, 0);
      }
    }
    return computePermutation(pPermutationIndices, pNumPartitions);
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
