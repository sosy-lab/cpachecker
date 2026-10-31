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

@JsonPropertyOrder({DssHeaderPayload.HEADER, DssViolationWitnessPayload.VIOLATION_PATH})
public record DssViolationWitnessPayload(
    @JsonProperty(value = DssHeaderPayload.HEADER, required = true) DssHeaderPayload header,
    @JsonProperty(value = VIOLATION_PATH, required = true) String violationPath)
    implements DssMessagePayload {
  public static final String WITNESS_TYPE = "witnessType";
  public static final String VIOLATION_PATH = "violationPath";

  @Override
  public DssMessagePayload withNewHeader(DssHeaderPayload pNewHeader) {
    return new DssViolationWitnessPayload(pNewHeader, violationPath);
  }
}
