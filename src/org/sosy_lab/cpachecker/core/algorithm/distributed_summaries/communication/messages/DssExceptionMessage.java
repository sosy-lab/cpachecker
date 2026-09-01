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

/**
 * Message for exceptions that occur during distributed summary computation. The content contains a
 * single key-value pair with the key "exception" and the value being the exception message.
 */
public class DssExceptionMessage extends DssMessage {

  DssExceptionMessage(String pSenderId, String pExceptionMessage) {
    super(
        pSenderId,
        DssMessageType.EXCEPTION,
        Optional.empty(),
        ImmutableList.of(),
        ImmutableMap.of(DssMessageKeys.EXCEPTION, pExceptionMessage));
  }

  DssExceptionMessage(String pSenderId, ImmutableMap<String, String> pExceptionMessage) {
    super(
        pSenderId,
        DssMessageType.EXCEPTION,
        Optional.empty(),
        ImmutableList.of(),
        pExceptionMessage);
  }

  @Override
  void validateParameters(
      Optional<AlgorithmStatus> pStatus,
      List<? extends Map<String, String>> pStates,
      Map<String, String> pContent) {
    checkArgument(pStatus.isEmpty(), "Exception message must not contain status");
    checkArgument(pStates.isEmpty(), "Exception message must not contain states");
    checkArgument(
        pContent.size() == 1
            && pContent.containsKey(DssMessageKeys.EXCEPTION)
            && pContent.get(DssMessageKeys.EXCEPTION) != null
            && !pContent.get(DssMessageKeys.EXCEPTION).isEmpty(),
        "Exception message requires exactly one non-empty content entry: %s",
        DssMessageKeys.EXCEPTION);
  }
}
