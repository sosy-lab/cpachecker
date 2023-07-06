// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.infer;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryConnection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryExceptionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryAnalysisOptions;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryObserverWorker;

public class InferObserverWorker extends BlockSummaryObserverWorker {

  public InferObserverWorker(
      String pId,
      BlockSummaryConnection pConnection,
      BlockSummaryAnalysisOptions pOptions,
      int pNumberOfBlocks) {
    super(pId, pConnection, pOptions, pNumberOfBlocks);
  }

  @Override
  public Collection<BlockSummaryMessage> processMessage(BlockSummaryMessage pMessage) {
    switch (pMessage.getType()) {
      case INFER_ROOT_PROOF -> {
        shutdown = true;
        result = Optional.of(Result.TRUE);
        statusObserver.updateStatus(pMessage);
      }
      case INFER_ROOT_VIOLATIONS -> {
        shutdown = true;
        result = Optional.of(Result.FALSE);
        statusObserver.updateStatus(pMessage);
      }
      case ERROR -> {
        shutdown = true;
        errorMessage = Optional.of(((BlockSummaryExceptionMessage) pMessage).getErrorMessage());
      }
      default -> { }
    }
    return ImmutableList.of();
  }
}
