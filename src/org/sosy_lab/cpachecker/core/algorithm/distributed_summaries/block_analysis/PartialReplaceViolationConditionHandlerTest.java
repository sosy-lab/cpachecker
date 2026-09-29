// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.SetMultimap;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.DssSingleWorkerStatistics;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssViolationConditionMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis.StateAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

public class PartialReplaceViolationConditionHandlerTest {

  private static final String S1 = "successor-1";
  private static final String S2 = "successor-2";

  private final Precision precision = mock(Precision.class);

  /** Pairs of distinct condition objects that count as equal, in both directions. */
  private final SetMultimap<AbstractState, AbstractState> equal = HashMultimap.create();

  private DssBlockAnalysis analysis;
  private DssMessageFactory messageFactory;
  private PartialReplaceViolationConditionHandler handler;
  private int messageCounter = 0;

  @Before
  public void setUp() throws Exception {
    messageFactory =
        new DssMessageFactory(new DssAnalysisOptions(Configuration.defaultConfiguration()));
    analysis = mock(DssBlockAnalysis.class);
    when(analysis.statistics()).thenReturn(new DssSingleWorkerStatistics("test-block"));
    when(analysis.violationConditionEqual(any(), any()))
        .thenAnswer(
            invocation -> {
              AbstractState condition1 = invocation.getArgument(0);
              AbstractState condition2 = invocation.getArgument(1);
              return condition1 == condition2 || equal.containsEntry(condition1, condition2);
            });
    handler = new PartialReplaceViolationConditionHandler(analysis);
  }

  private AbstractState condition() {
    return mock(AbstractState.class);
  }

  private AbstractState conditionEqualTo(AbstractState pOther) {
    AbstractState condition = condition();
    equal.put(condition, pOther);
    equal.put(pOther, condition);
    return condition;
  }

  private DssViolationConditionMessage message(String pSender, AbstractState... pConditions)
      throws Exception {
    DssViolationConditionMessage message =
        messageFactory.createViolationConditionMessage(
            pSender,
            AlgorithmStatus.SOUND_AND_PRECISE,
            ImmutableMap.of("id", Integer.toString(messageCounter++)));
    ImmutableList.Builder<StateAndPrecision> conditions = ImmutableList.builder();
    for (AbstractState condition : pConditions) {
      conditions.add(new StateAndPrecision(condition, precision));
    }
    when(analysis.deserialize(message)).thenReturn(conditions.build());
    return message;
  }

  @Test
  public void equalSetStops() throws Exception {
    AbstractState x = condition();
    assertThat(handler.store(message(S1, x)).shouldProceed()).isTrue();
    assertThat(handler.store(message(S1, conditionEqualTo(x))).shouldProceed()).isFalse();
    assertThat(handler.states()).containsExactly(x);
  }

  @Test
  public void equalConditionKeepsStoredObject() throws Exception {
    // the engine identifies conditions by identity, so x must survive as the very same object
    AbstractState x = condition();
    AbstractState y = condition();
    handler.store(message(S1, x));
    assertThat(handler.store(message(S1, conditionEqualTo(x), y)).shouldProceed()).isTrue();
    assertThat(handler.states()).containsExactly(x, y);
  }

  @Test
  public void conditionNoLongerSentIsDropped() throws Exception {
    AbstractState x = condition();
    AbstractState y = condition();
    handler.store(message(S1, x, y));
    assertThat(handler.store(message(S1, conditionEqualTo(x))).shouldProceed()).isTrue();
    assertThat(handler.states()).containsExactly(x);
  }

  @Test
  public void equalConditionsShareExplorationButRetainTheirOwners() throws Exception {
    AbstractState x = condition();
    AbstractState sameAsX = conditionEqualTo(x);
    handler.store(message(S1, x));
    assertThat(handler.store(message(S2, sameAsX)).shouldProceed()).isFalse();
    assertThat(handler.states()).containsExactly(x);
    assertThat(handler.store(message(S1)).shouldProceed()).isFalse();
    assertThat(handler.states()).containsExactly(x);
    assertThat(handler.store(message(S2)).shouldProceed()).isTrue();
    assertThat(handler.states()).isEmpty();
  }

  @Test
  public void duplicatesWithinOneMessageAreExploredOnce() throws Exception {
    AbstractState x = condition();
    assertThat(handler.store(message(S1, x, conditionEqualTo(x))).shouldProceed()).isTrue();
    assertThat(handler.states()).containsExactly(x);
  }
}
