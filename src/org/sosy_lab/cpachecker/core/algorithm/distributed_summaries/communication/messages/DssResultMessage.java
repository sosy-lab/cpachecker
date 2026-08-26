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

public class DssResultMessage extends DssMessage {

  DssResultMessage(String pSenderId, String pResult) {
    super(
        pSenderId,
        DssMessageType.RESULT,
        Optional.empty(),
        ImmutableMap.of(DssMessageFormat.RESULT_KEY, pResult));
  }

  DssResultMessage(String pSenderId, ImmutableMap<String, String> pResult) {
    super(pSenderId, DssMessageType.RESULT, Optional.empty(), pResult);
  }

  @Override
  boolean isValid(Map<String, String> pContent) {
    return (pContent.size() == 1)
        && pContent.containsKey(DssMessageFormat.RESULT_KEY)
        && pContent.get(DssMessageFormat.RESULT_KEY) != null
        && !pContent.get(DssMessageFormat.RESULT_KEY).isEmpty();
  }
}
