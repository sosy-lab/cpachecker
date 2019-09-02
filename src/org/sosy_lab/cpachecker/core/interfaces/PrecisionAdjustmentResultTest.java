/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
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
    PrecisionAdjustmentResult r1 = PrecisionAdjustmentResult.create(
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
    PrecisionAdjustmentResult r1 = PrecisionAdjustmentResult.create(
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
    PrecisionAdjustmentResult r1 = PrecisionAdjustmentResult.create(
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
