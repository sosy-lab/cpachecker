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
import org.junit.Test;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssComponentKey;

public class PredicateSerializationTest {

  private static final String PREFIX =
      DssComponentKey.PREDICATE_STATE.getSerializedName() + ".";

  @Test
  public void ssaVariablePayloadRoundTrip() {
    DssSSAVariablePayload original = new DssSSAVariablePayload(2, "struct foo");

    assertThat(DssSSAVariablePayload.deserialize(original.serialize()))
        .isEqualTo(original);
  }

  @Test
  public void predicatePayloadRoundTripWithReadableFormula() {
    DssPredicateStatePayload original =
        new DssPredicateStatePayload(
            "(= x 0)",
            ImmutableMap.of(
                "x", new DssSSAVariablePayload(1, "int"),
                "y", new DssSSAVariablePayload(2, "struct foo")),
            "serialized-pts",
            "x == 0");

    assertThat(DssPredicateStatePayload.fromContentMap(original.asContentMap()))
        .isEqualTo(original);
  }

  @Test
  public void predicatePayloadRoundTripWithoutReadableFormula() {
    DssPredicateStatePayload original =
        new DssPredicateStatePayload(
            "true", ImmutableMap.of(), "serialized-pts", null);

    assertThat(DssPredicateStatePayload.fromContentMap(original.asContentMap()))
        .isEqualTo(original);
    assertThat(original.asContentMap())
        .doesNotContainKey(PREFIX + SerializePredicateStateOperator.READABLE_KEY);
  }

  @Test
  public void payloadRejectsInvalidSsaJson() {
    ImmutableMap<String, String> content = content("not-json", "formula", "pts");

    assertThrows(
        IllegalStateException.class,
        () -> DssPredicateStatePayload.fromContentMap(content));
  }

  @Test
  public void payloadRejectsMalformedSsaVariable() {
    ImmutableMap<String, String> content = content("{\"x\":\"1\"}", "formula", "pts");

    assertThrows(
        IllegalArgumentException.class,
        () -> DssPredicateStatePayload.fromContentMap(content));
  }

  @Test
  public void payloadRejectsNonNumericSsaIndex() {
    ImmutableMap<String, String> content =
        content("{\"x\":\"not-an-index int\"}", "formula", "pts");

    assertThrows(
        NumberFormatException.class,
        () -> DssPredicateStatePayload.fromContentMap(content));
  }

  @Test
  public void payloadRejectsMissingSsaMap() {
    ImmutableMap<String, String> content =
        ImmutableMap.of(
            PREFIX + SerializePredicateStateOperator.STATE_KEY, "formula",
            PREFIX + SerializePredicateStateOperator.PTS_KEY, "pts");

    assertThrows(
        NullPointerException.class,
        () -> DssPredicateStatePayload.fromContentMap(content));
  }

  @Test
  public void payloadRejectsMissingFormula() {
    ImmutableMap<String, String> content = content("{}", null, "pts");

    assertThrows(
        NullPointerException.class,
        () -> DssPredicateStatePayload.fromContentMap(content));
  }

  @Test
  public void payloadRejectsMissingPts() {
    ImmutableMap<String, String> content = content("{}", "formula", null);

    assertThrows(
        NullPointerException.class,
        () -> DssPredicateStatePayload.fromContentMap(content));
  }

  private static ImmutableMap<String, String> content(
      String pSsaMap, String pFormula, String pPts) {
    ImmutableMap.Builder<String, String> content = ImmutableMap.builder();
    content.put(PREFIX + SerializePredicateStateOperator.SSA_KEY, pSsaMap);
    if (pFormula != null) {
      content.put(PREFIX + SerializePredicateStateOperator.STATE_KEY, pFormula);
    }
    if (pPts != null) {
      content.put(PREFIX + SerializePredicateStateOperator.PTS_KEY, pPts);
    }
    return content.buildOrThrow();
  }
}
