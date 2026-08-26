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
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssWitnessMessage.WitnessType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.cpa.pathrestriction.SegmentedPaths;

public class DssMessageFactory {

  private final boolean exportTimestamp;

  public DssMessageFactory(DssAnalysisOptions pOptions) {
    exportTimestamp = pOptions.isDebugModeEnabled();
  }

  private ImmutableMap<String, String> serializeStatus(AlgorithmStatus pStatus) {
    ContentBuilder contentBuilder = ContentBuilder.builder();
    return contentBuilder
        .pushLevel(DssMessageFormat.STATUS_KEY)
        .put(DssMessageFormat.SOUND_KEY, Boolean.toString(pStatus.isSound()))
        .put(DssMessageFormat.PRECISE_KEY, Boolean.toString(pStatus.isPrecise()))
        .put(DssMessageFormat.PROPERTY_KEY, Boolean.toString(pStatus.wasPropertyChecked()))
        .build();
  }

  private ImmutableMap<String, String> witnessType(WitnessType pWitnessType) {
    return ImmutableMap.of(DssMessageFormat.WITNESS_TYPE_KEY, pWitnessType.name());
  }

  public DssPostConditionMessage createDssPostConditionMessage(
      String pSenderId, AlgorithmStatus pStatus, ImmutableMap<String, String> pStateContent) {
    return new DssPostConditionMessage(
        pSenderId,
        pStatus,
        ImmutableMap.<String, String>builder().putAll(pStateContent).buildOrThrow());
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
        pStatus,
        ImmutableMap.<String, String>builder()
            .put(DssMessageFormat.UNREACHABLE_BLOCK_END_KEY, "true")
            .buildOrThrow());
  }

  public DssViolationConditionMessage createViolationConditionMessage(
      String pSenderId, AlgorithmStatus pStatus, ImmutableMap<String, String> pStateContent) {
    return new DssViolationConditionMessage(
        pSenderId,
        pStatus,
        ImmutableMap.<String, String>builder().putAll(pStateContent).buildOrThrow());
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
            .put(DssMessageFormat.VIOLATION_PATH_KEY, violationWitness.serialize())
            .buildOrThrow());
  }

  public DssResultMessage createDssResultMessage(String pSenderId, Result pResult) {
    return new DssResultMessage(pSenderId, pResult.name());
  }

  public DssExceptionMessage createDssExceptionMessage(String pSenderId, Throwable pThrowable) {
    return new DssExceptionMessage(pSenderId, Throwables.getStackTraceAsString(pThrowable));
  }

  public DssMessagePayload export(DssMessage pMessage) {
    DssMessagePayload payload = pMessage.asJsonPayload();
    return exportTimestamp ? payload : payload.withoutTimestamp();
  }
}
