// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.execution;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.defaults.DummyTargetState;
import org.sosy_lab.cpachecker.core.defaults.SingletonAbstractState;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.alwaystop.AlwaysTopCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.execution.ExecutionState.StackFrame;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class ExecutionPrecisionAdjustmentTest {

  private Optional<PrecisionAdjustmentResult> adjust(
      ExecutionState pState, Optional<AbstractState> pAdjustedState) throws Exception {
    ExecutionPrecisionAdjustment adjustment =
        new ExecutionPrecisionAdjustment(
            (state, precision, states, projection, fullState) ->
                pAdjustedState.map(
                    adjusted ->
                        new PrecisionAdjustmentResult(adjusted, precision, Action.CONTINUE)));
    return adjustment.prec(
        pState,
        SingletonPrecision.getInstance(),
        new ReachedSetFactory(
                Configuration.defaultConfiguration(), LogManager.createTestLogManager())
            .create(AlwaysTopCPA.INSTANCE),
        Functions.identity(),
        pState);
  }

  @Test
  public void replacingWrappedStatePreservesVerdictRestrictionsAndStack() throws Exception {
    StackFrame stack = new StackFrame(null, "main", null);
    AbstractState replacement =
        new CompositeState(ImmutableList.of(SingletonAbstractState.INSTANCE));
    for (AlgorithmStatus status :
        new AlgorithmStatus[] {
          AlgorithmStatus.SOUND_AND_PRECISE,
          AlgorithmStatus.SOUND_AND_IMPRECISE,
          AlgorithmStatus.UNSOUND_AND_PRECISE,
          AlgorithmStatus.UNSOUND_AND_IMPRECISE,
        }) {
      ExecutionState state = new ExecutionState(SingletonAbstractState.INSTANCE, stack, status);
      ExecutionState adjusted =
          (ExecutionState) adjust(state, Optional.of(replacement)).orElseThrow().abstractState();
      assertThat(adjusted.getWrappedState()).isSameInstanceAs(replacement);
      assertThat(adjusted.getCallStack()).isSameInstanceAs(stack);
      assertThat(adjusted.getStatus()).isEqualTo(status);
    }
  }

  @Test
  public void targetIntroducedByPrecisionAdjustmentNeedsPrecision() {
    ExecutionState state =
        new ExecutionState(
            SingletonAbstractState.INSTANCE, null, AlgorithmStatus.SOUND_AND_IMPRECISE);
    assertThrows(
        CPATransferException.class,
        () -> adjust(state, Optional.of(DummyTargetState.withoutTargetInformation())));
  }

  @Test
  public void removingUnsoundStateCannotProveSafety() {
    ExecutionState state =
        new ExecutionState(
            SingletonAbstractState.INSTANCE, null, AlgorithmStatus.UNSOUND_AND_PRECISE);
    assertThrows(CPATransferException.class, () -> adjust(state, Optional.empty()));
  }

  @Test
  public void removingSoundButImpreciseStateIsAllowed() throws Exception {
    ExecutionState state =
        new ExecutionState(
            SingletonAbstractState.INSTANCE, null, AlgorithmStatus.SOUND_AND_IMPRECISE);
    assertThat(adjust(state, Optional.empty())).isEmpty();
  }
}
