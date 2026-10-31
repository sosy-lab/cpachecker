// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.payloads;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;

@JsonPropertyOrder({DssHeaderPayload.HEADER, DssResultPayload.RESULT})
public record DssResultPayload(
    @JsonProperty(value = DssHeaderPayload.HEADER, required = true) DssHeaderPayload header,
    @JsonProperty(value = RESULT, required = true) Result result)
    implements DssMessagePayload {

  public static final String RESULT = "result";

  @Override
  public DssMessagePayload withNewHeader(DssHeaderPayload pNewHeader) {
    return new DssResultPayload(pNewHeader, result);
  }
}
