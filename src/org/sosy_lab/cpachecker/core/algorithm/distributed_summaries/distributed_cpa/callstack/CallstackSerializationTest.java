// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssComponentKey;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageKeys;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.callstack.DssCallstackState;

public class CallstackSerializationTest {

  private static final String STATE_KEY =
      DssComponentKey.CALLSTACK_STATE.getSerializedName() + "." + DssMessageKeys.STATE;

  @Test
  public void payloadRoundTrip() {
    DssCallstackStatePayload original =
        new DssCallstackStatePayload(
            false,
            ImmutableList.of(
                new DssCallstackEntryPayload(1, "main"), new DssCallstackEntryPayload(2, "foo")));

    assertThat(DssCallstackStatePayload.fromContentMap(original.asContentMap()))
        .isEqualTo(original);
  }

  @Test
  public void serializerSerializesNormalCallstack() {
    CFANode mainNode = CFANode.newDummyCFANode();
    CFANode fooNode = CFANode.newDummyCFANode();
    CallstackState state =
        new CallstackState(new CallstackState(null, "main", mainNode), "foo", fooNode);

    ImmutableMap<String, String> serialized =
        new SerializeCallstackStateOperator(ImmutableMap.of(mainNode, 1, fooNode, 2))
            .serialize(state);

    assertThat(serialized)
        .containsExactly(
            STATE_KEY,
            "1.main" + DistributedCallstackCPA.DELIMITER + "2.foo",
            DssComponentKey.CALLSTACK_STATE.getSerializedName()
                + "."
                + DistributedCallstackCPA.ALLOW_ALL_TRANSFERS_KEY,
            "false");
  }

  @Test
  public void serializerSerializesAllowAllTransfers() {
    CFANode node = CFANode.newDummyCFANode();
    DssCallstackState state =
        new DssCallstackState(new CallstackState(null, "main", node), true);

    ImmutableMap<String, String> serialized =
        new SerializeCallstackStateOperator(ImmutableMap.of()).serialize(state);

    assertThat(serialized)
        .containsExactly(
            STATE_KEY,
            "",
            DssComponentKey.CALLSTACK_STATE.getSerializedName()
                + "."
                + DistributedCallstackCPA.ALLOW_ALL_TRANSFERS_KEY,
            "true");
  }

  @Test
  public void payloadRejectsMalformedEntry() {
    ImmutableMap<String, String> content =
        ImmutableMap.of(STATE_KEY, "not-a-callstack-entry");

    assertThrows(
        IllegalArgumentException.class,
        () -> DssCallstackStatePayload.fromContentMap(content));
  }

  @Test
  public void payloadRejectsMissingState() {
    assertThrows(
        NullPointerException.class,
        () -> DssCallstackStatePayload.fromContentMap(ImmutableMap.of()));
  }
}
