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
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssComponentKey;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageKeys;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssStatePayload;

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

  @Override
  public ImmutableMap<String, String> asContentMap() {
    return ImmutableMap.of(
        getComponentKey().getSerializedName() + "." + DssMessageKeys.STATE,
        Joiner.on(", ").join(entries));
  }

  public static DssFunctionPointerStatePayload fromContentMap(
      ImmutableMap<String, String> pContent) {
    String serialized =
        pContent.get(
            DssComponentKey.FUNCTION_POINTER_STATE.getSerializedName()
                + "."
                + DssMessageKeys.STATE);
    Preconditions.checkNotNull(serialized, "If entry is contained, it cannot be null");
    List<String> serializedEntries = Splitter.on(", ").splitToList(serialized);
    ImmutableList.Builder<DssFunctionPointerEntryPayload> entries =
        ImmutableList.builderWithExpectedSize(serializedEntries.size());

    for (String s : serializedEntries) {
      if (s.isBlank()) {
        continue;
      }
      entries.add(DssFunctionPointerEntryPayload.deserialize(s));
    }
    return new DssFunctionPointerStatePayload(entries.build());
  }
}
