// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

public class PredicatePrecisionSerializationTest {

  private AbstractionManager abstractionManager;
  private DssMessageFactory messageFactory;

  @Before
  public void setUp() throws Exception {
    abstractionManager = mock(AbstractionManager.class);
    messageFactory =
        new DssMessageFactory(new DssAnalysisOptions(Configuration.defaultConfiguration()));
  }

  private ImmutableMap<String, String> serialize(PredicatePrecision pPrecision) {
    // A precision without predicates never asks the formula manager to dump one.
    return new SerializePredicatePrecisionOperator(
            mock(FormulaManagerView.class), ImmutableMap.of())
        .serializePrecision(pPrecision);
  }

  private PredicatePrecision deserialize(ImmutableMap<String, String> pContent) {
    DssMessage message =
        messageFactory.createDssPostConditionMessage(
            "sender", AlgorithmStatus.SOUND_AND_PRECISE, pContent);
    return (PredicatePrecision)
        new DeserializePredicatePrecisionOperator(
                abstractionManager, id -> CFANode.newDummyCFANode())
            .deserializePrecision(message);
  }

  /**
   * The serialized content has to stay non-empty even for an empty precision, because {@link
   * DssMessage#getPrecisionContent} rejects an empty section.
   */
  @Test
  public void emptyPrecisionSerializesToNonEmptyContent() {
    assertThat(serialize(PredicatePrecision.empty())).isNotEmpty();
  }

  /**
   * An empty precision has to survive the round trip unchanged. It used to come back with a global
   * {@code false} predicate, because the empty set of global predicates was serialized as an empty
   * string, which was read back as a single blank predicate.
   */
  @Test
  public void emptyPrecisionRoundTripsToEmptyPrecision() {
    PredicatePrecision deserialized = deserialize(serialize(PredicatePrecision.empty()));

    assertThat(deserialized.isEmpty()).isTrue();
    assertThat(deserialized).isEqualTo(PredicatePrecision.empty());
    verify(abstractionManager, never()).parsePredicate(any());
  }

  /** No predicate is created from a blank value, in whichever section it appears. */
  @Test
  public void blankValuesYieldNoPredicates() {
    String prefix = PredicatePrecision.class.getName() + ".";
    ImmutableMap<String, String> content =
        ImmutableMap.of(
            prefix + SerializePredicatePrecisionOperator.DSS_MESSAGE_GLOBAL_KEY,
            "",
            prefix
                + SerializePredicatePrecisionOperator.DSS_MESSAGE_FUNCTION_PREDICATES_KEY
                + ".main",
            "",
            prefix + SerializePredicatePrecisionOperator.DSS_MESSAGE_LOCAL_PREDICATES_KEY + ".0",
            "",
            prefix
                + SerializePredicatePrecisionOperator.DSS_MESSAGE_LOCATION_INSTANCES_KEY
                + ".0,1",
            "");

    assertThat(deserialize(content).isEmpty()).isTrue();
    verify(abstractionManager, never()).parsePredicate(any());
  }
}
