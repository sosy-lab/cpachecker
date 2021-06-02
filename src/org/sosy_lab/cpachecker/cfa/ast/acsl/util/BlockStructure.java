// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.util;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class BlockStructure {

  private final CFA cfa;
  private final ImmutableSet<Block> blocks;

  public BlockStructure(CFA pCfa, Set<Block> pBlocks) {
    cfa = pCfa;
    blocks = ImmutableSet.copyOf(pBlocks);
  }

  public Set<Block> getBlocks() {
    return blocks;
  }

  public Block getInnermostBlockOf(CFANode node) {
    Block innermostBlock = null;
    for (Block block : blocks) {
      if (block.getContainedNodes().contains(node)
          && (innermostBlock == null || innermostBlock.contains(block))) {
        innermostBlock = block;
      }
    }
    return innermostBlock;
  }

  public Block getInnermostBlockOf(FileLocation location) {
    Block innermostBlock = null;
    for (Block block : blocks) {
      if (block.getStartOffset() < location.getNodeOffset()
          && location.getNodeOffset() < block.getEndOffset()
          && (innermostBlock == null || innermostBlock.contains(block))) {
        innermostBlock = block;
      }
    }
    return innermostBlock;
  }

  public Set<CFANode> getNodesInInnermostBlockOf(CFANode node) {
    return getInnermostBlockOf(node).getContainedNodes();
  }
}
