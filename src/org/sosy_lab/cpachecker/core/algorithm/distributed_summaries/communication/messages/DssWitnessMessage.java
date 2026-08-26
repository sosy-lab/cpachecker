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
import java.util.Optional;

/**
 * Message sent by analysis workers carrying witness information: either the ARG states of all
 * preconditions after a TRUE result ({@link WitnessType#CORRECTNESS}), or the violation path after
 * a FALSE result ({@link WitnessType#VIOLATION}), differentiated by the field {@link
 * DssMessageFormat#WITNESS_TYPE_KEY}
 */
public class DssWitnessMessage extends DssMessage {

  public enum WitnessType {
    CORRECTNESS,
    VIOLATION
  }

  DssWitnessMessage(String pSenderId, ImmutableMap<String, String> pContent) {
    super(pSenderId, DssMessageType.WITNESS, Optional.empty(), pContent);
  }

  @Override
  boolean isValid(Map<String, String> pContent) {
    String witnessType = pContent.get(DssMessageFormat.WITNESS_TYPE_KEY);
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
      case VIOLATION -> pContent.containsKey(DssMessageFormat.VIOLATION_PATH_KEY);
    };
  }
}
