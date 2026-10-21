// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.collect.ImmutableList;
import java.util.List;

@JsonPropertyOrder({
  DssHeaderPayload.HEADER,
  DssStatusPayload.STATUS,
  DssStatePayload.STATES,
  DssPostConditionPayload.UNREACHABLE_BLOCK_END
})
public record DssPostConditionPayload(
    @JsonProperty(value = DssHeaderPayload.HEADER, required = true) DssHeaderPayload header,
    @JsonProperty(value = DssStatusPayload.STATUS, required = true) DssStatusPayload status,
    @JsonProperty(value = DssStatePayload.STATES, required = true)
        ImmutableList<DssStateAndPrecisionPayload> states,
    @JsonProperty(value = UNREACHABLE_BLOCK_END, required = true) boolean unreachableBlockEnd)
    implements DssMessagePayload {

  public static final String UNREACHABLE_BLOCK_END = "unreachableBlockEnd";

  @JsonCreator
  public DssPostConditionPayload(
      @JsonProperty(value = DssHeaderPayload.HEADER, required = true) DssHeaderPayload pHeader,
      @JsonProperty(value = DssStatusPayload.STATUS, required = true) DssStatusPayload pStatus,
      @JsonProperty(value = DssStatePayload.STATES, required = true)
          List<DssStateAndPrecisionPayload> pStates,
      @JsonProperty(value = UNREACHABLE_BLOCK_END, required = true) boolean pUnreachableBlockEnd) {
    this(pHeader, pStatus, ImmutableList.copyOf(pStates), pUnreachableBlockEnd);
  }

  @Override
  public DssMessagePayload withNewHeader(DssHeaderPayload pNewHeader) {
    return new DssPostConditionPayload(pNewHeader, status, states, unreachableBlockEnd);
  }
}
