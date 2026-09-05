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
import org.sosy_lab.cpachecker.cpa.pathrestriction.SegmentedPaths;

public final class DssViolationWitnessMessage implements DssWitnessMessage {
  private final String senderId;
  private final Instant timestamp;
  private final SegmentedPaths violationPath;

  public DssViolationWitnessMessage(String pSenderId, SegmentedPaths pViolationPath) {
    this(pSenderId, Instant.now(), pViolationPath);
  }

  DssViolationWitnessMessage(String pSenderId, Instant pTimestamp, SegmentedPaths pViolationPath) {
    senderId = Preconditions.checkNotNull(pSenderId);
    timestamp = Preconditions.checkNotNull(pTimestamp);
    violationPath = Preconditions.checkNotNull(pViolationPath);
  }

  @Override
  public String getSenderId() {
    return senderId;
  }

  @Override
  public Instant getTimestamp() {
    return timestamp;
  }

  public SegmentedPaths getViolationPath() {
    return violationPath;
  }

  @Override
  public WitnessType getWitnessType() {
    return WitnessType.VIOLATION;
  }

  @Override
  public DssMessagePayload asJsonPayloadWithIdentifier(int pIdentifier) {
    return new DssMessagePayload(
        DssHeaderPayload.forMessage(senderId, getType(), timestamp, pIdentifier),
        null,
        ImmutableList.of(),
        ImmutableMap.of(
            DssMessageKeys.WITNESS_TYPE,
            getWitnessType().name(),
            DssMessageKeys.VIOLATION_PATH,
            violationPath.serialize()));
  }
}
