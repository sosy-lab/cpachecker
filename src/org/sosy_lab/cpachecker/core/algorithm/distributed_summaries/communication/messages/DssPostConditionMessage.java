// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;

public class DssPostConditionMessage extends DssMessage {

  public static final String DSS_MESSAGE_REACHABLE_KEY = "reachable";

  private final boolean reachable;
  private final List<String> receivers;

  DssPostConditionMessage(
      String pSenderId, List<String> pReceivers, ImmutableMap<String, String> pContent) {
    super(pSenderId, DssMessageType.POST_CONDITION, pContent);
    reachable = Boolean.parseBoolean(pContent.get(DSS_MESSAGE_REACHABLE_KEY));
    receivers = pReceivers;
  }

  public List<String> getReceivers() {
    return receivers;
  }

  @Override
  boolean isValid(Map<String, String> pContent) {
    return !pContent.isEmpty()
        && pContent.containsKey(DSS_MESSAGE_REACHABLE_KEY)
        && pContent.get(DSS_MESSAGE_REACHABLE_KEY) != null
        && (pContent.get(DSS_MESSAGE_REACHABLE_KEY).equalsIgnoreCase("true")
            || pContent.get(DSS_MESSAGE_REACHABLE_KEY).equalsIgnoreCase("false"));
  }

  /**
   * Indicates whether the post-condition represents an actual reachable state
   * or if the block analysis of the predecessor was unable to reach its block end.
   * @return true if the post-condition is reachable, false otherwise.
   */
  public boolean isReachable() {
    return reachable;
  }
}
