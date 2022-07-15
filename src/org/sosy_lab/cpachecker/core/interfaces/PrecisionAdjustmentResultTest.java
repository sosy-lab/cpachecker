// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

import com.google.common.testing.ClassSanityTester;
import org.junit.Test;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;

public class PrecisionAdjustmentResultTest {

  @Test
  public void testCreation() {
    AbstractState state = mock(AbstractState.class);
    Precision precision = mock(Precision.class);
    Action action = Action.BREAK;

    PrecisionAdjustmentResult r = PrecisionAdjustmentResult.create(state, precision, action);
    assertThat(r.abstractState()).isSameInstanceAs(state);
    assertThat(r.precision()).isSameInstanceAs(precision);
    assertThat(r.action()).isSameInstanceAs(action);
  }

  @Test
  public void testEquals() {
    new ClassSanityTester().testEquals(PrecisionAdjustmentResult.class);
  }

  @Test
  public void testNulls() {
    new ClassSanityTester().testNulls(PrecisionAdjustmentResult.class);
  }

  @Test
  public void testWithAbstractState() {
    PrecisionAdjustmentResult r1 =
        PrecisionAdjustmentResult.create(
            mock(AbstractState.class), mock(Precision.class), Action.CONTINUE);

    AbstractState newState = mock(AbstractState.class);
    PrecisionAdjustmentResult r2 = r1.withAbstractState(newState);

    assertThat(r2.abstractState()).isSameInstanceAs(newState);
    assertThat(r2.precision()).isSameInstanceAs(r1.precision());
    assertThat(r2.action()).isSameInstanceAs(r1.action());

    assertThat(r2.abstractState()).isNotEqualTo(r1.abstractState());
    assertThat(r2).isNotEqualTo(r1);
  }

  @Test
  public void testWithPrecision() {
    PrecisionAdjustmentResult r1 =
        PrecisionAdjustmentResult.create(
            mock(AbstractState.class), mock(Precision.class), Action.CONTINUE);

    Precision newPrecision = mock(Precision.class);
    PrecisionAdjustmentResult r2 = r1.withPrecision(newPrecision);

    assertThat(r2.abstractState()).isSameInstanceAs(r1.abstractState());
    assertThat(r2.precision()).isSameInstanceAs(newPrecision);
    assertThat(r2.action()).isSameInstanceAs(r1.action());

    assertThat(r2.precision()).isNotEqualTo(r1.precision());
    assertThat(r2).isNotEqualTo(r1);
  }

  @Test
  public void testWithAction() {
    PrecisionAdjustmentResult r1 =
        PrecisionAdjustmentResult.create(
            mock(AbstractState.class), mock(Precision.class), Action.CONTINUE);

    Action newAction = Action.BREAK;
    PrecisionAdjustmentResult r2 = r1.withAction(newAction);

    assertThat(r2.abstractState()).isSameInstanceAs(r1.abstractState());
    assertThat(r2.precision()).isSameInstanceAs(r1.precision());
    assertThat(r2.action()).isSameInstanceAs(newAction);

    assertThat(r2.action()).isNotEqualTo(r1.action());
    assertThat(r2).isNotEqualTo(r1);
  }
}
