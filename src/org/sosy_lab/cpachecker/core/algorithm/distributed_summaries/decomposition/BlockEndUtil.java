// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.cpa.block.BlockState.BlockStateType;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class BlockEndUtil {

  public static final String UNIQUE_DESCRIPTION = "<<distributed-block-summary-block-end>>";

  public static BlankEdge getBlockEndBlankEdge(FileLocation pFileLocation, CFANode pPredecessor) {
    return new BlankEdge(
        "",
        pFileLocation,
        pPredecessor,
        new CFANode(pPredecessor.getFunction()),
        UNIQUE_DESCRIPTION);
  }

  public static boolean hasAbstractionOccurred(BlockNode pBlockNode, ReachedSet pReachedSet) {
    if (pBlockNode.getEdgesInBlock().stream()
        .noneMatch(e -> e.getDescription().equals(UNIQUE_DESCRIPTION))) {
      return false;
    }
    if (pReachedSet.getReached(pBlockNode.getLastNode()).isEmpty()) {
      return false;
    }
    for (AbstractState abstractState : pReachedSet) {
      if (Objects.requireNonNull(AbstractStates.extractStateByType(abstractState, BlockState.class))
              .getType()
          == BlockStateType.ABSTRACTION) {
        return false;
      }
    }
    return true;
  }
}
