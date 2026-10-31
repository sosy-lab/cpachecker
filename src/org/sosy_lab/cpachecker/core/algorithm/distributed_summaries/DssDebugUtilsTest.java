// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssExceptionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssPostConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssResultMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssViolationWitnessMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.payloads.DssComponentKey;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.payloads.DssNoPrecisionPayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.payloads.DssStateAndPrecisionPayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.DssCompositeStatePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa.DssBlockStatePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.location.DssLocationStatePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.DssPredicateStatePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.DssSSAVariablePayload;
import org.sosy_lab.cpachecker.cpa.pathrestriction.SegmentedPaths;

public class DssDebugUtilsTest {

  private static DssStateAndPrecisionPayload locationState(int pNodeId) {
    return new DssStateAndPrecisionPayload(
        new DssLocationStatePayload(pNodeId), DssNoPrecisionPayload.INSTANCE);
  }

  private static DssPostConditionMessage postCondition(
      ImmutableList<DssStateAndPrecisionPayload> pStates) {
    return new DssPostConditionMessage(
        "B1", AlgorithmStatus.SOUND_AND_PRECISE, ImmutableList.copyOf(pStates));
  }

  @Test
  public void summarizeRendersSubtypeSpecificInformation() {
    DssMessage condition = postCondition(ImmutableList.of(locationState(1)));
    DssMessage result = new DssResultMessage("worker", Result.TRUE);
    DssMessage exception = new DssExceptionMessage("worker", "first line\nsecond line");
    DssMessage witness = new DssViolationWitnessMessage("worker", SegmentedPaths.EMPTY);

    assertThat(DssDebugUtils.summarize(condition))
        .contains("POST_CONDITION from B1 states=1 status=sound+precise");
    assertThat(DssDebugUtils.summarize(result)).contains("RESULT from worker result=TRUE");
    assertThat(DssDebugUtils.summarize(exception))
        .contains("EXCEPTION from worker exception=first line");
    assertThat(DssDebugUtils.summarize(witness))
        .contains("WITNESS from worker witnessType=VIOLATION");
  }

  @Test
  public void describeRendersNestedPayloadsAndOmitsNoPrecision() {
    DssPredicateStatePayload predicateState =
        new DssPredicateStatePayload(
            "x == 0", ImmutableMap.of("x", new DssSSAVariablePayload(3, "int")), "pts-1", "x == 0");
    DssMessage message =
        postCondition(
            ImmutableList.of(
                new DssStateAndPrecisionPayload(predicateState, DssNoPrecisionPayload.INSTANCE)));

    String description = DssDebugUtils.describe(message);

    assertThat(description).contains("senderId");
    assertThat(description).contains("predicateState");
    assertThat(description).contains("ssaMap.[x].index");
    assertThat(description).contains("3");
    assertThat(description).contains("readableFormula");
    assertThat(description).contains("<opaque blob");
    assertThat(description).doesNotContain("noPrecision");
  }

  @Test
  public void describeHandlesMessagesWithoutStatesAndEmptyBlockWitnesses() {
    DssMessage result = new DssResultMessage("worker", Result.FALSE);
    DssMessage blockMessage =
        postCondition(
            ImmutableList.of(
                new DssStateAndPrecisionPayload(
                    new DssBlockStatePayload("unique-id", "block-1", "", ImmutableList.of()),
                    DssNoPrecisionPayload.INSTANCE)));

    String resultDescription = DssDebugUtils.describe(result);
    String blockDescription = DssDebugUtils.describe(blockMessage);

    assertThat(resultDescription).contains("RESULT from worker result=FALSE");
    assertThat(resultDescription).doesNotContain("state 0");
    assertThat(blockDescription).contains("blockState");
    assertThat(blockDescription).contains("witnessSegments=0");
  }

  @Test
  public void messageTableReportsStateCountsForMessagesWithAndWithoutStates() {
    String table =
        DssDebugUtils.messageTable(
            ImmutableList.<DssMessage>of(
                new DssResultMessage("worker", Result.TRUE),
                postCondition(ImmutableList.of(locationState(1)))));

    assertThat(table).contains("RESULT");
    assertThat(table).contains("POST_CONDITION");
    assertThat(table).contains("worker");
    assertThat(table).containsMatch("(?m)^0\\s+RESULT\\s+worker\\s+0\\s+");
    assertThat(table).containsMatch("(?m)^1\\s+POST_CONDITION\\s+B1\\s+1\\s+");
  }

  @Test
  public void diffReportsNestedStateChangesAndImmutableListLengthChanges() {
    DssMessage left = postCondition(ImmutableList.of(locationState(1)));
    DssMessage right = postCondition(ImmutableList.of(locationState(2), locationState(3)));

    String diff = DssDebugUtils.diff(left, right);

    assertThat(diff).contains("payload.states.[0].state.nodeId");
    assertThat(diff).contains("payload.states.[1]");
    assertThat(diff).contains("<absent>");
    assertThat(diff).contains("1");
    assertThat(diff).contains("2");
    assertThat(diff).contains("3");
  }

  @Test
  public void diffReportsEqualPayloadsAsEqual() {
    DssMessage message = postCondition(ImmutableList.of(locationState(1)));

    String diff = DssDebugUtils.diff(message, message);

    assertThat(diff).contains("contents are equal");
  }

  @Test
  public void compositePayloadUsesStableComponentNamesInDescription() {
    DssPredicateStatePayload predicateState =
        new DssPredicateStatePayload("x == 0", ImmutableMap.of(), "pts", null);
    DssStateAndPrecisionPayload composite =
        new DssStateAndPrecisionPayload(
            new DssCompositeStatePayload(
                ImmutableMap.of(
                    DssComponentKey.LOCATION_STATE.getSerializedName(),
                    new DssLocationStatePayload(42),
                    DssComponentKey.PREDICATE_STATE.getSerializedName(),
                    predicateState)),
            DssNoPrecisionPayload.INSTANCE);

    String description = DssDebugUtils.describe(postCondition(ImmutableList.of(composite)));

    assertThat(description).contains("compositeState");
    assertThat(description).contains("components.[locationState].nodeId");
    assertThat(description).contains("components.[predicateState].formula");
  }
}
