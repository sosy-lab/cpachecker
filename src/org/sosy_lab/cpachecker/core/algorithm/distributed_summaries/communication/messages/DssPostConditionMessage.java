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

  private final List<String> receivers;

  DssPostConditionMessage(
      String pSenderId, List<String> pReceivers, ImmutableMap<String, String> pContent) {
    super(pSenderId, DssMessageType.POST_CONDITION, pContent);
    receivers = pReceivers;
  }

  public List<String> getReceivers() {
    return receivers;
  }

  @Override
  boolean isValid(Map<String, String> pContent) {
    return !pContent.isEmpty();
  }
}
