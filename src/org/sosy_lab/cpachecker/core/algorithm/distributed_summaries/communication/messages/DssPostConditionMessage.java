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
import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;

public final class DssPostConditionMessage implements DssConditionMessage {

  private final String senderId;
  private final Instant timestamp;
  private final AlgorithmStatus status;
  private final ImmutableList<ImmutableMap<String, String>> states;
  private final boolean unreachableBlockEnd;

  public DssPostConditionMessage(
      String pSenderId,
      AlgorithmStatus pStatus,
      ImmutableList<ImmutableMap<String, String>> pStates) {
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
      ImmutableList<ImmutableMap<String, String>> pStates,
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

  public ImmutableList<ImmutableMap<String, String>> getStates() {
    return states;
  }

  public boolean indicatesUnreachableBlockEnd() {
    return unreachableBlockEnd;
  }

  public DssMessageType getType() {
    return DssMessageType.POST_CONDITION;
  }

  @Override
  public DssMessagePayload asJsonPayloadWithIdentifier(int pIdentifier) {
    ImmutableMap<String, String> content =
        unreachableBlockEnd
            ? ImmutableMap.of(DssMessageKeys.UNREACHABLE_BLOCK_END, Boolean.toString(true))
            : ImmutableMap.of();

    return new DssMessagePayload(
        DssHeaderPayload.forMessage(senderId, getType(), timestamp, pIdentifier),
        DssStatusPayload.fromAlgorithmStatus(status),
        states,
        content);
  }
}
