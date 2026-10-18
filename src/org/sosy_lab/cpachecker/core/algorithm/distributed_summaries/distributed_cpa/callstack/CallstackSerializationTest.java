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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssComponentKey;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageKeys;

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
  public void payloadRejectsMalformedEntry() {
    ImmutableMap<String, String> content = ImmutableMap.of(STATE_KEY, "not-a-callstack-entry");

    assertThrows(
        IllegalArgumentException.class, () -> DssCallstackStatePayload.fromContentMap(content));
  }

  @Test
  public void payloadRejectsMissingState() {
    assertThrows(
        NullPointerException.class,
        () -> DssCallstackStatePayload.fromContentMap(ImmutableMap.of()));
  }
}
