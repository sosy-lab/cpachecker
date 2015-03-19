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

import static org.mockito.Mockito.mock;

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;

import com.google.common.testing.ClassSanityTester;

public class PrecisionAdjustmentResultTest {

  @Test
  public void testCreation() {
    AbstractState state = mock(AbstractState.class);
    Precision precision = mock(Precision.class);
    Action action = Action.BREAK;

    PrecisionAdjustmentResult r = PrecisionAdjustmentResult.create(state, precision, action);
    Assert.assertSame(state, r.abstractState());
    Assert.assertSame(precision, r.precision());
    Assert.assertSame(action, r.action());
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

    Assert.assertSame(newState, r2.abstractState());
    Assert.assertSame(r1.precision(), r2.precision());
    Assert.assertSame(r1.action(), r2.action());

    Assert.assertNotEquals(r1.abstractState(), r2.abstractState());
    Assert.assertNotEquals(r1, r2);
  }

  @Test
  public void testWithPrecision() {
    PrecisionAdjustmentResult r1 = PrecisionAdjustmentResult.create(
        mock(AbstractState.class), mock(Precision.class), Action.CONTINUE);

    Precision newPrecision = mock(Precision.class);
    PrecisionAdjustmentResult r2 = r1.withPrecision(newPrecision);

    Assert.assertSame(r1.abstractState(), r2.abstractState());
    Assert.assertSame(newPrecision, r2.precision());
    Assert.assertSame(r1.action(), r2.action());

    Assert.assertNotEquals(r1.precision(), r2.precision());
    Assert.assertNotEquals(r1, r2);
  }

  @Test
  public void testWithAction() {
    PrecisionAdjustmentResult r1 = PrecisionAdjustmentResult.create(
        mock(AbstractState.class), mock(Precision.class), Action.CONTINUE);

    Action newAction = Action.BREAK;
    PrecisionAdjustmentResult r2 = r1.withAction(newAction);

    Assert.assertSame(r1.abstractState(), r2.abstractState());
    Assert.assertSame(r1.precision(), r2.precision());
    Assert.assertSame(newAction, r2.action());

    Assert.assertNotEquals(r1.action(), r2.action());
    Assert.assertNotEquals(r1, r2);
  }
}
