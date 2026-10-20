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

@JsonPropertyOrder({DssMessageKeys.HEADER, DssMessageKeys.VIOLATION_PATH})
public record DssViolationWitnessPayload(
    @JsonProperty(value = DssMessageKeys.HEADER, required = true) DssHeaderPayload header,
    @JsonProperty(value = DssMessageKeys.VIOLATION_PATH, required = true) String violationPath)
    implements DssMessagePayload {
  @Override
  public DssMessagePayload withNewHeader(DssHeaderPayload pNewHeader) {
    return new DssViolationWitnessPayload(pNewHeader, violationPath);
  }
}
