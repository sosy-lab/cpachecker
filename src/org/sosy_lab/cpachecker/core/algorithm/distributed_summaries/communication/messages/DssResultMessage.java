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

public class DssResultMessage extends DssMessage {

  DssResultMessage(String pSenderId, String pResult) {
    super(
        pSenderId,
        DssMessageType.RESULT,
        Optional.empty(),
        ImmutableList.of(),
        ImmutableMap.of(DssMessageKeys.RESULT, pResult));
  }

  DssResultMessage(String pSenderId, ImmutableMap<String, String> pResult) {
    super(pSenderId, DssMessageType.RESULT, Optional.empty(), ImmutableList.of(), pResult);
  }

  @Override
  void validateParameters(
      Optional<AlgorithmStatus> pStatus,
      List<? extends Map<String, String>> pStates,
      Map<String, String> pContent) {

    checkArgument(pStatus.isEmpty(), "Result message must not contain status");
    checkArgument(pStates.isEmpty(), "Result message must not contain states");
    checkArgument(
        pContent.size() == 1
            && pContent.containsKey(DssMessageKeys.RESULT)
            && pContent.get(DssMessageKeys.RESULT) != null
            && !pContent.get(DssMessageKeys.RESULT).isEmpty(),
        "Result message requires exactly one non-empty content entry: %s",
        DssMessageKeys.RESULT);
  }
}
