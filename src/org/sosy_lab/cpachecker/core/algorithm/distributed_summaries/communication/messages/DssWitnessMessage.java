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
 * Message sent by analysis workers with the information required to construct a correctness
 * witness, e.g., serialized loop-head preconditions. The content may be empty, e.g., if the block
 * did not contribute to the correctness witness or the analysis did not finish with a correctness
 * result.
 */
public class DssWitnessMessage extends DssMessage {

  DssWitnessMessage(String pSenderId, ImmutableMap<String, String> pContent) {
    super(pSenderId, DssMessageType.WITNESS, pContent);
  }

  @Override
  boolean isValid(Map<String, String> pContent) {
    return !pContent.isEmpty();
  }
}
