// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;

public class DssResultMessage extends DssMessage {

  public static final String DSS_MESSAGE_RESULT_KEY = "result";
  public static final String DSS_MESSAGE_VIOLATION_PATH_KEY = "violationPath";

  DssResultMessage(String pSenderId, String pResult) {
    super(pSenderId, DssMessageType.RESULT, ImmutableMap.of(DSS_MESSAGE_RESULT_KEY, pResult));
    Preconditions.checkArgument(
        !pResult.equals(Result.FALSE.name()), "Result False without violation path");
  }

  DssResultMessage(String pSenderId, String pResult, String violationPath) {
    super(
        pSenderId,
        DssMessageType.RESULT,
        ImmutableMap.of(
            DSS_MESSAGE_RESULT_KEY, pResult, DSS_MESSAGE_VIOLATION_PATH_KEY, violationPath));
    Preconditions.checkArgument(
        pResult.equals(Result.FALSE.name()),
        "violation path should only be used with a FALSE result");
  }

  DssResultMessage(String pSenderId, ImmutableMap<String, String> pResult) {
    super(pSenderId, DssMessageType.RESULT, pResult);
  }

  @Override
  boolean isValid(Map<String, String> pContent) {
    return (pContent.size() == 1 || pContent.size() == 2)
        && pContent.containsKey(DSS_MESSAGE_RESULT_KEY)
        && pContent.get(DSS_MESSAGE_RESULT_KEY) != null
        && !pContent.get(DSS_MESSAGE_RESULT_KEY).isEmpty()
        && (!pContent.get(DSS_MESSAGE_RESULT_KEY).equals(Result.FALSE.name())
            || (pContent.containsKey(DSS_MESSAGE_VIOLATION_PATH_KEY)
                && pContent.get(DSS_MESSAGE_VIOLATION_PATH_KEY) != null));
  }
}
