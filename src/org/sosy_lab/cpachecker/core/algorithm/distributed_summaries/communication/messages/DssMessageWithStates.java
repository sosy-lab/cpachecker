// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.payloads.DssStateAndPrecisionPayload;

/** Messages containing state information */
public sealed interface DssMessageWithStates extends DssMessage
    permits DssConditionMessage, DssCorrectnessWitnessMessage {
  ImmutableList<DssStateAndPrecisionPayload> getStates();

  default int getNumberOfContainedStates() {
    return getStates().size();
  }
}
