// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.payloads.DssComponentKey;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.payloads.DssStatePayload;

public record DssFunctionPointerStatePayload(
    @JsonProperty(ENTRIES) ImmutableList<DssFunctionPointerEntryPayload> entries)
    implements DssStatePayload {
  public static final String ENTRIES = "entries";

  @JsonCreator
  DssFunctionPointerStatePayload(
      @JsonProperty(ENTRIES) List<DssFunctionPointerEntryPayload> pEntries) {
    this(ImmutableList.copyOf(pEntries));
  }

  @JsonIgnore
  @Override
  public DssComponentKey getComponentKey() {
    return DssComponentKey.FUNCTION_POINTER_STATE;
  }
}
