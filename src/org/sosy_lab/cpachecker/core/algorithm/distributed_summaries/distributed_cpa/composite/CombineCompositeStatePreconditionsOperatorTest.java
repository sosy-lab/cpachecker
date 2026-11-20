// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.Optional;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombinePreconditionsOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.EqualityCombinePreconditionsOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage.CoverageOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;

public class CombineCompositeStatePreconditionsOperatorTest {

  /** A powerset over two concrete values. */
  private record SetState(int bits) implements AbstractState {}

  private static DistributedConfigurableProgramAnalysis component(boolean disjunctive) {
    DistributedConfigurableProgramAnalysis cpa = mock(DistributedConfigurableProgramAnalysis.class);
    CoverageOperator coverage =
        new CoverageOperator() {
          @Override
          public boolean isSubsumed(AbstractState first, AbstractState second) {
            return first.equals(second);
          }

          @Override
          public boolean isBasedOnEquality() {
            return !disjunctive;
          }
        };
    when(cpa.getCoverageOperator()).thenReturn(coverage);
    CombinePreconditionsOperator operator;
    if (disjunctive) {
      operator =
          new CombinePreconditionsOperator() {
            @Override
            public AbstractState combinePreconditions(Collection<AbstractState> states) {
              return new SetState(
                  states.stream().mapToInt(s -> ((SetState) s).bits()).reduce(0, (a, b) -> a | b));
            }

            @Override
            public Optional<AbstractState> combineIfPossible(Collection<AbstractState> states) {
              return Optional.of(combinePreconditions(states));
            }
          };
    } else {
      operator = new EqualityCombinePreconditionsOperator(coverage, SetState.class);
    }
    when(cpa.getCombineOperator()).thenReturn(operator);
    return cpa;
  }

  private static CompositeState state(int first, int second) {
    return new CompositeState(ImmutableList.of(new SetState(first), new SetState(second)));
  }

  @Test
  public void combinesOnePowersetComponentWithEqualProgramPointComponents() throws Exception {
    CombineCompositeStatePreconditionsOperator operator =
        new CombineCompositeStatePreconditionsOperator(
            ImmutableList.of(component(false), component(true)), CFANode.newDummyCFANode());
    CompositeState result =
        (CompositeState)
            operator.combineIfPossible(ImmutableList.of(state(1, 1), state(1, 2))).orElseThrow();
    assertThat(result.getWrappedStates())
        .containsExactly(new SetState(1), new SetState(3))
        .inOrder();
    assertThat(operator.combineIfPossible(ImmutableList.of(state(1, 1), state(2, 2)))).isEmpty();
  }

  @Test
  public void combinesWhenOnlyOneOfTwoPowersetComponentsVaries() throws Exception {
    CombineCompositeStatePreconditionsOperator operator =
        new CombineCompositeStatePreconditionsOperator(
            ImmutableList.of(component(true), component(true)), CFANode.newDummyCFANode());
    CompositeState result =
        (CompositeState)
            operator.combineIfPossible(ImmutableList.of(state(1, 1), state(1, 2))).orElseThrow();
    assertThat(result.getWrappedStates())
        .containsExactly(new SetState(1), new SetState(3))
        .inOrder();
  }

  @Test
  public void doesNotLoseCorrelationsBetweenPowersetComponents() throws Exception {
    CombineCompositeStatePreconditionsOperator operator =
        new CombineCompositeStatePreconditionsOperator(
            ImmutableList.of(component(true), component(true)), CFANode.newDummyCFANode());
    assertThat(operator.combineIfPossible(ImmutableList.of(state(1, 1), state(2, 2)))).isEmpty();
  }

  @Test
  public void preservesEqualNonDistributedComponentsAndRejectsDifferentOnes() throws Exception {
    CombineCompositeStatePreconditionsOperator operator =
        new CombineCompositeStatePreconditionsOperator(
            ImmutableList.of(mock(ConfigurableProgramAnalysis.class), component(true)),
            CFANode.newDummyCFANode());
    CompositeState result =
        (CompositeState)
            operator.combineIfPossible(ImmutableList.of(state(1, 1), state(1, 2))).orElseThrow();
    assertThat(result.getWrappedStates())
        .containsExactly(new SetState(1), new SetState(3))
        .inOrder();
    assertThat(operator.combineIfPossible(ImmutableList.of(state(1, 1), state(2, 2)))).isEmpty();
  }

  @Test
  public void unsupportedOperatorDoesNotOptInToExactCombination() throws Exception {
    CombinePreconditionsOperator operator = states -> new SetState(3);
    assertThat(operator.combineIfPossible(ImmutableList.of(new SetState(1), new SetState(2))))
        .isEmpty();
  }
}
