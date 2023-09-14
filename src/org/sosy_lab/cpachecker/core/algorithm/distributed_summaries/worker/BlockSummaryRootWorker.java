// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;

public class BlockSummaryRootWorker extends BlockSummaryWorker {

  private final BlockNode root;
  private final BlockSummaryConnection connection;
  private boolean shutdown;

  BlockSummaryRootWorker(
      String pId,
      BlockSummaryConnection pConnection,
      BlockSummaryAnalysisOptions pOptions,
      BlockNode pNode) {
    super("root-worker-" + pId, pOptions);
    checkArgument(
        pNode.isRoot() && pNode.isEmpty() && pNode.getLast().equals(pNode.getFirst()),
        "Root node must be empty and cannot have predecessors: " + "%s",
        pNode);

    connection = pConnection;
    shutdown = false;
    root = pNode;
  }

  @Override
  public Collection<BlockSummaryMessage> processMessage(BlockSummaryMessage pMessage) {
    return switch (pMessage.getType()) {
      case ERROR_CONDITION -> {
        if (pMessage.getTargetNodeNumber() == root.getLast().getNodeNumber()) {
          yield ImmutableSet.of(
              BlockSummaryMessage.newResultMessage(
                  root.getId(), root.getLast().getNodeNumber(), Result.FALSE));
        }
        yield ImmutableSet.of();
      }
      case FOUND_RESULT, ERROR -> {
        shutdown = true;
        yield ImmutableSet.of();
      }
      case STATISTICS, BLOCK_POSTCONDITION, ERROR_CONDITION_UNREACHABLE -> ImmutableSet.of();
    };
  }

  @Override
  public BlockSummaryConnection getConnection() {
    return connection;
  }

  @Override
  public boolean shutdownRequested() {
    return shutdown;
  }
}
