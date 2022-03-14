// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy.partitioning;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.sosy_lab.cpachecker.core.interfaces.pcc.WeightedBalancedGraphPartitioner;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedGraph;

public class RandomBalancedWeightedGraphPartitioner extends RandomBalancedGraphPartitioner
    implements WeightedBalancedGraphPartitioner {

  @Override
  public List<Set<Integer>> computePartitioning(int pNumPartitions, WeightedGraph wGraph)
      throws InterruptedException {
    checkArgument(
        pNumPartitions > 0 && wGraph != null,
        "Partitioniong must contain at most 1 partition. Graph may not be null.");
    if (pNumPartitions == 1) { // 1-partitioning easy special case (all nodes in one partition)
      // No expensive computations to do
      return wGraph.getGraphAsOnePartition();
    }
    if (pNumPartitions >= wGraph.getNumNodes()) { // Each Node has its own partition
      return wGraph.getNodesSeperatelyPartitioned(pNumPartitions);
    }

    // There is more than one partition, and at least one partition contains more than 1 node

    List<Set<Integer>> partitioning = new ArrayList<>(pNumPartitions);
    for (int i = 0; i < pNumPartitions; i++) {
      partitioning.add(new HashSet<>());
    }

    Random randomGen = new Random(0);

    for (int i = 0; i < wGraph.getNumNodes(); i++) {
      partitioning.get(randomGen.nextInt(pNumPartitions)).add(i);
    }

    return partitioning;
  }
}
