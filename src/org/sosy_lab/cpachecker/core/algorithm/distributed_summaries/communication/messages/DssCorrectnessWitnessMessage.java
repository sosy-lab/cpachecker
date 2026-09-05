// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.Instant;

public final class DssCorrectnessWitnessMessage implements DssWitnessMessage, DssMessageWithStates {
  private final String senderId;
  private final Instant timestamp;
  private final ImmutableList<ImmutableMap<String, String>> states;

  public DssCorrectnessWitnessMessage(
      String pSenderId, ImmutableList<ImmutableMap<String, String>> pStates) {
    this(pSenderId, Instant.now(), pStates);
  }

  DssCorrectnessWitnessMessage(
      String pSenderId, Instant pTimestamp, ImmutableList<ImmutableMap<String, String>> pStates) {
    senderId = Preconditions.checkNotNull(pSenderId);
    timestamp = Preconditions.checkNotNull(pTimestamp);
    states = ImmutableList.copyOf(Preconditions.checkNotNull(pStates));
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
  public ImmutableList<ImmutableMap<String, String>> getStates() {
    return states;
  }

  @Override
  public WitnessType getWitnessType() {
    return WitnessType.CORRECTNESS;
  }

  @Override
  public DssMessagePayload asJsonPayloadWithIdentifier(int pIdentifier) {
    return new DssMessagePayload(
        DssHeaderPayload.forMessage(senderId, getType(), timestamp, pIdentifier),
        null,
        states,
        ImmutableMap.of(DssMessageKeys.WITNESS_TYPE, getWitnessType().name()));
  }
}
