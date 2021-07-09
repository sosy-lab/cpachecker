// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.tree;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public class BlockTree {

  private final BlockNode root;

  public BlockTree(BlockNode pRoot) {
    root = pRoot;
  }

  public boolean isEmpty() {
    return root.getSuccessors().isEmpty();
  }

  public BlockNode getRoot() {
    return root;
  }

  public Set<BlockNode> getDistinctNodes() {
    Set<BlockNode> nodes = new HashSet<>();
    ArrayDeque<BlockNode> waiting = new ArrayDeque<>();
    waiting.add(root);
    while(!waiting.isEmpty()) {
      BlockNode top = waiting.pop();
      if (!nodes.contains(top)) {
        nodes.add(top);
        waiting.addAll(top.getSuccessors());
      }
    }
    return nodes;
  }

}
