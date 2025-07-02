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

public class DssViolationConditionMessage extends DssMessage {

  public static final String DSS_MESSAGE_FIRST_KEY = "first";
  public static final String DSS_MESSAGE_PREFIX_KEY = "prefix";

  private final String prefix;
  private final boolean first;

  DssViolationConditionMessage(String pSenderId, ImmutableMap<String, String> pContent) {
    super(pSenderId, DssMessageType.VIOLATION_CONDITION, pContent);
    prefix = pContent.get(DSS_MESSAGE_PREFIX_KEY);
    first = Boolean.parseBoolean(pContent.get(DSS_MESSAGE_FIRST_KEY));
  }

  @Override
  boolean isValid(Map<String, String> pContent) {
    return !pContent.isEmpty()
        && pContent.containsKey(DSS_MESSAGE_PREFIX_KEY)
        && pContent.get(DSS_MESSAGE_PREFIX_KEY) != null
        && pContent.containsKey(DSS_MESSAGE_FIRST_KEY)
        && pContent.get(DSS_MESSAGE_FIRST_KEY) != null;
  }

  public String getPrefix() {
    return prefix;
  }

  public boolean isFirst() {
    return first;
  }
}
