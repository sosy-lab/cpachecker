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

@JsonPropertyOrder({DssStateAndPrecisionPayload.STATE, DssStateAndPrecisionPayload.PRECISION})
public record DssStateAndPrecisionPayload(
    @JsonProperty(value = DssStateAndPrecisionPayload.STATE, required = true)
        DssStatePayload state,
    @JsonProperty(value = DssStateAndPrecisionPayload.PRECISION, required = true)
        DssPrecisionPayload precision) {
  public static final String STATE = "state";
  public static final String PRECISION = "precision";
}
