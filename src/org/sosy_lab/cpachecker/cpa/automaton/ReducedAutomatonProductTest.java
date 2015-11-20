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

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.cpachecker.cpa.automaton.ReducedAutomatonProduct.ProductState;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.common.truth.Truth;


public class ReducedAutomatonProductTest {

  private Automaton a3_l2;
  private Automaton a2_l2;
  private Automaton a1_l1;
  private Automaton sa1_l1;
  private Automaton sa2_l2;
  private Automaton sa3_l2;
  private Automaton a_seq1;
  private Automaton a_seq2;
  private Automaton a_seq3;
  private Automaton a_seq100;

  private AutomatonTransition newTrans (AutomatonBoolExpr pTrigger, String pTarget) {
    return new AutomatonTransition(
        pTrigger,
        ImmutableList.<AutomatonAction>of(),
        pTarget);
  }

  private Automaton newLabelMatchingAutomaton(String pLabel) throws InvalidAutomatonException, IOException {
    final String automataName = "A1_" + pLabel;
    final String initialStateName = "q0";

    List<AutomatonInternalState> automatonStates = Lists.newArrayList();

    automatonStates.add(new AutomatonInternalState("q0",
        Lists.newArrayList(
            newTrans(new AutomatonBoolExpr.Negation(new AutomatonBoolExpr.MatchLabelExact(pLabel)), "q0"),
            newTrans(new AutomatonBoolExpr.MatchLabelExact(pLabel), "q1"),
            newTrans(new AutomatonBoolExpr.MatchLabelExact(pLabel), "q2")
        ), false, true));

    automatonStates.add(new AutomatonInternalState("q1",
        ImmutableList.<AutomatonTransition>of(),
        true, false));

    automatonStates.add(new AutomatonInternalState("q2",
        Lists.newArrayList(
            newTrans(AutomatonBoolExpr.TRUE, "q2")
        ), false, false));

    Automaton a = new Automaton(automataName, Maps.<String, AutomatonVariable>newHashMap(), automatonStates, initialStateName);

    try(BufferedWriter w = Files.newWriter(Paths.createTempPath(automataName + "_", ".dot").toFile(), Charset.defaultCharset())) {
      a.writeDotFile(w);
    }

    return a;
  }

  private Automaton newSimpleLabelMatchingAutomaton(String pLabel) throws InvalidAutomatonException, IOException {
    final String automataName = "A2_" + pLabel;
    final String initialStateName = "q0";

    List<AutomatonInternalState> automatonStates = Lists.newArrayList();

    automatonStates.add(new AutomatonInternalState("q0",
        Lists.newArrayList(
            newTrans(new AutomatonBoolExpr.Negation(new AutomatonBoolExpr.MatchLabelExact(pLabel)), "q0"),
            newTrans(new AutomatonBoolExpr.MatchLabelExact(pLabel), "q1")
        ), false, true));

    automatonStates.add(new AutomatonInternalState("q1",
        ImmutableList.<AutomatonTransition>of(),
        true, false));

    Automaton a = new Automaton(automataName, Maps.<String, AutomatonVariable>newHashMap(), automatonStates, initialStateName);

    try(BufferedWriter w = Files.newWriter(Paths.createTempPath(automataName + "_", ".dot").toFile(), Charset.defaultCharset())) {
      a.writeDotFile(w);
    }

    return a;
  }

  private Automaton similarPrefixMatchingAutomaton(int prefixLen) throws InvalidAutomatonException, IOException {
    final String automataName = "A_SEQ_" + Integer.toString(prefixLen);
    final String initialStateName = "q0";

    List<AutomatonInternalState> automatonStates = Lists.newArrayList();

    String succStateName = null;

    for (int i = 0; i<prefixLen; i++) {
      final String matchLabel = String.format("L%d", i);
      final String stateName = String.format("q%d", i);
      succStateName = String.format("q%d", i+1);
      automatonStates.add(new AutomatonInternalState(stateName,
          Lists.newArrayList(
              newTrans(new AutomatonBoolExpr.Negation(new AutomatonBoolExpr.MatchLabelExact(matchLabel)), stateName),
              newTrans(new AutomatonBoolExpr.MatchLabelExact(matchLabel), succStateName)
          ), false, false));
    }

    automatonStates.add(new AutomatonInternalState(succStateName,
        ImmutableList.<AutomatonTransition>of(),
        true, false));

    Automaton a = new Automaton(automataName, Maps.<String, AutomatonVariable>newHashMap(), automatonStates, initialStateName);

    try(BufferedWriter w = Files.newWriter(Paths.createTempPath(automataName + "_", ".dot").toFile(), Charset.defaultCharset())) {
      a.writeDotFile(w);
    }

    return a;
  }

  @Before
  public void setUp() throws Exception {

    a1_l1 = newLabelMatchingAutomaton("L1");
    a2_l2 = newLabelMatchingAutomaton("L2");
    a3_l2 = newLabelMatchingAutomaton("L2");

    sa1_l1 = newSimpleLabelMatchingAutomaton("L1");
    sa2_l2 = newSimpleLabelMatchingAutomaton("L2");
    sa3_l2 = newSimpleLabelMatchingAutomaton("L2");

    a_seq1 = similarPrefixMatchingAutomaton(1);
    a_seq2 = similarPrefixMatchingAutomaton(2);
    a_seq3 = similarPrefixMatchingAutomaton(3);
    a_seq100 = similarPrefixMatchingAutomaton(100);
  }

  @Test
  public void testEquality() {
    Truth.assertThat(sa2_l2.getInitialState()).isEqualTo(sa2_l2.getInitialState());

    ProductState p1 = ProductState.of(Lists.newArrayList(
        Collections.singletonList(sa2_l2.getInitialState()),
        Collections.singletonList(sa3_l2.getInitialState()))).iterator().next();
    ProductState p2 = ProductState.of(Lists.newArrayList(
        Collections.singletonList(sa2_l2.getInitialState()),
        Collections.singletonList(sa3_l2.getInitialState()))).iterator().next();

    Truth.assertThat(p1).isEqualTo(p2);
  }

  @Test
  public void testSimple() throws InvalidAutomatonException, FileNotFoundException, IOException {
    Automaton a_1_1 = ReducedAutomatonProduct.productOf(sa1_l1, sa1_l1, "sa_1_1");

    try(BufferedWriter w = Files.newWriter(Paths.get("/tmp/" + a_1_1.getName() + ".dot").toFile(), Charset.defaultCharset())) {
      a_1_1.writeDotFile(w);
    }

    Automaton a_1_2 = ReducedAutomatonProduct.productOf(sa1_l1, sa2_l2, "sa_1_2");

    try(BufferedWriter w = Files.newWriter(Paths.get("/tmp/" + a_1_2.getName() + ".dot").toFile(), Charset.defaultCharset())) {
      a_1_2.writeDotFile(w);
    }

    // TODO: asserts
  }

  @Test
  public void test() throws InvalidAutomatonException {
    Automaton a_1_1 = ReducedAutomatonProduct.productOf(a1_l1, a1_l1, "a_1_1");
    Automaton a_1_2 = ReducedAutomatonProduct.productOf(a2_l2, a1_l1, "a_1_2");
    Automaton a_2_2 = ReducedAutomatonProduct.productOf(a3_l2, a2_l2, "a_2_2");

    // TODO: asserts
  }

  @Test
  public void testPrefix() throws InvalidAutomatonException, FileNotFoundException, IOException {
    Automaton s_1_2 = ReducedAutomatonProduct.productOf(a_seq1, a_seq2, "s_1_2");

    try(BufferedWriter w = Files.newWriter(Paths.get("/tmp/" + s_1_2.getName() + ".dot").toFile(), Charset.defaultCharset())) {
      s_1_2.writeDotFile(w);
    }

    // TODO: asserts
  }


}
