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
package org.sosy_lab.cpachecker.core.interfaces.pcc;

import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.pcc.strategy.partialcertificate.PartialReachedSetDirectedGraph;

/**
 * Interface for algorithms that compute balanced graph partitionings.
 *
 * Since the problem is NP complete and no polytime approximation exists, almost equal size partitions may be computed.
 */
public interface BalancedGraphPartitioner {

  /**
   * Divides the node of <code>pGraph</code> into <code>pNumPartitions</code> disjunct sets of almost equal size.
   *
   * @param pNumPartitions - number of disjunct sets, greater 1
   * @param pGraph - directed graph whose nodes should be partitioned
   * @return the partitioning, each set contains the indices of the nodes which it contains
   */
  public List<Set<Integer>> computePartitioning(int pNumPartitions, PartialReachedSetDirectedGraph pGraph)
      throws InterruptedException;
}
