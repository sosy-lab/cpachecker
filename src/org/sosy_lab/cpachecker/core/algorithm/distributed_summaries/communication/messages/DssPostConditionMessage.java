// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;

public final class DssPostConditionMessage implements DssConditionMessage {

  private final String senderId;
  private final Instant timestamp;
  private final AlgorithmStatus status;
  private final ImmutableList<DssStateAndPrecisionPayload> states;
  private final boolean unreachableBlockEnd;

  public DssPostConditionMessage(
      String pSenderId,
      AlgorithmStatus pStatus,
      ImmutableList<DssStateAndPrecisionPayload> pStates) {
    this(pSenderId, Instant.now(), pStatus, pStates, false);
  }

  public static DssPostConditionMessage unreachableBlockEndMessage(
      String pSenderId, AlgorithmStatus pStatus) {
    return new DssPostConditionMessage(pSenderId, Instant.now(), pStatus, ImmutableList.of(), true);
  }

  DssPostConditionMessage(
      String pSenderId,
      Instant pTimestamp,
      AlgorithmStatus pStatus,
      ImmutableList<DssStateAndPrecisionPayload> pStates,
      boolean pUnreachableBlockEnd) {
    senderId = Preconditions.checkNotNull(pSenderId);
    timestamp = Preconditions.checkNotNull(pTimestamp);
    status = Preconditions.checkNotNull(pStatus);
    states = ImmutableList.copyOf(pStates);
    unreachableBlockEnd = pUnreachableBlockEnd;

    Preconditions.checkArgument(
        !states.isEmpty() || unreachableBlockEnd,
        "Post-Condition message requires at least one state or an unreachable block-end marker.");
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

  public boolean indicatesUnreachableBlockEnd() {
    return unreachableBlockEnd;
  }

  @Override
  public DssMessageType getType() {
    return DssMessageType.POST_CONDITION;
  }

  @Override
  public DssMessagePayload asJsonPayloadWithIdentifier(int pIdentifier) {
    return new DssPostConditionPayload(
        DssHeaderPayload.forMessage(senderId, timestamp, pIdentifier),
        DssStatusPayload.fromAlgorithmStatus(status),
        states,
        unreachableBlockEnd);
  }
}
