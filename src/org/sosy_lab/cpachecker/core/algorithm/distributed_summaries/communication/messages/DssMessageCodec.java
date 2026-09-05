// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage.DssMessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssWitnessMessage.WitnessType;
import org.sosy_lab.cpachecker.cpa.pathrestriction.SegmentedPaths;

public final class DssMessageCodec {
  public static DssMessage fromJson(Path pJson) throws IOException {
    DssMessagePayload payload = DssMessagePayload.fromJson(pJson);
    return fromPayload(payload);
  }

  public static DssMessage fromPayload(DssMessagePayload pPayload) {
    DssHeaderPayload header = pPayload.header();
    Optional<AlgorithmStatus> algorithmStatus =
        pPayload.status() == null
            ? Optional.empty()
            : Optional.of(pPayload.status().toAlgorithmStatus());
    ImmutableList<ImmutableMap<String, String>> states = pPayload.states();
    ImmutableMap<String, String> content = pPayload.content();
    String senderId = header.senderId();
    Optional<Instant> timestampOpt = header.timestampAsInstant();
    Instant timestamp = timestampOpt.isPresent() ? timestampOpt.orElseThrow() : Instant.now();
    DssMessageType type = header.messageType();

    checkArgument(
        !hasStatus(type) || algorithmStatus.isPresent(), "Message type requires status: %s", type);
    checkArgument(
        hasStatus(type) || algorithmStatus.isEmpty(), "Message type can't have status: %s", type);

    return switch (type) {
      case POST_CONDITION -> {
        boolean unreachableBlockEnd =
            Boolean.parseBoolean(
                content.getOrDefault(DssMessageKeys.UNREACHABLE_BLOCK_END, "false"));
        yield new DssPostConditionMessage(
            senderId, timestamp, algorithmStatus.orElseThrow(), states, unreachableBlockEnd);
      }
      case VIOLATION_CONDITION ->
          new DssViolationConditionMessage(
              senderId, timestamp, algorithmStatus.orElseThrow(), states);
      case EXCEPTION -> {
        String exceptionMessage = content.get(DssMessageKeys.EXCEPTION);
        yield new DssExceptionMessage(senderId, timestamp, exceptionMessage);
      }
      case RESULT -> {
        Result result = Result.valueOf(content.get(DssMessageKeys.RESULT));
        yield new DssResultMessage(senderId, timestamp, result);
      }
      case WITNESS -> {
        WitnessType witnessType = WitnessType.valueOf(content.get(DssMessageKeys.WITNESS_TYPE));
        yield switch (witnessType) {
          case VIOLATION ->
              new DssViolationWitnessMessage(
                  senderId,
                  timestamp,
                  SegmentedPaths.deserialize(content.get(DssMessageKeys.VIOLATION_PATH)));
          case CORRECTNESS -> new DssCorrectnessWitnessMessage(senderId, timestamp, states);
        };
      }
    };
  }

  private static boolean hasStatus(DssMessageType pType) {
    return pType == DssMessage.DssMessageType.POST_CONDITION
        || pType == DssMessage.DssMessageType.VIOLATION_CONDITION;
  }
}
