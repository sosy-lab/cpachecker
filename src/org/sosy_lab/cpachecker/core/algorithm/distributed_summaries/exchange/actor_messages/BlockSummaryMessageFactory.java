// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages;

import com.google.common.base.Throwables;
import com.google.common.base.Verify;
import java.time.Instant;
import java.util.Map;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryAnalysisOptions;

// Reason for SupressWarnings: for debugging AND ONLY for debugging,
// we create the messages with a timestamp
@SuppressWarnings("deprecation")
public class BlockSummaryMessageFactory {

  private final boolean debugMode;

  public BlockSummaryMessageFactory(BlockSummaryAnalysisOptions pOptions) {
    debugMode = pOptions.isDebugModeEnabled();
  }

  public BlockSummaryMessage newBlockPostCondition(
      String pUniqueBlockId,
      int pTargetNodeNumber,
      BlockSummaryMessagePayload pPayload,
      boolean pReachable) {
    BlockSummaryMessagePayload newPayload =
        BlockSummaryMessagePayload.builder()
            .addAllEntries(pPayload)
            .addEntry(BlockSummaryMessagePayload.REACHABLE, Boolean.toString(pReachable))
            .buildPayload();
    if (debugMode) {
      return new BlockSummaryPostConditionMessage(
          pUniqueBlockId, pTargetNodeNumber, newPayload, getTimestampForMessage());
    } else {
      return new BlockSummaryPostConditionMessage(pUniqueBlockId, pTargetNodeNumber, newPayload);
    }
  }

  public BlockSummaryMessage newErrorConditionMessage(
      String pUniqueBlockId,
      int pTargetNodeNumber,
      BlockSummaryMessagePayload pPayload,
      boolean pFirst,
      String pOrigin) {
    BlockSummaryMessagePayload newPayload =
        BlockSummaryMessagePayload.builder()
            .addAllEntries(pPayload)
            .addEntry(BlockSummaryMessagePayload.FIRST, Boolean.toString(pFirst))
            .addEntry(BlockSummaryMessagePayload.ORIGIN, pOrigin)
            .buildPayload();
    if (debugMode) {
      return new BlockSummaryErrorConditionMessage(
          pUniqueBlockId, pTargetNodeNumber, newPayload, getTimestampForMessage());
    } else {
      return new BlockSummaryErrorConditionMessage(pUniqueBlockId, pTargetNodeNumber, newPayload);
    }
  }

  public BlockSummaryMessage newResultMessage(
      String pUniqueBlockId, int pTargetNodeNumber, Result pResult) {
    BlockSummaryMessagePayload payload =
        BlockSummaryMessagePayload.builder()
            .addEntry(BlockSummaryMessagePayload.RESULT, pResult.name())
            .buildPayload();
    if (debugMode) {
      return new BlockSummaryResultMessage(
          pUniqueBlockId, pTargetNodeNumber, payload, getTimestampForMessage());
    } else {
      return new BlockSummaryResultMessage(pUniqueBlockId, pTargetNodeNumber, payload);
    }
  }

  public BlockSummaryMessage newErrorMessage(String pUniqueBlockId, Throwable pException) {
    BlockSummaryMessagePayload payload =
        BlockSummaryMessagePayload.builder()
            .addEntry(
                BlockSummaryMessagePayload.EXCEPTION, Throwables.getStackTraceAsString(pException))
            .buildPayload();
    if (debugMode) {
      return new BlockSummaryExceptionMessage(pUniqueBlockId, 0, payload, getTimestampForMessage());
    } else {
      return new BlockSummaryExceptionMessage(pUniqueBlockId, 0, payload);
    }
  }

  public BlockSummaryMessage newStatisticsMessage(
      String pUniqueBlockId, Map<String, Object> pStats) {
    BlockSummaryMessagePayload payload =
        BlockSummaryMessagePayload.builder()
            .addEntry(BlockSummaryMessagePayload.STATS, pStats)
            .buildPayload();
    if (debugMode) {
      return new BlockSummaryStatisticsMessage(
          pUniqueBlockId, 0, payload, getTimestampForMessage());
    } else {
      return new BlockSummaryStatisticsMessage(pUniqueBlockId, 0, payload);
    }
  }

  private Instant getTimestampForMessage() {
    Verify.verify(debugMode, "Timestamp should only be created in debug mode");
    return Instant.now();
  }
}
