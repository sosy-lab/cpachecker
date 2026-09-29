// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;

public class PredicatePrecisionDictionaryTest {

  private static final String PRECISION = PredicatePrecision.class.getName() + ".";
  private static final String P = "(declare-fun p () Bool)\n(assert p)";
  private static final String Q = "(declare-fun q () Bool)\n(assert q)";

  @Test
  public void sharesPredicatesAcrossStatesAndScopes() {
    ImmutableMap<String, String> content =
        ImmutableMap.of(
            "states",
            "2",
            "state0." + PRECISION + "global",
            P,
            "state0." + PRECISION + "localPredicates.4",
            P + " , " + Q,
            "state1." + PRECISION + "global",
            P,
            "state1." + PRECISION + "functionPredicates.main",
            Q,
            "state1." + PRECISION + "locationInstances.4,1",
            P + " , " + Q);
    ImmutableMap<String, String> encoded = PredicatePrecisionDictionary.encode(content);
    assertThat(encoded.values().stream().filter(P::equals).count()).isEqualTo(1);
    assertThat(encoded.values().stream().filter(Q::equals).count()).isEqualTo(1);
    assertThat(PredicatePrecisionDictionary.decode(encoded)).isEqualTo(content);
    assertThat(PredicatePrecisionDictionary.encode(encoded)).isEqualTo(encoded);

    DssMessage message = new DssPostConditionMessage("test", content);
    DssMessage restored = DssMessage.fromJson(message.asJson());
    assertThat(
            restored.advance("state0").getPrecisionContent(PredicatePrecision.class).get("global"))
        .isEqualTo(P);
    assertThat(
            restored
                .advance("state1")
                .getPrecisionContent(PredicatePrecision.class)
                .pushLevel("locationInstances")
                .get("4,1"))
        .isEqualTo(P + " , " + Q);
  }

  @Test
  public void sharesDefinitionsBetweenStateAndBoundaryPrecision() {
    ImmutableMap<String, String> content =
        ImmutableMap.of(
            DssMessage.PRECISION_UPDATE_KEY,
            "true",
            "states",
            "1",
            "state0." + PRECISION + "localPredicates.4",
            P,
            DssMessage.SHARED_PRECISION_KEY + "." + PRECISION + "localPredicates.4",
            P + " , " + Q);
    DssMessage message = new DssViolationConditionMessage("test", content);
    DssMessage restored = DssMessage.fromJson(message.asJson());
    assertThat(restored.hasPrecisionUpdate()).isTrue();
    assertThat(restored.isPrecisionOnly()).isFalse();
    assertThat(
            restored.asJson().get(DssMessage.DSS_MESSAGE_CONTENT_ID).values().stream()
                .filter(P::equals)
                .count())
        .isEqualTo(1);
    assertThat(
            restored
                .advance(DssMessage.SHARED_PRECISION_KEY)
                .getPrecisionContent(PredicatePrecision.class)
                .pushLevel("localPredicates")
                .get("4"))
        .isEqualTo(P + " , " + Q);
    assertThat(
            restored
                .advance("state0")
                .getPrecisionContent(PredicatePrecision.class)
                .pushLevel("localPredicates")
                .get("4"))
        .isEqualTo(P);
  }

  @Test
  public void removesRepeatedPredicatesWithinAScopeAndPreservesEmptyPrecision() {
    ImmutableMap<String, String> content =
        ImmutableMap.of(
            "state0." + PRECISION + "global", "", "state1." + PRECISION + "global", P + " , " + P);
    ImmutableMap<String, String> decoded =
        PredicatePrecisionDictionary.decode(PredicatePrecisionDictionary.encode(content));
    assertThat(decoded)
        .containsExactly("state0." + PRECISION + "global", "", "state1." + PRECISION + "global", P);
  }

  @Test
  public void leavesAlreadyUniquePredicatesWithoutDictionaryOverhead() {
    ImmutableMap<String, String> content =
        ImmutableMap.of(
            "state0." + PRECISION + "global", P,
            DssMessage.SHARED_PRECISION_KEY + "." + PRECISION + "localPredicates.4", Q);
    assertThat(PredicatePrecisionDictionary.encode(content)).isEqualTo(content);
    DssMessage restored =
        DssMessage.fromJson(new DssPostConditionMessage("test", content).asJson());
    assertThat(restored.asJson().get(DssMessage.DSS_MESSAGE_CONTENT_ID)).isEqualTo(content);
    assertThat(
            restored.advance("state0").getPrecisionContent(PredicatePrecision.class).get("global"))
        .isEqualTo(P);
  }

  @Test
  public void leavesEmptyPrecisionsWithoutDictionaryOverhead() {
    ImmutableMap<String, String> content =
        ImmutableMap.of(
            "state0." + PRECISION + "global", "",
            DssMessage.SHARED_PRECISION_KEY + "." + PRECISION + "global", "");
    assertThat(PredicatePrecisionDictionary.encode(content)).isEqualTo(content);
    assertThat(PredicatePrecisionDictionary.decode(content)).isEqualTo(content);
  }

  @Test
  public void acceptsLegacyContentAndRejectsDanglingReferences() {
    ImmutableMap<String, String> content = ImmutableMap.of(PRECISION + "global", P);
    assertThat(PredicatePrecisionDictionary.decode(content)).isEqualTo(content);
    ImmutableMap<String, String> danglingReference =
        ImmutableMap.of("predicateEncoding", "1", PRECISION + "global", "3");
    assertThrows(
        IllegalArgumentException.class,
        () -> PredicatePrecisionDictionary.decode(danglingReference));
  }

  @Test
  public void expandsOnlyTheRequestedPrecision() {
    ImmutableMap<String, String> content =
        ImmutableMap.of(
            "predicateEncoding",
            "1",
            "predicateDefinitions.0",
            P,
            PRECISION + "global",
            "0",
            "state1." + PRECISION + "global",
            "missing");
    assertThat(PredicatePrecisionDictionary.decode(content, PRECISION))
        .containsExactly(PRECISION + "global", P);
    assertThrows(
        IllegalArgumentException.class, () -> PredicatePrecisionDictionary.decode(content));
  }
}
