// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces.pcc;

import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.PartialReachedSetDirectedGraph;

/**
 * Interface for algorithms that compute balanced graph partitionings.
 *
 * <p>Since the problem is NP complete and no polytime approximation exists, almost equal size
 * partitions may be computed.
 */
public interface BalancedGraphPartitioner {

  /**
   * Divides the node of <code>pGraph</code> into <code>pNumPartitions</code> disjunct sets of
   * almost equal size.
   *
   * @param pNumPartitions - number of disjunct sets, greater 1
   * @param pGraph - directed graph whose nodes should be partitioned
   * @return the partitioning, each set contains the indices of the nodes which it contains
   */
  List<Set<Integer>> computePartitioning(int pNumPartitions, PartialReachedSetDirectedGraph pGraph)
      throws InterruptedException;
}
