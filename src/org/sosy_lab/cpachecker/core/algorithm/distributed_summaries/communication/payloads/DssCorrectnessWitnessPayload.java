// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.payloads;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.collect.ImmutableList;
import java.util.List;

@JsonPropertyOrder({DssHeaderPayload.HEADER, DssStatePayload.STATES})
public record DssCorrectnessWitnessPayload(
    @JsonProperty(value = DssHeaderPayload.HEADER, required = true) DssHeaderPayload header,
    @JsonProperty(value = DssStatePayload.STATES, required = true)
        ImmutableList<DssStateAndPrecisionPayload> states)
    implements DssMessagePayload {

  @JsonCreator
  public DssCorrectnessWitnessPayload(
      @JsonProperty(value = DssHeaderPayload.HEADER, required = true) DssHeaderPayload pHeader,
      @JsonProperty(value = DssStatePayload.STATES, required = true)
          List<DssStateAndPrecisionPayload> pStates) {
    this(pHeader, ImmutableList.copyOf(pStates));
  }

  @Override
  public DssMessagePayload withNewHeader(DssHeaderPayload pNewHeader) {
    return new DssCorrectnessWitnessPayload(pNewHeader, states);
  }
}
