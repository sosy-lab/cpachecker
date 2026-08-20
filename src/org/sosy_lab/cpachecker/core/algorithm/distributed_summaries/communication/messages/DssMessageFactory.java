// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssWitnessMessage.WitnessType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.cpa.pathrestriction.SegmentedPaths;

public class DssMessageFactory {

  private final boolean exportTimestamp;

  public static final String DSS_MESSAGE_STATUS_KEY = "status";
  public static final String DSS_MESSAGE_PRECISE_KEY = "precise";
  public static final String DSS_MESSAGE_PROPERTY_KEY = "property";
  public static final String DSS_MESSAGE_SOUND_KEY = "sound";
  public static final String DSS_MESSAGE_UNREACHABLE_BLOCK_END_KEY = "unreachableBlockEnd";
  public static final String DSS_MESSAGE_INCOMPLETE_SOURCES_KEY = "incompleteSources";

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

  private ImmutableMap<String, String> witnessType(WitnessType pWitnessType) {
    return ImmutableMap.of(DssWitnessMessage.DSS_MESSAGE_WITNESS_TYPE_KEY, pWitnessType.name());
  }

  /**
   * Creates a postcondition message.
   *
   * @param pIncompleteSources the blocks that keep these summaries from covering the whole block
   *     end, see {@link DssMessage#getIncompleteSources()}
   */
  public DssPostConditionMessage createDssPostConditionMessage(
      String pSenderId,
      AlgorithmStatus pStatus,
      ImmutableMap<String, String> pStateContent,
      Set<String> pIncompleteSources) {
    ImmutableMap.Builder<String, String> content =
        ImmutableMap.<String, String>builder()
            .putAll(serializeStatus(pStatus))
            .putAll(pStateContent);
    if (!pIncompleteSources.isEmpty()) {
      content.put(DSS_MESSAGE_INCOMPLETE_SOURCES_KEY, Joiner.on(',').join(pIncompleteSources));
    }
    return new DssPostConditionMessage(pSenderId, content.buildOrThrow());
  }

  /**
   * Creates a postcondition message that reports the end of the sender's block as unreachable,
   * i.e., the block produced no state at its final location.
   *
   * <p>The message deliberately carries no states: the flag alone is the signal, so neither sender
   * nor receiver has to (de)serialize an abstract state to communicate unreachability.
   *
   * @param pSenderId the ID of the block whose end is unreachable
   * @param pStatus the status of the analysis that found the block end to be unreachable
   */
  public DssPostConditionMessage createDssUnreachableBlockEndMessage(
      String pSenderId, AlgorithmStatus pStatus) {
    return new DssPostConditionMessage(
        pSenderId,
        ImmutableMap.<String, String>builder()
            .putAll(serializeStatus(pStatus))
            .put(DSS_MESSAGE_UNREACHABLE_BLOCK_END_KEY, "true")
            .buildOrThrow());
  }

  public DssViolationConditionMessage createViolationConditionMessage(
      String pSenderId, AlgorithmStatus pStatus, ImmutableMap<String, String> pStateContent) {
    return new DssViolationConditionMessage(
        pSenderId,
        ImmutableMap.<String, String>builder()
            .putAll(serializeStatus(pStatus))
            .putAll(pStateContent)
            .buildOrThrow());
  }

  public DssWitnessMessage createDssCorrectnessWitnessMessage(
      String pSenderId, ImmutableMap<String, String> pSerializedRelevantPreconditions) {
    return new DssWitnessMessage(
        pSenderId,
        ImmutableMap.<String, String>builder()
            .putAll(witnessType(WitnessType.CORRECTNESS))
            .putAll(pSerializedRelevantPreconditions)
            .buildOrThrow());
  }

  public DssWitnessMessage createDssViolationWitnessMessage(
      String pSenderId, SegmentedPaths violationWitness) {
    return new DssWitnessMessage(
        pSenderId,
        ImmutableMap.<String, String>builder()
            .putAll(witnessType(WitnessType.VIOLATION))
            .put(DssWitnessMessage.DSS_MESSAGE_VIOLATION_PATH_KEY, violationWitness.serialize())
            .buildOrThrow());
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
