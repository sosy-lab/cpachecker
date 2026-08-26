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
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;

public class DssPostConditionMessage extends DssMessage {

  DssPostConditionMessage(
      String pSenderId, AlgorithmStatus pStatus, ImmutableMap<String, String> pContent) {
    super(pSenderId, DssMessageType.POST_CONDITION, Optional.of(pStatus), pContent);
  }

  @Override
  boolean isValid(Map<String, String> pContent) {
    return !pContent.isEmpty();
  }
}
