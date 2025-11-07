// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.parallel_suitcase;

import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/** Interface for partitioning strategies */
public interface PartitioningStrategy {

  /**
   * Partition the test targets into multiple subsets
   *
   * @param testTargets The set of test targets to partition
   * @param numPartitions The number of partitions to create
   * @return List of partitioned subsets
   */
  List<Set<CFAEdge>> partition(Set<CFAEdge> testTargets, int numPartitions);
}
