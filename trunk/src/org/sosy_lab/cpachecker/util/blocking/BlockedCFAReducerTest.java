// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.blocking;

import static com.google.common.truth.Truth.assertThat;
import static org.sosy_lab.cpachecker.cfa.model.CFANode.newDummyCFANode;

import com.google.common.io.CharStreams;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;

@SuppressWarnings("unused")
public class BlockedCFAReducerTest {

  private BlockedCFAReducer reducer;

  @Before
  public void setUp() throws InvalidConfigurationException {
    reducer =
        new BlockedCFAReducer(
            Configuration.defaultConfiguration(), LogManager.createTestLogManager());
  }

  private void assertCfaIsEmpty(Map<ReducedNode, Map<ReducedNode, Set<ReducedEdge>>> inlinedCfa) {
    StringBuilder sb = new StringBuilder();
    try {
      reducer.printInlinedCfa(inlinedCfa, new BufferedWriter(CharStreams.asWriter(sb)));
    } catch (IOException e) {
      throw new AssertionError(e);
    }
    assertThat(sb.toString()).isEmpty();
  }

  @Test
  public void testApplySequenceRule_SimpleSequence() {
    ReducedNode entryNode = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode exitNode = new ReducedNode(newDummyCFANode("test"), false);
    ReducedFunction funct = new ReducedFunction(entryNode, exitNode);

    ReducedNode n1 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n2 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n3 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n4 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n5 = new ReducedNode(newDummyCFANode("test"), false);

    funct.addEdge(entryNode, n1);
    funct.addEdge(n1, n2);
    funct.addEdge(n2, n3);
    funct.addEdge(n3, n4);
    funct.addEdge(n4, n5);
    funct.addEdge(n5, exitNode);

    assertThat(reducer.applySequenceRule(funct)).isTrue();
    assertThat(funct.getNumOfActiveNodes()).isEqualTo(2);
  }

  @Test
  public void testApplySequenceRule_ForLoop() {
    ReducedNode entryNode = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode exitNode = new ReducedNode(newDummyCFANode("test"), false);
    ReducedFunction funct = new ReducedFunction(entryNode, exitNode);

    ReducedNode n1 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n2 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n3 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n4 = new ReducedNode(newDummyCFANode("loophead"), false);
    n4.getWrapped().setLoopStart();
    ReducedNode n5 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n6 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n7 = new ReducedNode(newDummyCFANode("test"), false);

    ReducedNode n8 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n9 = new ReducedNode(newDummyCFANode("test"), false);

    funct.addEdge(entryNode, n1);
    funct.addEdge(n1, n2);
    funct.addEdge(n2, n3);
    funct.addEdge(n3, n4);

    funct.addEdge(n4, n8);
    funct.addEdge(n4, n5);
    funct.addEdge(n5, n6);
    funct.addEdge(n6, n7);
    funct.addEdge(n7, n4);

    funct.addEdge(n8, n9);
    funct.addEdge(n9, exitNode);

    while (reducer.applySequenceRule(funct)) {}

    assertThat(funct.getNumOfActiveNodes()).isEqualTo(3);
    assertThat(funct.getNumLeavingEdges(n4)).isEqualTo(2);
  }

  @Test
  public void testAllRules_ForLoopWithSequence_reduce() {
    ReducedNode entryNode = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode exitNode = new ReducedNode(newDummyCFANode("test"), false);
    ReducedFunction funct = new ReducedFunction(entryNode, exitNode);

    ReducedNode n4 = new ReducedNode(newDummyCFANode("loophead"), false);
    n4.getWrapped().setLoopStart();

    ReducedNode n5 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n6 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n7 = new ReducedNode(newDummyCFANode("test"), false);

    ReducedNode n8 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n9 = new ReducedNode(newDummyCFANode("test"), false);

    ReducedNode n20 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n21 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n22 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n23 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n24 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n25 = new ReducedNode(newDummyCFANode("test"), false);

    funct.addEdge(entryNode, n4);

    funct.addEdge(n4, n8);
    funct.addEdge(n4, n5);
    funct.addEdge(n5, n6);
    funct.addEdge(n6, n7);
    funct.addEdge(n7, n4);

    funct.addEdge(n8, n9);
    funct.addEdge(n9, n20);
    funct.addEdge(n20, n21);
    funct.addEdge(n21, n22);
    funct.addEdge(n22, n23);
    funct.addEdge(n23, n24);
    funct.addEdge(n24, n25);
    funct.addEdge(n25, exitNode);

    boolean sequenceApplied;
    do {
      Map<ReducedNode, Map<ReducedNode, Set<ReducedEdge>>> inlinedCfa = funct.getInlinedCfa();
      assertCfaIsEmpty(inlinedCfa);

      sequenceApplied = reducer.applySequenceRule(funct);
    } while (sequenceApplied);

    assertThat(funct.getNumOfActiveNodes()).isEqualTo(3);
    assertThat(funct.getNumLeavingEdges(n4)).isEqualTo(2);
  }

  @Test
  public void testAllRules_ForLoopWithSequence() {
    ReducedNode entryNode = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode exitNode = new ReducedNode(newDummyCFANode("test"), false);
    ReducedFunction funct = new ReducedFunction(entryNode, exitNode);

    ReducedNode n1 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n2 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n3 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n4 = new ReducedNode(newDummyCFANode("loophead"), false);
    n4.getWrapped().setLoopStart();
    ReducedNode n5 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n6 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n7 = new ReducedNode(newDummyCFANode("test"), false);

    ReducedNode n8 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n9 = new ReducedNode(newDummyCFANode("test"), false);

    ReducedNode n20 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n21 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n22 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n23 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n24 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n25 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n26 = new ReducedNode(newDummyCFANode("test"), false);

    funct.addEdge(entryNode, n1);
    funct.addEdge(n1, n2);
    funct.addEdge(n2, n3);
    funct.addEdge(n3, n4);

    funct.addEdge(n4, n8);
    funct.addEdge(n4, n5);
    funct.addEdge(n5, n6);
    funct.addEdge(n6, n7);
    funct.addEdge(n7, n4);

    funct.addEdge(n8, n9);

    funct.addEdge(n9, n20);
    funct.addEdge(n20, n21);
    funct.addEdge(n21, n22);
    funct.addEdge(n22, n23);
    funct.addEdge(n23, n24);
    funct.addEdge(n24, n25);
    funct.addEdge(n25, n26);
    funct.addEdge(n26, exitNode);

    boolean sequenceApplied, choiceApplied;
    do {
      Map<ReducedNode, Map<ReducedNode, Set<ReducedEdge>>> inlinedCfa = funct.getInlinedCfa();
      assertCfaIsEmpty(inlinedCfa);

      sequenceApplied = reducer.applySequenceRule(funct);
      choiceApplied = reducer.applyChoiceRule(funct);
    } while (sequenceApplied || choiceApplied);

    assertThat(funct.getNumOfActiveNodes()).isEqualTo(3);
    assertThat(funct.getNumLeavingEdges(n4)).isEqualTo(2);
  }

  @Test
  public void testAllRules_ForLoopWithSequence2() {
    ReducedNode entryNode = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode exitNode = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n4 = new ReducedNode(newDummyCFANode("loophead"), false);
    ReducedNode n6 = new ReducedNode(newDummyCFANode("test"), false);
    n4.getWrapped().setLoopStart();

    ReducedFunction funct = new ReducedFunction(entryNode, exitNode);

    funct.addEdge(entryNode, n4);
    funct.addEdge(n4, n6);
    funct.addEdge(n6, n4);
    funct.addEdge(n4, exitNode);

    boolean sequenceApplied, choiceApplied;
    do {
      Map<ReducedNode, Map<ReducedNode, Set<ReducedEdge>>> inlinedCfa = funct.getInlinedCfa();
      assertCfaIsEmpty(inlinedCfa);

      sequenceApplied = reducer.applySequenceRule(funct);
      choiceApplied = reducer.applyChoiceRule(funct);
    } while (sequenceApplied || choiceApplied);

    assertThat(funct.getNumOfActiveNodes()).isEqualTo(3);
    assertThat(funct.getNumLeavingEdges(n4)).isEqualTo(2);
  }

  @Test
  public void testAllRules_ForLoopWithSequence3() {
    ReducedNode entryNode = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode exitNode = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n4 = new ReducedNode(newDummyCFANode("loophead"), false);
    ReducedNode n6 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n10 = new ReducedNode(newDummyCFANode("test"), false);
    n4.getWrapped().setLoopStart();

    ReducedFunction funct = new ReducedFunction(entryNode, exitNode);

    funct.addEdge(entryNode, n4);
    funct.addEdge(n6, n4);
    funct.addEdge(n4, n6);
    funct.addEdge(n4, n10);
    funct.addEdge(n10, exitNode);

    boolean sequenceApplied, choiceApplied;
    do {
      Map<ReducedNode, Map<ReducedNode, Set<ReducedEdge>>> inlinedCfa = funct.getInlinedCfa();
      assertCfaIsEmpty(inlinedCfa);

      sequenceApplied = reducer.applySequenceRule(funct);
      choiceApplied = reducer.applyChoiceRule(funct);
    } while (sequenceApplied || choiceApplied);

    assertThat(funct.getNumOfActiveNodes()).isEqualTo(3);
    assertThat(funct.getNumLeavingEdges(n4)).isEqualTo(2);
  }

  @Test
  public void testApplySequenceRule_RepeatUntilLoop() {
    ReducedNode entryNode = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode exitNode = new ReducedNode(newDummyCFANode("test"), false);
    ReducedFunction funct = new ReducedFunction(entryNode, exitNode);

    ReducedNode n1 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n2 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n3 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n4 = new ReducedNode(newDummyCFANode("loophead"), false);
    n4.getWrapped().setLoopStart();

    ReducedNode n5 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n6 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n7 = new ReducedNode(newDummyCFANode("test"), false);

    ReducedNode n8 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n9 = new ReducedNode(newDummyCFANode("test"), false);

    funct.addEdge(entryNode, n1);
    funct.addEdge(n1, n2);
    funct.addEdge(n2, n3);
    funct.addEdge(n3, n4);
    funct.addEdge(n4, n5);
    funct.addEdge(n5, n6);
    funct.addEdge(n6, n7);

    funct.addEdge(n7, n4);
    funct.addEdge(n7, n8);

    funct.addEdge(n8, n9);
    funct.addEdge(n9, exitNode);

    while (reducer.applySequenceRule(funct)) {}

    assertThat(funct.getNumOfActiveNodes()).isEqualTo(3);
    assertThat(funct.getNumEnteringEdges(n4)).isEqualTo(2);
  }

  @Test
  public void testApplySequenceRule_RepeatUntilLoop2() {
    ReducedNode entryNode = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode exitNode = new ReducedNode(newDummyCFANode("test"), false);
    ReducedFunction funct = new ReducedFunction(entryNode, exitNode);

    ReducedNode n5 = new ReducedNode(newDummyCFANode("loophead"), false);
    n5.getWrapped().setLoopStart();
    ReducedNode n8 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n9 = new ReducedNode(newDummyCFANode("test"), false);

    funct.addEdge(entryNode, n5);
    funct.addEdge(n5, n8);
    funct.addEdge(n8, n5);
    funct.addEdge(n8, n9);
    funct.addEdge(n9, exitNode);

    do {
      //       Map<ReducedNode, Map<ReducedNode, ReducedEdge>> inlinedCfa = funct.getInlinedCfa();
      //       assertCfaIsEmpty(inlinedCfa);
    } while (reducer.applySequenceRule(funct));

    assertThat(funct.getNumOfActiveNodes()).isEqualTo(3);
    assertThat(funct.getNumEnteringEdges(n5)).isEqualTo(2);
  }

  @Test
  public void testApplySequenceRule_RepeatUntilLoop3() {
    ReducedNode entryNode = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode exitNode = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n4 = new ReducedNode(newDummyCFANode("loophead"), false);
    ReducedNode n7 = new ReducedNode(newDummyCFANode("test"), false);
    n4.getWrapped().setLoopStart();

    ReducedFunction funct = new ReducedFunction(entryNode, exitNode);

    funct.addEdge(entryNode, n4);
    funct.addEdge(n4, exitNode);
    funct.addEdge(n4, n7);
    funct.addEdge(n7, n4);

    do {
      //       Map<ReducedNode, Map<ReducedNode, ReducedEdge>> inlinedCfa = funct.getInlinedCfa();
      //       assertCfaIsEmpty(inlinedCfa);
    } while (reducer.applySequenceRule(funct));

    assertThat(funct.getNumOfActiveNodes()).isEqualTo(3);
    assertThat(funct.getNumEnteringEdges(n4)).isEqualTo(2);
  }

  @Test
  public void testApplySequenceRule_IfBranch() {
    ReducedNode entryNode = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode exitNode = new ReducedNode(newDummyCFANode("test"), false);

    ReducedNode n2 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n3 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n4 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n5 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n6 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n7 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n8 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n9 = new ReducedNode(newDummyCFANode("test"), false);

    ReducedFunction funct = new ReducedFunction(entryNode, exitNode);

    funct.addEdge(entryNode, n2);
    funct.addEdge(n2, n3);
    funct.addEdge(n3, n4);
    funct.addEdge(n3, n8);
    funct.addEdge(n8, n9);
    funct.addEdge(n9, n7);
    funct.addEdge(n4, n5);
    funct.addEdge(n5, n6);
    funct.addEdge(n6, n7);
    funct.addEdge(n7, exitNode);

    do {
      Map<ReducedNode, Map<ReducedNode, Set<ReducedEdge>>> inlinedCfa = funct.getInlinedCfa();
      assertCfaIsEmpty(inlinedCfa);
    } while (reducer.applySequenceRule(funct));

    assertThat(funct.getNumOfActiveNodes()).isEqualTo(3);
  }

  @Test
  public void testApplyReductionSequences_IfBranch() {
    ReducedNode entryNode = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode exitNode = new ReducedNode(newDummyCFANode("test"), false);

    ReducedNode n2 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n3 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n4 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n1000 = new ReducedNode(newDummyCFANode("test"), false);

    ReducedFunction funct = new ReducedFunction(entryNode, exitNode);

    funct.addEdge(entryNode, n2);
    funct.addEdge(n2, n1000);
    funct.addEdge(n2, n3);
    funct.addEdge(n3, n4);
    funct.addEdge(n2, n4);
    funct.addEdge(n4, exitNode);

    Map<ReducedNode, Map<ReducedNode, Set<ReducedEdge>>> inlinedCfa = funct.getInlinedCfa();
    assertCfaIsEmpty(inlinedCfa);

    reducer.applyReductionSequences(funct);

    inlinedCfa = funct.getInlinedCfa();
    assertCfaIsEmpty(inlinedCfa);

    assertThat(funct.getNumOfActiveNodes()).isEqualTo(3);
  }

  @Test
  public void testApplyChoiceRule_IfBranch_NoChange() {
    ReducedNode entryNode = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode exitNode = new ReducedNode(newDummyCFANode("test"), false);

    ReducedNode n2 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n3 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n4 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n5 = new ReducedNode(newDummyCFANode("test"), false);

    ReducedFunction funct = new ReducedFunction(entryNode, exitNode);

    funct.addEdge(entryNode, n2);
    funct.addEdge(n2, n3);
    funct.addEdge(n2, n4);
    funct.addEdge(n3, n5);
    funct.addEdge(n4, n5);
    funct.addEdge(n5, exitNode);

    while (reducer.applyChoiceRule(funct)) {}

    assertThat(funct.getNumOfActiveNodes()).isEqualTo(6);
  }

  @Test
  public void testApplyChoiceRule_IfBranch_Change() {
    ReducedNode entryNode = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode exitNode = new ReducedNode(newDummyCFANode("test"), false);

    ReducedNode n2 = new ReducedNode(newDummyCFANode("test"), false);
    ReducedNode n3 = new ReducedNode(newDummyCFANode("test"), false);

    ReducedFunction funct = new ReducedFunction(entryNode, exitNode);

    funct.addEdge(entryNode, n2);
    funct.addEdge(n2, n3);
    funct.addEdge(n2, n3);
    funct.addEdge(n3, exitNode);

    while (reducer.applyChoiceRule(funct)) {}

    assertThat(funct.getNumOfActiveNodes()).isEqualTo(4);
  }
}
