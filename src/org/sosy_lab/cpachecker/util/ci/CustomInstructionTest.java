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
package org.sosy_lab.cpachecker.util.ci;

import com.google.common.truth.Truth;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class CustomInstructionTest {
  private CustomInstructionApplications cia;
  private AppliedCustomInstruction aci;
  private Map<CFANode, AppliedCustomInstruction> cis;
  private CustomInstruction ci;
  private Constructor<?extends AbstractState> locConstructor;
  private CFA cfa;
  private CFANode startNode;
  private Collection<CFANode> endNodes;
  private ARGState start;
  private ARGState end;

  @Before
  public void init() throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException,
      IllegalArgumentException, InvocationTargetException, IOException,
      ParserException, InterruptedException {
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

    List<String> input = new ArrayList<>();
    input.add("a");
    List<String> output = new ArrayList<>();
    output.add("b");
    ci = new CustomInstruction(startNode, endNodes, input, output, ShutdownNotifier.createDummy());

    cis = new HashMap<>();
    aci = new AppliedCustomInstruction(startNode, endNodes,
        Collections.<String>emptyList(), Collections.<String>emptyList(), Collections.<String>emptyList(),
        Pair.of(Collections.<String> emptyList(), ""),
        SSAMap.emptySSAMap());

    cis.put(startNode, aci);

    cia = new CustomInstructionApplications(cis, ci);
    start = new ARGState(locConstructor.newInstance(startNode, true), null);
    end = new ARGState(locConstructor.newInstance(endNodes.iterator().next(), true), null);
  }

  @Test
  public void testIsStartState() throws CPAException, InstantiationException, IllegalAccessException,
      IllegalArgumentException, InvocationTargetException {
    ARGState notStart =
        new ARGState(locConstructor.newInstance(startNode.getLeavingEdge(0).getSuccessor(), true), start);
    ARGState noLocation = new ARGState(new CallstackState(null, "main", startNode), null);

    // test applied custom instruction
    Truth.assertThat(aci.isStartState(start)).isTrue();
    Truth.assertThat(aci.isStartState(notStart)).isFalse();
    try {
      aci.isStartState(noLocation);
    } catch (CPAException e) {
      Truth.assertThat(e).isInstanceOf(CPAException.class);
    }

    // test custom instruction application
    Truth.assertThat(cia.isStartState(start)).isTrue();
    Truth.assertThat(cia.isStartState(notStart)).isFalse();
    try {
      cia.isStartState(noLocation);
    } catch (CPAException e) {
      Truth.assertThat(e).isInstanceOf(CPAException.class);
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
    } catch (CPAException e) {
      Truth.assertThat(e).isInstanceOf(CPAException.class);
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
    } catch (CPAException e) {
      Truth.assertThat(e).isInstanceOf(CPAException.class);
      Truth.assertThat(e).hasMessage("The state does not represent start of known custom instruction");
    }
    // test if input parameter does not contain location state
    try {
      cia.getAppliedCustomInstructionFor(new ARGState(new CallstackState(null, "main", startNode), null));
    } catch (CPAException e) {
      Truth.assertThat(e).isInstanceOf(CPAException.class);
    }
  }

  @Test
  public void testGetSignature() {
    ci =
        new CustomInstruction(
            null,
            null,
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            ShutdownNotifier.createDummy());
    Truth.assertThat(ci.getSignature()).isEqualTo("() -> ()");

    List<String> inputVars = new ArrayList<>();
    inputVars.add("var");
    List<String> outputVars = new ArrayList<>();
    outputVars.add("var0");
    ci = new CustomInstruction(null, null, inputVars, outputVars, ShutdownNotifier.createDummy());
    Truth.assertThat(ci.getSignature()).isEqualTo("(var) -> (var0@1)");

    inputVars = new ArrayList<>();
    inputVars.add("f::var1");
    inputVars.add("var2");
    outputVars = new ArrayList<>();
    outputVars.add("var3");
    outputVars.add("f::var4");
    outputVars.add("var5");
    ci = new CustomInstruction(null, null, inputVars, outputVars, ShutdownNotifier.createDummy());
    Truth.assertThat(ci.getSignature()).isEqualTo("(|f::var1|, var2) -> (var3@1, |f::var4@1|, var5@1)");
  }

  @Test
  public void testGetFakeSMTDescription() {
    ci =
        new CustomInstruction(
            null,
            null,
            Collections.<String>emptyList(),
            Collections.<String>emptyList(),
            ShutdownNotifier.createDummy());
    Pair<List<String>, String> pair = ci.getFakeSMTDescription();
    Truth.assertThat(pair.getFirst()).isEmpty();
    Truth.assertThat(pair.getSecond()).isEqualTo("(define-fun ci() Bool true)");

    List<String> inputVars = new ArrayList<>();
    inputVars.add("var");
    ci =
        new CustomInstruction(
            null, null, inputVars, Collections.<String>emptyList(), ShutdownNotifier.createDummy());
    pair = ci.getFakeSMTDescription();
    Truth.assertThat(pair.getFirst()).hasSize(1);
    Truth.assertThat(pair.getFirst().get(0)).isEqualTo("(declare-fun var () Int)");
    Truth.assertThat(pair.getSecond()).isEqualTo("(define-fun ci() Bool(= var 0))");

    List<String> outputVars = new ArrayList<>();
    outputVars.add("var1");
    ci =
        new CustomInstruction(
            null,
            null,
            Collections.<String>emptyList(),
            outputVars,
            ShutdownNotifier.createDummy());
    pair = ci.getFakeSMTDescription();
    Truth.assertThat(pair.getFirst()).hasSize(1);
    Truth.assertThat(pair.getFirst().get(0)).isEqualTo("(declare-fun var1@1 () Int)");
    Truth.assertThat(pair.getSecond()).isEqualTo("(define-fun ci() Bool (= var1@1 0))");

    inputVars = new ArrayList<>();
    inputVars.add("var1");
    outputVars = new ArrayList<>();
    outputVars.add("var2");
    ci = new CustomInstruction(null, null, inputVars, outputVars, ShutdownNotifier.createDummy());
    pair = ci.getFakeSMTDescription();
    Truth.assertThat(pair.getFirst()).hasSize(2);
    Truth.assertThat(pair.getFirst().get(0)).isEqualTo("(declare-fun var1 () Int)");
    Truth.assertThat(pair.getFirst().get(1)).isEqualTo("(declare-fun var2@1 () Int)");
    Truth.assertThat(pair.getSecond()).isEqualTo("(define-fun ci() Bool(and (= var1 0) (= var2@1 0)))");

    inputVars = new ArrayList<>();
    inputVars.add("var");
    inputVars.add("f::var1");
    inputVars.add("var2");
    outputVars = new ArrayList<>();
    outputVars.add("var3");
    outputVars.add("f::var4");
    ci = new CustomInstruction(null, null, inputVars, outputVars, ShutdownNotifier.createDummy());
    pair = ci.getFakeSMTDescription();
    Truth.assertThat(pair.getFirst()).hasSize(5);
    Truth.assertThat(pair.getFirst().get(0)).isEqualTo("(declare-fun var () Int)");
    Truth.assertThat(pair.getFirst().get(1)).isEqualTo("(declare-fun |f::var1| () Int)");
    Truth.assertThat(pair.getFirst().get(2)).isEqualTo("(declare-fun var2 () Int)");
    Truth.assertThat(pair.getFirst().get(3)).isEqualTo("(declare-fun var3@1 () Int)");
    Truth.assertThat(pair.getFirst().get(4)).isEqualTo("(declare-fun |f::var4@1| () Int)");
    Truth.assertThat(pair.getSecond()).isEqualTo("(define-fun ci() Bool(and (= var 0)(and (= |f::var1| 0)(and (= var2 0)(and (= var3@1 0) (= |f::var4@1| 0))))))");
  }

  @Test
  public void testInspectAppliedCustomInstruction() throws AppliedCustomInstructionParsingFailedException,
    InterruptedException, IOException, ParserException, SecurityException, IllegalArgumentException {
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
      if (node instanceof CLabelNode) {
        if (((CLabelNode) node).getLabel().startsWith("start_ci")) {
          startNode = node;
        }
        if (((CLabelNode) node).getLabel().startsWith("end_ci")) {
          for(CFANode predecessor: CFAUtils.allPredecessorsOf(node)) {
            endNodes.add(predecessor);
          }
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

    List<String> input = new ArrayList<>();
    input.add("main::y");
    input.add("main::z");
    List<String> output = new ArrayList<>();
    output.add("main::x");
    output.add("main::z");
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
    Truth.assertThat(pair.getSecond()).isEqualTo("(define-fun ci() Bool(and (= 7 0)(and (= |main::b| 0)(and (= |main::a@1| 0) (= |main::b@1| 0)))))");

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
  public void testGetInputVariables() throws SecurityException, IllegalArgumentException {
    Truth.assertThat(aci.getInputVariables()).isEmpty();

    List<String> inputVariables = new ArrayList<>(1);
    inputVariables.add("main::a");
    aci = new AppliedCustomInstruction(startNode, endNodes, inputVariables, Collections.<String>emptyList(), inputVariables,
        Pair.of(Collections.<String> emptyList(), ""), SSAMap.emptySSAMap());
    Truth.assertThat(aci.getInputVariables()).containsExactly("main::a");
  }

  @Test
  public void testGetOutputVariables() {
    Truth.assertThat(aci.getOutputVariables()).isEmpty();

    List<String> outputVariables = new ArrayList<>(1);
    outputVariables.add("main::a");
    aci = new AppliedCustomInstruction(startNode, endNodes, Collections.<String>emptyList(),  outputVariables, Collections.<String>emptyList(),
        Pair.of(Collections.<String> emptyList(), ""), SSAMap.emptySSAMap());
    Truth.assertThat(aci.getOutputVariables()).containsExactly("main::a");
  }

  @Test
  public void testGetInputVariablesAndConstants() {
    Truth.assertThat(aci.getOutputVariables()).isEmpty();

    List<String> inputVarsAndConstants = new ArrayList<>(2);
    inputVarsAndConstants.add("main::a");
    inputVarsAndConstants.add("1");
    aci = new AppliedCustomInstruction(startNode, endNodes,  Collections.singletonList("main::a"), Collections.<String>emptyList(),
        inputVarsAndConstants, Pair.of(Collections.<String> emptyList(), ""), SSAMap.emptySSAMap());
    Truth.assertThat(aci.getInputVariablesAndConstants()).containsExactlyElementsIn(inputVarsAndConstants).inOrder();
  }
}
