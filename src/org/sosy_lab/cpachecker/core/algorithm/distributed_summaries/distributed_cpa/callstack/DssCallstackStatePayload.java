// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentBuilder;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssComponentKey;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageKeys;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssStatePayload;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;

public record DssCallstackStatePayload(
    boolean allowAllTransfers, ImmutableList<DssCallstackEntryPayload> states)
    implements DssStatePayload {
  @Override
  public DssComponentKey getComponentKey() {
    return DssComponentKey.CALLSTACK_STATE;
  }

  @Override
  public ImmutableMap<String, String> asContentMap() {
    return ContentBuilder.builder()
        .pushLevel(DssComponentKey.serializedNameFor(CallstackState.class))
        .put(DssMessageKeys.STATE, Joiner.on(DistributedCallstackCPA.DELIMITER).join(states))
        // the receiving block has to know whether the sending block knew its callstack
        .put(DistributedCallstackCPA.ALLOW_ALL_TRANSFERS_KEY, Boolean.toString(allowAllTransfers))
        .build();
  }

  public static DssCallstackStatePayload fromContentMap(Map<String, String> pContent) {
    // states of a block analysis that did not know its callstack must stay unrestricted
    boolean allowAllTransfers =
        Boolean.parseBoolean(
            pContent.getOrDefault(
                DssComponentKey.serializedNameFor(CallstackState.class)
                    + "."
                    + DistributedCallstackCPA.ALLOW_ALL_TRANSFERS_KEY,
                "false"));
    String serializedStates =
        Preconditions.checkNotNull(
            pContent.get(
                DssComponentKey.serializedNameFor(CallstackState.class)
                    + "."
                    + DssMessageKeys.STATE));
    ImmutableList<DssCallstackEntryPayload> states =
        serializedStates.isBlank()
            ? ImmutableList.of()
            : Splitter.on(DistributedCallstackCPA.DELIMITER).splitToList(serializedStates).stream()
                .map(DssCallstackEntryPayload::fromString)
                .collect(ImmutableList.toImmutableList());
    return new DssCallstackStatePayload(allowAllTransfers, states);
  }
}
