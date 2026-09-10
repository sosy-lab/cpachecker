// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssComponentKey;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageKeys;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssStatePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer.DssFunctionPointerEntry.TargetKind;

public record DssFunctionPointerStatePayload(ImmutableList<DssFunctionPointerEntry> entries)
    implements DssStatePayload {
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
    ImmutableList.Builder<DssFunctionPointerEntry> entries =
        ImmutableList.builderWithExpectedSize(serializedEntries.size());

    for (String s : serializedEntries) {
      if (s.isBlank()) {
        continue;
      }
      List<String> parts = Splitter.on(":").limit(3).splitToList(s);
      assert parts.size() >= 2;
      switch (parts.getFirst()) {
        case "I" ->
            entries.add(new DssFunctionPointerEntry(parts.get(1), TargetKind.INVALID, null));
        case "0" -> entries.add(new DssFunctionPointerEntry(parts.get(1), TargetKind.NULL, null));
        case "U" ->
            entries.add(new DssFunctionPointerEntry(parts.get(1), TargetKind.UNKNOWN, null));
        case "N" ->
            entries.add(new DssFunctionPointerEntry(parts.get(1), TargetKind.NAMED, parts.get(2)));
        default -> throw new AssertionError("Unknown FunctionPointerState");
      }
    }
    return new DssFunctionPointerStatePayload(entries.build());
  }
}
