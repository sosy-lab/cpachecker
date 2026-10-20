// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa.DssBlockStatePayload;

public final class DssViolationConditionMessage implements DssConditionMessage {

  private final String senderId;
  private final Instant timestamp;
  private final AlgorithmStatus status;
  private final ImmutableList<DssStateAndPrecisionPayload> states;

  public DssViolationConditionMessage(
      String pSenderId,
      AlgorithmStatus pStatus,
      ImmutableList<DssStateAndPrecisionPayload> pStates) {
    this(pSenderId, Instant.now(), pStatus, pStates);
  }

  DssViolationConditionMessage(
      String pSenderId,
      Instant pTimestamp,
      AlgorithmStatus pStatus,
      ImmutableList<DssStateAndPrecisionPayload> pStates) {
    senderId = Preconditions.checkNotNull(pSenderId);
    timestamp = Preconditions.checkNotNull(pTimestamp);
    status = Preconditions.checkNotNull(pStatus);
    states = ImmutableList.copyOf(Preconditions.checkNotNull(pStates));

    Preconditions.checkArgument(
        !states.isEmpty(), "Violation-condition message requires at least one state");
  }

  @Override
  public String getSenderId() {
    return senderId;
  }

  @Override
  public Instant getTimestamp() {
    return timestamp;
  }

  @Override
  public AlgorithmStatus getAlgorithmStatus() {
    return status;
  }

  @Override
  public ImmutableList<DssStateAndPrecisionPayload> getStates() {
    return states;
  }

  @Override
  public DssMessageType getType() {
    return DssMessageType.VIOLATION_CONDITION;
  }

  @Override
  public DssMessagePayload asJsonPayloadWithIdentifier(int pIdentifier) {
    return new DssViolationConditionPayload(
        DssHeaderPayload.forMessage(senderId, timestamp, pIdentifier),
        DssStatusPayload.fromAlgorithmStatus(status),
        states);
  }

  public String extractBlockStateWitnessString() {
    checkState(getNumberOfContainedStates() >= 1, "No state to extract witness from");
    DssBlockStatePayload blockStatePayload =
        states.stream()
            .map(DssStateAndPrecisionPayload::state)
            .map(state -> state.findComponent(DssComponentKey.BLOCK_STATE))
            .flatMap(Optional::stream)
            .map(DssBlockStatePayload.class::cast)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Missing block state"));
    return blockStatePayload.getWitnessString();
  }
}
