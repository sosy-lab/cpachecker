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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DssMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;

// Reason for SupressWarnings: for debugging AND ONLY for debugging,
// we create the messages with a timestamp
@SuppressWarnings("deprecation")
public class DssMessageFactory {

  private final boolean debugMode;

  public DssMessageFactory(DssAnalysisOptions pOptions) {
    debugMode = pOptions.isDebugModeEnabled();
  }

  public DssMessage newBlockPostCondition(
      String pUniqueBlockId,
      int pTargetNodeNumber,
      DssMessagePayload pPayload,
      boolean pReachable) {
    DssMessagePayload newPayload =
        DssMessagePayload.builder()
            .addAllEntries(pPayload)
            .addEntry(DssMessagePayload.REACHABLE, Boolean.toString(pReachable))
            .buildPayload();
    if (debugMode) {
      return new DssPostConditionMessage(
          pUniqueBlockId, pTargetNodeNumber, newPayload, getTimestampForMessage());
    } else {
      return new DssPostConditionMessage(pUniqueBlockId, pTargetNodeNumber, newPayload);
    }
  }

  public DssMessage newErrorConditionMessage(
      String pUniqueBlockId,
      int pTargetNodeNumber,
      DssMessagePayload pPayload,
      boolean pFirst,
      String pOrigin) {
    DssMessagePayload newPayload =
        DssMessagePayload.builder()
            .addAllEntries(pPayload)
            .addEntry(DssMessagePayload.FIRST, Boolean.toString(pFirst))
            .addEntry(DssMessagePayload.ORIGIN, pOrigin)
            .buildPayload();
    if (debugMode) {
      return new DssErrorConditionMessage(
          pUniqueBlockId, pTargetNodeNumber, newPayload, getTimestampForMessage());
    } else {
      return new DssErrorConditionMessage(pUniqueBlockId, pTargetNodeNumber, newPayload);
    }
  }

  public DssMessage newResultMessage(
      String pUniqueBlockId, int pTargetNodeNumber, Result pResult) {
    DssMessagePayload payload =
        DssMessagePayload.builder()
            .addEntry(DssMessagePayload.RESULT, pResult.name())
            .buildPayload();
    if (debugMode) {
      return new DssResultMessage(
          pUniqueBlockId, pTargetNodeNumber, payload, getTimestampForMessage());
    } else {
      return new DssResultMessage(pUniqueBlockId, pTargetNodeNumber, payload);
    }
  }

  public DssMessage newErrorMessage(String pUniqueBlockId, Throwable pException) {
    DssMessagePayload payload =
        DssMessagePayload.builder()
            .addEntry(
                DssMessagePayload.EXCEPTION, Throwables.getStackTraceAsString(pException))
            .buildPayload();
    if (debugMode) {
      return new DssExceptionMessage(pUniqueBlockId, 0, payload, getTimestampForMessage());
    } else {
      return new DssExceptionMessage(pUniqueBlockId, 0, payload);
    }
  }

  public DssMessage newStatisticsMessage(
      String pUniqueBlockId, Map<String, Object> pStats) {
    DssMessagePayload payload =
        DssMessagePayload.builder()
            .addEntry(DssMessagePayload.STATS, pStats)
            .buildPayload();
    if (debugMode) {
      return new DssStatisticsMessage(
          pUniqueBlockId, 0, payload, getTimestampForMessage());
    } else {
      return new DssStatisticsMessage(pUniqueBlockId, 0, payload);
    }
  }

  private Instant getTimestampForMessage() {
    Verify.verify(debugMode, "Timestamp should only be created in debug mode");
    return Instant.now();
  }
}
