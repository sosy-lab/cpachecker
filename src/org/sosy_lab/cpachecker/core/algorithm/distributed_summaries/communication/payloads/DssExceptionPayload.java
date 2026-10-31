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

@JsonPropertyOrder({DssHeaderPayload.HEADER, DssExceptionPayload.EXCEPTION})
public record DssExceptionPayload(
    @JsonProperty(value = DssHeaderPayload.HEADER, required = true) DssHeaderPayload header,
    @JsonProperty(value = EXCEPTION, required = true) String exceptionMessage)
    implements DssMessagePayload {
  public static final String EXCEPTION = "exception";

  @Override
  public DssMessagePayload withNewHeader(DssHeaderPayload pNewHeader) {
    return new DssExceptionPayload(pNewHeader, exceptionMessage);
  }
}
