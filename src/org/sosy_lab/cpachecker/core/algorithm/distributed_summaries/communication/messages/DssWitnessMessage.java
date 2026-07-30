// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import com.google.common.collect.ImmutableMap;
import java.util.Map;

/**
 * Message sent by analysis workers carrying witness information: either the ARG states of all
 * preconditions after a TRUE result ({@link WitnessType#CORRECTNESS}), or the violation path after
 * a FALSE result ({@link WitnessType#VIOLATION}), differentiated by the field {@link
 * #DSS_MESSAGE_WITNESS_TYPE_KEY}
 */
public class DssWitnessMessage extends DssMessage {

  public enum WitnessType {
    CORRECTNESS,
    VIOLATION
  }

  public static final String DSS_MESSAGE_WITNESS_TYPE_KEY = "witnessType";
  public static final String DSS_MESSAGE_VIOLATION_PATH_KEY = "violationPath";

  DssWitnessMessage(String pSenderId, ImmutableMap<String, String> pContent) {
    super(pSenderId, DssMessageType.WITNESS, pContent);
  }

  @Override
  boolean isValid(Map<String, String> pContent) {
    String witnessType = pContent.get(DSS_MESSAGE_WITNESS_TYPE_KEY);
    if (witnessType == null) {
      return false;
    }
    WitnessType type;
    try {
      type = WitnessType.valueOf(witnessType);
    } catch (IllegalArgumentException e) {
      return false;
    }
    return switch (type) {
      case CORRECTNESS -> pContent.size() > 1;
      case VIOLATION -> pContent.containsKey(DSS_MESSAGE_VIOLATION_PATH_KEY);
    };
  }
}
