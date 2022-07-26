// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;

public class GivenSizeDecomposer implements CFADecomposer {

  private final CFADecomposer decomposer;
  private final int desiredNumberOfBlocks;

  /**
   * A decomposer that merges as many parts as possible to maybe reach the desired number of blocks
   *
   * @param pDecomposer parent decomposer
   * @param pDesiredNumber desired number of blocks
   * @throws InvalidConfigurationException thrown if configuration is invalid
   */
  public GivenSizeDecomposer(CFADecomposer pDecomposer, int pDesiredNumber)
      throws InvalidConfigurationException {
    decomposer = pDecomposer;
    desiredNumberOfBlocks = pDesiredNumber;
  }

  @Override
  public BlockTree cut(CFA cfa) {
    BlockTree tree = decomposer.cut(cfa);
    int oldSize = tree.getDistinctNodes().size();
    while (oldSize >= desiredNumberOfBlocks) {
      mergeTree(tree);
      int newSize = tree.getDistinctNodes().size();
      if (newSize == oldSize) {
        break;
      }
      oldSize = newSize;
    }
    return tree;
  }

  private void mergeTree(BlockTree tree) {
    Set<BlockNode> nodes = new HashSet<>(tree.getDistinctNodes());
    nodes.remove(tree.getRoot());
    Multimap<String, BlockNode> compatibleBlocks = ArrayListMultimap.create();
    nodes.forEach(
        n ->
            compatibleBlocks.put(
                "N" + n.getStartNode().getNodeNumber() + "N" + n.getLastNode(), n));
    for (String key : ImmutableSet.copyOf(compatibleBlocks.keySet())) {
      List<BlockNode> mergeNodes = new ArrayList<>(compatibleBlocks.removeAll(key));
      if (nodes.size() <= desiredNumberOfBlocks) {
        break;
      }
      if (mergeNodes.size() > 1) {
        BlockNode current = mergeNodes.remove(0);
        nodes.remove(current);
        for (int i = mergeNodes.size() - 1; i >= 0; i--) {
          BlockNode remove = mergeNodes.remove(i);
          nodes.remove(remove);
          current = tree.mergeSameStartAndEnd(current, remove);
        }
        nodes.add(current);
        compatibleBlocks.put(key, current);
      }
    }
    Set<BlockNode> alreadyFound = new HashSet<>();
    while (desiredNumberOfBlocks < nodes.size()) {
      Optional<BlockNode> potentialNode =
          nodes.stream()
              .filter(n -> n.getSuccessors().size() == 1 && !alreadyFound.contains(n))
              .findAny();
      if (potentialNode.isEmpty()) {
        break;
      }
      BlockNode node = potentialNode.orElseThrow();
      alreadyFound.add(node);
      if (node.isRoot()) {
        continue;
      }
      BlockNode singleSuccessor = Iterables.getOnlyElement(node.getSuccessors());
      if (singleSuccessor.getPredecessors().size() == 1) {
        BlockNode merged = tree.mergeSingleSuccessors(node, singleSuccessor);
        nodes.remove(node);
        nodes.remove(singleSuccessor);
        nodes.add(merged);
      }
    }
  }
}
