/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.core.algorithm.tiger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.CFAGoal;

@Options(prefix = "tiger.partitioning")
public class PartitionProvider {
  enum PartitionSizeDistribution {
    TOTAL,
    RELATIVE
  }
  enum Strategy {
    RANDOM,
    DOMINATION,
    REVERSEDOMINATION,
    asd
  }


  @Option(
    secure = true,
    name = "partitionSizeDistribution",
    description = "Selects the partition distribution style")
  private PartitionSizeDistribution partitionSizeDistribution = PartitionSizeDistribution.RELATIVE;

  @Option(secure = true, name = "partitionSize", description = "Selects the size of partitions")
  private int partitionSize = 25;

  @Option(
    secure = true,
    name = "strategy",
    description = "Selects the strategy of partition distribution")
  private Strategy strategy = Strategy.RANDOM;

  @Option(
    secure = true,
    name = "minimumPartitionSize",
    description = "Selects the minimum partition size")
  private int minimumPartitionSize = 25;

  @Option(
    secure = true,
    name = "lessGoalsPerPartitionTolerance",
    description = "Selects the tolerance of having less goals in certain paritions")
  private int lessGoalsPerPartitionTolerance = 10;

  @Option(
    secure = true,
    name = "additionalGoalsPerPartitionTolerance",
    description = "Selects the tolerance of having more goals in certain paritions")
  private int additionalGoalsPerPartitionTolerance = 5;

  public PartitionProvider(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
  }

  private int partitionSize(Collection<?> allEdges) {
    int size = 0;
    if (partitionSizeDistribution == PartitionSizeDistribution.TOTAL) {
      size = partitionSize;
    } else {
      // need double prevent calculation with integers, which will truncate the result before ceil
      size = (int) Math.ceil((double) allEdges.size() * partitionSize / 100);
    }
    if (size > minimumPartitionSize) {
      return size;
    } else {
      return minimumPartitionSize;
    }
  }

  private List<Set<CFAGoal>> createOrderedPartition(List<CFAGoal> allEdges) {
    List<Set<CFAGoal>> partitioning = new ArrayList<>();
    HashSet<CFAGoal> partition = new HashSet<>();
    int size = partitionSize(allEdges);
    boolean noNewPartition = false;
    int edgesLeft = allEdges.size();

    for (CFAGoal edge : allEdges) {
      if (!noNewPartition
          && (edgesLeft <= lessGoalsPerPartitionTolerance
              || (edgesLeft <= additionalGoalsPerPartitionTolerance))) {
        int overFillingGoals = partition.size() - size + edgesLeft;
        if (!(overFillingGoals <= additionalGoalsPerPartitionTolerance)) {
          partitioning.add(partition);
          partition = new HashSet<>();
        }
        noNewPartition = true;
      }
      if (partition.size() >= size && !noNewPartition) {
        partitioning.add(partition);
        partition = new HashSet<>();
      }
      partition.add(edge);
      edgesLeft--;
    }
    if (partition.size() > 0 && !partitioning.contains(partition)) {
      partitioning.add(partition);
    }

    return partitioning;
  }

  private List<Set<CFAGoal>> createRandomPartition(Set<CFAGoal> allEdges) {
    ArrayList<CFAGoal> shuffledEdges = new ArrayList<>(allEdges);
    Collections.shuffle(shuffledEdges);
    return createOrderedPartition(shuffledEdges);
  }

  private Comparator<CFAGoal> createDomComperator(boolean reverse) {
    return new Comparator<CFAGoal>() {
      @Override
      public int compare(CFAGoal pO1, CFAGoal pO2) {
        // TODO Auto-generated method stub
        List<CFAEdge> o1Edges = pO1.getCFAEdgesGoal().getPath().getEdges();
        assert o1Edges.size() == 1;
        List<CFAEdge> o2Edges = pO2.getCFAEdgesGoal().getPath().getEdges();
        assert o2Edges.size() == 1;
        CFANode o1Succ = o1Edges.get(0).getSuccessor();
        CFANode o2Succ = o2Edges.get(0).getSuccessor();
        if (reverse) {
          return o1Succ.getReversePostorderId() - o2Succ.getReversePostorderId();
        } else {
          return o2Succ.getReversePostorderId() - o1Succ.getReversePostorderId();
        }
      }
    };
  }

  private List<Set<CFAGoal>> createDominationPartition(Set<CFAGoal> allEdges) {
    List<CFAGoal> sortedEdges = new ArrayList<>(allEdges);
    sortedEdges.sort(createDomComperator(false));
    return createOrderedPartition(sortedEdges);
  }

  private List<Set<CFAGoal>> createReverseDominationPartition(Set<CFAGoal> allEdges) {
    List<CFAGoal> sortedEdges = new ArrayList<>(allEdges);
    sortedEdges.sort(createDomComperator(true));
    return createOrderedPartition(sortedEdges);
  }

  public List<Set<CFAGoal>>
      createPartition(Set<CFAGoal> allEdges, @SuppressWarnings("unused") CFA cfa) {
    if (strategy == Strategy.RANDOM) {
      return createRandomPartition(allEdges);
    }
    if (strategy == Strategy.DOMINATION) {
      return createDominationPartition(allEdges);
    }
    if (strategy == Strategy.REVERSEDOMINATION) {
      return createReverseDominationPartition(allEdges);
    }
    return null;
  }
}
