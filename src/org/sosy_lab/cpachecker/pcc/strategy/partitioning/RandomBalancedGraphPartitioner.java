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
import org.sosy_lab.cpachecker.core.interfaces.pcc.BalancedGraphPartitioner;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.PartialReachedSetDirectedGraph;

public class RandomBalancedGraphPartitioner implements BalancedGraphPartitioner {

  @Override
  public List<Set<Integer>> computePartitioning(
      int pNumPartitions, PartialReachedSetDirectedGraph pGraph) {
    checkArgument(
        pNumPartitions > 0 && pGraph != null,
        "Partitioniong must contain at most 1 partition. Graph may not be null.");
    List<Set<Integer>> partitioning = new ArrayList<>(pNumPartitions);
    for (int i = 0; i < pNumPartitions; i++) {
      partitioning.add(new HashSet<Integer>());
    }

    Random randomGen = new Random(0);

    for (int i = 0; i < pGraph.getNumNodes(); i++) {
      partitioning.get(randomGen.nextInt(pNumPartitions)).add(i);
    }

    return partitioning;
  }
}
