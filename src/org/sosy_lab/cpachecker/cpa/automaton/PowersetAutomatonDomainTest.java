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

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Property;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.ResultValue;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;


public class PowersetAutomatonDomainTest {

  private PowersetAutomatonState e_q1_q2;
  private PowersetAutomatonState e_q2_q3;
  private PowersetAutomatonState e_q2_q3_copy;

  private PowersetAutomatonDomain dom;

  @Before
  public void setUp() throws Exception {
    final ControlAutomatonCPA cpa = Mockito.mock(ControlAutomatonCPA.class);
    final Automaton a = Mockito.mock(Automaton.class);

    Mockito.when(a.getName()).thenReturn("Automaton");
    Mockito.when(cpa.getAutomaton()).thenReturn(a);

    final AutomatonInternalState q1 = new AutomatonInternalState("q1", AutomatonBoolExpr.TRUE);
    final AutomatonInternalState q2 = new AutomatonInternalState("q2", AutomatonBoolExpr.TRUE);
    final AutomatonInternalState q3 = new AutomatonInternalState("q3", AutomatonBoolExpr.TRUE);

    dom = new PowersetAutomatonDomain(PowersetAutomatonState.TOP);

    e_q1_q2 = new PowersetAutomatonState(ImmutableSet.copyOf(
        new AutomatonState[]{
            AutomatonState.automatonStateFactory(
                ImmutableMap.<String, AutomatonVariable>of(),
                q1,
                cpa,
                ImmutableList.<Pair<AStatement, Boolean>>of(),
                0, 0, ImmutableMap.<Property, ResultValue<?>>of()),

            AutomatonState.automatonStateFactory(
                ImmutableMap.<String, AutomatonVariable>of(),
                q2,
                cpa,
                ImmutableList.<Pair<AStatement, Boolean>>of(),
                0, 0, ImmutableMap.<Property, ResultValue<?>>of())
    }));

    e_q2_q3 = new PowersetAutomatonState(ImmutableSet.copyOf(
        new AutomatonState[]{
            AutomatonState.automatonStateFactory(
                ImmutableMap.<String, AutomatonVariable>of(),
                q2,
                cpa,
                ImmutableList.<Pair<AStatement, Boolean>>of(),
                0, 0, ImmutableMap.<Property, ResultValue<?>>of()),

            AutomatonState.automatonStateFactory(
                ImmutableMap.<String, AutomatonVariable>of(),
                q3,
                cpa,
                ImmutableList.<Pair<AStatement, Boolean>>of(),
                0, 0, ImmutableMap.<Property, ResultValue<?>>of())
    }));

    e_q2_q3_copy = new PowersetAutomatonState(ImmutableSet.copyOf(
        new AutomatonState[]{
            AutomatonState.automatonStateFactory(
                ImmutableMap.<String, AutomatonVariable>of(),
                q2,
                cpa,
                ImmutableList.<Pair<AStatement, Boolean>>of(),
                0, 0, ImmutableMap.<Property, ResultValue<?>>of()),

            AutomatonState.automatonStateFactory(
                ImmutableMap.<String, AutomatonVariable>of(),
                q3,
                cpa,
                ImmutableList.<Pair<AStatement, Boolean>>of(),
                0, 0, ImmutableMap.<Property, ResultValue<?>>of())
    }));
  }

  @Test
  public void testJoin1() throws CPAException {
    AbstractState result = dom.join(e_q1_q2, e_q2_q3);

    assertThat(result).isInstanceOf(PowersetAutomatonState.class);
    assertThat(((PowersetAutomatonState) result).getNumberOfStates()).isEqualTo(3);
  }

  @Test
  public void testJoinWithTop() throws CPAException {
    AbstractState result = dom.join(e_q1_q2, PowersetAutomatonState.TOP);

    assertThat(result).isInstanceOf(PowersetAutomatonState.class);
    assertThat(result).isEqualTo(PowersetAutomatonState.TOP);

    result = dom.join(PowersetAutomatonState.TOP, e_q1_q2);

    assertThat(result).isInstanceOf(PowersetAutomatonState.class);
    assertThat(result).isEqualTo(PowersetAutomatonState.TOP);
  }

  @Test
  public void testIsLessOrEqual1() throws CPAException {

    assertThat(dom.isLessOrEqual(e_q1_q2, PowersetAutomatonState.TOP)).isTrue();
    assertThat(dom.isLessOrEqual(PowersetAutomatonState.TOP, e_q1_q2)).isFalse();
    assertThat(dom.isLessOrEqual(e_q2_q3, e_q1_q2)).isFalse();
  }

  @Test
  public void testIsLessOrEqual2() throws CPAException {

    assertThat(dom.isLessOrEqual(e_q1_q2, e_q2_q3_copy)).isFalse();
    assertThat(dom.isLessOrEqual(e_q2_q3, e_q2_q3_copy)).isTrue();
  }

}
