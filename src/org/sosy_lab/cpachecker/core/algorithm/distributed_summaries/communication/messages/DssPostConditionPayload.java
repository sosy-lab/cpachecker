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
  DssMessageKeys.HEADER,
  DssMessageKeys.STATUS,
  DssMessageKeys.STATES,
  DssMessageKeys.UNREACHABLE_BLOCK_END
})
public record DssPostConditionPayload(
    @JsonProperty(value = DssMessageKeys.HEADER, required = true) DssHeaderPayload header,
    @JsonProperty(value = DssMessageKeys.STATUS, required = true) DssStatusPayload status,
    @JsonProperty(value = DssMessageKeys.STATES, required = true)
        ImmutableList<DssStateAndPrecisionPayload> states,
    @JsonProperty(value = DssMessageKeys.UNREACHABLE_BLOCK_END, required = true)
        boolean unreachableBlockEnd)
    implements DssMessagePayload {

  @JsonCreator
  public DssPostConditionPayload(
      @JsonProperty(value = DssMessageKeys.HEADER, required = true) DssHeaderPayload pHeader,
      @JsonProperty(value = DssMessageKeys.STATUS, required = true) DssStatusPayload pStatus,
      @JsonProperty(value = DssMessageKeys.STATES, required = true)
          List<DssStateAndPrecisionPayload> pStates,
      @JsonProperty(value = DssMessageKeys.UNREACHABLE_BLOCK_END, required = true)
          boolean pUnreachableBlockEnd) {
    this(pHeader, pStatus, ImmutableList.copyOf(pStates), pUnreachableBlockEnd);
  }

  @Override
  public DssMessagePayload withNewHeader(DssHeaderPayload pNewHeader) {
    return new DssPostConditionPayload(pNewHeader, status, states, unreachableBlockEnd);
  }
}
