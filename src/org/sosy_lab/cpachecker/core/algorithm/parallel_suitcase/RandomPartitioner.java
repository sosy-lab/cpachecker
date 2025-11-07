// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.parallel_suitcase;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/** Random partitioning strategy that divides test targets into roughly equal subsets */
public class RandomPartitioner implements PartitioningStrategy {

  @Override
  public List<Set<CFAEdge>> partition(Set<CFAEdge> testTargets, int numPartitions) {
    Preconditions.checkArgument(numPartitions > 0, "Number of partitions must be positive");

    if (testTargets.isEmpty()) {
      return createEmptyPartitions(numPartitions);
    }

    // Convert to list and shuffle for random distribution
    List<CFAEdge> shuffledTargets = new ArrayList<>(testTargets);
    // currently fixed seed, might need to be configurable
    Collections.shuffle(shuffledTargets, new Random(0));

    // Initialize partitions
    List<Set<CFAEdge>> partitions = new ArrayList<>(numPartitions);
    for (int i = 0; i < numPartitions; i++) {
      partitions.add(new HashSet<>());
    }

    // Distribute targets evenly using round-robin
    for (int i = 0; i < shuffledTargets.size(); i++) {
      int partitionIndex = i % numPartitions;
      partitions.get(partitionIndex).add(shuffledTargets.get(i));
    }

    return partitions;
  }

  private List<Set<CFAEdge>> createEmptyPartitions(int numPartitions) {
    List<Set<CFAEdge>> partitions = new ArrayList<>(numPartitions);
    for (int i = 0; i < numPartitions; i++) {
      partitions.add(new HashSet<>());
    }
    return partitions;
  }
}
