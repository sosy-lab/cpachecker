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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;

/**
 * Message sent by analysis workers carrying witness information: either the ARG states of all
 * preconditions after a TRUE result ({@link WitnessType#CORRECTNESS}), or the violation path after
 * a FALSE result ({@link WitnessType#VIOLATION}), differentiated by the field {@link
 * DssMessageFormat#WITNESS_TYPE_KEY}
 */
public class DssWitnessMessage extends DssMessage {

  public enum WitnessType {
    CORRECTNESS,
    VIOLATION
  }

  DssWitnessMessage(String pSenderId, ImmutableList<ImmutableMap<String, String>> pStates) {
    super(pSenderId, DssMessageType.WITNESS, Optional.empty(), pStates, ImmutableMap.of());
  }

  DssWitnessMessage(
      String pSenderId,
      ImmutableList<ImmutableMap<String, String>> pStates,
      ImmutableMap<String, String> pContent) {
    super(pSenderId, DssMessageType.WITNESS, Optional.empty(), pStates, pContent);
  }

  @Override
  void validateParameters(
      Optional<AlgorithmStatus> pStatus,
      List<? extends Map<String, String>> pStates,
      Map<String, String> pContent) {
    checkArgument(pStatus.isEmpty(), "Witness message must not contain status");

    String witnessType = pContent.get(DssMessageFormat.WITNESS_TYPE_KEY);
    checkArgument(witnessType != null, "Witness message requires witnessType");
    WitnessType type;
    try {
      type = WitnessType.valueOf(witnessType);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Unknown witness type: " + witnessType, e);
    }
    switch (type) {
      case CORRECTNESS -> {
        // since the multiple states field was removed, correctness witness messages can exist w/
        // only the witness type, which was already checked
      }
      case VIOLATION ->
          checkArgument(
              pContent.containsKey(DssMessageFormat.VIOLATION_PATH_KEY),
              "Violation witness requires violationPath");
    }
  }
}
