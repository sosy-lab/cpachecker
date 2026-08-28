// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
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

public class DssViolationConditionMessage extends DssMessage {

  DssViolationConditionMessage(
      String pSenderId,
      AlgorithmStatus pStatus,
      ImmutableList<ImmutableMap<String, String>> pStates,
      ImmutableMap<String, String> pContent) {
    super(pSenderId, DssMessageType.VIOLATION_CONDITION, Optional.of(pStatus), pStates, pContent);
  }

  @Override
  void validateParameters(
      Optional<AlgorithmStatus> pStatus,
      List<? extends Map<String, String>> pStates,
      Map<String, String> pContent) {
    checkArgument(pStatus.isPresent(), "Violation-condition message requires status");
    checkArgument(!pStates.isEmpty(), "Violation-condition message requires at least one state");
  }
}
