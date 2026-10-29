// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({DssLocationInstancePayload.NODE_ID, DssLocationInstancePayload.INSTANCE})
public record DssLocationInstancePayload(
    @JsonProperty(NODE_ID) int nodeId, @JsonProperty(INSTANCE) int instance) {
  public static final String NODE_ID = "nodeId";
  public static final String INSTANCE = "instance";
}
