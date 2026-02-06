// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssStatisticsMessage.StatisticsKey;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;

public class DssMessageFactory {

  private final boolean exportTimestamp;

  public static final String DSS_MESSAGE_STATUS_KEY = "status";
  public static final String DSS_MESSAGE_PRECISE_KEY = "precise";
  public static final String DSS_MESSAGE_PROPERTY_KEY = "property";
  public static final String DSS_MESSAGE_SOUND_KEY = "sound";

  public DssMessageFactory(DssAnalysisOptions pOptions) {
    exportTimestamp = pOptions.isDebugModeEnabled();
  }

  private ImmutableMap<String, String> serializeStatus(AlgorithmStatus pStatus) {
    ContentBuilder contentBuilder = ContentBuilder.builder();
    return contentBuilder
        .pushLevel(DSS_MESSAGE_STATUS_KEY)
        .put(DSS_MESSAGE_SOUND_KEY, Boolean.toString(pStatus.isSound()))
        .put(DSS_MESSAGE_PRECISE_KEY, Boolean.toString(pStatus.isPrecise()))
        .put(DSS_MESSAGE_PROPERTY_KEY, Boolean.toString(pStatus.wasPropertyChecked()))
        .build();
  }

  public DssPostConditionMessage createDssPreconditionMessage(
      String pSenderId,
      boolean pReachable,
      AlgorithmStatus pStatus,
      List<String> pReceivers,
      ImmutableMap<String, String> pStateContent) {
    return new DssPostConditionMessage(
        pSenderId,
        pReceivers,
        ImmutableMap.<String, String>builder()
            .putAll(serializeStatus(pStatus))
            .put(DssPostConditionMessage.DSS_MESSAGE_REACHABLE_KEY, Boolean.toString(pReachable))
            .putAll(pStateContent)
            .buildOrThrow());
  }

  public DssViolationConditionMessage createViolationConditionMessage(
      String pSenderId,
      AlgorithmStatus pStatus,
      boolean isFirst,
      ImmutableMap<String, String> pStateContent) {
    return new DssViolationConditionMessage(
        pSenderId,
        ImmutableMap.<String, String>builder()
            .putAll(serializeStatus(pStatus))
            .putAll(pStateContent)
            .put(DssViolationConditionMessage.DSS_MESSAGE_FIRST_KEY, Boolean.toString(isFirst))
            .buildOrThrow());
  }

  public DssStatisticsMessage createDssStatisticsMessage(
      String pSenderId, ImmutableMap<StatisticsKey, String> pContent) {
    ImmutableMap.Builder<String, String> serializedContentBuilder = ImmutableMap.builder();
    for (Map.Entry<StatisticsKey, String> entry : pContent.entrySet()) {
      serializedContentBuilder.put(entry.getKey().name(), entry.getValue());
    }
    return new DssStatisticsMessage(pSenderId, serializedContentBuilder.buildOrThrow());
  }

  public DssResultMessage createDssResultMessage(String pSenderId, Result pResult) {
    return new DssResultMessage(pSenderId, pResult.name());
  }

  public DssExceptionMessage createDssExceptionMessage(String pSenderId, Throwable pThrowable) {
    return new DssExceptionMessage(pSenderId, Throwables.getStackTraceAsString(pThrowable));
  }

  public ImmutableMap<String, ImmutableMap<String, String>> export(DssMessage pMessage) {
    if (!exportTimestamp) {
      ImmutableMap<String, ImmutableMap<String, String>> messageContent = pMessage.asJson();
      ImmutableMap.Builder<String, ImmutableMap<String, String>> noTimestampMessage =
          ImmutableMap.builder();
      ImmutableMap<String, String> header =
          Objects.requireNonNull(
              messageContent.get(DssMessage.DSS_MESSAGE_HEADER_ID),
              "Header must not be null in DssMessage export");
      Map<String, String> filteredHeader =
          Maps.filterKeys(header, key -> key != null && !key.equals("timestamp"));
      noTimestampMessage.put(DssMessage.DSS_MESSAGE_HEADER_ID, ImmutableMap.copyOf(filteredHeader));
      noTimestampMessage.put(
          DssMessage.DSS_MESSAGE_CONTENT_ID,
          Objects.requireNonNull(
              messageContent.get(DssMessage.DSS_MESSAGE_CONTENT_ID),
              "Content must not be null in DssMessage export"));
      return noTimestampMessage.buildOrThrow();
    }
    return pMessage.asJson();
  }
}
