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
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.io.Paths;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;


public class ReducedAutomatonProductTest {

  private Automaton a3_l2;
  private Automaton a2_l2;
  private Automaton a1_l1;

  private AutomatonTransition newTrans (AutomatonBoolExpr pTrigger, String pTarget) {
    return new AutomatonTransition(
        pTrigger,
        ImmutableList.<AutomatonBoolExpr>of(),
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

  @Before
  public void setUp() throws Exception {

    a1_l1 = newLabelMatchingAutomaton("L1");
    a2_l2 = newLabelMatchingAutomaton("L2");
    a3_l2 = newLabelMatchingAutomaton("L2");

  }

  @Test
  public void test() {
    Automaton a_1_1 = ReducedAutomatonProduct.productOf(a1_l1, a1_l1);
    Automaton a_1_2 = ReducedAutomatonProduct.productOf(a1_l1, a2_l2);
    Automaton a_2_2 = ReducedAutomatonProduct.productOf(a2_l2, a3_l2);

    // TODO: asserts
  }

}
