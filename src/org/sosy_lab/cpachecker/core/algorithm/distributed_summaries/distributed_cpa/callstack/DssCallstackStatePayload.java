// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssComponentKey;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssStatePayload;

@JsonPropertyOrder({DssCallstackStatePayload.ALLOW_TRANSFERS, DssCallstackStatePayload.STATES})
public record DssCallstackStatePayload(
    @JsonProperty(ALLOW_TRANSFERS) boolean allowAllTransfers,
    @JsonProperty(STATES) ImmutableList<DssCallstackEntryPayload> states)
    implements DssStatePayload {
  public static final String ALLOW_TRANSFERS = "allowAllTransfers";
  public static final String STATES = "states";

  @JsonCreator
  public DssCallstackStatePayload(
      @JsonProperty(ALLOW_TRANSFERS) boolean pAllowAllTransfers,
      @JsonProperty(STATES) List<DssCallstackEntryPayload> pStates) {
    this(pAllowAllTransfers, ImmutableList.copyOf(pStates));
  }

  @JsonIgnore
  @Override
  public DssComponentKey getComponentKey() {
    return DssComponentKey.CALLSTACK_STATE;
  }

}
