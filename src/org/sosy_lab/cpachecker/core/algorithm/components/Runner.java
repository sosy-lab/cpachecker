// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components;

import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.components.tree.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.components.tree.BlockTree;

public class Runner {

  private final BlockNode block;
  private final Algorithm analysis;

  private Runner(BlockNode pBlock, Algorithm pAlgorithm) {
    block = pBlock;
    analysis = pAlgorithm;
  }

  public static Runner on(BlockNode node, Algorithm algorithm) {
    return new Runner(node, algorithm);
  }

  public static void analyzeTree(BlockTree tree, Algorithm algorithm) {
    tree.getDistinctNodes().forEach(node -> on(node, algorithm).analyzeBlock());
  }

  public void analyzeBlock() {
    // TODO
  }
}
