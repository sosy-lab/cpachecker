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

public class DssExceptionMessage extends DssMessage {

  public static final String DSS_MESSAGE_EXCEPTION_KEY = "exception";

  DssExceptionMessage(String pSenderId, String pExceptionMessage) {
    super(
        pSenderId,
        DssMessageType.EXCEPTION,
        ImmutableMap.of(DSS_MESSAGE_EXCEPTION_KEY, pExceptionMessage));
  }

  DssExceptionMessage(String pSenderId, ImmutableMap<String, String> pExceptionMessage) {
    super(pSenderId, DssMessageType.EXCEPTION, pExceptionMessage);
  }

  @Override
  boolean isValid(Map<String, String> pContent) {
    return pContent.size() == 1
        && pContent.containsKey(DSS_MESSAGE_EXCEPTION_KEY)
        && pContent.get(DSS_MESSAGE_EXCEPTION_KEY) != null
        && !pContent.get(DSS_MESSAGE_EXCEPTION_KEY).isEmpty();
  }
}
