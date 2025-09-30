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

public class DssPreconditionMessage extends DssMessage {

  public static final String DSS_MESSAGE_REACHABLE_KEY = "reachable";
  public static final String DSS_PRECONDITION_SOUND_KEY = "precondition_sound";

  private final boolean reachable;
  private final boolean sound;
  private final List<String> receivers;

  DssPreconditionMessage(
      String pSenderId, List<String> pReceivers, ImmutableMap<String, String> pContent) {
    super(pSenderId, DssMessageType.PRECONDITION, pContent);
    reachable = Boolean.parseBoolean(pContent.get(DSS_MESSAGE_REACHABLE_KEY));
    sound = Boolean.parseBoolean(pContent.get(DSS_PRECONDITION_SOUND_KEY));
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

  public boolean isReachable() {
    return reachable;
  }

  public boolean isSound() {
    return sound;
  }
}
