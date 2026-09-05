// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
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

  private ImmutableMap<String, String> witnessType(WitnessType pWitnessType) {
    return ImmutableMap.of(DssMessageKeys.WITNESS_TYPE, pWitnessType.name());
  }

  public DssPostConditionMessage createDssPostConditionMessage(
      String pSenderId,
      AlgorithmStatus pStatus,
      ImmutableList<ImmutableMap<String, String>> pStates) {
    return new DssPostConditionMessage(pSenderId, pStatus, pStates);
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
    return DssPostConditionMessage.unreachableBlockEndMessage(pSenderId, pStatus);
  }

  public DssViolationConditionMessage createViolationConditionMessage(
      String pSenderId,
      AlgorithmStatus pStatus,
      ImmutableList<ImmutableMap<String, String>> pStates) {
    return new DssViolationConditionMessage(pSenderId, pStatus, pStates);
  }

  public DssWitnessMessage createDssCorrectnessWitnessMessage(
      String pSenderId,
      ImmutableList<ImmutableMap<String, String>> pSerializedRelevantPreconditions) {
    return new DssCorrectnessWitnessMessage(pSenderId, pSerializedRelevantPreconditions);
  }

  public DssWitnessMessage createDssViolationWitnessMessage(
      String pSenderId, SegmentedPaths violationWitness) {
    return new DssViolationWitnessMessage(pSenderId, violationWitness);
  }

  public DssResultMessage createDssResultMessage(String pSenderId, Result pResult) {
    return new DssResultMessage(pSenderId, pResult);
  }

  public DssExceptionMessage createDssExceptionMessage(String pSenderId, Throwable pThrowable) {
    return new DssExceptionMessage(pSenderId, Throwables.getStackTraceAsString(pThrowable));
  }

  public DssMessagePayload export(DssMessage pMessage) {
    DssMessagePayload payload = pMessage.asJsonPayload();
    return exportTimestamp ? payload : payload.withoutTimestamp();
  }
}
