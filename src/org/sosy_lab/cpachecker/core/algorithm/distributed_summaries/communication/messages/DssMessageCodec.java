// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import org.sosy_lab.cpachecker.cpa.pathrestriction.SegmentedPaths;

public final class DssMessageCodec {
  public static DssMessage fromJson(Path pJson) throws IOException {
    DssMessagePayload payload = DssMessageJsonUtils.read(pJson);
    return fromPayload(payload);
  }

  public static DssMessage fromPayload(DssMessagePayload pPayload) {
    DssHeaderPayload header = pPayload.header();
    String senderId = header.senderId();
    Optional<Instant> timestampOpt = header.timestampAsInstant();
    Instant timestamp = timestampOpt.isPresent() ? timestampOpt.orElseThrow() : Instant.now();

    return switch (pPayload) {
      case DssPostConditionPayload postConditionPayload ->
          new DssPostConditionMessage(
              senderId,
              timestamp,
              postConditionPayload.status().toAlgorithmStatus(),
              postConditionPayload.states(),
              postConditionPayload.unreachableBlockEnd());
      case DssViolationConditionPayload violationConditionPayload ->
          new DssViolationConditionMessage(
              senderId,
              timestamp,
              violationConditionPayload.status().toAlgorithmStatus(),
              violationConditionPayload.states());
      case DssExceptionPayload exceptionPayload ->
          new DssExceptionMessage(senderId, timestamp, exceptionPayload.exceptionMessage());
      case DssResultPayload resultPayload ->
          new DssResultMessage(senderId, timestamp, resultPayload.result());
      case DssCorrectnessWitnessPayload correctnessWitnessPayload ->
          new DssCorrectnessWitnessMessage(senderId, timestamp, correctnessWitnessPayload.states());
      case DssViolationWitnessPayload violationWitnessPayload ->
          new DssViolationWitnessMessage(
              senderId,
              timestamp,
              SegmentedPaths.deserialize(violationWitnessPayload.violationPath()));
    };
  }
}
