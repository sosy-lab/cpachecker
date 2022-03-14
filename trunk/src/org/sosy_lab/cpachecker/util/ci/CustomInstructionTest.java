// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ci;

import static com.google.common.truth.Truth.assert_;

import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth;
import java.lang.reflect.Constructor;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.annotations.SuppressForbidden;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class CustomInstructionTest {
  private CustomInstructionApplications cia;
  private AppliedCustomInstruction aci;
  private Map<CFANode, AppliedCustomInstruction> cis;
  private CustomInstruction ci;
  private Constructor<? extends AbstractState> locConstructor;
  private CFA cfa;
  private CFANode startNode;
  private Set<CFANode> endNodes;
  private ARGState start;
  private ARGState end;

  @Before
  @SuppressForbidden("reflection only in test")
  public void init() throws Exception {
    cfa =
        TestDataTools.makeCFA(
            "void main(int a) {",
            "  int a;",
            "  if (a>0) {",
            "    a=a+1;",
            "  } else {",
            "    a=a-1;",
            "  }",
            "}");

    locConstructor = LocationState.class.getDeclaredConstructor(CFANode.class, boolean.class);
    locConstructor.setAccessible(true);

    startNode = cfa.getMainFunction();
    endNodes = new HashSet<>();
    for (CFAEdge edge : CFAUtils.allEnteringEdges(cfa.getMainFunction().getExitNode())) {
      endNodes.add(edge.getPredecessor());
    }

    ImmutableList<String> input = ImmutableList.of("a");
    ImmutableList<String> output = ImmutableList.of("b");
    ci = new CustomInstruction(startNode, endNodes, input, output, ShutdownNotifier.createDummy());

    cis = new HashMap<>();
    aci =
        new AppliedCustomInstruction(
            startNode,
            endNodes,
            ImmutableList.of(),
            ImmutableList.of(),
            ImmutableList.of(),
            Pair.of(ImmutableList.of(), ""),
            SSAMap.emptySSAMap());

    cis.put(startNode, aci);

    cia = new CustomInstructionApplications(cis, ci);
    start = new ARGState(locConstructor.newInstance(startNode, true), null);
    end = new ARGState(locConstructor.newInstance(endNodes.iterator().next(), true), null);
  }

  @Test
  public void testIsStartState() throws Exception {
    ARGState notStart =
        new ARGState(
            locConstructor.newInstance(startNode.getLeavingEdge(0).getSuccessor(), true), start);
    ARGState noLocation = new ARGState(new CallstackState(null, "main", startNode), null);

    // test applied custom instruction
    Truth.assertThat(aci.isStartState(start)).isTrue();
    Truth.assertThat(aci.isStartState(notStart)).isFalse();
    try {
      aci.isStartState(noLocation);
      assert_().fail();
    } catch (CPAException e) {
    }

    // test custom instruction application
    Truth.assertThat(cia.isStartState(start)).isTrue();
    Truth.assertThat(cia.isStartState(notStart)).isFalse();
    try {
      cia.isStartState(noLocation);
      assert_().fail();
    } catch (CPAException e) {
    }
  }

  @Test
  public void testIsEndState() throws CPAException, IllegalArgumentException {
    ARGState noLocation = new ARGState(new CallstackState(null, "main", startNode), null);

    // test applied custom instruction
    Truth.assertThat(aci.isEndState(end)).isTrue();
    Truth.assertThat(aci.isEndState(start)).isFalse();
    try {
      aci.isEndState(noLocation);
      assert_().fail();
    } catch (CPAException e) {
    }

    // test custom instruction application
    Truth.assertThat(cia.isEndState(end, startNode)).isTrue();
    Truth.assertThat(cia.isEndState(start, startNode)).isFalse();

    Truth.assertThat(cia.isEndState(end, start)).isTrue();
    Truth.assertThat(cia.isEndState(start, start)).isFalse();
  }

  @Test
  public void testGetAppliedCustomInstruction() throws IllegalArgumentException, CPAException {
    Truth.assertThat(cia.getAppliedCustomInstructionFor(start)).isEqualTo(cis.get(startNode));
    // test if input parameter not a start state
    try {
      cia.getAppliedCustomInstructionFor(end);
      assert_().fail();
    } catch (CPAException e) {
      Truth.assertThat(e)
          .hasMessageThat()
          .isEqualTo("The state does not represent start of known custom instruction");
    }
    // test if input parameter does not contain location state
    try {
      cia.getAppliedCustomInstructionFor(
          new ARGState(new CallstackState(null, "main", startNode), null));
      assert_().fail();
    } catch (CPAException e) {
    }
  }

  @Test
  public void testGetSignature() {
    ci =
        new CustomInstruction(
            null, null, ImmutableList.of(), ImmutableList.of(), ShutdownNotifier.createDummy());
    Truth.assertThat(ci.getSignature()).isEqualTo("() -> ()");

    ImmutableList<String> inputVars = ImmutableList.of("var");
    ImmutableList<String> outputVars = ImmutableList.of("var0");
    ci = new CustomInstruction(null, null, inputVars, outputVars, ShutdownNotifier.createDummy());
    Truth.assertThat(ci.getSignature()).isEqualTo("(var) -> (var0@1)");

    inputVars = ImmutableList.of("f::var1", "var2");
    outputVars = ImmutableList.of("var3", "f::var4", "var5");
    ci = new CustomInstruction(null, null, inputVars, outputVars, ShutdownNotifier.createDummy());
    Truth.assertThat(ci.getSignature())
        .isEqualTo("(|f::var1|, var2) -> (var3@1, |f::var4@1|, var5@1)");
  }

  @Test
  public void testGetFakeSMTDescription() {
    ci =
        new CustomInstruction(
            null, null, ImmutableList.of(), ImmutableList.of(), ShutdownNotifier.createDummy());
    Pair<List<String>, String> pair = ci.getFakeSMTDescription();
    Truth.assertThat(pair.getFirst()).isEmpty();
    Truth.assertThat(pair.getSecond()).isEqualTo("(define-fun ci() Bool true)");

    ImmutableList<String> inputVars = ImmutableList.of("var");
    ci =
        new CustomInstruction(
            null, null, inputVars, ImmutableList.of(), ShutdownNotifier.createDummy());
    pair = ci.getFakeSMTDescription();
    Truth.assertThat(pair.getFirst()).hasSize(1);
    Truth.assertThat(pair.getFirst().get(0)).isEqualTo("(declare-fun var () Int)");
    Truth.assertThat(pair.getSecond()).isEqualTo("(define-fun ci() Bool(= var 0))");

    ImmutableList<String> outputVars = ImmutableList.of("var1");
    ci =
        new CustomInstruction(
            null, null, ImmutableList.of(), outputVars, ShutdownNotifier.createDummy());
    pair = ci.getFakeSMTDescription();
    Truth.assertThat(pair.getFirst()).hasSize(1);
    Truth.assertThat(pair.getFirst().get(0)).isEqualTo("(declare-fun var1@1 () Int)");
    Truth.assertThat(pair.getSecond()).isEqualTo("(define-fun ci() Bool (= var1@1 0))");

    inputVars = ImmutableList.of("var1");
    outputVars = ImmutableList.of("var2");
    ci = new CustomInstruction(null, null, inputVars, outputVars, ShutdownNotifier.createDummy());
    pair = ci.getFakeSMTDescription();
    Truth.assertThat(pair.getFirst()).hasSize(2);
    Truth.assertThat(pair.getFirst().get(0)).isEqualTo("(declare-fun var1 () Int)");
    Truth.assertThat(pair.getFirst().get(1)).isEqualTo("(declare-fun var2@1 () Int)");
    Truth.assertThat(pair.getSecond())
        .isEqualTo("(define-fun ci() Bool(and (= var1 0) (= var2@1 0)))");

    inputVars = ImmutableList.of("var", "f::var1", "var2");
    outputVars = ImmutableList.of("var3", "f::var4");
    ci = new CustomInstruction(null, null, inputVars, outputVars, ShutdownNotifier.createDummy());
    pair = ci.getFakeSMTDescription();
    Truth.assertThat(pair.getFirst()).hasSize(5);
    Truth.assertThat(pair.getFirst().get(0)).isEqualTo("(declare-fun var () Int)");
    Truth.assertThat(pair.getFirst().get(1)).isEqualTo("(declare-fun |f::var1| () Int)");
    Truth.assertThat(pair.getFirst().get(2)).isEqualTo("(declare-fun var2 () Int)");
    Truth.assertThat(pair.getFirst().get(3)).isEqualTo("(declare-fun var3@1 () Int)");
    Truth.assertThat(pair.getFirst().get(4)).isEqualTo("(declare-fun |f::var4@1| () Int)");
    Truth.assertThat(pair.getSecond())
        .isEqualTo(
            "(define-fun ci() Bool(and (= var 0)(and (= |f::var1| 0)(and (= var2 0)(and (= var3@1"
                + " 0) (= |f::var4@1| 0))))))");
  }

  @Test
  public void testInspectAppliedCustomInstruction() throws Exception {
    cfa =
        TestDataTools.makeCFA(
            "extern int f2(int);",
            "int f(int x) {",
            "  return x * x;",
            "}",
            "void main() {",
            "  int z;",
            "  int y;",
            "  start_ci: int x = 5 * z;",
            "  if (!(x>y)) {",
            "    if (z>0) {",
            "      x + y;",
            "      z = x + y;",
            "      z = f(x);",
            "      x = f2(y);",
            "    }",
            "  }",
            "  end_ci_1: int b;",
            "  int a = 5 * b;",
            "  if (!(a>7)) {",
            "    if (b>0) {",
            "      a + 7;",
            "      b = a + 7;",
            "      b = f(a);",
            "      a = f2(7);",
            "    }",
            "  }",
            "  x = x + 1;",
            "}");

    CFANode aciStartNode = null, aciEndNode = null;

    Set<CFANode> visitedNodes = new HashSet<>();
    Queue<CFANode> queue = new ArrayDeque<>();
    queue.add(cfa.getFunctionHead("main"));
    CFANode node;
    endNodes = new HashSet<>();
    startNode = null;

    while (!queue.isEmpty()) {
      node = queue.poll();
      if (node instanceof CFALabelNode) {
        if (((CFALabelNode) node).getLabel().startsWith("start_ci")) {
          startNode = node;
        }
        if (((CFALabelNode) node).getLabel().startsWith("end_ci")) {
          CFAUtils.allPredecessorsOf(node).copyInto(endNodes);
        }
      }
      for (CFAEdge e : CFAUtils.allLeavingEdges(node)) {
        if (!visitedNodes.contains(e.getSuccessor())) {
          queue.add(e.getSuccessor());
          visitedNodes.add(e.getSuccessor());
        }
        if (e.getCode().equals("int a = 5 * b;")) {
          aciStartNode = e.getPredecessor();
        }
        if (e.getCode().equals("x = x + 1;")) {
          aciEndNode = e.getPredecessor();
        }
      }
    }
    Truth.assertThat(aciStartNode).isNotNull();
    Truth.assertThat(aciEndNode).isNotNull();
    Truth.assertThat(startNode).isNotNull();
    Truth.assertThat(endNodes).hasSize(1);

    ImmutableList<String> input = ImmutableList.of("main::y", "main::z");
    ImmutableList<String> output = ImmutableList.of("main::x", "main::z");
    ci = new CustomInstruction(startNode, endNodes, input, output, ShutdownNotifier.createDummy());

    aci = ci.inspectAppliedCustomInstruction(aciStartNode);

    Collection<String> inputVars = new ArrayList<>();
    inputVars.add("main::b");
    Truth.assertThat(aci.getInputVariables()).containsExactlyElementsIn(inputVars);
    Collection<String> outputVars = new ArrayList<>();
    outputVars.add("main::a");
    outputVars.add("main::b");
    Truth.assertThat(aci.getOutputVariables()).containsExactlyElementsIn(outputVars);

    Pair<List<String>, String> pair = aci.getFakeSMTDescription();
    Truth.assertThat(pair.getFirst()).hasSize(3);
    Truth.assertThat(pair.getFirst().get(0)).isEqualTo("(declare-fun |main::b| () Int)");
    Truth.assertThat(pair.getFirst().get(1)).isEqualTo("(declare-fun |main::a@1| () Int)");
    Truth.assertThat(pair.getFirst().get(2)).isEqualTo("(declare-fun |main::b@1| () Int)");
    Truth.assertThat(pair.getSecond())
        .isEqualTo(
            "(define-fun ci() Bool(and (= 7 0)(and (= |main::b| 0)(and (= |main::a@1| 0) (="
                + " |main::b@1| 0)))))");

    SSAMap ssaMap = aci.getIndicesForReturnVars();
    List<String> variables = new ArrayList<>();
    variables.add("main::a");
    variables.add("main::b");
    Truth.assertThat(ssaMap.allVariables()).containsExactlyElementsIn(variables);
    Truth.assertThat(ssaMap.getIndex(variables.get(0))).isEqualTo(1);
    Truth.assertThat(ssaMap.getIndex(variables.get(1))).isEqualTo(1);

    Collection<CFANode> aciNodes = new ArrayList<>(2);
    aciNodes.add(aciStartNode);
    aciNodes.add(aciEndNode);
    Truth.assertThat(aci.getStartAndEndNodes()).containsExactlyElementsIn(aciNodes);
  }

  @Test
  public void testGetInputVariables() {
    Truth.assertThat(aci.getInputVariables()).isEmpty();

    List<String> inputVariables = new ArrayList<>(1);
    inputVariables.add("main::a");
    aci =
        new AppliedCustomInstruction(
            startNode,
            endNodes,
            inputVariables,
            ImmutableList.of(),
            inputVariables,
            Pair.of(ImmutableList.of(), ""),
            SSAMap.emptySSAMap());
    Truth.assertThat(aci.getInputVariables()).containsExactly("main::a");
  }

  @Test
  public void testGetOutputVariables() {
    Truth.assertThat(aci.getOutputVariables()).isEmpty();

    List<String> outputVariables = new ArrayList<>(1);
    outputVariables.add("main::a");
    aci =
        new AppliedCustomInstruction(
            startNode,
            endNodes,
            ImmutableList.of(),
            outputVariables,
            ImmutableList.of(),
            Pair.of(ImmutableList.of(), ""),
            SSAMap.emptySSAMap());
    Truth.assertThat(aci.getOutputVariables()).containsExactly("main::a");
  }

  @Test
  public void testGetInputVariablesAndConstants() {
    Truth.assertThat(aci.getOutputVariables()).isEmpty();

    List<String> inputVarsAndConstants = new ArrayList<>(2);
    inputVarsAndConstants.add("main::a");
    inputVarsAndConstants.add("1");
    aci =
        new AppliedCustomInstruction(
            startNode,
            endNodes,
            ImmutableList.of("main::a"),
            ImmutableList.of(),
            inputVarsAndConstants,
            Pair.of(ImmutableList.of(), ""),
            SSAMap.emptySSAMap());
    Truth.assertThat(aci.getInputVariablesAndConstants())
        .containsExactlyElementsIn(inputVarsAndConstants)
        .inOrder();
  }
}
