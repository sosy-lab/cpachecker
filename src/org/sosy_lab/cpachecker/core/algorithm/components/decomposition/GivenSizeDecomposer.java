// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.decomposition;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;

public class GivenSizeDecomposer implements CFADecomposer {

  private final int desiredNumberOfBlocks;
  private final CFADecomposer decomposer;

  public GivenSizeDecomposer(int pDesiredNumberOfBlocks, CFADecomposer pDecomposer) {
    desiredNumberOfBlocks = pDesiredNumberOfBlocks;
    decomposer = pDecomposer;
  }

  @Override
  public BlockTree cut(CFA cfa) {
    BlockTree tree = decomposer.cut(cfa);
    Set<BlockNode> nodes = new HashSet<>(tree.getDistinctNodes());
    Multimap<String, BlockNode> compatibleBlocks = ArrayListMultimap.create();
    nodes.forEach(n -> compatibleBlocks.put("N" + n.getStartNode().getNodeNumber() + "N" + n.getLastNode(), n));
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
    while(desiredNumberOfBlocks < nodes.size()) {
      Optional<BlockNode> potentialNode = nodes.stream().filter(n -> n.getSuccessors().size() == 1 && !alreadyFound.contains(n)).findAny();
      if (potentialNode.isEmpty()) {
        break;
      }
      BlockNode node = potentialNode.orElseThrow();
      alreadyFound.add(node);
      List<BlockNode> successors = new ArrayList<>(node.getSuccessors());
      assert successors.size() == 1;
      BlockNode singleSuccessor = successors.get(0);
      if (singleSuccessor.getPredecessors().size() == 1) {
        BlockNode merged = tree.mergeSingleSuccessors(node, singleSuccessor);
        nodes.remove(node);
        nodes.remove(singleSuccessor);
        nodes.add(merged);
      }
    }
    return tree;
  }
}
