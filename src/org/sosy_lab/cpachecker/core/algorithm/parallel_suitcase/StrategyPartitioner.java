// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.graph.dominance.DomTree;

/**
 * Strategy-based partitioner using dominator tree information. Groups edges by their immediate
 * dominator to maintain control-flow locality, then distributes groups randomly to achieve target
 * partition count. This implementation performs only one round of strategic grouping.
 */
public class StrategyPartitioner implements PartitioningStrategy {

  private final DomTree<CFANode> domTree;
  private final CFANode root;

  /**
   * Constructs a StrategyPartitioner with dominator tree built from CFA.
   *
   * @param cfa Control Flow Automaton for the program
   */
  public StrategyPartitioner(CFA cfa) {
    this.domTree = DomTree.forGraph(cfa, cfa.getMainFunction());
    this.root =
        domTree.getRoot().orElseThrow(() -> new IllegalStateException("No root node found"));
  }

  @Override
  public List<Set<CFAEdge>> partition(Set<CFAEdge> testTargets, int numPartitions, CFA cfa) {
    Preconditions.checkArgument(numPartitions > 0, "Number of partitions must be positive");

    if (testTargets.isEmpty()) {
      return createEmptyPartitions(numPartitions);
    }

    // Step 1: Group edges by their immediate dominator
    List<Set<CFAEdge>> dominatorGroups = groupByImmediateDominator(testTargets);

    // Step 2: Distribute groups to final partitions
    return distributeGroupsToPartitions(dominatorGroups, numPartitions);
  }

  /**
   * Groups edges based on the immediate dominator of their start node. Edges with the same
   * immediate dominator are placed in the same group to maintain control-flow locality.
   *
   * @param testTargets Set of CFA edges to partition
   * @return List of groups, each containing edges with the same dominator
   */
  private List<Set<CFAEdge>> groupByImmediateDominator(Set<CFAEdge> testTargets) {
    Map<CFANode, Set<CFAEdge>> groupMap = new HashMap<>();

    for (CFAEdge edge : testTargets) {
      // Use the start node of the edge to find its dominator
      CFANode startNode = edge.getPredecessor();

      // Get immediate dominator, fall back to root if none exists
      CFANode dominator = domTree.getParent(startNode).orElse(root);

      // Add edge to the group for this dominator
      groupMap.computeIfAbsent(dominator, k -> new HashSet<>()).add(edge);
    }

    return new ArrayList<>(groupMap.values());
  }

  /**
   * Distributes dominator groups to final partitions using random distribution. Each group stays
   * together (edges in same group go to same partition). Groups are assigned to partitions in
   * round-robin fashion after shuffling.
   *
   * @param dominatorGroups List of groups created by dominator grouping
   * @param numPartitions Target number of partitions
   * @return List of partitions, each containing edges from one or more groups
   */
  private List<Set<CFAEdge>> distributeGroupsToPartitions(
      List<Set<CFAEdge>> dominatorGroups, int numPartitions) {

    // Initialize empty partitions
    List<Set<CFAEdge>> partitions = new ArrayList<>(numPartitions);
    for (int i = 0; i < numPartitions; i++) {
      partitions.add(new HashSet<>());
    }

    // Shuffle groups for random distribution
    // Fixed seed ensures reproducibility
    List<Set<CFAEdge>> shuffledGroups = new ArrayList<>(dominatorGroups);
    Collections.shuffle(shuffledGroups, new Random(0));

    // Assign each complete group to a partition using round-robin
    for (int i = 0; i < shuffledGroups.size(); i++) {
      int partitionIndex = i % numPartitions;
      partitions.get(partitionIndex).addAll(shuffledGroups.get(i));
    }

    return partitions;
  }

  /**
   * Creates empty partitions for the case when test targets set is empty.
   *
   * @param numPartitions Number of partitions to create
   * @return List of empty partitions
   */
  private List<Set<CFAEdge>> createEmptyPartitions(int numPartitions) {
    List<Set<CFAEdge>> partitions = new ArrayList<>(numPartitions);
    for (int i = 0; i < numPartitions; i++) {
      partitions.add(new HashSet<>());
    }
    return partitions;
  }
}
