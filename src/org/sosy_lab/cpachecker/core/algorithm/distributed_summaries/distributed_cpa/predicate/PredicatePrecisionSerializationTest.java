// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssComponentKey;

public class PredicatePrecisionSerializationTest {

  private static final String PREFIX =
      DssComponentKey.PREDICATE_PRECISION.getSerializedName() + ".";

  @Test
  public void payloadRoundTripPreservesAllPredicateKinds() {
    DssLocationInstancePayload location = new DssLocationInstancePayload(7, 2);
    DssPredicatePrecisionPayload original =
        new DssPredicatePrecisionPayload(
            ImmutableMap.of(location, ImmutableSet.of("x > 0", "y == 1")),
            ImmutableMap.of(7, ImmutableSet.of("x != 0", "z < 4")),
            ImmutableMap.of("main", ImmutableSet.of("a == b", "p != 0")),
            ImmutableSet.of("global > 0", "global != 1"));

    ImmutableMap<String, String> originalMap = original.asContentMap();
    DssPredicatePrecisionPayload parsed = DssPredicatePrecisionPayload.fromContentMap(originalMap);
    assertThat(parsed).isEqualTo(original);
  }

  @Test
  public void payloadContentMapRoundTripPreservesAllEntries() {
    DssPredicatePrecisionPayload original =
        new DssPredicatePrecisionPayload(
            ImmutableMap.of(
                new DssLocationInstancePayload(1, 0), ImmutableSet.of("x > 0"),
                new DssLocationInstancePayload(2, 1), ImmutableSet.of("y == 1")),
            ImmutableMap.of(1, ImmutableSet.of("x > 0")),
            ImmutableMap.of("main", ImmutableSet.of("x > 0")),
            ImmutableSet.of("x > 0"));

    ImmutableMap<String, String> content = original.asContentMap();

    assertThat(DssPredicatePrecisionPayload.fromContentMap(content).asContentMap())
        .containsExactlyEntriesIn(content);
  }

  @Test
  public void emptyPayloadRoundTrip() {
    DssPredicatePrecisionPayload original =
        new DssPredicatePrecisionPayload(
            ImmutableMap.of(), ImmutableMap.of(), ImmutableMap.of(), ImmutableSet.of());

    assertThat(DssPredicatePrecisionPayload.fromContentMap(original.asContentMap()))
        .isEqualTo(original);
  }

  @Test
  public void payloadRejectsMalformedLocationInstanceKey() {
    ImmutableMap<String, String> content =
        ImmutableMap.of(
            PREFIX
                + SerializePredicatePrecisionOperator.DSS_MESSAGE_LOCATION_INSTANCES_KEY
                + ".invalid",
            "x > 0",
            PREFIX + SerializePredicatePrecisionOperator.DSS_MESSAGE_GLOBAL_KEY,
            "global > 0");

    assertThrows(
        IllegalArgumentException.class, () -> DssPredicatePrecisionPayload.fromContentMap(content));
  }

  @Test
  public void payloadRejectsMalformedLocalPredicateKey() {
    ImmutableMap<String, String> content =
        ImmutableMap.of(
            PREFIX
                + SerializePredicatePrecisionOperator.DSS_MESSAGE_LOCAL_PREDICATES_KEY
                + ".not-a-node-id",
            "x > 0",
            PREFIX + SerializePredicatePrecisionOperator.DSS_MESSAGE_GLOBAL_KEY,
            "global > 0");

    assertThrows(
        NumberFormatException.class, () -> DssPredicatePrecisionPayload.fromContentMap(content));
  }

  @Test
  public void payloadRejectsMissingGlobalPredicates() {
    ImmutableMap<String, String> content = ImmutableMap.of();

    assertThrows(
        NullPointerException.class, () -> DssPredicatePrecisionPayload.fromContentMap(content));
  }
}
