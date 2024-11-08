// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages;

import com.google.common.base.Throwables;
import java.util.Map;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;

public class BlockSummaryMessageFactory {

  public static BlockSummaryMessage newBlockPostCondition(
      String pUniqueBlockId,
      int pTargetNodeNumber,
      BlockSummaryMessagePayload pPayload,
      boolean pReachable) {
    BlockSummaryMessagePayload newPayload =
        BlockSummaryMessagePayload.builder()
            .addAllEntries(pPayload)
            .addEntry(BlockSummaryMessagePayload.REACHABLE, Boolean.toString(pReachable))
            .buildPayload();
    return new BlockSummaryPostConditionMessage(pUniqueBlockId, pTargetNodeNumber, newPayload);
  }

  public static BlockSummaryMessage newErrorConditionMessage(
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
    return new BlockSummaryErrorConditionMessage(pUniqueBlockId, pTargetNodeNumber, newPayload);
  }

  public static BlockSummaryMessage newErrorConditionUnreachableMessage(
      String pUniqueBlockId, String denied) {
    return new BlockSummaryErrorConditionUnreachableMessage(
        pUniqueBlockId,
        0,
        BlockSummaryMessagePayload.builder()
            .addEntry(BlockSummaryMessagePayload.REASON, denied)
            .addEntry("readable", denied)
            .buildPayload());
  }

  public static BlockSummaryMessage newResultMessage(
      String pUniqueBlockId, int pTargetNodeNumber, Result pResult) {
    BlockSummaryMessagePayload payload =
        BlockSummaryMessagePayload.builder()
            .addEntry(BlockSummaryMessagePayload.RESULT, pResult.name())
            .buildPayload();
    return new BlockSummaryResultMessage(pUniqueBlockId, pTargetNodeNumber, payload);
  }

  public static BlockSummaryMessage newErrorMessage(String pUniqueBlockId, Throwable pException) {
    return new BlockSummaryExceptionMessage(
        pUniqueBlockId,
        0,
        BlockSummaryMessagePayload.builder()
            .addEntry(
                BlockSummaryMessagePayload.EXCEPTION, Throwables.getStackTraceAsString(pException))
            .buildPayload());
  }

  public static BlockSummaryMessage newStatisticsMessage(
      String pUniqueBlockId, Map<String, Object> pStats) {
    return new BlockSummaryStatisticsMessage(
        pUniqueBlockId,
        0,
        BlockSummaryMessagePayload.builder()
            .addEntry(BlockSummaryMessagePayload.STATS, pStats)
            .buildPayload());
  }
}
