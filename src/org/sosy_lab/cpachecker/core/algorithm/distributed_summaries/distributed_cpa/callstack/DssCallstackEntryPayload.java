// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({DssCallstackEntryPayload.NODE_ID, DssCallstackEntryPayload.FUNCTION})
public record DssCallstackEntryPayload(
    @JsonProperty(NODE_ID) int nodeId, @JsonProperty(FUNCTION) String function) {
  public static final String NODE_ID = "nodeId";
  public static final String FUNCTION = "function";
}
