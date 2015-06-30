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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

import com.google.common.truth.Truth;

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
      IllegalArgumentException, InvocationTargetException, AppliedCustomInstructionParsingFailedException, IOException,
      ParserException, InterruptedException {
    String testProgram = ""
        + "void main(int a){"
        + "int a;"
        + "if(a>0) {a=a+1;} else {a=a-1;}"
        + "}";
    cfa = TestDataTools.makeCFA(testProgram);

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
    ci = new CustomInstruction(startNode, endNodes, input, output, ShutdownNotifier.create());

    cis = new HashMap<>();
    aci = new AppliedCustomInstruction(startNode, endNodes,
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
  public void testIsEndState() throws CPAException, InstantiationException, IllegalAccessException,
      IllegalArgumentException, InvocationTargetException {
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
  public void testGetAppliedCustomInstruction() throws InstantiationException, IllegalAccessException,
      IllegalArgumentException, InvocationTargetException, CPAException {
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
}
