// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;

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
        ImmutableMap.of(DssMessageFormat.EXCEPTION_KEY, pExceptionMessage));
  }

  DssExceptionMessage(String pSenderId, ImmutableMap<String, String> pExceptionMessage) {
    super(pSenderId, DssMessageType.EXCEPTION, Optional.empty(), pExceptionMessage);
  }

  @Override
  boolean isValid(Map<String, String> pContent) {
    return pContent.size() == 1
        && pContent.containsKey(DssMessageFormat.EXCEPTION_KEY)
        && pContent.get(DssMessageFormat.EXCEPTION_KEY) != null
        && !pContent.get(DssMessageFormat.EXCEPTION_KEY).isEmpty();
  }
}
