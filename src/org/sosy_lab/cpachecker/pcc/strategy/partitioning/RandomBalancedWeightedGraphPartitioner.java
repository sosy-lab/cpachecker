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
package org.sosy_lab.cpachecker.pcc.strategy.partitioning;

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
    if (pNumPartitions <= 0 || wGraph == null) { throw new IllegalArgumentException(
        "Partitioniong must contain at most 1 partition. Graph may not be null."); }
    if (pNumPartitions == 1) { //1-partitioning easy special case (all nodes in one partition)
      //No expensive computations to do
      return wGraph.getGraphAsOnePartition();
    }
    if (pNumPartitions >= wGraph.getNumNodes()) {//Each Node has its own partition
      return wGraph.getNodesSeperatelyPartitioned(pNumPartitions);
    }

    //There is more than one partition, and at least one partition contains more than 1 node

    List<Set<Integer>> partitioning =  new ArrayList<>(pNumPartitions);
    for (int i=0;i<pNumPartitions;i++) {
      partitioning.add(new HashSet<Integer>());
    }

    Random randomGen = new Random();

    for (int i = 0; i < wGraph.getNumNodes(); i++) {
      partitioning.get(randomGen.nextInt(pNumPartitions)).add(i);
    }

    return partitioning;
  }

}
