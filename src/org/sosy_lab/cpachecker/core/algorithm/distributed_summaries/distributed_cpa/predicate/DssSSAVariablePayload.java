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

@JsonPropertyOrder({DssSSAVariablePayload.INDEX, DssSSAVariablePayload.SERIALIZED_TYPE})
public record DssSSAVariablePayload(
    @JsonProperty(INDEX) int index, @JsonProperty(SERIALIZED_TYPE) String serializedType) {
  public static final String INDEX = "index";
  public static final String SERIALIZED_TYPE = "serializedType";
}
