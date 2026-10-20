// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;

@JsonPropertyOrder({DssMessageKeys.HEADER, DssMessageKeys.RESULT})
public record DssResultPayload(
    @JsonProperty(value = DssMessageKeys.HEADER, required = true) DssHeaderPayload header,
    @JsonProperty(value = DssMessageKeys.RESULT, required = true) Result result)
    implements DssMessagePayload {

  @Override
  public DssMessagePayload withNewHeader(DssHeaderPayload pNewHeader) {
    return new DssResultPayload(pNewHeader, result);
  }
}
