// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.util;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class BlockStructure {
  private final ImmutableSet<ACSLBlock> blocks;

  BlockStructure(Collection<ACSLBlock> pBlocks) {
    blocks = ImmutableSet.copyOf(pBlocks);
  }

  public Set<ACSLBlock> getBlocks() {
    return blocks;
  }

  public ACSLBlock getInnermostBlockOf(CFANode node) {
    ACSLBlock innermostBlock = null;
    for (ACSLBlock block : blocks) {
      if (block.getContainedNodes().contains(node)
          && (innermostBlock == null || innermostBlock.contains(block))) {
        innermostBlock = block;
      }
    }
    return innermostBlock;
  }

  public ACSLBlock getInnermostBlockOf(FileLocation location) {
    ACSLBlock innermostBlock = null;
    for (ACSLBlock block : blocks) {
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
