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
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedGraph;

/**
 * Interface for algorithms that compute balanced graph partitionings even on weighted graphs.
 *
 * <p>Since the problem is NP complete and no polytime approximation exists, almost equal size
 * partitions may be computed.
 */
public interface WeightedBalancedGraphPartitioner extends BalancedGraphPartitioner {

  /**
   * Divides the node of <code>wGraph</code> into <code>pNumPartitions</code> disjunct sets of
   * almost equal size.
   *
   * @param pNumPartitions - number of disjunct sets, greater 1
   * @param wGraph - directed, weighted graph whose nodes should be partitioned
   * @return the partitioning, each set contains the indices of the nodes which it contains
   */
  List<Set<Integer>> computePartitioning(int pNumPartitions, WeightedGraph wGraph)
      throws InterruptedException;
}
