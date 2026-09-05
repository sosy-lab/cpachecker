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
import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.cpa.block.BlockState;

public final class DssViolationConditionMessage implements DssConditionMessage {

  private final String senderId;
  private final Instant timestamp;
  private final AlgorithmStatus status;
  private final ImmutableList<ImmutableMap<String, String>> states;

  public DssViolationConditionMessage(
      String pSenderId,
      AlgorithmStatus pStatus,
      ImmutableList<ImmutableMap<String, String>> pStates) {
    this(pSenderId, Instant.now(), pStatus, pStates);
  }

  DssViolationConditionMessage(
      String pSenderId,
      Instant pTimestamp,
      AlgorithmStatus pStatus,
      ImmutableList<ImmutableMap<String, String>> pStates) {
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
  public ImmutableList<ImmutableMap<String, String>> getStates() {
    return states;
  }

  @Override
  public DssMessageType getType() {
    return DssMessageType.VIOLATION_CONDITION;
  }

  @Override
  public DssMessagePayload asJsonPayloadWithIdentifier(int pIdentifier) {
    return new DssMessagePayload(
        DssHeaderPayload.forMessage(senderId, getType(), timestamp, pIdentifier),
        DssStatusPayload.fromAlgorithmStatus(status),
        states,
        ImmutableMap.of());
  }

  public final String extractBlockStateWitnessString() {
    checkState(getNumberOfContainedStates() >= 1, "No state to extract witness from");

    return ContentReader.read(states.getFirst())
        .pushLevel(BlockState.class.getName())
        .get(SerializeOperator.STATE_KEY);
  }
}
