/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.interfaces.pcc;

import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.PartialReachedSetDirectedGraph;
import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.WeightedGraph;

/**
 * Interface for graph partitioning algorithms, which are able to refine a partitioning.
 * Refining a partitioning means, that the partitioning is improved, with respect to some chosen optimiization criterium.
 * Usually this is done by some local criteria.
 */
public interface PartitioningRefiner {

  /**
   * Refine an initially given partitioning on a given graph. Usually improving a chosen local criterion.
   *
   * Operations are directly applied on the partitioning, i.e. it is changed.
   * @param partitioning initial partitioning to be improved.
   * @param pGraph given graph
   * @param numPartitions number of partitions to be created
   * @return the total gain according to the improvements
   */
  int refinePartitioning(List<Set<Integer>> partitioning,
      PartialReachedSetDirectedGraph pGraph, int numPartitions);

  /**
   * Refine an initially given partitioning on a given weighted graph. Usually improving a chosen local criterion.
   *
   * Operations are directly applied on the partitioning, i.e. it is changed.
   * @param partitioning initial partitioning to be improved.
   * @param wGraph a given weighted graph
   * @param numPartitions  number of partitions to be created
   * @return the total gain according to the improvements
   */
  int refinePartitioning(List<Set<Integer>> partitioning,
      WeightedGraph wGraph, int numPartitions);

}