/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.automaton;
import static org.mockito.Mockito.mock;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.truth.Truth;

public class AutomatonDomainTest {

  private ControlAutomatonCPA cpa;

  @Before
  public void setup() {
    cpa = mock(ControlAutomatonCPA.class);
  }

  private AutomatonState createTestState(String pName) {
    final AutomatonInternalState internalState = new AutomatonInternalState(
        pName, Collections.<AutomatonTransition>emptyList());

    return AutomatonState.automatonStateFactory(
        Collections.<String,AutomatonVariable>emptyMap(),
        internalState, cpa,
        0, 0, false, null);
  }

  @Test
  public void testLessOrEqual() throws CPAException {
    AutomatonState inactive = new AutomatonState.INACTIVE(cpa);
    AutomatonState top = new AutomatonState.TOP(cpa);
    AutomatonState q1 = createTestState("q1");

    AutomatonDomain dom = new AutomatonDomain(top, inactive);

    Truth.assertThat(dom.isLessOrEqual(inactive, top)).isTrue();
    Truth.assertThat(dom.isLessOrEqual(top, inactive)).isFalse();

    Truth.assertThat(dom.isLessOrEqual(q1, inactive)).isTrue();
    Truth.assertThat(dom.isLessOrEqual(inactive, q1)).isFalse();
  }

}
