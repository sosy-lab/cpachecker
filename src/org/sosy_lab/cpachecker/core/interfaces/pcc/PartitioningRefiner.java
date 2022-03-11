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
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedGraph;

/**
 * Interface for graph partitioning algorithms, which are able to refine a partitioning. Refining a
 * partitioning means, that the partitioning is improved, with respect to some chosen optimiization
 * criterium. Usually this is done by some local criteria.
 */
public interface PartitioningRefiner {

  /**
   * Refine an initially given partitioning on a given graph. Usually improving a chosen local
   * criterion.
   *
   * <p>Operations are directly applied on the partitioning, i.e. it is changed.
   *
   * @param partitioning initial partitioning to be improved.
   * @param pGraph given graph
   * @param numPartitions number of partitions to be created
   * @return the total gain according to the improvements
   */
  int refinePartitioning(
      List<Set<Integer>> partitioning, PartialReachedSetDirectedGraph pGraph, int numPartitions);

  /**
   * Refine an initially given partitioning on a given weighted graph. Usually improving a chosen
   * local criterion.
   *
   * <p>Operations are directly applied on the partitioning, i.e. it is changed.
   *
   * @param partitioning initial partitioning to be improved.
   * @param wGraph a given weighted graph
   * @param numPartitions number of partitions to be created
   * @return the total gain according to the improvements
   */
  int refinePartitioning(List<Set<Integer>> partitioning, WeightedGraph wGraph, int numPartitions);
}
